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
import sun.java2d.pipe.BufferedBufImgOps;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;

/**
 *
 * @author procheta
 */
public class StatCalculator {
    
   public ArrayList readValues(String fileName) throws FileNotFoundException, IOException
   {
        ArrayList  ar = new ArrayList();
        FileReader fr = new FileReader(new File(fileName));
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        while(line != null)
        {
            ar.add(Double.parseDouble(line));
            line = br.readLine();
        }      
        
        return ar;   
   }
   
   public double rmsCalc(ArrayList a1, ArrayList a2)
   {
       double sum = 0; 
       
       for(int i = 0; i < a1.size(); i++)
        {
            sum = (((double)a1.get(i) - (double)a2.get(i)));
        }  
   
       sum /= a1.size();
       sum = Math.sqrt(sum);
       return sum;
   }
   
   public double correlationCalc(ArrayList a1, ArrayList a2)
   {
       double xArray[],yArray[];
       int count;
       
       xArray = new double[a1.size()];
       yArray = new double[a2.size()];
       
       for(count = 0; count < a1.size();count++)
       {
           xArray[count] = (double)a1.get(count); 
           yArray[count] = (double)a2.get(count); 
       }      
       
       PearsonsCorrelation prc = new PearsonsCorrelation();
       return prc.correlation(xArray,yArray);
  
   }
   
    public double kendalTauCalc(ArrayList a1, ArrayList a2)
   {
       double xArray[],yArray[];
       int count;
       
       xArray = new double[a1.size()];
       yArray = new double[a2.size()];
       
       for(count = 0; count < a1.size();count++)
       {
           xArray[count] = (double)a1.get(count); 
           yArray[count] = (double)a2.get(count); 
       }      
       
       KendallsCorrelation kc = new KendallsCorrelation();
       return kc.correlation(xArray, yArray);
  
   }
   
   
   
   public static void main(String []args) throws IOException{
       StatCalculator stc = new StatCalculator();
       System.out.println(stc.rmsCalc(stc.readValues("/home/procheta/Documents/trec_eval.8.1/MAP_OUTPUT.txt"),stc.readValues("/home/procheta/Documents/trec_eval.8.1/sop.txt")));
       System.out.println(stc.correlationCalc(stc.readValues("/home/procheta/Documents/trec_eval.8.1/sop.txt"),stc.readValues("/home/procheta/Documents/trec_eval.8.1/MAP_OUTPUT.txt")));  
       System.out.println(stc.kendalTauCalc(stc.readValues("/home/procheta/Documents/trec_eval.8.1/sop.txt"),stc.readValues("/home/procheta/Documents/trec_eval.8.1/MAP_OUTPUT.txt")));  
   
   }
    
}
