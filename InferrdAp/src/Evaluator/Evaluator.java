/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Evaluator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author procheta
 */
class PerQueryRelDocs {

    String qid;
    HashMap<String, Integer> relMap; // keyed by docid, entry stores the rel value
    HashMap<String, Integer> irrelMap; // keyed by docid, entry stores the irrel value
    ArrayList<Double> relCentroid;
    ArrayList<Double> irrelCentroid;
    HashMap<String, ArrayList<Double>> vectorMap; // keyed by docid, vector
    ArrayList<String> pool; // list of all the  judged docs
    int numRel;
    HashMap<String, Double> perQuerydocCosineSim;
    IndexReader reader;
    IndexSearcher searcher;

    public PerQueryRelDocs(String qid, IndexReader reader, String cosineFilename) throws IOException, ParseException {
        this.qid = qid;
        numRel = 0;
        relMap = new HashMap<>();
        irrelMap = new HashMap<>();
        pool = new ArrayList<>();
        this.reader = reader;
        searcher = new IndexSearcher(reader);
        perQuerydocCosineSim = new HashMap<>();
        relCentroid = new ArrayList<Double>(300);
        irrelCentroid = new ArrayList<Double>(300);
        vectorMap = new HashMap<>();
        HashMap<String, ArrayList<Double>> h = null;
        precomputeCosineSim(h);
    }

    public PerQueryRelDocs(String qid, IndexReader reader) throws IOException, ParseException {
        this.qid = qid;
        numRel = 0;
        relMap = new HashMap<>();
        irrelMap = new HashMap<>();
        pool = new ArrayList<>();
        this.reader = reader;
        searcher = new IndexSearcher(reader);
        relCentroid = new ArrayList<Double>(30);
        irrelCentroid = new ArrayList<Double>(300);
        perQuerydocCosineSim = new HashMap<>();
        vectorMap = new HashMap<>();
    }

    void addTuple(String docId, int rel, ArrayList<Double> ar) {
        if (relMap.get(docId) != null) {
            return;
        }
        if (rel > 0) {
            numRel++;
            relMap.put(docId, rel);
            for (int i = 0; i < ar.size(); i++) {
                try {
                    relCentroid.set(i, relCentroid.get(i) + ar.get(i));
                } catch (Exception e) {
                    relCentroid.add(ar.get(i));
                }

            }
            pool.add(docId);
        }
        if (rel == 0) {
            irrelMap.put(docId, rel);
            for (int i = 0; i < ar.size(); i++) {
                try {
                    irrelCentroid.set(i, irrelCentroid.get(i) + ar.get(i));
                } catch (Exception e) {
                    irrelCentroid.add(ar.get(i));
                }

            }
            pool.add(docId);
        }
        vectorMap.put(docId, ar);

    }

    public void precomputeCosineSim(HashMap<String, ArrayList<Double>> h) throws IOException, ParseException {
        Iterator it = irrelMap.keySet().iterator();

        while (it.hasNext()) {
            String irrelDocId = (String) it.next();
            DocVector firstDoc = new DocVector(irrelDocId, reader, searcher);
            firstDoc.vector = h.get(irrelDocId);
            Iterator it2 = relMap.keySet().iterator();
            while (it2.hasNext()) {

                String secondDocName = (String) it2.next();
                // DocVector secondDoc = new DocVector(secondDocName, reader, searcher);
                DocVector secondDoc = new DocVector(secondDocName, reader, searcher);
                secondDoc.vector = h.get(secondDocName);
                //  perQuerydocCosineSim.put(irrelDocId + "#" + secondDocName, firstDoc.cosineSim(secondDoc, reader));
                perQuerydocCosineSim.put(irrelDocId + "#" + secondDocName, firstDoc.computeCosineSim(secondDoc));
                // System.out.println(firstDoc.computeCosineSim(secondDoc));
            }

        }
        it = relMap.keySet().iterator();
        while (it.hasNext()) {
            String relDocId = (String) it.next();
            DocVector firstDoc = new DocVector(relDocId, reader, searcher);
            Iterator it2 = relMap.keySet().iterator();
            while (it2.hasNext()) {
                String secondDocName = (String) it2.next();
                DocVector secondDoc = new DocVector(secondDocName, reader, searcher);
                perQuerydocCosineSim.put(relDocId + "#" + secondDocName, firstDoc.cosineSim(secondDoc, reader));
            }
        }

    }
}

class AllRelRcds {

    String qrelsFile;
    HashMap<String, PerQueryRelDocs> perQueryRels;
    int totalNumRel;
    IndexReader reader;
    String cosineFile;
    String cosineMode;

    public AllRelRcds(String qrelsFile, IndexReader reader, String cosineFile, String mode) {
        this.qrelsFile = qrelsFile;
        perQueryRels = new HashMap<>();
        totalNumRel = 0;
        this.reader = reader;
        this.cosineFile = cosineFile;
        this.cosineMode = mode;
    }

    
    void load(int startQid, int endQid, String relFileName, String irrelFileName, String vectorFolderLocation) throws Exception {
        FileReader fr = new FileReader(qrelsFile);
        BufferedReader br = new BufferedReader(fr);

        FileReader fr1 = new FileReader(vectorFolderLocation + startQid + ".txt");
        BufferedReader br1 = new BufferedReader(fr1);
        String line1;
        String line;
        Integer d1 = startQid;
        line1 = br1.readLine();
        while ((line = br.readLine()) != null) {
            if (line1 != null) {
                storeRelRcd(line, line1);
                line1 = br1.readLine();
            } else {
                d1++;
                br1.close();
                fr1.close();
                // System.out.println(vectorFolderLocation + d1 + ".txt");
                fr1 = new FileReader(vectorFolderLocation + d1 + ".txt");
                br1 = new BufferedReader(fr1);
                line1 = br1.readLine();
                storeRelRcd(line, line1);
            }
        }
        Integer d = startQid;
        for (d = startQid; d <= endQid; d++) {
            PerQueryRelDocs reldoc = perQueryRels.get(d.toString());
            // System.out.println(reldoc);
            for (int i = 0; i < reldoc.relCentroid.size(); i++) {
                reldoc.relCentroid.set(i, reldoc.relCentroid.get(i) / reldoc.relMap.size());
            }
        }
        br.close();
       
    }

