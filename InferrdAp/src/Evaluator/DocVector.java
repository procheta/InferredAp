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

    public DocVector(String docid, String indexPath) throws IOException, ParseException {
        this.docid = docid;
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));

        Analyzer analyzer = new KeywordAnalyzer();
        IndexSearcher isearcher = new IndexSearcher(reader);
        QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, "id", analyzer);
        IndexSearcher searcher = new IndexSearcher(reader);
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

    public double computeCosineSimilarity(DocVector d) {
        int j = 0;
        int norm1 = 0;
        int norm2 = 0;
        double sum = 0;
        for (int i = 0; i < termVector.size(); i++) {
            if (termVector.get(i).term.compareTo(d.termVector.get(j).term) > 0) {
                if (j < d.termVector.size()) {
                    j++;
                    norm1 = norm1 + termVector.get(i).freq * termVector.get(i).freq;
                }                
            }
            if (termVector.get(i).term.compareTo(d.termVector.get(j).term) < 0) {
                if (j < d.termVector.size()) {
                    i++;
                    norm2 = norm2 + d.termVector.get(j).freq * d.termVector.get(j).freq;
                }                
            }
            
             if (termVector.get(i).term.compareTo(d.termVector.get(j).term) == 0) {
                 sum += d.termVector.get(j).freq * termVector.get(i).freq;
                if (j < d.termVector.size()) 
                    j++;
                 if(j == d.termVector.size())
                     break;
                    norm2 = norm2 + d.termVector.get(j).freq * d.termVector.get(j).freq;
                    norm1 = norm1 + termVector.get(i).freq * termVector.get(i).freq;
                                
            }
             
                if(j == d.termVector.size())
                    break;
        }

        return sum/(Math.sqrt(norm1) * Math.sqrt(norm2));
    }

}
