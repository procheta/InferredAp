/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package KDE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

/**
 *
 * @author procheta
 *
 */
class cosineSimilarity implements java.io.Serializable {

    String firstdocid;
    String seconddocid;
    double cosineSimValue;

    public cosineSimilarity(String firstdocid, String seconddocid, double cosineSimValue) {
        this.firstdocid = firstdocid;
        this.seconddocid = seconddocid;
        this.cosineSimValue = cosineSimValue;
    }

}

public class KDEImplementation {

    public double computeCosineSimilarity(int index1, int index2, IndexReader reader) throws IOException {
        //System.out.println(index1);
        // System.out.println("index2 "+index2);
        Terms terms1 = reader.getTermVector(index1, "words");
        Terms terms2 = reader.getTermVector(index2, "words");
       // System.out.println("Terms" + terms1);

        //System.out.println(index1);
        // System.out.println(index2);
        TermsEnum termsEnum1 = null;
        TermsEnum termsEnum2 = null;
        termsEnum1 = terms1.iterator(null);

        double norm1 = 0;
        double norm2 = 0;
        BytesRef text1;
        BytesRef text2;

        text1 = termsEnum1.term();
        termsEnum2 = terms2.iterator(null);
        text2 = termsEnum2.term();
        // System.out.println("Text2"+ text2.utf8ToString());
        double sum = 0;

        while ((text1 = termsEnum1.next()) != null) {
            //    System.out.println("Text1 " + text1.utf8ToString());
            try {
                String term1 = text1.utf8ToString();
                String term2 = "";
                try {
                    term2 = text2.utf8ToString();
                } catch (Exception e) {
                    break;
                }
                int freq = (int) termsEnum1.totalTermFreq();
                norm1 = norm1 + freq * freq;
                if (term1.compareTo(term2) > 0) {
                    // if(it1.hasNext()) 
                    if ((text2 = termsEnum2.next()) != null) {

                        term2 = text2.utf8ToString();
                        freq = (int) termsEnum2.totalTermFreq();
                        norm2 = norm2 + freq * freq;
                    }
                }
                if (term1.compareTo(term2) < 0) {
                    if ((text1 = termsEnum1.next()) != null) {

                        term1 = text1.utf8ToString();
                        freq = (int) termsEnum1.totalTermFreq();
                        norm1 = norm1 + freq * freq;
                    }
                }

                if (term1.compareTo(term2) == 0) {
                    sum = sum + (int) termsEnum1.totalTermFreq() * (int) termsEnum2.totalTermFreq();;
                //  System.out.println(term1);
                    //System.out.println(sum);
                    if ((text1 = termsEnum1.next()) != null) {

                        term1 = text1.utf8ToString();
                        freq = (int) termsEnum1.totalTermFreq();
                        norm1 = norm1 + freq * freq;
                    }
                    if ((text2 = termsEnum2.next()) != null) {

                        term2 = text2.utf8ToString();
                        freq = (int) termsEnum2.totalTermFreq();
                        norm2 = norm2 + freq * freq;
                    }

                }
            } catch (Exception e) {

            }
           
        }
        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);
      
        sum = sum / (norm1 * norm2);
        //    System.out.println("Cosine Similarity" + sum);
        return sum;
    }

    public int getIndex(String docid, IndexReader reader) throws IOException, ParseException {
        int index = 0;
        //   System.out.println(docid);
        Analyzer analyzer = new KeywordAnalyzer();
        IndexSearcher isearcher = new IndexSearcher(reader);
        QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, "id", analyzer);

        IndexSearcher searcher = new IndexSearcher(reader);
        Query query = parser.parse(docid);
        TopDocs topdocs = searcher.search(query, 1);
         index = topdocs.scoreDocs[0].doc;
        
        return index;
    }
    
    
    public HashMap<String, Double> calculateKde(Set judgedRel, ArrayList<String> unjudged, IndexReader reader,int qid,HashMap<Integer, HashMap<String, Double>> h1, HashMap<Integer, HashMap<String, Double>> h2 ) throws IOException {
        Iterator it = unjudged.iterator();
        HashMap<String, Double> estmatedList = new HashMap<String, Double>();
        double score = 0;
        //HashMap<Integer, HashMap<String, Double>> h1 = loadCosineValue("/home/procheta/Documents/Store.txt");
        //HashMap<Integer, HashMap<String, Double>> h2 = loadCosineValue("/home/procheta/Documents/Store1.txt");
        HashMap<String,Double> h3 = h1.get(qid);
         HashMap<String,Double> h4 = h2.get(qid);
        //System.out.println("h4 "+h4);
        for (int i = 0; i < unjudged.size(); i++) {
            String docid = unjudged.get(i);
            //  System.out.println(docid);
            Iterator it2 = judgedRel.iterator();
            score = 0;
             String docidair="";
            double sim;
            while (it2.hasNext()) {
                String docid2 = (String) it2.next();
                try {
                    docidair = docid+docid2;
                  
                   // double sim = computeCosineSimilarity(getIndex(docid, reader), getIndex(docid2, reader),reader);
                  if(h3.containsKey(docidair))
                  {    sim = h3.get(docidair);
                     // System.out.println("jjjj");
                  }
                  else
                      sim = h4.get(docidair);
                    score += Math.exp(((1 - sim) * (1 - sim)) / 2);
                } catch (Exception e) {
                   // e.printStackTrace();
                    sim = 0;
                     score += Math.exp(((1 - sim) * (1 - sim)) / 2);
                    // System.out.println("docidPair  "+docidair);
                   // System.out.println(qid);
                }
            }
            score = score / judgedRel.size();
            score = score / Math.sqrt(2 * 3.14);
           // System.out.println("score " + score);
            estmatedList.put(docid, score);

        }

        return estmatedList;
    }

   

   
    public static void main(String[] args) throws IOException, ParseException {
        KDEImplementation kde = new KDEImplementation();
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File("/media/procheta/3CE80E12E80DCADA/newIndex2")));

        kde.getIndex("FBIS3-5642", reader);
        kde.computeCosineSimilarity(kde.getIndex("FT923-1528", reader), kde.getIndex("FBIS3-5642", reader), reader);
        reader.close();
        // System.out.println(kde.getIndex("FBIS4-3895", reader));
    }

}
