/* Constructing the ID3 Decision Tree for Machine Learning problems */
/* Author: Harihara K Narayanan - Grad Student, Texas A&M University */

package ml;
 
import java.io.*;
import java.lang.reflect.Array;
import java.lang.Integer;
import java.lang.Float;
import java.lang.Math;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.HashMap;
import java.math.BigDecimal;
import java.math.RoundingMode;

 
public class ID3
{
		private static String[][]input;
		private static int n,t; //number of attributes and number of test_cases;
		
		private static LinkedList unusedAttr = new LinkedList();
		
		private static int countInstances (int[] testCases, int attributeIndex) //Counts the number of unique values of a particular attribute over the given testcases. 
		{
			TreeSet ts = new TreeSet();
			
			for(int i = 0; i<Array.getLength(testCases); i++)
				ts.add(input[new Integer(Array.get(testCases,i).toString()).intValue()][attributeIndex]);
			
			return ts.size();		
			
		}
		
		private static float getEntropy(int[] testCases)//Calculate the entropy of the given testCases. 
		{
			 HashMap hMap = new HashMap();
			 String classLabel;
			 int occ;
			 BigDecimal answer = BigDecimal.ZERO;
			 BigDecimal fraction;
			 
			 for(int i=0; i<Array.getLength(testCases); i++)
			 {
				 classLabel = input[new Integer(Array.get(testCases,i).toString()).intValue()][n];
				 
				 if(hMap.containsValue(classLabel))
				 {
					 occ = new Integer(hMap.get(classLabel).toString).intValue();
					 hMap.remove(classLabel);
					 hMap.put(classLabel, new Integer(occ+1));
				 }
				 else
					 hMap.put(classLabel, new Integer("1"));
			}
			
			for(String key : hMap.keySet())
			{
				fraction = new BigDecimal(hMap.get(key).toString()).divide(new BigDecimal(Array.getLength(testCases)), 2, RoundingMode.HALF_DOWN);
				fraction = fraction.multiply(new BigDecimal(Math.log(fraction.doubleValue())));
				answer = answer + (fraction.negate());
			}
			return answer.floatValue();	 	 
		}
			 
		private static float getInformationGain(int[] testCases, int attributeIndex)//Calculate the Information Gain if we split by attributeIndex for the given testCases.
		{
			HashMap hMap = new HashMap();
			String classLabel, indexes;
			BigDecimal answer = BigDecimal.ZERO;
			
			for(i=0; i<Array.length(testCases); i++)
			{
				classLabel = input[new Integer(Array.get(testCases,i).toString()).intValue()][attribute_index];
				
				if(hMap.contains(classLabel))
				{
					indexes = hMap.get(classLabel).toString();
					indexes = indexes.append(";"+String.valueOf(i))
					hMap.put(classLabel, indexes); 
				}
				
				else
					hMap.put(classLabel, String.valueOf(i));
			}
			
			int[] allTestCases;
			String[] result;
			
			for(String key: hMap.keySet())
			{
				result = hMap.get(key).toString().split(";");
				allTestCases = new int[Array.length(result)];
				
				for(i=0; i<Array.length(result); i++)
					allTestCases[i] = new Integer(result[i]).intValue();
				
				answer = answer.add(new BigDecimal(getEntropy(allTestCases)));
			}		
			
			answer = answer.subtract(new BigDecimal(getEntropy(testCases));
			return answer.floatValue();
			
		}
		
		
		private class TreeNode //Decision Tree data structure.
		{
			int[] testCases;
			int splitAttribute;
			
			TreeNode childPtr[];
			TreeNode parentPtr;
			
			TreeNode(int[] cases)
			{
				int size = Array.getLength(cases);
							
				for(int i=0; i<Array.getLength(cases); i++)
					testCases[i] = cases[i];
			}
			
			boolean isFinished() //See if all testCases have the same class value output.
			{
				if (countInstances(testCases, n) == 1)
					return true;
				else
					return false;
			}
			
			void findSplitAttribute() //Find the best splitAttribute - using Information Gain theory.
			{
				float max = FLOAT.MIN_VALUE;
				
							
				
			}
			
		}
		
		public static void createDecisionTree(String filename, int numAttributes, int testCases, boolean randomize) throws FileNotFoundException, IOException //Build Decision Tree for input.
        {
                FileInputStream fstream = new FileInputStream(filename);
                DataInputStream in = new DataInputStream(fstream);
                
                //Parse the first line to see if continuous or discrete attributes. 
                //To be done later.
                
                n = numAttributes;
                t = testCases;
 
                int i, lineCount = 0;
                for(i=0; i<n; i++)
					unusedAttr.add(new Integer(i));
                
                input = new String[testCases][numAttributes+1];
                String line;
                
                while(lineCount<testCases)
                {
					input[lineCount] = (in.readLine()).split(",");
					if (Array.getLength(input[lineCount]) != numAttributes+1) //number of attributes provided in the line is incorrect. 
					{
						System.out.println("Invalid data entry");
						System.exit(0);
					}	
					lineCount++;
                }
                
                //Start constructing the tree.
                //TreeNode node = new TreeNode(          
                
                
                
                
                
 
        }
}
