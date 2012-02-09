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
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Collection;
import java.util.Stack;
import java.util.ArrayList;
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
		
		private static TreeSet getInstances(int [] testCases, int attributeIndex)//Returns an instance TreeSet containing all instances.
		{
			TreeSet ts = new TreeSet();
			
			for(int i = 0; i<Array.getLength(testCases); i++)
				ts.add(input[new Integer(Array.get(testCases,i).toString()).intValue()][attributeIndex]);
				
			return ts;	
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
					 occ = new Integer(hMap.get(classLabel).toString()).intValue();
					 hMap.remove(classLabel);
					 hMap.put(classLabel, new Integer(occ+1));
				 }
				 else
					 hMap.put(classLabel, new Integer("1"));
			}
			Collection hColl = hMap.values();
			Iterator hIt = hColl.iterator();
			
			while(hIt.hasNext())
			{
				fraction = new BigDecimal(hIt.next().toString()).divide(new BigDecimal(Array.getLength(testCases)), 2, RoundingMode.HALF_DOWN);
				fraction = fraction.multiply(new BigDecimal(Math.log(fraction.doubleValue())));
				answer = answer.add(fraction.negate());
			}
			return answer.floatValue();	 	 
		}
			 
		private static float getInformationGain(int[] testCases, int attributeIndex)//Calculate the Information Gain if we split by attributeIndex for the given testCases.
		{
			HashMap hMap = new HashMap();
			String classLabel, indexes;
			BigDecimal answer = BigDecimal.ZERO;
			int i;
			
			for(i=0; i<Array.getLength(testCases); i++)
			{
				classLabel = input[new Integer(Array.get(testCases,i).toString()).intValue()][attributeIndex];
				
				if(hMap.containsKey(classLabel))
				{
					indexes = hMap.get(classLabel).toString();
					indexes = indexes.concat(";"+String.valueOf(i));
					hMap.put(classLabel, indexes); 
				}
				
				else
					hMap.put(classLabel, String.valueOf(i));
			}
			
			int[] allTestCases;
			String[] result;
			
			Collection hColl = hMap.values();
			Iterator hIt = hColl.iterator();
			while(hIt.hasNext())
			{
				result = hIt.next().toString().split(";");
				allTestCases = new int[Array.getLength(result)];
				
				for(i=0; i<Array.getLength(result); i++)
					allTestCases[i] = new Integer(result[i]).intValue();
				
				answer = answer.add(new BigDecimal(getEntropy(allTestCases)));
			}		
			
			answer = answer.subtract(new BigDecimal(getEntropy(testCases)));
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
				float max = Float.MIN_VALUE;
				float val;
				int finalAttribute = -1, attribute;
				
				ListIterator lIt = unusedAttr.listIterator();
				
				while(lIt.hasNext())
				{
					attribute = new Integer(lIt.next().toString()).intValue();
					val = getInformationGain(testCases, attribute);
					if (val>max)
					{
						max = val;
						finalAttribute = attribute;
					}
				}
				
				splitAttribute = finalAttribute;
			}
			
		}
		
		public void createDecisionTree(String filename, int numAttributes, int testCases, boolean randomize) throws FileNotFoundException, IOException //Build Decision Tree for input.
        {
                FileInputStream fstream = new FileInputStream(filename);
                DataInputStream in = new DataInputStream(fstream);
                
                //Parse the first line to see if continuous or discrete attributes. 
                //To be done later.
                
                n = numAttributes;
                t = testCases;
 
                int i, j, lineCount = 0;
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
                
                int[] cases = new int[testCases];
                for(i=0;i<testCases;i++)
					cases[i] = i;
                
                //Start constructing the tree.
                Stack<TreeNode> treeStack = new Stack<TreeNode>();
                TreeNode node = new TreeNode(cases);
                node.parentPtr = null;
                TreeSet tempSet;
                ArrayList tempList;
                int tempSize;
				int[] tempArray;	
                treeStack.push(node);
                
                while(!treeStack.isEmpty() && unusedAttr.size()>0)
                {
					TreeNode tempNode = treeStack.pop();
					
					if(countInstances(tempNode.testCases, numAttributes) == 1)
						continue;
										
					tempNode.findSplitAttribute();
					unusedAttr.remove(new Integer(tempNode.splitAttribute));
										
					tempSet = getInstances(tempNode.testCases, tempNode.splitAttribute);
					tempSize = tempSet.size();
					tempNode.childPtr = new TreeNode [tempSize]; //Create as many child nodes as there are categories in the splitAttribute.					
					
					for(i=0; i<tempSize; i++) 
					{
							tempList = new ArrayList();
							for(j=0; j<testCases; j++)
							{
								if (input[j][numAttributes] == tempSet.first())
									tempList.add(j);
									
							}
							
							tempArray = new int[tempList.size()];
							
							for(j=0; j<tempList.size(); j++)
								tempArray[j] = new Integer(tempList.get(j).toString()).intValue();
								
							
							TreeNode tempNode2 = new TreeNode(tempArray);
							tempNode2.parentPtr = tempNode;
							treeStack.push(tempNode2);
							tempSet.remove(tempSet.first());
					}
										
				}
         }
}
