/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dl4jtest;

import com.google.common.collect.HashBiMap;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.deeplearning4j.bagofwords.vectorizer.BagOfWordsVectorizer;
import org.deeplearning4j.bagofwords.vectorizer.TextVectorizer;
import org.deeplearning4j.datasets.creator.MnistDataSetCreator;
import org.deeplearning4j.datasets.fetchers.BaseDataFetcher;
import org.deeplearning4j.datasets.iterator.BaseDatasetIterator;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RBM;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.text.sentenceiterator.LuceneSentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.UimaTokenizerFactory;
import org.jblas.util.Random;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 *
 * @author Debasis
 */
public class Doc2VecWithAutoEncoder {

    ArrayList<String> docIds;
    ArrayList<String> labels;

    public Doc2VecWithAutoEncoder() {
        docIds = new ArrayList<>();
        labels = new ArrayList<>();

    }

    // ++Procheta
    public void getDocIds(String qid, String qrelPath) throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(new File(qrelPath));
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        int startflag = 0;

        while (line != null) {
            String st[] = line.split(" ");
            if (startflag == 0 && st[0].equals(qid)) {
                startflag = 1;
            }
            if (startflag == 1 && st[0].equals(qid)) {
                docIds.add(st[2]);
                labels.add(st[3]);
            }
            if (startflag == 1 && !st[0].equals(qid)) {
                break;
            }
            line = br.readLine();
        }

    }

    // ++ Procheta This function will save the output of a layer
    public void saveModel(DataSetIterator iter, String outputFilename, MultiLayerNetwork model) throws IOException {
        FileWriter fw = new FileWriter(new File(outputFilename));
        BufferedWriter bw = new BufferedWriter(fw);
        while (iter.hasNext()) {
            DataSet v = iter.next();
            INDArray st = model.output(v.getFeatures());
            bw.write(st.toString());
            bw.newLine();
        }
        bw.close();
    }

    //++Procheta
    public ArrayList<String> subsample(int depth, String filePath, String qid,String folderLocation) throws FileNotFoundException, IOException {

        FileReader fr = new FileReader(new File(filePath));
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        HashSet<String> docids = new HashSet<String>();
        while (line != null) {

            FileReader fr2 = new FileReader(new File(folderLocation + line));
            BufferedReader br2 = new BufferedReader(fr2);
            String line2 = br2.readLine();
            int startFlag = 0;
            int i = 0;
            while (line2 != null) {
                String st[] = line2.split("	");
                if (st[0].equals(qid)) {
                    startFlag = 1;
                }
                if (startFlag == 1 && st[0].equals(qid) && i < depth) {
                    docids.add(st[2]);
                    i++;
                }
                if (startFlag == 1 && !st[0].equals(qid)) {
                    break;
                }

                line2 = br2.readLine();
            }
            line = br.readLine();

        }

        ArrayList<String> docs = new ArrayList<>();
        Iterator it = docids.iterator();
        while (it.hasNext()) {
            docs.add((String) it.next());
        }
        System.out.println(docids.size());
        ArrayList<String> sampledDocs = new ArrayList<>();
        int poolsize = (int) (docids.size() * .80);
        Random rn = new Random();
        System.out.println(poolsize);
        for (int i = 0; i < poolsize; i++) {
            int num = rn.nextInt(docids.size()) + 1;
            
            sampledDocs.add(docs.get(num));
        }
        return sampledDocs;

    }

    // +++Procheta 
    // saving the sampled Docids
    public void saveSampleDocId(ArrayList docIds, String fileName) throws IOException {
        FileWriter fw = new FileWriter(new File(fileName));
        System.out.println(docIds.size());
        BufferedWriter bw = new BufferedWriter(fw);
        for (int i = 0; i < docIds.size(); i++) {
            bw.write((String) docIds.get(i));
            bw.newLine();
        }
        bw.close();
        fw.close();

    }
    
    // A small unit test for testing the vectorization
    public static void main(String[] args) throws FileNotFoundException, IOException {

        if (args.length < 1) {
            args = new String[1];
            args[0] = "/home/procheta/NetBeansProjects/Dl4jTest/src/dl4jtest/init.properties";
        }
        String[] docs = {
            "The cat sat on the mat",
            "The dog sat on the mat",
            "The chicken ate the corn",
            "The corn was sweet",
            "The milk was sweet",
            "The dog sat on the mat",
            "The cat drank the milk",
            "The dog ate the bone"
        };

        try {
            Properties prop = new Properties();
            prop.load(new FileReader(args[0]));
            LuceneDocFetcher luceneDocFetcher;

            // test loading a simple collection of docs...
            // Create in-memory index
            RAMDirectory ramdir = new RAMDirectory();

            IndexWriterConfig iwcfg = new IndexWriterConfig(new EnglishAnalyzer());
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(ramdir, iwcfg);
            for (String doc : docs) {
                try {
                    Document lDoc = new Document();
                    lDoc.add(new Field(LuceneDocFetcher.CONTENET_FIELD_NAME,
                            doc, Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES));
                    writer.addDocument(lDoc);
                } catch (Exception e) {
                }
            }
            writer.close();
            Path path = Paths.get(prop.getProperty("index"));
            Directory dir = FSDirectory.open(path);

            Doc2VecWithAutoEncoder dva = new Doc2VecWithAutoEncoder();
            System.out.println(prop.getProperty("depth"));
           ArrayList<String> docIds;
            docIds = dva.subsample(Integer.parseInt(prop.getProperty("depth")), prop.getProperty("fileList"), prop.getProperty("qid"), prop.getProperty("folderPath"));
            dva.getDocIds(prop.getProperty("qid"), prop.getProperty("qrel"));
            dva.saveSampleDocId(docIds, prop.getProperty("sampleOutput"));
            // pass the in-mem index reader to the vectorizer
           /* luceneDocFetcher = new LuceneDocFetcher(dir, dva.docIds);
            System.out.println("done");
            DataSetIterator iter = new BaseDatasetIterator(1, dva.docIds.size(), luceneDocFetcher);
            while (iter.hasNext()) {
                DataSet v = iter.next();

                System.out.println(v.getFeatures());
            }

            // test auto-encoding
            final int vocabSize = luceneDocFetcher.getDimension();
            //int seed = Random.nextInt(vocabSize);
            int iterations = 2;
            int listenerFreq = iterations / 5;

            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    //.seed(seed)
                    .iterations(iterations)
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .list(2)
                    .layer(0, new RBM.Builder().nIn(vocabSize).nOut(5).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
                    .layer(1, new RBM.Builder().nIn(5).nOut(10).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
                    //.pretrain(true)
                    //.backprop(true)

                    //.layer(2, new RBM.Builder().nIn(500).nOut(250).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
                    //.layer(3, new RBM.Builder().nIn(250).nOut(100).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
                    //.layer(4, new RBM.Builder().nIn(100).nOut(30).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build()) 

                    /*
                     //encoding stops
                     .layer(5, new RBM.Builder().nIn(30).nOut(100).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build()) 	

                     //decoding starts
                     .layer(6, new RBM.Builder().nIn(100).nOut(250).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
                     .layer(7, new RBM.Builder().nIn(250).nOut(500).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
                     .layer(8, new RBM.Builder().nIn(500).nOut(1000).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
                     .layer(9, new OutputLayer.Builder(LossFunctions.LossFunction.RMSE_XENT).nIn(1000).nOut(vocabSize).build())
                     .pretrain(true).backprop(true)
                     */
                 /*   .build();

            MultiLayerNetwork model = new MultiLayerNetwork(conf);
            model.init();

            model.setListeners(Arrays.asList((IterationListener) new ScoreIterationListener(listenerFreq)));
            model.fit(iter);

            System.out.println("Output layer: ");
            iter.reset();
            while (iter.hasNext()) {
                DataSet v = iter.next();

                // System.out.println(model.output(v.getFeatures()));
            }
            //++Procheta
            iter.reset();
            dva.saveModel(iter, prop.getProperty("output"), model);*/
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
