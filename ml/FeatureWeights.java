package ml;

import java.util.*;
import java.io.*;
import java.math.*;
import java.lang.*;
import java.lang.reflect.*;

public class FeatureWeights
{
	private static String input[][], thresholdVal[][];
	private static String fileName, delimStr;
	private static String firstLine[], missingValues[], inputTest[];
	private static int n, t, numSplits,maxSplits = 10, valSize;
	
	public FeatureWeights(String fName, int numAttributes, int testCases, String delimiter, int limitSplits) throws IOException
	{
		fileName = fName;
		n = numAttributes;
		t = testCases;
		numSplits = limitSplits;
		delimStr = delimiter;
		
		FileInputStream fstream = new FileInputStream(fileName);
        DataInputStream in = new DataInputStream(fstream);
        
        //Parse the first line to see if continuous or discrete attributes. 
        firstLine = new String[n];
        
        firstLine = in.readLine().split(delimiter);
        //firstLine[n] = "d";
        
        int i, j, lineCount = 0;
                     
        //for(i=0; i<n; i++)
		//	unusedAttr.add(new Integer(i));
                            
        input = new String[t][n+1];
        String line;
        
        int invalidLines = 0;    
        while((lineCount + invalidLines)<t)
        {
			try
			{
				input[lineCount] = (in.readLine()).split(delimiter);
			}
			catch(NullPointerException e)
			{
				invalidLines++;continue;
			}
			if (Array.getLength(input[lineCount]) != n+1 || (Array.get(input[lineCount],n).toString().compareTo("?") == 0)) //number of attributes provided in the line is incorrect. 
			{
				invalidLines++;continue;
			}
			lineCount++;	
			
        }
        
        if(invalidLines == t)
        {
			System.out.println("All lines invalid - Check the supplied attribute number");
			System.exit(0);
		}
        if (invalidLines > 0)
			System.out.println("Not Considering "+invalidLines+" invalid training cases");
			
		if(numSplits > maxSplits || numSplits > (t/2))
		{
			System.out.println("numSplits should be less than or equal to "+Math.min(t/2,limitSplits));
			System.exit(1);
		}
			
		t = testCases - invalidLines;	
		
		thresholdVal = new String[n][numSplits - 1];
		
		boolean allCont = false;
		if(Array.getLength(firstLine) == 1)
		{
			if(firstLine[0].compareTo("c") == 0)
				allCont = true;
			else if(firstLine[0].compareTo("d") == 0)
				return;
			else
			{
				System.out.println("Invalid first line - it should be c or d");
				System.exit(1);
			}
		}
		          
        for(i=0; i<n; i++)
        {
			if(allCont || firstLine[i].compareTo("c") == 0) //Continuous Attribute
			{
				for(j=0; j<numSplits-1; j++)
					thresholdVal[i][j] = calculateThreshold(i,j);
			}
			else if(firstLine[i].compareTo("d") != 0)
			{
				System.out.println("Invalid first line - Training data (it should specify if the attributes are c or d)");
				System.exit(1);
			}
		}
		
		for(i=0; i<t; i++)
		{
			for(j=0; j<n; j++)
			{
				if(allCont || firstLine[j].compareTo("c") == 0)
					input[i][j] = makeDiscrete(input[i][j], j);
				
			}
		}
		
		//Substitute missing values with Most common value (for discrete) or average value (for continuous)
		missingValues = new String[n+1];
		for(i=0; i<n; i++)
			missingValues[i] = "?";
		
		for(i=0; i<t; i++)
		{
			for(j=0; j<=n; j++)
			{
				if(input[i][j].compareTo("?") == 0)
				{
					if(allCont||firstLine[j].compareTo("c") == 0)
						substituteMissingValue(true, i, j);
					else
						substituteMissingValue(false, i, j);
				}
			}
		}

	}
	
	public float[] getWeights()
	{
		float weightAttr[] = new float[n];
		int testCases[] = new int[t], i;
		for(i=0; i<t; i++)
			testCases[i] = i;
		
		float minValue = Float.MAX_VALUE;
		
		for(i=0; i<n; i++)
		{
			weightAttr[i] = getInformationGain(testCases, i);
			if (minValue > weightAttr[i])
				minValue = weightAttr[i];
		}
		
		if(minValue < 0)
		{
			for(i=0; i<n; i++)
				weightAttr[i] = weightAttr[i] - minValue;
		}
			
		return weightAttr;
	}
	