    void storeRelRcd(String line, String line1) throws IOException, ParseException {
        String[] tokens = line.split("\\s+");
        VectorExtractor ve = new VectorExtractor();
        ArrayList vector = ve.getVector(line1);
        String qid = tokens[0];
        PerQueryRelDocs relTuple = perQueryRels.get(qid);
        if (relTuple == null) {
            relTuple = new PerQueryRelDocs(qid, reader);
            perQueryRels.put(qid, relTuple);
        }
        relTuple.addTuple(tokens[2], Integer.parseInt(tokens[3]), vector);
    }

    public void storeCosineSimilarity(String fileName, int startQrelNo, int endQrelNo, String vectorFolderLocation) throws IOException, ParseException {
        FileWriter fw = new FileWriter(new File(fileName), true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.newLine();
        VectorExtractor ve = new VectorExtractor();
        for (int qid = startQrelNo; qid <= endQrelNo; qid++) {
            Integer qidValue = qid;
            PerQueryRelDocs perqd = perQueryRels.get(qidValue.toString());
            HashMap<String, ArrayList<Double>> h = ve.extractVector(vectorFolderLocation + qid + ".txt");
            perqd.precomputeCosineSim(h);
            bw.write("#" + qidValue.toString());
            bw.newLine();
            Iterator it = perqd.perQuerydocCosineSim.keySet().iterator();

            while (it.hasNext()) {
                String docPair = (String) it.next();
                String st[] = docPair.split("#");
                bw.write(st[0] + " " + st[1] + " " + perqd.perQuerydocCosineSim.get(docPair));
                bw.newLine();
            }
        }
        bw.close();
    }

    public void loadCosineValue(String fileName, int startQid, int endQid) throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(new File(fileName));
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        HashMap<Integer, HashMap<String, Double>> qidCosineMap = new HashMap<>();
        String qid = "";
        HashMap<String, Double> docPairCosineMAp = new HashMap();
        int flag = 0;
        int flag2 = 0;
        int flag3 = 0;
        Integer start = startQid;
        Integer end = endQid;
        while (line != null) {
            if (line.startsWith("#")) {
                if (flag != 0) {
                    perQueryRels.get(qid).perQuerydocCosineSim = docPairCosineMAp;
                    docPairCosineMAp = new HashMap();
                    qid = line.substring(1, line.length());
                } else {
                    qid = line.substring(1, line.length());
                    if (qid.equals(start.toString())) {
                        flag = 1;
                        flag3 = 1;
                    }
                    docPairCosineMAp = new HashMap();
                }
                if (qid.equals(end.toString())) {
                    flag2 = 1;
                    break;
                } else {
                }
            } else if (flag3 == 1) {
                try {
                    String st[] = line.split(" ");
                    String docPair = st[0] + st[1];
                    docPairCosineMAp.put(docPair, Double.parseDouble(st[2]));
                } catch (Exception e) {
                }
            }
            if (flag2 == 1) {
                break;
            }
            line = br.readLine();
        }
        br.close();
        perQueryRels.get(qid).perQuerydocCosineSim = docPairCosineMAp;
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (Map.Entry<String, PerQueryRelDocs> e : perQueryRels.entrySet()) {
            PerQueryRelDocs perQryRelDocs = e.getValue();
            buff.append(e.getKey()).append("\n");
            for (Map.Entry<String, Integer> rel : perQryRelDocs.relMap.entrySet()) {
                String docName = rel.getKey();
                int relVal = rel.getValue();
                buff.append(docName).append(",").append(relVal).append("\t");
            }
            buff.append("\n");
        }
        return buff.toString();
    }

}

class ResultTuple implements Comparable<ResultTuple> {

    String docName; // doc name
    int rank;       // rank of retrieved document
    int rel;    // is this relevant? comes from qrel-info
    double simvalue; // rsv value

    public ResultTuple(String docName, int rank, double simvalue) {
        this.docName = docName;
        this.rank = rank;
        this.simvalue = simvalue;
    }

    @Override
    public int compareTo(ResultTuple t) {
        return rank < t.rank ? -1 : rank == t.rank ? 0 : 1;
    }
}

class RetrievedResults implements Comparable<RetrievedResults> {

    int qid;
    List<ResultTuple> rtuples;
    int numRelRet;
    HashMap<String, Double> docIdSimValueMap;
    float avgP;
    ArrayList<String> pool;
    double maxSimValue;
    double minSimValue;

    public RetrievedResults(String qid) throws IOException {
        this.qid = Integer.parseInt(qid);
        this.rtuples = new ArrayList<>(1000);
        avgP = -1;
        numRelRet = -1;
        pool = new ArrayList<>();
        docIdSimValueMap = new HashMap<>();
        minSimValue = 99999;
        maxSimValue = -9999999;
    }

