/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Evaluator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
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

    public DocVector(String docid,IndexReader reader, IndexSearcher searcher) throws IOException, ParseException {
        this.docid = docid;
        Analyzer analyzer = new KeywordAnalyzer();
        QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, "id", analyzer);
        Query query = parser.parse(docid);
        TopDocs topdocs = searcher.search(query, 1);
        int index = topdocs.scoreDocs[0].doc;

        Terms terms = reader.getTermVector(index, "words");
        TermsEnum termsEnum = null;

        termsEnum = terms.iterator(null);
        BytesRef term;
        while ((term = termsEnum.next()) != null) {

            TermFreq tf = new TermFreq(term.utf8ToString(), (int) termsEnum.totalTermFreq());
            this.termVector.add(tf);
        }

        reader.close();

    }

    public double cosineSim(DocVector d) {
        int j = 0;
        int firstsquaredLength = 0;
        int secondsquaredLength = 0;
        double freqSum = 0;
        for (int i = 0; i < termVector.size(); i++) {
            
            int compareToValue = termVector.get(i).term.compareTo(d.termVector.get(j).term);
            if (compareToValue > 0) {
                if (j < d.termVector.size()) {
                    j++;
                    firstsquaredLength = firstsquaredLength + termVector.get(i).freq * termVector.get(i).freq;
                }
            }
            if (compareToValue< 0) {
                if (j < d.termVector.size()) {
                    i++;
                    secondsquaredLength = secondsquaredLength + d.termVector.get(j).freq * d.termVector.get(j).freq;
                }
            }

            if (compareToValue == 0) {
                freqSum += d.termVector.get(j).freq * termVector.get(i).freq;
                if (j < d.termVector.size()) {
                    j++;
                }
                if (j == d.termVector.size()) {
                    break;
                }
                secondsquaredLength = secondsquaredLength + d.termVector.get(j).freq * d.termVector.get(j).freq;
                firstsquaredLength = firstsquaredLength + termVector.get(i).freq * termVector.get(i).freq;

            }

            if (j == d.termVector.size()) {
                break;
            }
        }

        return freqSum / (Math.sqrt(firstsquaredLength) * Math.sqrt(secondsquaredLength));
    }

}
