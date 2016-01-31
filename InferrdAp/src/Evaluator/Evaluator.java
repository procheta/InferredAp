/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Evaluator;

import kde.KDEImplementation;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
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
    ArrayList<String> pool; // list of all the docs
    int numRel;
    HashMap<String, Double> perQuerydocCosineSim;
    IndexReader reader;
    IndexSearcher searcher;

    public PerQueryRelDocs(String qid, String indexPath) throws IOException {
        this.qid = qid;
        numRel = 0;
        relMap = new HashMap<>();
        irrelMap = new HashMap<>();
        pool = new ArrayList<>();
        perQuerydocCosineSim = new HashMap<>();
        reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
        searcher = new IndexSearcher(reader);
    }

    void addTuple(String docId, int rel) {
        if (relMap.get(docId) != null) {
            return;
        }
        if (rel > 0) {
            numRel++;
            relMap.put(docId, rel);
            pool.add(docId);
        }
        if (rel == 0) {
            irrelMap.put(docId, rel);
            pool.add(docId);
        }
    }

    public void precomputeCosineSim() throws IOException, ParseException {
        Iterator it = irrelMap.keySet().iterator();

        while (it.hasNext()) {
            String irrelDocId = (String) it.next();
            //  System.out.println(docid1);
            DocVector doc1 = new DocVector(irrelDocId, reader, searcher);
            Iterator it2 = relMap.keySet().iterator();
            while (it2.hasNext()) {
                String docid2 = (String) it2.next();
                DocVector doc2 = new DocVector(docid2, reader, searcher);
                perQuerydocCosineSim.put(irrelDocId + "#" + docid2, doc1.cosineSim(doc2));

            }

        }
        it = relMap.keySet().iterator();
        while (it.hasNext()) {
            String relDocId = (String) it.next();
            //  System.out.println(docid1);
            DocVector doc1 = new DocVector(relDocId,  reader, searcher);
            Iterator it2 = relMap.keySet().iterator();
            while (it2.hasNext()) {
                String docid2 = (String) it2.next();
                DocVector doc2 = new DocVector(docid2,  reader, searcher);
                perQuerydocCosineSim.put(relDocId + "#" + docid2, doc1.cosineSim(doc2));
            }
        }

    }
}

class AllRelRcds {

    String qrelsFile;
    HashMap<String, PerQueryRelDocs> perQueryRels;
    int totalNumRel;
    String indexPath;

    public AllRelRcds(String qrelsFile, String indexPath) {
        this.qrelsFile = qrelsFile;
        perQueryRels = new HashMap<>();
        totalNumRel = 0;
        this.indexPath = indexPath;
    }
  

    void load() throws Exception {
        //  System.out.println(qrelsFile);
        FileReader fr = new FileReader(qrelsFile);
        BufferedReader br = new BufferedReader(fr);
        String line;

        while ((line = br.readLine()) != null) {
            storeRelRcd(line);
        }

        br.close();
        fr.close();
    }

    void storeRelRcd(String line) throws IOException {
        String[] tokens = line.split("\\s+");
        String qid = tokens[0];
        PerQueryRelDocs relTuple = perQueryRels.get(qid);
        if (relTuple == null) {
            relTuple = new PerQueryRelDocs(qid, indexPath);
            perQueryRels.put(qid, relTuple);
        }
        relTuple.addTuple(tokens[2], Integer.parseInt(tokens[3]));
    }

