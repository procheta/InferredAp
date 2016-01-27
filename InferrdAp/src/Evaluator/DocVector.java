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

	/*DG_Comments:
	 * Don't open the IndexReader everytime while creating a DocVector object. This is highly inefficient
	 * as IndexReader.open() is an expensive system call.
	 *
	 * Don't create the IndexSearcher object everytime. Pass these around from the calling function.
	 *
	 */

    public DocVector(String docid, String indexPath) throws IOException, ParseException {
        this.docid = docid;
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));

        Analyzer analyzer = new KeywordAnalyzer();
        IndexSearcher isearcher = new IndexSearcher(reader);
		//+++DG_Comments: 
    	// Use TrecDocParse.FIELD_ID instead of "id"
        QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, "id", analyzer);

        IndexSearcher searcher = new IndexSearcher(reader);
		//+++DG_Comments: An easier and better way is to directly set the term as a single query term (a BooleanQuery with MUST clause)
		// than to use QueryParser
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

    public double computeCosineSimilarity(DocVector d) {  // DG_Comments: Change name to cosineSim()
        int j = 0;
		// +++DG_Comments: Give more meaningful names for norm1, norm2 and sum
        int norm1 = 0;
        int norm2 = 0;
        double sum = 0;
        for (int i = 0; i < termVector.size(); i++) {  // DG_Comments: why check on the bounds of this object only and not on d?
			// +++DG_Comments: Avoid multiple calls of the compareTo function by calling it only
			// once and assigning to a variable. You can then check the variable value
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
           	//+++DG_Comments: Don't need to check the 0 condition! Simply use else! 
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
