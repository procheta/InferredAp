/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Evaluator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author procheta
 */
public class createPlot {

    public void createScatterPlotFile(String firstFileName, String outputFileName) throws FileNotFoundException, IOException {
        FileReader firstFileReader = new FileReader(new File(firstFileName));

        BufferedReader firstBufferdReader = new BufferedReader(firstFileReader);

        String firstFileLine = firstBufferdReader.readLine();

        FileWriter fw = new FileWriter(new File(outputFileName));
        BufferedWriter bw = new BufferedWriter(fw);
        Integer count = 1;
        while (firstFileLine != null) {
            bw.write(count.toString());
            bw.write(" ");
            bw.write(firstFileLine);
            bw.newLine();
            firstFileLine = firstBufferdReader.readLine();
            count++;
        }

        bw.close();
        firstBufferdReader.close();
    }

    public void createScatterPlotFileforKDE(String firstFileName, String outputFileName) throws FileNotFoundException, IOException {
        FileReader firstFileReader = new FileReader(new File(firstFileName));

        BufferedReader firstBufferdReader = new BufferedReader(firstFileReader);

        String firstFileLine = firstBufferdReader.readLine();

        FileWriter fw = new FileWriter(new File(outputFileName));
        BufferedWriter bw = new BufferedWriter(fw);
        Integer count = 1;
        while (firstFileLine != null) {
            String st[] = firstFileLine.split(" ");
            bw.write(count.toString());
            bw.write(" ");
            bw.write(st[1]);
            bw.newLine();
            firstFileLine = firstBufferdReader.readLine();
            count++;
        }

        bw.close();
        firstBufferdReader.close();
    }

    public void createScatterPlotFileNew(String firstFileName, String secondFileName, String thirdFileName, String outputFileName) throws FileNotFoundException, IOException {
        FileReader firstFileReader = new FileReader(new File(firstFileName));
        FileReader secondFileReader = new FileReader(new File(secondFileName));
        BufferedReader firstBufferdReader = new BufferedReader(firstFileReader);
        BufferedReader secondBufferdReader = new BufferedReader(secondFileReader);
        HashMap<String, Double> h1 = new HashMap<>();
        String firstFileLine = firstBufferdReader.readLine();
        while (firstFileLine != null) {
            String st[] = firstFileLine.split(" ");
            h1.put(st[0], Double.parseDouble(st[1]));
            firstFileLine = firstBufferdReader.readLine();

        }
        firstBufferdReader.close();
        FileReader thirdFileReader = new FileReader(new File(thirdFileName));

        BufferedReader thirdBufferdReader = new BufferedReader(thirdFileReader);
        HashMap<String, Double> h2 = new HashMap<>();
        String secondFileLine = secondBufferdReader.readLine();
        String thirdFileLine = thirdBufferdReader.readLine();
        while (thirdFileLine != null) {

            h2.put(thirdFileLine, Double.parseDouble(secondFileLine));
            thirdFileLine = thirdBufferdReader.readLine();
            secondFileLine = secondBufferdReader.readLine();
        }

        FileWriter fw = new FileWriter(new File(outputFileName));
        BufferedWriter bw = new BufferedWriter(fw);
        Iterator it = h1.keySet().iterator();
        while (it.hasNext()) {
            String st = (String) it.next();
            bw.write(new Double(h1.get(st)).toString());
            bw.write(" ");
            bw.write(new Double(h2.get(st)).toString());
            bw.newLine();

        }

        bw.close();
        firstBufferdReader.close();
    }

    public void createScatterPlotFileNew1(String firstFileName, String secondFileName, String thirdFileName, String outputFileName) throws FileNotFoundException, IOException {
        FileReader firstFileReader = new FileReader(new File(firstFileName));
        FileReader secondFileReader = new FileReader(new File(secondFileName));
        BufferedReader firstBufferdReader = new BufferedReader(firstFileReader);
        BufferedReader secondBufferdReader = new BufferedReader(secondFileReader);

        FileReader thirdFileReader = new FileReader(new File(thirdFileName));
        HashMap<String, Double> h1 = new HashMap<>();
        BufferedReader thirdBufferdReader = new BufferedReader(thirdFileReader);
        String thirdFileLine = thirdBufferdReader.readLine();
        String firstFileLine = firstBufferdReader.readLine();
        while (firstFileLine != null) {
            
            h1.put(thirdFileLine, Double.parseDouble(firstFileLine));
            firstFileLine = firstBufferdReader.readLine();
            thirdFileLine = thirdBufferdReader.readLine();

        }
        firstBufferdReader.close();
        thirdFileReader.close();
        thirdFileReader = new FileReader(new File(thirdFileName));

        thirdBufferdReader = new BufferedReader(thirdFileReader);
        HashMap<String, Double> h2 = new HashMap<>();
        String secondFileLine = secondBufferdReader.readLine();
        thirdFileLine = thirdBufferdReader.readLine();
        while (thirdFileLine != null) {

            h2.put(thirdFileLine, Double.parseDouble(secondFileLine));
            thirdFileLine = thirdBufferdReader.readLine();
            secondFileLine = secondBufferdReader.readLine();
        }

        FileWriter fw = new FileWriter(new File(outputFileName));
        BufferedWriter bw = new BufferedWriter(fw);
        Iterator it = h1.keySet().iterator();
        while (it.hasNext()) {
            String st = (String) it.next();
            bw.write(new Double(h1.get(st)).toString());
            bw.write(" ");
            bw.write(new Double(h2.get(st)).toString());
            bw.newLine();

        }

        bw.close();
        firstBufferdReader.close();
    }

    public static void main(String args[]) throws IOException {
        createPlot cp = new createPlot();
        //cp.createScatterPlotFileforKDE("/media/procheta/D8CA50B2CA508F1E/TrecData/infAp.txt", "/home/procheta/Documents/trec_eval.8.1/scatter_output1.txt");
        //cp.createScatterPlotFile("/home/procheta/Documents/trec_eval.8.1/infAp.txt", "/home/procheta/Documents/trec_eval.8.1/scatter_output2.txt");
        cp.createScatterPlotFileNew("/media/procheta/D8CA50B2CA508F1E/TrecData/infAp.txt", "/home/procheta/Documents/trec_eval.8.1/map.txt", "/media/procheta/D8CA50B2CA508F1E/TrecData/trec8/fileList.txt", "/home/procheta/Documents/trec_eval.8.1/scatter_output2.txt");
     //     cp.createScatterPlotFileNew1("/home/procheta/Documents/trec_eval.8.1/infAp.txt", "/home/procheta/Documents/trec_eval.8.1/map.txt", "/media/procheta/D8CA50B2CA508F1E/TrecData/trec8/fileList.txt", "/home/procheta/Documents/trec_eval.8.1/scatter_output2.txt");
   
    }
}
