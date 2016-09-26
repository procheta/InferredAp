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
import java.util.LinkedList;

/**
 *
 * @author procheta
 */
public class VectorExtractor {
    
   

    public HashMap<String, ArrayList<Double>> extractVector(String FileName) throws FileNotFoundException, IOException {
        ArrayList<Double> vectorList = new ArrayList<Double>();
        HashMap<String, ArrayList<Double>> vectorFileMap = new HashMap<String, ArrayList<Double>>();
        FileReader fr = new FileReader(new File(FileName));
        BufferedReader br = new BufferedReader(fr);
        String st = br.readLine();

        while (st != null) {
            String st1[] = st.split("	");
            st1[1] = st1[1].replace("[", "");
            st1[1] = st1[1].replace("]", "");
            st1[1] = st1[1].replace(" ", "");
            String vectorNumbers[] = st1[1].split(",");
            vectorList = new ArrayList<Double>();
            for (int i = 0; i < vectorNumbers.length; i++) {
                vectorList.add(Double.parseDouble(vectorNumbers[i]));

            }
            vectorFileMap.put(st1[0], vectorList);
            st = br.readLine();
        }
        //System.out.println(vectorFileMap);
        return vectorFileMap;
    }
    
     public  ArrayList<Double> getVector(String st) throws FileNotFoundException, IOException {
        ArrayList<Double> vectorList = new ArrayList<Double>();   

            //System.out.println("gg"+st);
           String st1[] = st.split("	");
           st1[1] = st1[1].replace("[", "");
            st1[1] = st1[1].replace("]", "");
            st1[1] = st1[1].replace(" ", "");
            String vectorNumbers[] = st1[1].split(",");
            vectorList = new ArrayList<Double>();
            for (int i = 0; i < vectorNumbers.length; i++) {
                vectorList.add(Double.parseDouble(vectorNumbers[i]));

            } // */       
          
        //System.out.println(vectorFileMap);
        return vectorList;
    }

    public double computeSimilarity(ArrayList<Double> a1, ArrayList<Double> a2) {
        double d = 0;
        double similarity = 0;
        for (int i = 0; i < a1.size(); i++) {
            similarity += (a1.get(i) - a2.get(i)) * (a1.get(i) - a2.get(i));
        }
        return similarity;
    }

    public static void main(String[] args) throws IOException {
        VectorExtractor ve = new VectorExtractor();
        //ve.extractVector("/home/procheta/vecs/srbm.outvecs.train.415.txt");
        System.out.println(ve.extractVector("/home/procheta/vecs/srbm.outvecs.train.415.txt"));
    }

}
