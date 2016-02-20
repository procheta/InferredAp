/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Evaluator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import sun.java2d.pipe.BufferedBufImgOps;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;

/**
 *
 * @author procheta
 */
public class StatCalculator {

    public HashMap<String, Double> readValues(String firstFileName, String secondFileName) throws FileNotFoundException, IOException {
        HashMap<String, Double> runQidMap = new HashMap<>();
        FileReader firstFileReader = new FileReader(new File(firstFileName));
        BufferedReader firstBufferReader = new BufferedReader(firstFileReader);

        FileReader secondFileReader = new FileReader(new File(secondFileName));
        BufferedReader secondBufferReader = new BufferedReader(secondFileReader);

        String firstFileLine = firstBufferReader.readLine();
        String secondFileLine = secondBufferReader.readLine();
        while (firstFileLine != null) {
            String st1[] = firstFileLine.split(" ");
            String st2[] = secondFileLine.split(" ");
          //  runQidMap.put(st1[0], (Double.parseDouble(st1[1]) + Double.parseDouble(st2[1])) / 2);
            runQidMap.put(st1[0], (Double.parseDouble(st1[1])));
            firstFileLine = firstBufferReader.readLine();
            secondFileLine = secondBufferReader.readLine();
        }

        return runQidMap;
    }

    public HashMap<String, Double> readValuesforAP(String firstFileName, String secondFileName) throws FileNotFoundException, IOException {
        HashMap<String, Double> runQidMap = new HashMap<>();
        FileReader firstFileReader = new FileReader(new File(firstFileName));
        BufferedReader firstBufferReader = new BufferedReader(firstFileReader);

        FileReader secondFileReader = new FileReader(new File(secondFileName));
        BufferedReader secondBufferReader = new BufferedReader(secondFileReader);

        String firstFileLine = firstBufferReader.readLine();
        String secondFileLine = secondBufferReader.readLine();
        while (firstFileLine != null) {

            runQidMap.put(firstFileLine, Double.parseDouble(secondFileLine));
            firstFileLine = firstBufferReader.readLine();
            secondFileLine = secondBufferReader.readLine();
        }

        return runQidMap;
    }

    public double rmsCalc(HashMap<String, Double> kderunQidMap, HashMap<String, Double> aprunQidMap) {
        double sum = 0;
        Iterator it = kderunQidMap.keySet().iterator();
        while (it.hasNext()) {
            String run = (String) it.next();
            sum = (kderunQidMap.get(run) - aprunQidMap.get(run)) * (kderunQidMap.get(run) - aprunQidMap.get(run));

        }

        sum /= kderunQidMap.size();
        sum = Math.sqrt(sum);
        return sum;
    }

    public double correlationCalc(HashMap<String, Double> kderunQidMap, HashMap<String, Double> aprunQidMap) {
        double xArray[], yArray[];
        int count = 0;

        xArray = new double[kderunQidMap.size()];
        yArray = new double[kderunQidMap.size()];

        Iterator it = kderunQidMap.keySet().iterator();
        while (it.hasNext()) {
            String run = (String) it.next();
            xArray[count] = kderunQidMap.get(run);
            yArray[count++] = aprunQidMap.get(run);
        }

        PearsonsCorrelation prc = new PearsonsCorrelation();
        return prc.correlation(xArray, yArray);

    }

    public double kendalTauCalc(HashMap<String, Double> kderunQidMap, HashMap<String, Double> aprunQidMap) {
        double xArray[], yArray[];
        int count = 0;

        xArray = new double[kderunQidMap.size()];
        yArray = new double[kderunQidMap.size()];

        Iterator it = kderunQidMap.keySet().iterator();
        while (it.hasNext()) {
            String run = (String) it.next();
            xArray[count] = kderunQidMap.get(run);
            yArray[count++] = aprunQidMap.get(run);
        }

        KendallsCorrelation kc = new KendallsCorrelation();
        return kc.correlation(xArray, yArray);

    }

    public static void main(String[] args) throws IOException {
        StatCalculator stc = new StatCalculator();
        System.out.println(stc.rmsCalc(stc.readValuesforAP("/media/procheta/D8CA50B2CA508F1E/TrecData/trec8/fileList.txt", "/home/procheta/Documents/trec_eval.8.1/map.txt"), stc.readValues("/media/procheta/D8CA50B2CA508F1E/TrecData/infAp.txt", "/media/procheta/D8CA50B2CA508F1E/TrecData/infAp.txt")));
       System.out.println(stc.correlationCalc(stc.readValuesforAP("/media/procheta/D8CA50B2CA508F1E/TrecData/trec8/fileList.txt", "/home/procheta/Documents/trec_eval.8.1/map.txt"), stc.readValues("/media/procheta/D8CA50B2CA508F1E/TrecData/infAp.txt", "/media/procheta/D8CA50B2CA508F1E/TrecData/infAp.txt")));
      // System.out.println(stc.kendalTauCalc(stc.readValuesforAP("/media/procheta/D8CA50B2CA508F1E/TrecData/trec8/fileList.txt", "/home/procheta/Documents/trec_eval.8.1/infAp.txt"), stc.readValues("/media/procheta/D8CA50B2CA508F1E/TrecData/infAp.txt", "/media/procheta/D8CA50B2CA508F1E/TrecData/infAp.txt")));

    }

}