    void addTuple(String docName, int rank, double simvalue) {
        rtuples.add(new ResultTuple(docName, rank, simvalue));
        docIdSimValueMap.put(docName, simvalue);

        if (maxSimValue < simvalue) {
            maxSimValue = simvalue;
        }
        if (minSimValue > simvalue) {
            minSimValue = simvalue;
        }
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (ResultTuple rt : rtuples) {
            buff.append(qid).append("\t").
                    append(rt.docName).append("\t").
                    append(rt.rank).append("\t").
                    append(rt.rel).append("\n");
        }
        return buff.toString();
    }

    void fillRelInfo(PerQueryRelDocs relInfo) {
        String qid = relInfo.qid;

        for (ResultTuple rt : rtuples) {
            Integer relIntObj = relInfo.relMap.get(rt.docName);
            rt.rel = relIntObj == null ? 0 : relIntObj.intValue();
        }
    }

    @Override
    public int compareTo(RetrievedResults that) {
        return this.qid < that.qid ? -1 : this.qid == that.qid ? 0 : 1;
    }
}

class AllRetrievedResults {

    Map<String, RetrievedResults> allRetMap;
    String resFile;
    AllRelRcds allRelInfo;

    public AllRetrievedResults(String resFile) {
        this.resFile = resFile;
        allRetMap = new TreeMap<>();
    }

