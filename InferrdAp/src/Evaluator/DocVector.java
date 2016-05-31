/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Evaluator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

/**
 *
 * @author procheta
 */
class TermFreq {

    public String term;
    public int freq;

    public TermFreq(String term, int freq) {
        this.term = term;
        this.freq = freq;
    }
}

public class DocVector {

    String docid;
    public ArrayList<TermFreq> termVector;

    public DocVector(String docid, IndexReader reader, IndexSearcher searcher) throws IOException, ParseException {
        this.docid = docid;
        this.termVector = new ArrayList<TermFreq>();
        Analyzer analyzer = new KeywordAnalyzer();
        QueryParser parser = new QueryParser("id", analyzer);
        Query query = parser.parse(docid);
        TopDocs topdocs = searcher.search(query, 1);
        int index = topdocs.scoreDocs[0].doc;

        Terms terms = reader.getTermVector(index, "words");
        TermsEnum termsEnum = null;

        termsEnum = terms.iterator();
        BytesRef term;
        while ((term = termsEnum.next()) != null) {
            //  System.out.println("hhh");
            TermFreq tf = new TermFreq(term.utf8ToString(), (int) termsEnum.totalTermFreq());
            this.termVector.add(tf);
        }
       
    }

    public double cosineSim(DocVector d, IndexReader reader) throws IOException {
        int j = 0;
        double firstsquaredLength = 0;
        double secondsquaredLength = 0;
        double freqSum = 0;

        int i = 0;
        while (i < termVector.size()) {

            if (i != termVector.size() && j != d.termVector.size()) {
                if (termVector.get(i).term.compareTo(d.termVector.get(j).term) > 0) {
                    if (j < d.termVector.size()) {
                        secondsquaredLength = secondsquaredLength + d.termVector.get(j).freq * d.termVector.get(j).freq;//* Math.log(reader.numDocs()/reader.docFreq(new Term("words", d.termVector.get(j).term)));
                        //  System.out.println(reader.docFreq(new Term("words", d.termVector.get(j).term)));
                        j++;
                        // System.out.println("hhh");
                    }
                }
            }
            if (i != termVector.size() && j != d.termVector.size()) {
                if (termVector.get(i).term.compareTo(d.termVector.get(j).term) < 0) {
                    if (i < termVector.size()) {
                        firstsquaredLength = firstsquaredLength + termVector.get(i).freq * termVector.get(i).freq;// * Math.log(reader.numDocs()/reader.docFreq(new Term("words", termVector.get(i).term)));

                        i++;
                    }
                }
            }
            if (i != termVector.size() && j != d.termVector.size()) {
                if (termVector.get(i).term.compareTo(d.termVector.get(j).term) == 0) {
                    freqSum += d.termVector.get(j).freq * termVector.get(i).freq;
                    if (j < d.termVector.size()) {
                        secondsquaredLength = secondsquaredLength + d.termVector.get(j).freq * d.termVector.get(j).freq;//* Math.log(reader.numDocs()/reader.docFreq(new Term("words", d.termVector.get(j).term)))* Math.log(reader.numDocs()/reader.docFreq(new Term("words", d.termVector.get(j).term)));

                        j++;
                    }
                    if (i < termVector.size()) {
                        firstsquaredLength = firstsquaredLength + termVector.get(i).freq * termVector.get(i).freq;//* Math.log(reader.numDocs()/reader.docFreq(new Term("words", termVector.get(i).term)));

                        i++;
                    }
                }
            }
            if (j == d.termVector.size()) {
                // break;
                for (int k = i; k < termVector.size(); k++) {
                    firstsquaredLength = firstsquaredLength + termVector.get(k).freq * termVector.get(k).freq;//* Math.log(reader.numDocs()/reader.docFreq(new Term("words", termVector.get(i).term)));

                }
                i = termVector.size();
            }

        }
        if (j < d.termVector.size()) {
            for (int k = j; k < d.termVector.size(); k++) {
                secondsquaredLength = secondsquaredLength + d.termVector.get(k).freq * d.termVector.get(k).freq;//* Math.log(reader.numDocs()/reader.docFreq(new Term("words", d.termVector.get(k).term)));

            }
        }

        double f = freqSum / (Math.sqrt(firstsquaredLength) * Math.sqrt(secondsquaredLength));

        if (Double.isNaN(f)) {
            System.out.println((Math.sqrt(firstsquaredLength) * Math.sqrt(secondsquaredLength)));

        }
        // System.out.println(freqSum/ (Math.sqrt (firstsquaredLength)* Math.sqrt(secondsquaredLength)));
        return freqSum / (Math.sqrt(firstsquaredLength) * Math.sqrt(secondsquaredLength));
    }

    public HashMap<String, Double> computeCentroid(DocVector[] docArray) {

        int i = 0;
        HashMap<String, Double> termMap = new HashMap<String, Double>();
        int[] pointer = new int[docArray.length];
        String minTerm = "";
        for (i = 0; i < docArray.length; i++) {
            for (int j = 0; j < docArray[i].termVector.size(); j++) {
                String term = docArray[i].termVector.get(j).term;
                if (termMap.containsKey(term)) {
                    termMap.put(term, docArray[i].termVector.get(j).freq + termMap.get(term));
                } else {
                    termMap.put(term, (double) docArray[i].termVector.get(j).freq);

                }
            }//*/
        }

        Iterator it = termMap.keySet().iterator();
        while (it.hasNext()) {
            String st = (String) it.next();
            termMap.put(st, termMap.get(st) / docArray.length);
        }

        return termMap;

    }

}
