package ml;
 
import java.io.*;
import java.lang.reflect.Array;
import java.util.TreeSet;
import java.util.LinkedList;
import java.lang.Integer;
 
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
		
		private static int getInformationGain(int[] testCases, int attributeIndex)
		{
			 
			
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