    public void load() {
        String line;
        try (FileReader fr = new FileReader(resFile);
                BufferedReader br = new BufferedReader(fr);) {
            while ((line = br.readLine()) != null) {
                storeRetRcd(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    void storeRetRcd(String line) throws IOException {
        String[] tokens = line.split("\\s+");
        String qid = tokens[0];
        RetrievedResults res = allRetMap.get(qid);
        if (res == null) {
            res = new RetrievedResults(qid);

            allRetMap.put(qid, res);
        }
        res.addTuple(tokens[2], Integer.parseInt(tokens[3]), Double.parseDouble(tokens[4]));
        res.pool.add(tokens[2]);
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (Map.Entry<String, RetrievedResults> e : allRetMap.entrySet()) {
            RetrievedResults res = e.getValue();
            buff.append(res.toString()).append("\n");
        }
        return buff.toString();
    }

}

class AveragePrecision implements APComputer {

    PerQueryRelDocs reldocList;
    RetrievedResults retriveList;
    HashMap<Integer, ApCalData> rankData; //ontains mber of rdoc, irrdoc upto a rank
    String qrelno;
    String runNo;
    HashSet reldoc;
    IndexReader reader;

    public AveragePrecision(String qrelString, String run, Evaluator eval, IndexReader reader) {
        this.qrelno = qrelString;
        this.runNo = run;
        this.reldocList = eval.relRcds.perQueryRels.get(qrelString);
        this.retriveList = eval.retRcds.allRetMap.get(qrelString);
        this.reader = reader;
        this.rankData = new HashMap<>();
        this.reldoc = new HashSet();
    }

    public void processRetrievedResult() {
        int r = 0;
        int n = 0;
        int d = 0;

        for (int i = 0; i < retriveList.rtuples.size(); i++) {

            if (reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName)) {
                r++;
                d++;
                rankData.put(i, new ApCalData(r, n, d));

            } else if (reldocList.irrelMap.containsKey(retriveList.rtuples.get(i).docName)) {
                d++;
                n++;
                rankData.put(i, new ApCalData(r, n, d));
            }
        }

    }

    @Override
    public double evaluateAP() {
        processRetrievedResult();
        double sum = 0;
        int numberofRecords = 0;
        for (int pos = 0; pos < retriveList.rtuples.size(); pos++) {
            if ((reldocList.relMap.containsKey(retriveList.rtuples.get(pos).docName))) {
                sum += (double) (rankData.get(pos).relDocNo) / (double) (pos + 1);
            }
        }
        return sum / reldocList.relMap.size();
    }

}

class ApCalData {

    int relDocNo;
    int irrelDocNo;
    int dValue;  //Number of docs within depth 100 pool upto that rank

    public ApCalData(int relDocNo, int irrelDocNo, int dValue) {
        this.dValue = dValue;
        this.relDocNo = relDocNo;
        this.irrelDocNo = irrelDocNo;
    }

}

class InferredAp extends AveragePrecision implements APComputer {

    int maxIter;
    HashSet<String> sampledData;
    HashMap<Integer, ApCalData> rankData;
    String samplingMode;

    public InferredAp(String qrelString, int maxIter, String run, Evaluator eval, IndexReader reader, double percentage) {
        super(qrelString, run, eval, reader);
        this.maxIter = maxIter;
        sampledData = new HashSet<>();
        rankData = new HashMap<Integer, ApCalData>();
        this.samplingMode = eval.samplingMode;
        if (samplingMode.equals("load")) {
            loadsampling(eval.samplingFileName);
        } else {
            sampling(percentage);
        }
        this.processRetrievedResult();
    }

    public void sampling(double percentage) {

        Random ran = new Random();
        ArrayList<String> pool = reldocList.pool;

        int iter = 0;
        int rel_exists = 0;
        int irrel_exists = 0;
        int count = 0;
        int sampleSize = (int) (pool.size() * percentage);
        while (iter < maxIter) {

            count = 0;
            this.reldoc = new HashSet();
            while (count < sampleSize) {
                int random = ran.nextInt(pool.size());
                sampledData.add(pool.get(random));
                if (reldocList.relMap.containsKey(pool.get(random))) {
                    rel_exists = 1;
                    reldoc.add(pool.get(random));
                }
                if (reldocList.irrelMap.containsKey(pool.get(random))) {
                    irrel_exists = 1;
                }
                count++;
            }

            if (rel_exists == 1 && irrel_exists == 1) {
                break;
            }
            iter++;
        }
    }

    public void loadsampling(String FileName) {

        try {
            FileReader fr = new FileReader(FileName);
            BufferedReader br = new BufferedReader(fr);
            int startflag = 0;
            int endflag = 0;
            String line = br.readLine();
            while (line != null) {
                String st[] = line.split(" ");
                if (st[0].equals(qrelno)) {
                    startflag = 1;
                }
                if (startflag == 1 && !st[0].equals(qrelno)) {
                    break;
                }

                if ((st[3].equals("0") || st[3].equals("1")) && startflag == 1) {
                    sampledData.add(st[2]);
                    if (st[3].equals("1")) {
                        reldoc.add(st[2]);
                    }
                }
                line = br.readLine();
            }
        } catch (FileNotFoundException fe) {
            System.out.println("File Not found!!");
        } catch (IOException fe) {
            System.out.println("File Not found!!");
        }
    }

    public void processRetrievedResult() {
        int r = 0;
        int n = 0;
        int d = 0;

        for (int i = 0; i < retriveList.rtuples.size(); i++) {
            if (sampledData.contains(retriveList.rtuples.get(i).docName) && reldoc.contains(retriveList.rtuples.get(i).docName)) {

                r++;
                d++;
                rankData.put(i, new ApCalData(r, n, d));

            } else if (sampledData.contains(retriveList.rtuples.get(i).docName) && reldocList.irrelMap.containsKey(retriveList.rtuples.get(i).docName)) {
                d++;
                n++;
                rankData.put(i, new ApCalData(r, n, d));

            } else {
                if (reldocList.irrelMap.containsKey(retriveList.rtuples.get(i).docName) || reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName)) {
                    d++;
                }
                rankData.put(i, new ApCalData(r, n, d));
            }
        }

    }

    // Function to create doc array for rel docs within sample
    public DocVector[] creatRelDocArray() throws IOException, ParseException {
        Iterator it = sampledData.iterator();
        int index = 0;
        while (it.hasNext()) {
            String docId = (String) it.next();
            if (reldocList.relMap.containsKey(docId)) {
                index++;
            }

        }
        it = sampledData.iterator();

        DocVector[] docArray = new DocVector[index];
        index = 0;
        while (it.hasNext()) {
            String docId = (String) it.next();
            if (reldocList.relMap.containsKey(docId)) {
                docArray[index++] = new DocVector(docId, reader, reldocList.searcher);
            }

        }
        return docArray;
    }

    // Function to create doc array for irrel docs within sample
    public DocVector[] creatIrelDocArray() throws IOException, ParseException {
        Iterator it = sampledData.iterator();
        int index = 0;
        while (it.hasNext()) {
            String docId = (String) it.next();
            if (reldocList.irrelMap.containsKey(docId)) {
                index++;
            }

        }
        it = sampledData.iterator();

        DocVector[] docArray = new DocVector[index];
        index = 0;
        while (it.hasNext()) {
            String docId = (String) it.next();
            if (reldocList.irrelMap.containsKey(docId)) {
                docArray[index++] = new DocVector(docId, reader, reldocList.searcher);
            }

        }
        return docArray;
    }

    @Override
    public double evaluateAP() {
        double sum = 0;
        int numberofRecords = 0;
        for (int pos = 0; pos < retriveList.rtuples.size(); pos++) {
            double rank = pos + 1;
            if (sampledData.contains(retriveList.rtuples.get(pos).docName) && (reldocList.relMap.containsKey(retriveList.rtuples.get(pos).docName))) {

                if (pos != 0) {
                    sum += (1 / rank) + (pos / rank) * (rankData.get(pos - 1).dValue / (double) (pos)) * ((rankData.get(pos - 1).relDocNo + .00001)
                            / (rankData.get(pos - 1).irrelDocNo + rankData.get(pos - 1).relDocNo + 2 * .00001));
                } else {
                    sum += (1 / rank);
                }
            }
        }
        Iterator it = sampledData.iterator();
        while (it.hasNext()) {
            String st = (String) it.next();
            if (reldocList.relMap.containsKey(st)) {
                numberofRecords++;
            }

        }

        if (numberofRecords == 0) {
            return 0;
        } else {
            return sum / numberofRecords;
        }

    }

}

class InferredApKDE extends InferredAp implements APComputer {

    HashMap<String, Double> KDEValues;
    static final double val = Math.sqrt(2 * Math.PI);
    double h;
    double sigma;
    String cosineFolderPath;
    double lambda;
    HashMap<String, Double> probValues;
    IndexSearcher searcher;

    public void loadCosineValuePerQid() {
        try {
            FileReader fr = new FileReader(new File(cosineFolderPath + qrelno + ".txt"));
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();
            while (line != null) {
                String st[] = line.split(" ");
                reldocList.perQuerydocCosineSim.put(st[0] + st[1], Double.parseDouble(st[2]));
                line = br.readLine();
            }
        } catch (FileNotFoundException fe) {
            System.out.println("File Not found!!");
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public InferredApKDE(String qrelString, int maxIter, String run, Evaluator eval, IndexReader reader, double percentage, double h, double sigma, String cosineFolderPath, double lambda, IndexSearcher searcher) throws IOException {
        super(qrelString, maxIter, run, eval, reader, percentage);
        this.cosineFolderPath = cosineFolderPath;
        // loadCosineValuePerQid();
        this.h = h;
        this.sigma = sigma;
        this.lambda = lambda;
        this.searcher = searcher;
        probValues = new HashMap<>();
       

    }

    //store the centroid vector
    public void writeCentroid(HashMap<String, Double> termMap, String fileName) throws IOException {
        FileWriter fw = new FileWriter(new File(fileName), true);
        BufferedWriter bw = new BufferedWriter(fw);
        Iterator it = termMap.keySet().iterator();
        ArrayList ar = new ArrayList();
        bw.write("#" + qrelno);
        bw.newLine();
        while (it.hasNext()) {
            String st = (String) it.next();
            bw.write(st + " " + termMap.get(st));
            bw.newLine();
        }
        bw.close();
        fw.close();
    }

    //compute probabities using similarity with the centroid vectors
    public void computeprob(ArrayList<String> unjudged) throws IOException, ParseException {
        for (String docid : unjudged) {
            double score = 0;
            ArrayList<Double> ar = new ArrayList();
            int index = 0;
            if (reldocList.irrelMap.containsKey(docid) || reldocList.relMap.containsKey(docid)) {
                double dist1 = computeRelSim(docid);

                double dist2 = computeIrrelSim(docid);
                probValues.put(docid, dist1 / (dist2 + dist1));
            }
        }

        Iterator it = probValues.keySet().iterator();
        double sum = 0;
        while (it.hasNext()) {
            String st = (String) it.next();
            sum += probValues.get(st);
        }

        it = probValues.keySet().iterator();

        while (it.hasNext()) {
            String st = (String) it.next();
            probValues.put(st, probValues.get(st) / sum);
        }

    }

    //+++Debasis: I've cleaned up this function. Please stick to this cleanliness for the rest
    //of the functions...
    public HashMap<String, Double> computeKde(Set<String> judgedRel, ArrayList<String> unjudged, IndexReader reader, int qid, HashMap<String, Double> docPairCosineMap) throws IOException {
        HashMap<String, Double> estmatedList = new HashMap<String, Double>();
        double score, score1;
        double sim, dist;
        String docidair;
        for (String docid : unjudged) {

            score = 0;
            score1 = 0;
            ArrayList<Double> ar = new ArrayList();
            int index = 0;
            if (reldocList.irrelMap.containsKey(docid) || reldocList.relMap.containsKey(docid)) {
                for (String docId2 : judgedRel) {
                    sim = 0;
                    docidair = docid + docId2;
                    sim = docPairCosineMap.get(docidair);
                    ar.add(sim);
                    dist = 1 - sim;
                    score += Math.exp(-((dist * dist) / h) / (2 * sigma * sigma));
                }
                Collections.sort(ar);
                Collections.reverse(ar);
                score1 = Math.exp(-(((1 - ar.get(0)) * (1 - ar.get(0))) / h)) + Math.exp(-(((1 - ar.get(1)) * (1 - ar.get(1))) / h)) + Math.exp(-(((1 - ar.get(2)) * (1 - ar.get(2))) / h));
                score1 = score1 / 3;
                score1 = score1 / (2 * sigma * sigma);
                score = score / judgedRel.size();
                score = score / (val * h);
                estmatedList.put(docid, score1);
            }
        }
        return estmatedList;
    }

    public HashMap<String, Double> computeWeightedKde(Set<String> judgedRel, ArrayList<String> unjudged, IndexReader reader, int qid, HashMap<String, Double> docPairCosineMap, HashMap<String, Double> docRankMap) throws IOException {
        HashMap<String, Double> estmatedList = new HashMap<String, Double>();
        double score, score1;
        double sim, dist;
        String docidair;
        for (String docid : unjudged) {

            score = 0;
            score1 = 0;
            int index = 0;
            if (reldocList.irrelMap.containsKey(docid) || reldocList.relMap.containsKey(docid)) {
                for (String docId2 : judgedRel) {
                    sim = 0;
                    docidair = docid + docId2;
                    sim = docPairCosineMap.get(docidair);
                    dist = 1 - sim;
                    score += docRankMap.get(docId2) * Math.exp(-((dist * dist) / h) / (2 * sigma * sigma));
                }
                score = score / judgedRel.size();
                score = score / (val * h);
                estmatedList.put(docid, score);
            }
        }
        return estmatedList;
    }

    public void storeKDE(String fileName) throws IOException {
        FileWriter fw = new FileWriter(new File(fileName));
        BufferedWriter bw = new BufferedWriter(fw);
        Iterator it = KDEValues.keySet().iterator();
        bw.write("#" + qrelno);
        bw.newLine();
        while (it.hasNext()) {
            String st = (String) it.next();
            bw.write(st + " " + KDEValues.get(st));
            bw.newLine();

        }
        bw.close();
    }

    public HashMap<String, Double> computeKdeWithRsv(Set<String> judgedRel, ArrayList<String> unjudged, IndexReader reader, int qid, HashMap<String, Double> docPairCosineMap) throws IOException {
        HashMap<String, Double> estmatedList = new HashMap<String, Double>();
        double score;
        double sim, dist;
        String docidair;
        int i;
        for (String docid : unjudged) {
            score = 0;
            if (sampledData.contains(docid)) {
                for (String docId2 : judgedRel) {
                    sim = 0;
                    if (retriveList.docIdSimValueMap.containsKey(docId2)) {
                        dist = (retriveList.docIdSimValueMap.get(docid) - retriveList.docIdSimValueMap.get(docId2)) / (retriveList.maxSimValue - retriveList.minSimValue);
                    } else {
                        dist = (retriveList.docIdSimValueMap.get(docid) - retriveList.minSimValue) / (retriveList.maxSimValue - retriveList.minSimValue);
                    }
                    if (dist < 0) {
                        dist = -dist;
                    }
                    score += Math.exp(-((dist * dist / h) / (2 * sigma * sigma)));
                }
                score = score / judgedRel.size();
                score = score / (val * h);
                estmatedList.put(docid, score);
            }
        }
        return estmatedList;
    }

    //compute similarity with rel docs
    public double computeRelSim(String docid) throws IOException, ParseException {

        double dist = 0;
        double dist1 = 0;
        double dist2 = 0;
        ArrayList<Double> ar = reldocList.vectorMap.get(docid);
        VectorExtractor ve = new VectorExtractor();
        double sim = ve.computeSimilarity(reldocList.relCentroid, ar);
        
        return sim;
    }

    public double computeIrrelSim(String docid) throws IOException, ParseException {

        double dist = 0;
        double dist1 = 0;
        double dist2 = 0;
        ArrayList<Double> ar = reldocList.vectorMap.get(docid);
        VectorExtractor ve = new VectorExtractor();

        double sim = ve.computeSimilarity(reldocList.irrelCentroid, ar);

        return sim;
    }

    @Override
    public double evaluateAP() {
        double sum = 0;

        HashMap<String, Double> docProbmap = new HashMap<>();
        int flag1 = 0;

        int numberofRecords = 0;
        for (int pos = 0; pos < retriveList.rtuples.size(); pos++) {
            double rank = pos + 1;
            if (sampledData.contains(retriveList.rtuples.get(pos).docName) && (reldocList.relMap.containsKey(retriveList.rtuples.get(pos).docName))) {
                sum += (1 / rank);
                double probabilitySum = 0;
                for (int j = 0; j < pos; j++) {
                    if (!sampledData.contains(retriveList.rtuples.get(j).docName) && ((reldocList.relMap.containsKey(retriveList.rtuples.get(j).docName)) || (reldocList.irrelMap.containsKey(retriveList.rtuples.get(j).docName)))) {
                        try {
                            probabilitySum += probValues.get(retriveList.rtuples.get(j).docName);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        double probability = lambda * ((rankData.get(pos - 1).relDocNo + .0001) / (rankData.get(pos - 1).relDocNo + rankData.get(pos - 1).irrelDocNo + 2 * .0001)) + (1 - lambda) * probValues.get(retriveList.rtuples.get(j).docName);;
                        docProbmap.put(retriveList.rtuples.get(j).docName, probValues.get(retriveList.rtuples.get(j).docName));
                    }
                }
                if (pos != 0) {
                    sum += (1 / (double) rank) * ((rankData.get(pos - 1).relDocNo + rankData.get(pos - 1).irrelDocNo) * ((rankData.get(pos - 1).relDocNo + .0001) / (rankData.get(pos - 1).relDocNo + rankData.get(pos - 1).irrelDocNo + 2 * .0001)) + (lambda * (rankData.get(pos - 1).relDocNo + .0001) * (rankData.get(pos - 1).dValue - rankData.get(pos - 1).irrelDocNo - rankData.get(pos - 1).relDocNo) / (double) (rankData.get(pos - 1).irrelDocNo + rankData.get(pos - 1).relDocNo + 2 * .0001)) + ((1 - lambda) * probabilitySum));
                }
            }

        }
        Iterator it = sampledData.iterator();
        while (it.hasNext()) {
            String st = (String) it.next();
            if (reldocList.relMap.containsKey(st)) {
                numberofRecords++;
            }

        }

        reldocList.perQuerydocCosineSim = new HashMap<>();
        if (numberofRecords == 0) {
            return 0;
        } else {
            return sum / numberofRecords;
        }
    }

}

interface APComputer {

    double evaluateAP();
}

class EvaluateAll extends Evaluator {

    String runFileList;
    HashMap<String, Double> runApMap;
    String resultFolderPath;
    String runFileFolderPath;
    String cosineFolderPath;
    String kdeMode;

    public EvaluateAll(Properties prop) throws IOException, Exception {
        super(prop);
        runApMap = new HashMap<>();
        this.runFileList = prop.getProperty("run.file");
        this.resultFolderPath = prop.getProperty("resultFolderLocation");
        this.runFileFolderPath = prop.getProperty("runfileFolderLocation");
        this.cosineFolderPath = prop.getProperty("cosineFolderLocation");
        this.kdeMode = prop.getProperty("KDEmode");
    }

    public void computeMeanAp() throws FileNotFoundException, Exception {
        FileReader fr = new FileReader(new File(runFileList));
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        while (line != null) {

            retRcds.resFile = runFileFolderPath + "/" + line;
            retRcds.allRetMap = new TreeMap<>();
            retRcds.load();
            double apValue = evaluateQueries(.30);
            runApMap.put(line, apValue);
            line = br.readLine();
        }
        br.close();
    }

    public void storeRunMeanAp(String fileName) throws IOException {
        FileWriter fw = new FileWriter(new File(fileName));
        BufferedWriter bw = new BufferedWriter(fw);
        Iterator it = runApMap.keySet().iterator();
        while (it.hasNext()) {
            String run = (String) it.next();
            bw.write(run + " " + runApMap.get(run));
            bw.newLine();
        }
        bw.close();
    }

    public HashMap<String, Double> computeKDEValues(Set<String> relDOc, HashMap<String, Double> cosineMap, int qid, InferredApKDE iapk, HashMap<String, Double> docRankMap) throws IOException {

        if (kdeMode.equals("1")) {
            return iapk.computeKde(relDOc, iapk.retriveList.pool, reader, qid, cosineMap);
        } else if (kdeMode.equals("2")) {

            return iapk.computeWeightedKde(relDOc, iapk.retriveList.pool, reader, qid, cosineMap, docRankMap);

        } else if (kdeMode.equals("3")) {
            return iapk.computeKdeWithRsv(relDOc, iapk.retriveList.pool, reader, qid, cosineMap);

        } else {
            return null;
        }
    }

    public HashMap<String, Double> computeAvgRank(ArrayList runList, int qid, HashSet SampleData) {
        double rankSum = 0;
        HashMap<String, Double> docrankMap = new HashMap<>();
        for (int i = 0; i < runList.size(); i++) {
            retRcds.resFile = runFileFolderPath + "/" + runList.get(i);
            retRcds.allRetMap = new TreeMap<>();
            retRcds.load();
            Iterator it = SampleData.iterator();
            while (it.hasNext()) {
                String docname = (String) it.next();
                Integer h = qid;
                if (relRcds.perQueryRels.get(h.toString()).relMap.containsKey(docname)) {

                    double rank = retRcds.allRetMap.get(h.toString()).pool.indexOf(docname);
                    if (rank != -1) {
                        if (docrankMap.containsKey(docname)) {
                            docrankMap.put(docname, docrankMap.get(docname) + rank);
                        } else {
                            docrankMap.put(docname, rank);
                        }
                    } else {
                        if (docrankMap.containsKey(docname)) {
                            docrankMap.put(docname, docrankMap.get(docname) + 0);
                        } else {
                            double x = 0;
                            docrankMap.put(docname, x);
                        }

                    }

                }

            }
        }

        Iterator it = docrankMap.keySet().iterator();
        while (it.hasNext()) {
            String st = (String) it.next();
            double r = docrankMap.get(st);
            docrankMap.put(st, (r / 1000) / runList.size());
        }

        return docrankMap;
    }

    public void computeCosineValues(String fileName) throws IOException {
        HashMap<String, HashMap<String, ArrayList<Double>>> cosineMap = new HashMap<String, HashMap<String, ArrayList<Double>>>();

        VectorExtractor ve = new VectorExtractor();
        for (int i = startQid; i <= endQid; i++) {
            HashMap<String, ArrayList<Double>> h = ve.extractVector(fileName);
            Integer id = i;
            cosineMap.put(id.toString(), h);

        }
    }

    public void storeRunQid(String fileName) throws IOException, Exception {

        FileReader fr = new FileReader(new File(runFileList));
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        ArrayList<String> runlist = new ArrayList<>();
        while (line != null) {
            runlist.add(line);
            line = br.readLine();
        }
        br.close();
        HashMap<String, HashMap<Integer, Double>> runQidApMap = new HashMap<>();
        HashMap<String, Double> qidCosineMap = new HashMap<>();
        for (int pos = startQid; pos <= endQid; pos++) {
            Integer qidValue = pos;
            InferredApKDE iapk = new InferredApKDE(qidValue.toString(), 5, "", this, reader, .30, h, sigma, cosineFolderPath, lambda, relRcds.perQueryRels.get(qidValue.toString()).searcher);
            System.out.println(pos);
            qidCosineMap = iapk.reldocList.perQuerydocCosineSim;
            HashMap<String, Double> docRankMap = null;
            if (kdeMode.equals("2")) {
                docRankMap = computeAvgRank(runlist, pos, iapk.sampledData);
            }

            for (int j = 0; j < runlist.size(); j++) {
                retRcds.resFile = runFileFolderPath + "/" + runlist.get(j);
                retRcds.allRetMap = new TreeMap<>();
                retRcds.load();
                Set<String> rel = new HashSet<String>();
                Iterator it = iapk.sampledData.iterator();
                HashMap<String, Integer> rankMap = new HashMap<>();
                while (it.hasNext()) {
                    String st = (String) it.next();
                    if (iapk.reldoc.contains(st)) {
                        rel.add(st);
                    }
                }

                iapk.retriveList = this.retRcds.allRetMap.get(qidValue.toString());
                iapk.processRetrievedResult();

                iapk.computeprob(iapk.retriveList.pool);
                double g = iapk.evaluateAP();
              //  System.out.println("kk" + g);
                HashMap<Integer, Double> qidApMap = runQidApMap.get(runlist.get(j));
                if (qidApMap == null) {
                    qidApMap = new HashMap<>();
                }
                qidApMap.put(qidValue, g);
                runQidApMap.put(runlist.get(j), qidApMap);
            }

        }
        FileWriter fw = new FileWriter(new File(fileName));
        BufferedWriter bw = new BufferedWriter(fw);
        for (int j = 0; j < runlist.size(); j++) {
            HashMap<Integer, Double> values = runQidApMap.get(runlist.get(j));
            double sum = 0;
            for (int i = startQid; i <= endQid; i++) {
                sum += values.get(i);
            }
            bw.write(runlist.get(j) + " " + sum / (endQid - startQid + 1));
            bw.newLine();
            // System.out.println(sum / (endQid - startQid + 1));
        }
        bw.close();
    }
}

public class Evaluator {

    AllRelRcds relRcds;
    AllRetrievedResults retRcds;
    int startQid;
    int endQid;
    HashMap<Integer, Double> qidApMap;
    double MeanAp;
    IndexReader reader;
    String cosineMode;
    String cosineSimilarityFile;
    Properties prop;
    String flag;
    double h;
    double sigma;
    String samplingMode;
    String samplingFileName;
    double lambda;
    String relCentroidFileName;
    String irrelCentroidFileName;
    String vectorFolderLocation;

    public Evaluator(String qrelsFile, String resFile, String indexPath, String cosineMode, String cosineSimilarityFile, Properties prop) throws IOException {
        Path p = Paths.get(indexPath);
        reader = DirectoryReader.open(FSDirectory.open(p));
        this.cosineMode = cosineMode;
        relRcds = new AllRelRcds(qrelsFile, reader, cosineSimilarityFile, cosineMode);
        retRcds = new AllRetrievedResults(resFile);
        qidApMap = new HashMap<>();
        this.prop = prop;
        flag = prop.getProperty("flag");
        if (flag.equals("1")) {
            h = Double.parseDouble(prop.getProperty("h"));
            sigma = Double.parseDouble(prop.getProperty("sigma"));
            lambda = Double.parseDouble(prop.getProperty("lambda"));
        } else {
            h = -1;
            sigma = -1;
        }
        this.samplingMode = prop.getProperty("samplingMode");
        this.samplingFileName = prop.getProperty("samplingFileName");
        this.relCentroidFileName = prop.getProperty("centroidFile");
        this.irrelCentroidFileName = prop.getProperty("irrelcentroidFile");
        this.vectorFolderLocation = prop.getProperty("outputVectorfileLocation");
    }

    public Evaluator(Properties prop) throws Exception {
        String qrelsFile = prop.getProperty("qrels.file");
        String resFile = prop.getProperty("res.file");
        cosineMode = prop.getProperty("cosinemode");
        Path p = Paths.get(prop.getProperty("index.file"));
        reader = DirectoryReader.open(FSDirectory.open(p));
        relRcds = new AllRelRcds(qrelsFile, reader, prop.getProperty("cosine.file"), cosineMode);
        retRcds = new AllRetrievedResults(resFile);
        startQid = Integer.parseInt(prop.getProperty("qid.start"));
        endQid = Integer.parseInt(prop.getProperty("qid.end"));
        qidApMap = new HashMap<>();
        flag = prop.getProperty("flag");
        if (flag.equals("1")) {
            h = Double.parseDouble(prop.getProperty("h"));
            sigma = Double.parseDouble(prop.getProperty("sigma"));
            lambda = Double.parseDouble(prop.getProperty("lambda"));
        } else {
            h = -1;
            sigma = -1;
        }
        this.samplingMode = prop.getProperty("samplingMode");
        this.samplingFileName = prop.getProperty("samplingFileName");
        this.prop = prop;
        this.relCentroidFileName = prop.getProperty("centroidFile");
        this.irrelCentroidFileName = prop.getProperty("irrelcentroidFile");
        this.vectorFolderLocation = prop.getProperty("outputVectorfileLocation");
    }

    public void load() throws Exception {
        relRcds.load(startQid, endQid, relCentroidFileName, irrelCentroidFileName, vectorFolderLocation);
        System.out.println("rel records loaded");
        retRcds.load();
        System.out.println("ret records loaded");
    }

    public APComputer createAPEvaluator(String qid, int maxIter, double percentage) throws Exception {
        APComputer iapk;
        if (flag.equals("1")) {
            iapk = new InferredApKDE(qid, maxIter, "", this, reader, percentage, h, sigma, cosineSimilarityFile, lambda, relRcds.perQueryRels.get(qid).searcher);
        } else if (flag.equals("2")) {
            iapk = new AveragePrecision(qid, "", this, reader);
        } else {
            iapk = new InferredAp(qid, maxIter, "", this, reader, percentage);

        }
        return iapk;

    }

    public double evaluateQueries(double percentage) throws Exception {
        double sum = 0;
        APComputer iapk;
        for (int qid = startQid; qid <= endQid; qid++) {
            Integer h = qid;
            double g;
            iapk = createAPEvaluator(h.toString(), 5, percentage);
            g = iapk.evaluateAP();
            sum += g;
            Double h1;
            qidApMap.put(qid, g);

        }
        sum /= (endQid - startQid + 1);
        reader.close();
        return sum;
    }

    public void storeApValues(String fileName) throws IOException {
        FileWriter fw = new FileWriter(new File(fileName));
        BufferedWriter bw = new BufferedWriter(fw);
        Iterator it = qidApMap.keySet().iterator();
        while (it.hasNext()) {
            String qid = (String) it.next();
            bw.write(qid + " " + qidApMap.get(qid));
            bw.newLine();
        }
        bw.close();
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(relRcds.toString()).append("\n");
        buff.append(retRcds.toString());
        return buff.toString();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "/home/procheta/NetBeansProjects/InferrdAp/src/Evaluator/init.properties";
        }
        try {
            Properties prop = new Properties();
            prop.load(new FileReader(args[0]));
            EvaluateAll eval = new EvaluateAll(prop);
            eval.load();
            // eval.computeMeanAp();
            //  eval.storeRunMeanAp(prop.getProperty("storeMeanAp"));
            eval.storeRunQid(prop.getProperty("storeMeanAp"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
