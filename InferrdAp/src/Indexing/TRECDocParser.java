/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Indexing;

import java.io.BufferedReader;
import org.apache.lucene.analysis.Analyzer;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import java.util.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.IndexWriter;

import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author procheta
 */
public class TRECDocParser extends DefaultHandler {

    Properties prop;
    File indexDir;
    IndexWriter writer;
    Analyzer analyzer;
    List<String> stopwords;
    int pass;

    static final public String FIELD_ID = "id";
    static final public String FIELD_ANALYZED_CONTENT = "words";  // Standard analyzer w/o stopwords.

    private TRECDocParser() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    protected List<String> buildStopwordList(String stopwordFileName) {
        List<String> stopwords = new ArrayList<>();
        String stopFile = prop.getProperty(stopwordFileName);
        String line;
        System.out.println(stopFile);
        try (FileReader fr = new FileReader(stopFile);
                BufferedReader br = new BufferedReader(fr)) {
            while ((line = br.readLine()) != null) {
                stopwords.add(line.trim());
            }
            br.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stopwords;
    }

    Analyzer constructAnalyzer() {
        Analyzer eanalyzer; 
       // eanalyzer = new EnglishAnalyzer(Version.LUCENE_5_4_0,StopFilter.makeStopSet(Version.LUCENE_5_4_0, buildStopwordList("stopfile")));
        eanalyzer = new EnglishAnalyzer(StopFilter.makeStopSet(buildStopwordList("stopfile")));
        
        return eanalyzer;
    }

    public TRECDocParser(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        analyzer = constructAnalyzer();
        String indexPath = prop.getProperty("index");
        indexDir = new File(indexPath);
        System.out.println(indexPath);
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public Properties getProperties() {
        return prop;
    }

    void processAll() throws Exception {
        System.out.println("Indexing TREC collection...");

        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        Path p = indexDir.toPath();

        writer = new IndexWriter(FSDirectory.open(p), iwcfg);

        indexAll();

        writer.close();
    }

    public File getIndexDir() {
        return indexDir;
    }

    void indexAll() throws Exception {
        if (writer == null) {
            System.err.println("Skipping indexing... Index already exists at " + indexDir.getName() + "!!");
            return;
        }

        File topDir = new File(prop.getProperty("coll"));
        indexDirectory(topDir);
    }

    private void indexDirectory(File dir) throws Exception {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {

                System.out.println("Indexing directory " + f.getName());
                indexDirectory(f);  // recurse
            } else {
                try {
                    indexFile(f);
                } catch (Exception e) {
                      System.out.println("Error: "+f);
                }

            }
        }
    }

    Document constructDoc(String id, String content) throws IOException {
        Document doc = new Document();
        doc.add(new Field(FIELD_ID, id, Field.Store.YES, Field.Index.NOT_ANALYZED));

        // For the 1st pass, use a standard analyzer to write out
        // the words (also store the term vector)
        doc.add(new Field(FIELD_ANALYZED_CONTENT, content,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));

        return doc;
    }

    void indexFile(File file) throws Exception {
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;
        Document doc;

        System.out.println("Indexing file: " + file.getName());

        StringBuffer txtbuff = new StringBuffer();
        while ((line = br.readLine()) != null) {
            txtbuff.append(line).append("\n");
        }
        String content = txtbuff.toString();

        org.jsoup.nodes.Document jdoc = Jsoup.parse(content);
        Elements docElts = jdoc.select("DOC");

        for (Element docElt : docElts) {
            Element docIdElt = docElt.select("DOCNO").first();
            doc = constructDoc(docIdElt.text(), docElt.text());
            writer.addDocument(doc);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java TrecDocIndexer <prop-file>");
            args[0] = "/home/procheta/NetBeansProjects/TrecDocParse/src/trecdocparse/init.properties";
            System.out.println(args[0]);
        }

        try {
            TRECDocParser indexer = new TRECDocParser(args[0]);
            indexer.processAll();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
