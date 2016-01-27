/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Evaluator;

import KDE.KDEImplementation;
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
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author procheta
 */


// DG_Comments: Some top level comments: Split this BIG file into meaningful smaller ones, e.g. you might
// logically split into loading the datastructures for retrieval and relevance (separately) and then finally use them for computation in evaluation.
// Currently, it's too garbled up...
//
class PerQueryRelDocs {

    String qid;
    HashMap<String, Integer> relMap; // keyed by docid, entry stores the rel value
    HashMap<String, Integer> irrelMap; // keyed by docid, entry stores the irrel value
    ArrayList<String> pool; // list of all the docs
    int numRel;

    public PerQueryRelDocs(String qid) {
        this.qid = qid;
        numRel = 0;
        relMap = new HashMap<>();
        irrelMap = new HashMap<>();
        pool = new ArrayList<>();
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

    public HashMap<String, Double> precomputeCosineSim(String indexPath) throws IOException, ParseException {

        HashMap<String, Double> perQuerydocCosineSim = new HashMap<>();  // +++DG_Comments: Make perQuerydocCosineSim a member of this class. No point of returning. Keep this stored
		// as a part of this object which can then be used later on.

        Iterator it = irrelMap.keySet().iterator();
        //System.out.println(infAp.reldocList.irrelMap.size());
        //  System.out.println(infAp.reldocList.relMap.size());

		// +++DG_Comments: Change the loops to the more readable for (String relDocId: relMap.keySet()) {... for (String nrelDocId: irrelMap.keySet()) { ...  
        while (it.hasNext()) {
            String docid1 = (String) it.next();
            //  System.out.println(docid1);
            DocVector doc1 = new DocVector(docid1, indexPath);  //DG_Comments: Make the IndexReader object a member of this class. IndexSearcher can be obtained with a method call (I guess). Ensure the searcher is set from AllRelRcds. Create IndexReader once in AllRelRcds and pass it around in each PerQueryRelDocs.
            Iterator it2 = relMap.keySet().iterator();  
            while (it2.hasNext()) {
                String docid2 = (String) it2.next();
                DocVector doc2 = new DocVector(docid2, indexPath);
                perQuerydocCosineSim.put(docid1 + "#" + docid2, doc1.computeCosineSimilarity(doc2));

            }

        }
        it = relMap.keySet().iterator();
        while (it.hasNext()) {
            String docid1 = (String) it.next();
            //  System.out.println(docid1);
            DocVector doc1 = new DocVector(docid1, indexPath);
            Iterator it2 = relMap.keySet().iterator();
            while (it2.hasNext()) {
                String docid2 = (String) it2.next();
                DocVector doc2 = new DocVector(docid2, indexPath);
                perQuerydocCosineSim.put(docid1 + "#" + docid2, doc1.computeCosineSimilarity(doc2));
            }
        }

        return perQuerydocCosineSim;

    }
}

class AllRelRcds {

    String qrelsFile;
    HashMap<String, PerQueryRelDocs> perQueryRels;
    int totalNumRel;

    public AllRelRcds(String qrelsFile) {
        this.qrelsFile = qrelsFile;
        perQueryRels = new HashMap<>();
        totalNumRel = 0;
    }

