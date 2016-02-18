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

    public static void main(String args[]) throws IOException {
        createPlot cp = new createPlot();
        cp.createScatterPlotFile("/home/procheta/Documents/trec_eval.8.1/sop.txt", "/home/procheta/Documents/trec_eval.8.1/scatter_output1.txt");
        cp.createScatterPlotFile("/home/procheta/Documents/trec_eval.8.1/MAP_OUTPUT.txt", "/home/procheta/Documents/trec_eval.8.1/scatter_output2.txt");

    }
}
