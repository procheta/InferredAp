/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Dl4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.deeplearning4j.datasets.fetchers.BaseDataFetcher;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

/**
 *
 * @author Debasis
 */
class TermInfo {

    String term;
    int id;
    int tf;

    public TermInfo(String term, int id) {
        this.term = term;
        this.id = id;
    }
}

// Fetch the document text from an existing Lucene index
public class LuceneDocFetcher extends BaseDataFetcher {

    DataSet dataSet;

    int globalTermId;
    Map<String, Integer> termSeen;
    List<Map<String, TermInfo>> docWordMaps;

    public static final String CONTENET_FIELD_NAME = "words";

    protected int getTermId(String term) {
        if (termSeen.containsKey(term)) {
            return termSeen.get(term);
        }
        int termId = globalTermId++;
        termSeen.put(term, termId);
        return termId;
    }

    protected Map<String, TermInfo> buildTerms(IndexReader reader, int docId) throws Exception {
        Map<String, TermInfo> wmap = new HashMap<>();
        Terms tfvector;
        TermsEnum termsEnum;
        String termText;
        BytesRef term;

        tfvector = reader.getTermVector(docId, CONTENET_FIELD_NAME);

        // Construct the normalized tf vector
        termsEnum = tfvector.iterator(); // access the terms for this field
        while ((term = termsEnum.next()) != null) { // explore the terms for this field
            termText = term.utf8ToString();

            TermInfo termInfo = wmap.get(termText);
            if (termInfo == null) {
                termInfo = new TermInfo(termText, getTermId(termText));
            }
            termInfo.tf++;
            wmap.put(termText, termInfo);
        }

        return wmap;
    }

    DataSet constructTermVector(Map<String, TermInfo> docWordMap) {
        INDArray onehotDocVec = Nd4j.create(1, globalTermId);
        INDArray labels = Nd4j.create(1, 1);

        for (Map.Entry<String, TermInfo> e : docWordMap.entrySet()) {
            TermInfo termInfo = e.getValue();
            onehotDocVec.putScalar(termInfo.id, 1); // set present terms as 1, rest all 0s
        }
        return new DataSet(onehotDocVec, labels);
    }

    //++Procheta
    //TermVector with labels
    DataSet constructTermVector(Map<String, TermInfo> docWordMap, ArrayList<String> labels) {
        INDArray onehotDocVec = Nd4j.create(1, globalTermId);
        INDArray classLabels = Nd4j.create(1, labels.size());

        for (Map.Entry<String, TermInfo> e : docWordMap.entrySet()) {
            TermInfo termInfo = e.getValue();
            onehotDocVec.putScalar(termInfo.id, 1); // set present terms as 1, rest all 0s
        }
        for(int i = 0; i < labels.size();i++)
        {
            classLabels.putScalar(i, Double.parseDouble(labels.get(i)));
        }
        return new DataSet(onehotDocVec, classLabels);
    }

    // Iterate over every document in this index and vectorize each
    public LuceneDocFetcher(Directory dir, ArrayList<String> docIds) throws Exception {

        globalTermId = 0;
        termSeen = new HashMap<>();

        IndexReader reader = DirectoryReader.open(dir);
        // totalExamples = reader.numDocs();
        //++Procheta
        totalExamples = docIds.size();
        docWordMaps = new ArrayList<>(totalExamples);

        // build the per-doc word maps
        for (int i = 0; i < totalExamples; i++) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Similarity sm = new DefaultSimilarity();
            searcher.setSimilarity(sm);
            Analyzer analyzer = new KeywordAnalyzer();
            //System.out.println(id);
            QueryParser queryParser = new QueryParser("id", analyzer);
            Query query = queryParser.parse(docIds.get(i));
            TopDocs topDocs = searcher.search(query, 3);
            //System.out.println(query.toString());
            ScoreDoc[] hits = topDocs.scoreDocs;
            // System.out.println(hits.length);
            Document doc = searcher.doc(hits[0].doc);

            docWordMaps.add(buildTerms(reader, hits[0].doc));
        }

        // iterate through the word maps and build the one-hot vectors
        List<DataSet> allDocVecs = new ArrayList<>(totalExamples);
        for (Map<String, TermInfo> docwordMap : docWordMaps) {
            allDocVecs.add(constructTermVector(docwordMap));
        }

        // Merge all doc vecs into one dataset
        this.dataSet = DataSet.merge(allDocVecs);

        reader.close();
    }
    //++Procheta
    // Iterate over every document in this index and vectorize each with labels

    public LuceneDocFetcher(Directory dir, ArrayList<String> docIds, ArrayList<String> labels) throws Exception {

        globalTermId = 0;
        termSeen = new HashMap<>();

        IndexReader reader = DirectoryReader.open(dir);
        // totalExamples = reader.numDocs();
        //++Procheta
        totalExamples = docIds.size();
        docWordMaps = new ArrayList<>(totalExamples);

        // build the per-doc word maps
        for (int i = 0; i < totalExamples; i++) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Similarity sm = new DefaultSimilarity();
            searcher.setSimilarity(sm);
            Analyzer analyzer = new KeywordAnalyzer();
            //System.out.println(id);
            QueryParser queryParser = new QueryParser("id", analyzer);
            Query query = queryParser.parse(docIds.get(i));
            TopDocs topDocs = searcher.search(query, 3);
            //System.out.println(query.toString());
            ScoreDoc[] hits = topDocs.scoreDocs;
            // System.out.println(hits.length);
            Document doc = searcher.doc(hits[0].doc);

            docWordMaps.add(buildTerms(reader, hits[0].doc));
        }

        // iterate through the word maps and build the one-hot vectors
        List<DataSet> allDocVecs = new ArrayList<>(totalExamples);
        for (Map<String, TermInfo> docwordMap : docWordMaps) {
            allDocVecs.add(constructTermVector(docwordMap, labels));
        }

        // Merge all doc vecs into one dataset
        this.dataSet = DataSet.merge(allDocVecs);

        reader.close();
    }

    public int getDimension() {
        return globalTermId;
    }

    @Override
    public void fetch(int numExamples) {
        List<DataSet> newData = new ArrayList<>();
        for (int grabbed = 0; grabbed < numExamples && cursor < dataSet.numExamples(); cursor++, grabbed++) {
            newData.add(dataSet.get(cursor));
        }
        this.curr = DataSet.merge(newData);
    }
}
