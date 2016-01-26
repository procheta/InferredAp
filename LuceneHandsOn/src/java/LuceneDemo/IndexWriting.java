/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LuceneDemo;

import java.io.IOException;
import java.nio.file.Paths;
import org.apache.lucene.analysis.core.KeywordAnalyzer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author procheta
 */
public class IndexWriting {
    public static void indexer() throws IOException {
		StandardAnalyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);

		config.setOpenMode(OpenMode.CREATE);
		// config.setOpenMode(OpenMode.CREATE_OR_APPEND);
		// config.setOpenMode(OpenMode.APPEND);
		String indexDir = "index";
		Directory dir;
		dir = FSDirectory.open(Paths.get(indexDir));
		IndexWriter writer = new IndexWriter(dir, config);

		Document doc1 = new Document();
		doc1.add(new TextField("LastName", "Pandey", Field.Store.YES));
		doc1.add(new StringField("FirstName", "Prateek", Field.Store.YES));
		doc1.add(new IntField("Age", 24, Field.Store.YES));
		doc1.add(new TextField("Description",
				"Prateek is a highly motivated student who never comes late.",
				Field.Store.NO));

		writer.addDocument(doc1);

		Document doc2 = new Document();
		doc2.add(new TextField("LastName", "Banerjee", Field.Store.YES));
		doc2.add(new StringField("FirstName", "Sayandeep", Field.Store.YES));
		doc2.add(new IntField("Age", 24, Field.Store.YES));
		doc2.add(new TextField("Description", "Sayan Deep has a sharp brain.",
				Field.Store.NO));

		writer.addDocument(doc2);

		writer.close();

	}
    
    public static void search(String query) throws IOException, ParseException {
		int k = 10;
		StandardAnalyzer analyzer = new StandardAnalyzer();
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths
				.get(".//index")));
		IndexSearcher searcher = new IndexSearcher(reader);

		Query q = new QueryParser("Description", analyzer).parse(query);

		// String[] fields = {"Description","LastName","FirstName"};
		// Query q = new MultiFieldQueryParser(fields,analyzer).parse(query);

		TopDocs docs = searcher.search(q, k);
		ScoreDoc[] hits = docs.scoreDocs;

		System.out.println("Found " + hits.length + " hits.");
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			System.out.println((i + 1) + ". " + d.get("FirstName") + " "
					+ d.get("LastName"));
		}

		reader.close();
	}

public static void main(String[] args) {

		try {
                    QueryParser t = new AnalyzingQueryParser("id", new KeywordAnalyzer());
			indexer();
			// System.exit(1);
			search("Motivate");
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
}