    int getTotalNumRel() {
        if (totalNumRel > 0) {
            return totalNumRel;
        }

        for (Map.Entry<String, PerQueryRelDocs> e : perQueryRels.entrySet()) {
            PerQueryRelDocs perQryRelDocs = e.getValue();
            totalNumRel += perQryRelDocs.numRel;
        }
        return totalNumRel;
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

    void storeRelRcd(String line) {
        String[] tokens = line.split("\\s+");
        String qid = tokens[0];
        PerQueryRelDocs relTuple = perQueryRels.get(qid);
        if (relTuple == null) {
            relTuple = new PerQueryRelDocs(qid);
            perQueryRels.put(qid, relTuple);
        }
        relTuple.addTuple(tokens[2], Integer.parseInt(tokens[3]));
    }

    public void storeCosineSimilarity(String fileName, int startQrelNo, int endQrelNo, String indexPath) throws IOException, ParseException {
        FileWriter fw = new FileWriter(new File(fileName));
        BufferedWriter bw = new BufferedWriter(fw);
        for (int qid = startQrelNo; qid < endQrelNo; qid++) {

            PerQueryRelDocs perqd = perQueryRels.get(qid);
            HashMap<String, Double> cosinemap = perqd.precomputeCosineSim(indexPath);
            Iterator it = cosinemap.keySet().iterator();
            while (it.hasNext()) {
                String docPair = (String) it.next();
                String st[] = docPair.split("#");
                bw.write(st[0] + " " + st[1] + " " + cosinemap.get(docPair));
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

    PerQueryRelDocs getRelInfo(String qid) {
        return perQueryRels.get(qid);
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
    List<ResultTuple> rtuples;   // +++DG_Comments: You can take ArrayList here
    int numRelRet;
    float avgP;
    PerQueryRelDocs relInfo;

    public RetrievedResults(String qid) {
        this.qid = Integer.parseInt(qid);
        this.rtuples = new ArrayList<>(1000);
        avgP = -1;
        numRelRet = -1;
    }

    void addTuple(String docName, int rank) {
        rtuples.add(new ResultTuple(docName, rank));
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
        this.relInfo = relInfo;
    }

    float computeAP() {
        if (avgP > -1) {
            return avgP;
        }

        float prec = 0;
        int numRel = relInfo.numRel;
        int numRelSeen = 0;
        for (ResultTuple tuple : this.rtuples) {
            if (tuple.rel < 1) {
                continue;
            }
            numRelSeen++;
            prec += numRelSeen / (float) (tuple.rank);
        }
        numRelRet = numRelSeen;
        prec /= (float) numRel;
        this.avgP = prec;

        return prec;
    }

    float precAtTop(int k) {
        int numRelSeen = 0;
        int numSeen = 0;
        for (ResultTuple tuple : this.rtuples) {
            if (tuple.rel >= 1) {
                numRelSeen++;
            }
            if (++numSeen >= k) {
                break;
            }
        }
        return numRelSeen / (float) k;
    }

    float computeRecall() {
        if (numRelRet > -1) {
            return numRelRet;
        }
        int numRelSeen = 0;
        for (ResultTuple tuple : this.rtuples) {
            if (tuple.rel < 1) {
                continue;
            }
            numRelSeen++;
        }
        numRelRet = numRelSeen;
        return numRelSeen;
    }

    @Override
    public int compareTo(RetrievedResults that) {
        return this.qid < that.qid ? -1 : this.qid == that.qid ? 0 : 1;
    }
}

//+++DG_Comments: Class name with a lower case??!!
//There's no reason for this class to exist. Move the required (only) functions
//to InfAP class (or only if you need to the KDEInfAp class). I don't think you're
//doing anything KDE specific here. So, all you need is to move the functions to the InfAP class.
class meanInferredAp {

    HashMap<String, Double> meanInferApList;
    ArrayList<String> runfileList;
    HashMap<String, Double> actualApList;
    int startQrelno;
    int endQrelno;
    double rmsError;

    public meanInferredAp(int startQrelno, int endQrelNo, String runFileName) throws FileNotFoundException, IOException {

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

	//+++DG_Comments: You need this??
    public void listActualAp(Properties prop) {
        Evaluator eval = new Evaluator(prop);
        for (int i = startQrelno; i < endQrelno; i++) {
            Integer h = i;
            ActualAp aap = new ActualAp(h.toString(), eval);
            actualApList.put(h.toString(), aap.computeActualAp());

        }

    }

	//+++DG_Comments: Make this private
    public ArrayList getDocList(List<ResultTuple> ar) {
        ArrayList l = new ArrayList();
        for (int i = 0; i < ar.size(); i++) {
            l.add(ar.get(i).docName);
        }

        return l;
    }

	//+++DG_Comments: No KDE specific things here...
    public void computeMeanInferredApKDE(Properties prop, double percentage, int maxIter, String runfileName, HashMap<Integer, HashMap<String, Double>> h1, HashMap<Integer, HashMap<String, Double>> h2, Evaluator eval) throws Exception {

        String runFileLocation = prop.getProperty("runFileLocation");

        HashMap<String, HashMap<String, Double>> kdeMap = new HashMap<String, HashMap<String, Double>>();
        // eval.retRcds.resFile = runFileLocation + "/" + runfileName;
        HashMap<Integer, HashSet> sampleDataMap = new HashMap<>();
        HashMap<Integer, HashSet> relDocMap = new HashMap<>();
        HashMap<String, HashMap<String, Double>> cosineMap = new HashMap<>();

        for (int i = startQrelno; i <= 420; i++) {
            double sum = 0;
            Integer h = i;
            InferredAp iAp = new InferredAp(h.toString(), maxIter, runfileName, eval);
            iAp.sampling(percentage);
            iAp.processRetrievedResult();
            KDEImplementation kde = new KDEImplementation();
            IndexReader reader = DirectoryReader.open(FSDirectory.open(new File("/media/procheta/3CE80E12E80DCADA/newIndex2")));

            kdeMap.put(h.toString(), kde.calculateKde(iAp.reldocList.relMap.keySet(), getDocList(iAp.retriveList.rtuples), reader, i, h1, h2));
            // System.out.println("done "+i);
            sampleDataMap.put(h, iAp.sampledData);
            relDocMap.put(h, iAp.reldoc);

            reader.close();
        }
        System.out.println("done");
        // System.out.iAp.println(kdeMap);
        //System.out.println("");
        for (int i = startQrelno; i <= 420; i++) {
            double sum = 0;
            Integer h = i;

            InferredApKDE iAp = new InferredApKDE(h.toString(), maxIter, runfileName, eval);
            iAp.sampledData = sampleDataMap.get(h);
            iAp.reldoc = relDocMap.get(h);
            iAp.processRetrievedResult();
            KDEImplementation kde = new KDEImplementation();
            sum = iAp.computeInferredAp(kdeMap.get(h.toString())); // +++DG_Comments: Why store in hashmap and get here? Same loop is repeated. Why not just print the values? No need for containers to store them.
            System.out.println("Qid No " + i + " Inferred Ap value " + sum);
            meanInferApList.put(h.toString(), sum);//*/

        }//*/

    }

    public void computeMeanInferredAp(Properties prop, int percentage, int maxIter) throws Exception {

        Evaluator eval = new Evaluator(prop);
        String runFileLocation = prop.getProperty("runFileLocation");
        AllRunRetrievedResults alr = new AllRunRetrievedResults(prop.getProperty("run.file"), runFileLocation);
        eval.load();
        System.out.println("done");
        for (int i = startQrelno; i <= endQrelno; i++) {
            double sum = 0;
            Integer h = i;

            InferredAp iAp = new InferredAp(h.toString(), maxIter, "input.kdd8sh16", eval);
            iAp.sampling(percentage);
            eval.retRcds.resFile = runFileLocation + "/input.kdd8sh16";
            eval.retRcds = alr.allRunRetMap.get(runFileLocation + "/input.kdd8sh16");
            iAp.processRetrievedResult();
            System.out.println("Qid No " + i + " Inferred Ap value " + sum);
            meanInferApList.put(h.toString(), sum);//*/

        }

    }

    public void storeMeanInferredAp(String FileName, ArrayList runFileList) throws IOException {
        FileWriter fw = new FileWriter(new File(FileName));
        BufferedWriter bw = new BufferedWriter(fw);

        for (int count = 0; count < runFileList.size(); count++) {
            Integer h = count;

			//+++DG_Comments: If all you're doing is just printing the values,, why do you
			//bother saving them in a list. 
            Double infApValue = meanInferApList.get(runFileList.get(count));

            bw.write(runFileList.get(count) + "  " + infApValue.toString());
            bw.newLine();
        }
        bw.close();
    }

}

// DG_Comments: Why do you need a separate class for this?? Remove
class ActualAp {

    PerQueryRelDocs reldocList;
    RetrievedResults retriveList;
    String qrelno;
    String runNo;
    HashMap<Integer, Integer> relDocNo;
    HashMap<Integer, Integer> irrelDocNo;
    HashMap<Integer, inferredApCalData> rankData;
    HashSet<String> sampledData;

    public ActualAp(String qrelString, Evaluator eval) {
        this.reldocList = eval.relRcds.perQueryRels.get(qrelString);
        this.retriveList = eval.retRcds.allRetMap.get(qrelString);
        rankData = new HashMap<>();
    }

    public void computeRmserror(double[] a, double[] b) {

        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] - b[i]) * (a[i] - b[i]);
        }
        sum /= a.length;
        sum = Math.sqrt(sum);
        System.out.println("Rms " + sum);
    }

    public void sampling(int percentage) {

        Random ran = new Random();
        ArrayList<String> pool = reldocList.pool;
        int iter = 0;
        int rel_exists = 0;
        int irrel_exists = 0;
        int count = 0;
        int sampleSize = pool.size() * percentage;
        while (iter < 5) {

            count = 0;
            while (count < sampleSize) {
                int random = ran.nextInt(pool.size());
                sampledData.add(pool.get(random));
                if (reldocList.relMap.containsKey(pool.get(random))) {
                    rel_exists = 1;
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
        int r = 0, n = 0, d = 0;

        for (int i = 0; i < retriveList.rtuples.size(); i++) {

            if (reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName)) {
                r++;
                inferredApCalData iapc = new inferredApCalData(r, i, d);
                rankData.put(i, iapc);
            } else if (reldocList.irrelMap.containsKey(retriveList.rtuples.get(i).docName)) {
                n++;
                inferredApCalData iapc = new inferredApCalData(r, i, d);
                rankData.put(i, iapc);
            } else {
                d++;
                inferredApCalData iapc = new inferredApCalData(r, i, d);
                rankData.put(i, iapc);

            }

        }
    }

    public double computeActualAp() {

        double sum = 0;
        int numberofRecords = 0;

        for (int i = 1; i < retriveList.rtuples.size(); i++) {
            if (reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName)) {
                sum += (1 / (double) (i + 1)) + ((i) / (double) (i + 1)) * (rankData.get(i).dValue / (double) (i)) * ((rankData.get(i).relDocNo + .01) / (rankData.get(i).irrelDocNo + rankData.get(i).relDocNo + 2 * .01));

                numberofRecords++;

            }

        }

        if (numberofRecords == 0) {
            return 0;
        } else {
            return sum / numberofRecords;
        }
    }

    public HashMap<String, Double> computeMeanActualAp(Properties prop, int startQid, int endQid, ArrayList runFileList) throws Exception {

        Evaluator eval = new Evaluator(prop);
        String runFileLocation = prop.getProperty("runFileLocation");
        AllRunRetrievedResults alr = new AllRunRetrievedResults(prop.getProperty("run.file"), runFileLocation);
        eval.load();
        HashMap<String, Double> meanActualApMap = new HashMap<>();
        for (int j = 0; j < runFileList.size(); j++) {
            eval.retRcds.resFile = runFileLocation + runFileList.get(j);
            eval.retRcds = alr.allRunRetMap.get(runFileList.get(j));
            double sum = 0;
            for (int i = startQid; i <= endQid; i++) {

                Integer h = i;
                ActualAp ap = new ActualAp(h.toString(), eval);
                ap.processRetrievedResult();
                sum += ap.computeActualAp();
            }

            sum /= runFileList.size();
            meanActualApMap.put((String) runFileList.get(j), sum);//*/

        }

        return meanActualApMap;
    }

    public void storeActualAp(String FileName, int startQid, int endQid, ArrayList runFileList, Properties prop) throws IOException, Exception {
        FileWriter fw = new FileWriter(new File(FileName));
        BufferedWriter bw = new BufferedWriter(fw);

        HashMap<String, Double> meanActualApMap = computeMeanActualAp(prop, startQid, endQid, runFileList);
        // System.out.println(runFileList);

        for (int count = 0; count < runFileList.size(); count++) {
            Integer h = count;
            Double infApValue = meanActualApMap.get(runFileList.get(count));

            bw.write(runFileList.get(count) + "  " + infApValue.toString());
            bw.newLine();
        }
        bw.close();
    }
}

//+++DG_Comments: Check if you need this class....
class inferredApCalData {

    int relDocNo;
    int irrelDocNo;
    int dValue;

    public inferredApCalData(int relDocNo, int irrelDocNo, int dValue) {

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
    HashMap<Integer, inferredApCalData> rankData;
    String qrelno;
    String runNo;
    HashSet reldoc;

    public InferredAp(String qrelString, int maxIter, String run, Evaluator eval) throws Exception {
        this.qrelno = qrelString;
        this.maxIter = maxIter;
        sampledData = new HashSet<>();

        this.runNo = run;
        // System.out.println(qrelString);
        this.reldocList = eval.relRcds.perQueryRels.get(qrelString);
        //  System.out.println(qrelString);
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

                rankData.put(i, new inferredApCalData(r, n, d));

            } else if (sampledData.contains(retriveList.rtuples.get(i).docName) && reldocList.irrelMap.containsKey(retriveList.rtuples.get(i).docName)) {

                n++;
                rankData.put(i, new inferredApCalData(r, n, d));

            } else {
                if (!reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName) && !reldocList.irrelMap.containsKey(retriveList.rtuples.get(i).docName)) {
                    d++;
                }
                rankData.put(i, new inferredApCalData(r, n, d));

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
        for (int i = 1; i < retriveList.rtuples.size(); i++) {
            if (sampledData.contains(retriveList.rtuples.get(i).docName) && (reldocList.relMap.containsKey(retriveList.rtuples.get(i).docName))) {

                // sum += (1 / (double) (i + 1)) + ((i) / (double) (i + 1)) * (rankData.get(i).relDocNo / (double) (i)) * ((rankData.get(i).irrelDocNo + .01)
                //    / (rankData.get(i).irrelDocNo + rankData.get(i).dValue + 2 * .01));
                sum += (1 / (double) (i + 1));
                for (int j = 0; j < i; j++) {
                    if (!sampledData.contains(retriveList.rtuples.get(j).docName) && ((reldocList.relMap.containsKey(retriveList.rtuples.get(j).docName)) || (reldocList.irrelMap.containsKey(retriveList.rtuples.get(j).docName)))) {
                        try {
                            sum += (1 / (double) (j + 1)) * (rankData.get(j).dValue / (double) (j + 1)) * KDEValues.get(retriveList.rtuples.get(j).docName);
                        } catch (Exception e) {
                            System.out.println(retriveList.rtuples.get(j).docName);
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

class AllRunRetrievedResults {

    HashMap<String, AllRetrievedResults> allRunRetMap;
    String RunfileList;  // DG_Comments: Variable names begin with lower-case

    public AllRunRetrievedResults(String runFileName, String location) throws FileNotFoundException, IOException { 
		//DG_Comments: Write a private load function and call from the constructor or make it public and call from the caller.
		//Don't put everything in the constructor.
        this.RunfileList = runFileName;
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

//+++DG_Comments: Are you sure you need this class?
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

    void storeRetRcd(String line) {
        String[] tokens = line.split("\\s+");
        String qid = tokens[0];
        RetrievedResults res = allRetMap.get(qid);
        if (res == null) {
            res = new RetrievedResults(qid);
            allRetMap.put(qid, res);
        }
        res.addTuple(tokens[2], Integer.parseInt(tokens[3]));
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (Map.Entry<String, RetrievedResults> e : allRetMap.entrySet()) {
            RetrievedResults res = e.getValue();
            buff.append(res.toString()).append("\n");
        }
        return buff.toString();
    }

    public void fillRelInfo(AllRelRcds relInfo) {
        for (Map.Entry<String, RetrievedResults> e : allRetMap.entrySet()) {
            RetrievedResults res = e.getValue();
            PerQueryRelDocs thisRelInfo = relInfo.getRelInfo(String.valueOf(res.qid));
            res.fillRelInfo(thisRelInfo);
        }
        this.allRelInfo = relInfo;
    }

    String computeAll() {
        StringBuffer buff = new StringBuffer();
        float map = 0f;
        float gm_ap = 0f;
        float avgRecall = 0f;
        float numQueries = (float) allRetMap.size();
        float pAt5 = 0f;

        for (Map.Entry<String, RetrievedResults> e : allRetMap.entrySet()) {
            RetrievedResults res = e.getValue();
            float ap = res.computeAP();
            map += ap;
            gm_ap += Math.log(ap);
            avgRecall += res.computeRecall();
            pAt5 += res.precAtTop(5);
        }

        buff.append("recall:\t").append(avgRecall / (float) allRelInfo.getTotalNumRel()).append("\n");
        buff.append("map:\t").append(map / numQueries).append("\n");
        buff.append("gmap:\t").append((float) Math.exp(gm_ap / numQueries)).append("\n");
        buff.append("P@5:\t").append(pAt5 / numQueries).append("\n");

        return buff.toString();
    }
}

public class Evaluator {

    AllRelRcds relRcds;
    AllRetrievedResults retRcds;

    public Evaluator(String qrelsFile, String resFile) {
        relRcds = new AllRelRcds(qrelsFile);
        retRcds = new AllRetrievedResults(resFile);
    }

	//+++DG_Comments: Remove dead code! doSampling is never called!
    public void doSampling(String Filename, double percentage, int maxIter) throws IOException, Exception {
        FileWriter fw = new FileWriter(new File(Filename));
        BufferedWriter bw = new BufferedWriter(fw);
        Iterator it = relRcds.perQueryRels.keySet().iterator();
        Integer i = 401;
        while (it.hasNext()) {
            String qid = (String) it.next();
            InferredAp iap = new InferredAp(i.toString(), maxIter, "", this);

            iap.sampling(percentage);
            Iterator it2 = relRcds.perQueryRels.get(i.toString()).pool.iterator();
            while (it2.hasNext()) {
                String st = (String) it2.next();
                if (relRcds.perQueryRels.get(i.toString()).irrelMap.containsKey(st) && iap.sampledData.contains(st)) {
                    System.out.println(i.toString() + " " + "0" + " " + st + " " + "0");
                    bw.write(i.toString() + " " + "0" + " " + st + " " + "0");
                } else if (relRcds.perQueryRels.get(i.toString()).relMap.containsKey(st) && iap.sampledData.contains(st)) {
                    System.out.println(i.toString() + " " + "0" + " " + st + " " + "1");
                    bw.write(i.toString() + " " + "0" + st + " " + "1");
                } else if (relRcds.perQueryRels.get(i.toString()).relMap.containsKey(st) || relRcds.perQueryRels.get(i.toString()).irrelMap.containsKey(st)) {
                    System.out.println(i.toString() + " " + "0" + " " + st + " " + "-1");
                    bw.write(i.toString() + " " + "0" + " " + st + " " + "-1");
                }

            }
            // bw.write(qid + " "+ "0"+);

            i++;

        }
        bw.close();

    }

    public Evaluator(Properties prop) {

        String qrelsFile = prop.getProperty("qrels.file");
        String resFile = prop.getProperty("res.file");
        relRcds = new AllRelRcds(qrelsFile);
        retRcds = new AllRetrievedResults(resFile);
    }

    public void load() throws Exception {
        relRcds.load();
        retRcds.load();
    }

    public void fillRelInfo() {
        retRcds.fillRelInfo(relRcds);
    }

    public String computeAll() {
        return retRcds.computeAll();
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
            args[0] = "/home/procheta/NetBeansProjects/TrecDocParse/src/evaluator/init.properties";
        }
        try {
            Properties prop = new Properties();
            prop.load(new FileReader(args[0]));

			//+++DG_Comments: delegate the responsibility of reading the properties values to respective classes.
			//Make sure you just pass around the properties object.
            String qrelsFile = prop.getProperty("qrels.file");
            String resFile = prop.getProperty("res.file");
            String runFileList = prop.getProperty("run.file");
            String runFileLocation = prop.getProperty("runFileLocation");


			//+++DG_Comments: For this piece of code to work, you need to make KDEInfAp a special case (extended class)
			// of InfAP. Assuming then that the function computeAP is overrideen in the KDEInfAp class, you will then be able
			// to call like this...
            // boolean kdeMode = Boolean.parseBoolean(prop.getProperty("kdeinf", "false"));
			// InfAP infAPEvaluator = mode==true? new KDEInfAp(...) : new InfAP(...);
			// infAPEvaluator.computeAP();
			//
			// In fact, Evaluator should be your base class. Both InfAp and KDEInfAp are to be derived from it.
			// This will avoid passing around the Evaluator object to these two objects (see your own call in this main function).
			// InfAp IS-A Evaluator rather than InfAp HAS-A Evaluator
            String mode = prop.getProperty("mode");
            if (mode.equals("")) {
            } else {

            }



			//+++DG_Comments: This needs to be read from properties as well. I guess you would need to pass the
			//properties to the AllRelRcds because you would need the 
            IndexReader reader = DirectoryReader.open(FSDirectory.open(new File("/media/procheta/3CE80E12E80DCADA/newIndex2"))); // DG_Comments: This reader is not USED!!

            Evaluator evaluator = new Evaluator(qrelsFile, resFile);
            evaluator.load();
            //  evaluator.storeCosineSimilarity(401, 450, evaluator, "input.kdd8sh16", "/home/procheta/Documents/Store2.txt", reader);
            meanInferredAp meanInAp = new meanInferredAp(401, 450, runFileList);


			// +++DG_Comments: This is really bad coding style. There should be no requirement to pass around raw container
			// objects like this among the objects. If you see something like this, revisit your design!
			// As I said, store the pre-computed distances as a part of the PerQueryRelDocs themselves.
			// With the HAS-A changed to IS-A you won't need to load/store the computed values from the main.

            HashMap<Integer, HashMap<String, Double>> h1 = evaluator.relRcds.loadCosineValue("/home/procheta/Documents/Store.txt");
            HashMap<Integer, HashMap<String, Double>> h2 = evaluator.relRcds.loadCosineValue("/home/procheta/Documents/Store1.txt");

            reader.close();
            //meanInferredAp meanInAp = new meanInferredAp(401, 450, runFileList);
            meanInAp.computeMeanInferredApKDE(prop, .30, 5, "input.kdd8sh16", h1, h2, evaluator); // +++DG_Comments: Just pass prop. Nothing else!

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