	private void substituteMissingValue(boolean isContinuous, int testCase, int attrNum)
	{
		int i,j, missValues = 0;
		
		if(missingValues[attrNum].compareTo("?") == 0)
		{
			if(isContinuous)
			{
				BigDecimal sum = BigDecimal.ZERO;
				for(i=0; i<t; i++)
				{
					try
					{
						sum = sum.add(new BigDecimal(input[i][attrNum]));
					}
					catch(NumberFormatException e)
					{
						missValues++;
					}
				}
				input[testCase][attrNum] = sum.divide(new BigDecimal(t - missValues), 10, RoundingMode.HALF_DOWN).toString();
				missingValues[attrNum] = input[testCase][attrNum];
			}
			else
			{
				input[testCase][attrNum] = getMostCommonValue(attrNum);
				missingValues[attrNum] = input[testCase][attrNum];
			}
		}
		else
			input[testCase][attrNum] = missingValues[attrNum];
	}

	private String getMostCommonValue(int attrNum)
	{
		int i;
		int[] testCases = new int[t];
		
		String answer = "", currLabel; 
		Integer maxOcc = Integer.MIN_VALUE, currOcc;
		
		for(i=0; i<t; i++)
			testCases[i] = i;
			
		HashMap<String, Integer> hMap = getInstances(testCases, attrNum);
		
		Iterator<String> hIt = hMap.keySet().iterator();
		
		while(hIt.hasNext())
		{
			currLabel = hIt.next();
			currOcc = hMap.get(currLabel);
			
			if(maxOcc.compareTo(currOcc) == -1)
			{
				maxOcc = currOcc;
				answer = currLabel;
			}
		}
				
		return answer;
	}

	private float getEntropy(int[] testCases)//Calculate the entropy of the given testCases. 
	{
		 HashMap hMap = new HashMap();
		 String classLabel;
		 int occ;
		 BigDecimal answer = BigDecimal.ZERO;
		 BigDecimal fraction;

		 for(int i=0; i<Array.getLength(testCases); i++)
		 {
			 classLabel = input[testCases[i]][n];

			 if(hMap.containsKey(classLabel))
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
			fraction = new BigDecimal(hIt.next().toString()).divide(new BigDecimal(Array.getLength(testCases)), 10, RoundingMode.HALF_DOWN);
			fraction = fraction.multiply(new BigDecimal(Math.log(fraction.doubleValue())));
			answer = answer.add(fraction.negate());
		}
		return answer.floatValue();	 	 
	}
	
	private float getInformationGain(int[] testCases, int attributeIndex)//Calculate the Information Gain if we split by attributeIndex for the given testCases.
	{
		HashMap hMap = new HashMap();
		String classLabel, indexes;
		BigDecimal answer = BigDecimal.ZERO;
		int i;

		for(i=0; i<Array.getLength(testCases); i++)
		{
			classLabel = input[testCases[i]][attributeIndex];

			if(classLabel.compareTo("?") == 0)
				continue;

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
		return answer.negate().floatValue();

	}
	
	private String calculateThreshold(int attrIndex, int splitPortion)
	{
			ArrayList<BigDecimal> al = new ArrayList<BigDecimal>();
			int i,j;	
			BigDecimal val;
			
			for(i=0; i<t; i++)
			{
				if (input[i][attrIndex].compareTo("?")==0)
					continue;
				try
				{
					val = new BigDecimal(input[i][attrIndex].trim());
					al.add(val);
				}
				catch(NumberFormatException e)
				{
					//System.out.println(input[i][attrIndex]);
					System.out.println("Continuous attribute values should be real numbers");
					System.exit(0);
				}
				
			}
			
			Collections.sort(al);
			
			BigDecimal threshold = al.get(((splitPortion+1)*(al.size()-1))/numSplits);
			return threshold.toString();
	}
	
	private String makeDiscrete(String value, int attrIndex)
	{
			for(int i=0; i<numSplits-1; i++)
			{
				if (value.compareTo(thresholdVal[attrIndex][i]) <= 0)
					return new Integer(i+1).toString();
			}
			return new Integer(numSplits).toString();
	}
	
	private HashMap<String, Integer> getInstances(int [] testCases, int attributeIndex)//Returns an instance TreeSet containing all instances.
	{
			HashMap<String, Integer> instMap = new HashMap();
			int occ;
			
			for(int i = 0; i<Array.getLength(testCases); i++)
			{
				if(input[testCases[i]][attributeIndex].compareTo("?") == 0)
					continue;
				if (instMap.containsKey(input[testCases[i]][attributeIndex]))
				{
					occ = instMap.remove(input[testCases[i]][attributeIndex]);
					instMap.put(input[testCases[i]][attributeIndex], new Integer(occ+1));
				}
				else
					instMap.put(input[testCases[i]][attributeIndex], new Integer(1));
			}
			
			return instMap;
	}
	


		
			
}		

