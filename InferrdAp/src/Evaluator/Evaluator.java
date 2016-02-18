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
import java.util.Set;
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

    public PerQueryRelDocs(String qid, IndexReader reader, String cosineFilename) throws IOException, ParseException {
        this.qid = qid;
        numRel = 0;
        relMap = new HashMap<>();
        irrelMap = new HashMap<>();
        pool = new ArrayList<>();
        this.reader = reader;
        searcher = new IndexSearcher(reader);
        precomputeCosineSim();
    }

    public PerQueryRelDocs(String qid, IndexReader reader) throws IOException, ParseException {
        this.qid = qid;
        numRel = 0;
        relMap = new HashMap<>();
        irrelMap = new HashMap<>();
        pool = new ArrayList<>();
        this.reader = reader;
        searcher = new IndexSearcher(reader);
        perQuerydocCosineSim = new HashMap<>();
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
        // System.out.println(qid);
        // System.out.println(irrelMap.size());
        // System.out.println(relMap.size());
        while (it.hasNext()) {
            String irrelDocId = (String) it.next();
            // System.out.println(irrelDocId);
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
            DocVector doc1 = new DocVector(relDocId, reader, searcher);
            Iterator it2 = relMap.keySet().iterator();
            while (it2.hasNext()) {
                String docid2 = (String) it2.next();
                DocVector doc2 = new DocVector(docid2, reader, searcher);
                perQuerydocCosineSim.put(relDocId + "#" + docid2, doc1.cosineSim(doc2));
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
    String mode;

    public AllRelRcds(String qrelsFile, IndexReader reader, String cosineFile, String mode) {
        this.qrelsFile = qrelsFile;
        perQueryRels = new HashMap<>();
        totalNumRel = 0;
        this.reader = reader;
        this.cosineFile = cosineFile;
        this.mode = mode;
    }

    void load(int startQid, int endQid) throws Exception {
        //  System.out.println(qrelsFile);
        FileReader fr = new FileReader(qrelsFile);
        BufferedReader br = new BufferedReader(fr);
        String line;

        while ((line = br.readLine()) != null) {
            storeRelRcd(line);
        }

        br.close();
        fr.close();

        if (mode.equals("load")) {
            loadCosineValue(cosineFile, startQid, endQid);
        } else   if (mode.equals("store")) {
            storeCosineSimilarity(cosineFile, startQid, endQid);
        }
    }

    void storeRelRcd(String line) throws IOException, ParseException {
        String[] tokens = line.split("\\s+");
        String qid = tokens[0];
        PerQueryRelDocs relTuple = perQueryRels.get(qid);
        if (relTuple == null) {
            relTuple = new PerQueryRelDocs(qid, reader);
            perQueryRels.put(qid, relTuple);
        }
        relTuple.addTuple(tokens[2], Integer.parseInt(tokens[3]));
    }

    public void storeCosineSimilarity(String fileName, int startQrelNo, int endQrelNo) throws IOException, ParseException {
        FileWriter fw = new FileWriter(new File(fileName), true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.newLine();
        for (int qid = startQrelNo; qid <= endQrelNo; qid++) {
            Integer qidString = qid;
            PerQueryRelDocs perqd = perQueryRels.get(qidString.toString());
            perqd.precomputeCosineSim();
            bw.write("#" + qidString.toString());
            bw.newLine();
            Iterator it = perqd.perQuerydocCosineSim.keySet().iterator();
            while (it.hasNext()) {
                String docPair = (String) it.next();
                String st[] = docPair.split("#");
                bw.write(st[0] + " " + st[1] + " " + perqd.perQuerydocCosineSim.get(docPair));
                bw.newLine();

            }
            System.out.println("done");
        }
        bw.close();
    }

    public void loadCosineValue(String fileName, int startQid, int endQid) throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(new File(fileName));
        BufferedReader br = new BufferedReader(fr);

        String line = br.readLine();
        HashMap<Integer, HashMap<String, Double>> qidCosineMap = new HashMap<>();
        String qid = "";
        HashMap<String, Double> h1 = new HashMap();
        int flag = 0;
        int flag2 = 0;
        int flag3 = 0;
        Integer start = startQid;
        Integer end = endQid;
        while (line != null) {
            if (line.startsWith("#")) {
                if (flag != 0) {
                    perQueryRels.get(qid).perQuerydocCosineSim = h1;
                    h1 = new HashMap();
                    qid = line.substring(1, line.length());
                } else {
                    qid = line.substring(1, line.length());
                    if (qid.equals(start.toString())) {
                        flag = 1;
                        flag3 = 1;
                    }
                    h1 = new HashMap();
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
                    h1.put(docPair, Double.parseDouble(st[2]));
                } catch (Exception e) {
                }
            }
            if (flag2 == 1) {
                break;
            }
            line = br.readLine();
        }
        br.close();
        perQueryRels.get(qid).perQuerydocCosineSim = h1;

        // return qidCosineMap;
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
    ArrayList<String> pool;

    public RetrievedResults(String qid) throws IOException {
        this.qid = Integer.parseInt(qid);
        this.rtuples = new ArrayList<>(1000);
        avgP = -1;
        numRelRet = -1;
        pool = new ArrayList<>();

    }

    void addTuple(String docName, int rank) {
        rtuples.add(new ResultTuple(docName, rank));
        // pool.add(docName);
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

class InferredApCalData {

    int relDocNo;
    int irrelDocNo;
    int dValue;  //Number of docs within depth 100 pool upto that rank

    public InferredApCalData(int relDocNo, int irrelDocNo, int dValue) {

        this.dValue = dValue;
        this.relDocNo = relDocNo;
        this.irrelDocNo = irrelDocNo;
    }

}

class InferredAp implements APComputer {

    PerQueryRelDocs reldocList;
    RetrievedResults retriveList;
    int maxIter;
    HashSet<String> sampledData;
    HashMap<Integer, InferredApCalData> rankData;
    String qrelno;
    String runNo;
    HashSet reldoc;
    IndexReader reader;

    public InferredAp(String qrelString, int maxIter, String run, Evaluator eval, IndexReader reader, double percentage) throws Exception {
        this.qrelno = qrelString;
        this.maxIter = maxIter;
        sampledData = new HashSet<>();
        this.runNo = run;
        this.reldocList = eval.relRcds.perQueryRels.get(qrelString);
        this.retriveList = eval.retRcds.allRetMap.get(qrelString);
        this.reader = reader;
        this.rankData = new HashMap<>();
        sampling(percentage);
        processRetrievedResult();
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

    @Override
    public double evaluateAP() {
        double sum = 0;
        int numberofRecords = 0;

        for (int i = 0 ; i < retriveList.rtuples.size(); i++) {
            if (sampledData.contains(retriveList.rtuples.get(i).docName) && (reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName))) {
                if(i != 0)
                sum += (1 / (double) (i + 1)) + ((i) / (double) (i + 1)) * (rankData.get(i).dValue / (double) (i)) * ((rankData.get(i).relDocNo + .01)
                        / (rankData.get(i).irrelDocNo + rankData.get(i).relDocNo + 2 * .01));
                else
                    sum += (1 / (double) (i + 1));
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
    static final double val = Math.sqrt(2 * 3.14);
    double h;
    double sigma;

    public InferredApKDE(String qrelString, int maxIter, String run, Evaluator eval, IndexReader reader, double percentage, double h, double sigma) throws Exception {

        super(qrelString, maxIter, run, eval, reader, percentage);
        KDEValues = computeKde(this.sampledData, this.retriveList.pool, this.reader, Integer.parseInt(this.qrelno), this.reldocList.perQuerydocCosineSim);
        h = this.h;
        sigma = this.sigma;
    }

    public HashMap<String, Double> computeKde(Set<String> judgedRel, ArrayList<String> unjudged, IndexReader reader, int qid, HashMap<String, Double> docPairCosineMap) throws IOException {
        Iterator it = unjudged.iterator();
        HashMap<String, Double> estmatedList = new HashMap<String, Double>();
        double score = 0;
        for (String docid : unjudged) {

            score = 0;
            String docidair = "";
            double sim;
            for (String docId2 : judgedRel) {
                sim = 0;
                try {
                    docidair = docid + docId2;
                    sim = 0;
                    if (docPairCosineMap.containsKey(docidair)) {
                        sim = docPairCosineMap.get(docidair);
                    }
                    score += Math.exp(-((1 - sim) * (1 - sim)) / 2);
                } catch (Exception e) {

                    sim = 0;
                    score += Math.exp(-((1 - sim) * (1 - sim)) / 2);
                }
            }
            score = score / judgedRel.size();
            score = score / val;

            estmatedList.put(docid, score);
        }
        return estmatedList;
    }

    @Override
    public double evaluateAP() {
        double sum = 0;
        int numberofRecords = 0;
        for (int i = 1; i < retriveList.rtuples.size(); i++) {
            if (sampledData.contains(retriveList.rtuples.get(i).docName) && (reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName))) {
                sum += (1 / (double) (i + 1));
                for (int j = 0; j < i; j++) {
                    if (!sampledData.contains(retriveList.rtuples.get(j).docName) && ((reldocList.relMap.containsKey(retriveList.rtuples.get(j).docName)) || (reldocList.irrelMap.containsKey(retriveList.rtuples.get(j).docName)))) {
                        try {
                            sum += (1 / (double) (j + 1)) * (rankData.get(j).dValue / (double) (j + 1)) * KDEValues.get(retriveList.rtuples.get(j).docName);
                        } catch (Exception e) {
                            sum += 0;
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

    public EvaluateAll(Properties prop) throws IOException, Exception {
        super(prop);
        runApMap = new HashMap<>();
        this.runFileList = prop.getProperty("run.file");
        this.resultFolderPath = prop.getProperty("resultFolderLocation");
        this.runFileFolderPath = prop.getProperty("runfileFolderLocation");
    }

    public void computeMeanAp() throws FileNotFoundException, Exception {
        FileReader fr = new FileReader(new File(runFileList));
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        while (line != null) {

            retRcds.resFile = runFileFolderPath + "/" + line;
            retRcds.load();
            double apValue = evaluateQueries(.30);
            System.out.println(apValue);
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
            // System.out.println(runApMap.get(run));
            bw.newLine();
        }

        bw.close();
    }

    public void storeRunQid() throws IOException, Exception {
        FileReader fr = new FileReader(new File(runFileList));
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        while (line != null) {
            retRcds.resFile = runFileFolderPath + "/" + line;
            retRcds.load();
            String storeFileName = resultFolderPath + "/" + line;
            double apValue = evaluateQueries(.30);
            storeApValues(storeFileName);
            line = br.readLine();
        }
        br.close();
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
    String mode;
    String cosineSimilarityFile;
    Properties prop;
    String flag;
    double h;
    double sigma;

    public Evaluator(String qrelsFile, String resFile, String indexPath, String mode, String cosineSimilarityFile, Properties prop) throws IOException {
        reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
        mode = this.mode;
        relRcds = new AllRelRcds(qrelsFile, reader, cosineSimilarityFile, mode);
        retRcds = new AllRetrievedResults(resFile);
        qidApMap = new HashMap<>();
        this.prop = prop;
        flag = prop.getProperty("flag");
        if (flag.equals("1")) {
            h = Double.parseDouble(prop.getProperty("h"));
            sigma = Double.parseDouble(prop.getProperty("sigma"));
        } else {
            h = -1;
            sigma = -1;
        }
    }

    public Evaluator(Properties prop) throws Exception {
        String qrelsFile = prop.getProperty("qrels.file");
        String resFile = prop.getProperty("res.file");
        mode = prop.getProperty("mode");
        reader = DirectoryReader.open(FSDirectory.open(new File(prop.getProperty("index.file"))));
        relRcds = new AllRelRcds(qrelsFile, reader, prop.getProperty("cosine.file"), mode);
        retRcds = new AllRetrievedResults(resFile);
        startQid = Integer.parseInt(prop.getProperty("qid.start"));
        endQid = Integer.parseInt(prop.getProperty("qid.end"));
        qidApMap = new HashMap<>();
        flag = prop.getProperty("flag");
        this.prop = prop;
    }

    public void load() throws Exception {
        relRcds.load(startQid, endQid);
        retRcds.load();
    }

    public APComputer createAPEvaluator(String qid,int maxIter,double percentage) throws Exception {
        APComputer iapk;
        if (flag.equals("1")) {
            iapk = new InferredApKDE(qid, maxIter, "", this, reader, percentage, h, sigma);
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

        //System.out.println(sum);
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
            eval.computeMeanAp();
            eval.storeRunMeanAp(prop.getProperty("storeMeanAp"));
            // eval.storeRunQid();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