    public void storeCosineSimilarity(String fileName, int startQrelNo, int endQrelNo, String indexPath, IndexReader reader, IndexSearcher searcher) throws IOException, ParseException {
        FileWriter fw = new FileWriter(new File(fileName));
        BufferedWriter bw = new BufferedWriter(fw);
        for (int qid = startQrelNo; qid < endQrelNo; qid++) {

            PerQueryRelDocs perqd = perQueryRels.get(qid);
            perqd.precomputeCosineSim();
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

    public HashMap<Integer, HashMap<String, Double>> loadCosineValue(String fileName) throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(new File(fileName));
        BufferedReader br = new BufferedReader(fr);

        String line = br.readLine();
        HashMap<Integer, HashMap<String, Double>> qidCosineMap = new HashMap<>();
        String qid = "401";
        HashMap<String, Double> h1 = new HashMap<>();
        int flag = 0;
        int flag2 = 0;
        while (line != null) {
            if (line.startsWith("#")) {
                if (flag != 0) {
                    qidCosineMap.put(Integer.parseInt(qid), h1);
                    h1 = new HashMap<>();
                    qid = line.substring(1, line.length());
                } else {
                    qid = line.substring(1, line.length());
                    flag = 1;
                }
                if (qid.equals("421")) {
                    flag2 = 1;
                    break;
                }
                //   System.out.println(qid);
            } else {

                String st[] = line.split(" ");
                String docPair = st[0] + st[1];
                h1.put(docPair, Double.parseDouble(st[2]));

            }
            if (flag2 == 1) {
                break;
            }
            line = br.readLine();
            //System.out.println(line);
        }
        return qidCosineMap;
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

    public ResultTuple(String docName, int rank) {
        this.docName = docName;
        this.rank = rank;
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
    float avgP;
    ArrayList<String>pool;
    

    public RetrievedResults(String qid) throws IOException {
        this.qid = Integer.parseInt(qid);
        this.rtuples = new ArrayList<>(1000);
        avgP = -1;
        numRelRet = -1;
        pool = new ArrayList<>();
       
    }

    void addTuple(String docName, int rank) {
        rtuples.add(new ResultTuple(docName, rank));
        pool.add(docName);
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
       // this.relInfo = relInfo;
    }

    @Override
    public int compareTo(RetrievedResults that) {
        return this.qid < that.qid ? -1 : this.qid == that.qid ? 0 : 1;
    }
}

class AllRunRetrievedResults {

    HashMap<String, AllRetrievedResults> allRunRetMap;
    String runfileList;

    public AllRunRetrievedResults(String runFileName, String location) throws FileNotFoundException, IOException {
        this.runfileList = runFileName;
        allRunRetMap = new HashMap<>();
        FileReader fr = new FileReader(new File(runFileName));
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();

        while (line != null) {
            String runFileLocation = location + "/" + line;
            // System.out.println(runFileLocation);
            AllRetrievedResults alr = new AllRetrievedResults(runFileLocation);
            alr.load();
            allRunRetMap.put(line, alr);
            //System.out.println(line);
            line = br.readLine();
        }

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
        res.addTuple(tokens[2], Integer.parseInt(tokens[3]));
        res.pool.add(tokens[2]);
      //  System.out.println("hhh");
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
class MeanInferredAp {

    HashMap<String, Double> meanInferApList;
    ArrayList<String> runfileList;
    int startQrelno;
    int endQrelno;
    double rmsError;

    public MeanInferredAp(int startQrelno, int endQrelNo, String runFileName) throws FileNotFoundException, IOException {

        this.startQrelno = startQrelno;
        this.endQrelno = endQrelNo;

        FileReader fr = new FileReader(runFileName);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        runfileList = new ArrayList<>();

        while (line != null) {
            runfileList.add(line);
            line = br.readLine();
        }

        meanInferApList = new HashMap<>();

    }

    public void computeMeanInferredAp(Properties prop, int percentage, int maxIter) throws Exception {

        Evaluator eval = new Evaluator(prop);
        String runFileLocation = prop.getProperty("runFileLocation");
        AllRunRetrievedResults alr = new AllRunRetrievedResults(prop.getProperty("run.file"), runFileLocation);
        eval.load();
        for (int i = startQrelno; i <= endQrelno; i++) {
            double sum = 0;
            Integer h = i;
            InferredAp iAp = new InferredAp(h.toString(), maxIter, "input.kdd8sh16", eval);
            iAp.sampling(percentage);
            eval.retRcds.resFile = runFileLocation + "/input.kdd8sh16";
            eval.retRcds = alr.allRunRetMap.get(runFileLocation + "/input.kdd8sh16");
            iAp.processRetrievedResult();
            sum = iAp.computeInferredAp();
            System.out.println("Qid No " + i + " Inferred Ap value " + sum);
            meanInferApList.put(h.toString(), sum);//*/

        }

    }

    public void storeMeanInferredAp(String FileName, ArrayList runFileList) throws IOException {
        FileWriter fw = new FileWriter(new File(FileName));
        BufferedWriter bw = new BufferedWriter(fw);

        for (int count = 0; count < runFileList.size(); count++) {
            Integer h = count;
            Double infApValue = meanInferApList.get(runFileList.get(count));

            bw.write(runFileList.get(count) + "  " + infApValue.toString());
            bw.newLine();
        }
        bw.close();
    }

}

class InferredApCalData {

    int relDocNo;
    int irrelDocNo;
    int dValue;

    public InferredApCalData(int relDocNo, int irrelDocNo, int dValue) {

        this.dValue = dValue;
        this.relDocNo = relDocNo;
        this.irrelDocNo = irrelDocNo;

    }

}

class InferredAp {

    PerQueryRelDocs reldocList;
    RetrievedResults retriveList;
    int maxIter;
    HashSet<String> sampledData;
    HashMap<Integer, InferredApCalData> rankData;
    String qrelno;
    String runNo;
    HashSet reldoc;

    public InferredAp(String qrelString, int maxIter, String run, Evaluator eval) throws Exception {
        this.qrelno = qrelString;
        this.maxIter = maxIter;
        sampledData = new HashSet<>();
        this.runNo = run;
        this.reldocList = eval.relRcds.perQueryRels.get(qrelString);
        this.retriveList = eval.retRcds.allRetMap.get(qrelString);
        this.rankData = new HashMap<>();

    }

    public void sampling(double percentage) {

        Random ran = new Random();

        ArrayList<String> pool = reldocList.pool;

        int iter = 0;
        int rel_exists = 0;
        int irrel_exists = 0;
        int count = 0;
        // System.out.println(pool.size());
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

        // return sampledData;
    }

    public void processRetrievedResult() {
        int r = 0;
        int n = 0;
        int d = 0;
        for (int i = 0; i < retriveList.rtuples.size(); i++) {
            if (sampledData.contains(retriveList.rtuples.get(i).docName) && reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName)) {
                r++;

                rankData.put(i, new InferredApCalData(r, n, d));

            } else if (sampledData.contains(retriveList.rtuples.get(i).docName) && reldocList.irrelMap.containsKey(retriveList.rtuples.get(i).docName)) {

                n++;
                rankData.put(i, new InferredApCalData(r, n, d));

            } else {
                if (!reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName) && !reldocList.irrelMap.containsKey(retriveList.rtuples.get(i).docName)) {
                    d++;
                }
                rankData.put(i, new InferredApCalData(r, n, d));

            }

        }
    }

    public double computeInferredAp() {

        double sum = 0;
        int numberofRecords = 0;

        for (int i = 1; i < retriveList.rtuples.size(); i++) {
            if (sampledData.contains(retriveList.rtuples.get(i).docName) && (reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName))) {

                sum += (1 / (double) (i + 1)) + ((i) / (double) (i + 1)) * (rankData.get(i).relDocNo / (double) (i)) * ((rankData.get(i).irrelDocNo + .01)
                        / (rankData.get(i).irrelDocNo + rankData.get(i).dValue + 2 * .01));
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

class InferredApKDE extends InferredAp {

    public InferredApKDE(String qrelString, int maxIter, String run, Evaluator eval) throws Exception {
        super(qrelString, maxIter, run, eval);
    }

    public double computeInferredAp(HashMap<String, Double> KDEValues) {

        double sum = 0;
        int numberofRecords = 0;
        this.processRetrievedResult();
       // System.out.println("gggg");
        for (int i = 1; i < retriveList.rtuples.size(); i++) {
           
            if (sampledData.contains(retriveList.rtuples.get(i).docName) && (reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName))) {
                sum += (1 / (double) (i + 1));
                 
                for (int j = 0; j < i; j++) {
                    if (!sampledData.contains(retriveList.rtuples.get(j).docName) && ((reldocList.relMap.containsKey(retriveList.rtuples.get(j).docName)) || (reldocList.irrelMap.containsKey(retriveList.rtuples.get(j).docName)))) {
                        try {
                            sum += (1 / (double) (j + 1)) * (rankData.get(j).dValue / (double) (j + 1)) * KDEValues.get(retriveList.rtuples.get(j).docName);
                        } catch (Exception e) {
                            //System.out.println(retriveList.rtuples.get(j).docName);
                        }
                    } else {
                        if (sampledData.contains(retriveList.rtuples.get(i).docName) && (reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName))) {

                            sum += 0;
                        }
                    }
                }

                numberofRecords++;

            }

        }
        if (numberofRecords == 0) {
            return 0;
        } else {
            System.out.println(sum / numberofRecords);
            return sum / numberofRecords;
        }
    }
    
}



public class Evaluator {

    AllRelRcds relRcds;
    AllRetrievedResults retRcds;

    public Evaluator(String qrelsFile, String resFile, String indexPath) {
        relRcds = new AllRelRcds(qrelsFile, indexPath);
        retRcds = new AllRetrievedResults(resFile);
    }

    public Evaluator(Properties prop) {

        String qrelsFile = prop.getProperty("qrels.file");
        String resFile = prop.getProperty("res.file");
        String indexPath = prop.getProperty("index.file");
        relRcds = new AllRelRcds(qrelsFile, indexPath);
        retRcds = new AllRetrievedResults(resFile);
    }

    public void load() throws Exception {
        relRcds.load();
        retRcds.load();
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

            String qrelsFile = prop.getProperty("qrels.file");
            String resFile = prop.getProperty("res.file");
            String runFileList = prop.getProperty("run.file");
            String indexPath = prop.getProperty("index.file");
            String runFileLocation = prop.getProperty("runFileLocation");
            String mode = prop.getProperty("mode");
          /*  if (mode.equals("")) {
            } else {

            }*/
            IndexReader reader = DirectoryReader.open(FSDirectory.open(new File("/media/procheta/3CE80E12E80DCADA/newIndex2")));
            
            Evaluator evaluator = new Evaluator(qrelsFile, resFile, indexPath);
            evaluator.load();
            //  evaluator.storeCosineSimilarity(401, 450, evaluator, "input.kdd8sh16", "/home/procheta/Documents/Store2.txt", reader);
           // MeanInferredAp meanInAp = new MeanInferredAp(401, 450, runFileList);

            HashMap<Integer, HashMap<String, Double>> h1 = evaluator.relRcds.loadCosineValue("/home/procheta/Documents/Store.txt");
            HashMap<Integer, HashMap<String, Double>> h2 = evaluator.relRcds.loadCosineValue("/home/procheta/Documents/Store1.txt");

            reader.close();
            KDEImplementation kdei = new KDEImplementation();
        //    System.out.println(evaluator.retRcds.allRetMap.get("401").pool);
            InferredAp iap = new InferredAp("401", 5, mode, evaluator);
          //  System.out.println(iap.retriveList.pool);
           // System.out.println(new InferredAp(qrelsFile, 5, mode, evaluator).retriveList.pool);
        //   kdei.calculateKde(new InferredAp("401", 5, mode, evaluator).sampledData, new InferredAp(qrelsFile, 5, mode, evaluator).retriveList.pool, reader,401, h1, h2);
           InferredApKDE iak = new InferredApKDE("401", 5, "input.kdd8sh16", evaluator);
           iak.sampling(.30);
           
          // kdei.calculateKde(iak.sampledData, iak.retriveList.pool, reader, 401, h1, h2);
            iak.computeInferredAp( kdei.calculateKde(iak.sampledData, iak.retriveList.pool, reader, 401, h1, h2));

            //meanInferredAp meanInAp = new meanInferredAp(401, 450, runFileList);
            // meanInAp.computeMeanInferredApKDE(prop, .30, 5, "input.kdd8sh16", h1, h2, evaluator);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
