package ml;

import java.util.*;
import java.io.*;
import java.math.*;
import java.lang.*;
import java.lang.reflect.*;
import ml.FeatureWeights;

public class KNearestNeighbor
{
	private static String input[][];
	private static String fileName, delimStr;
	private static String firstLine[], missingValues[], inputTest[];
	private static BigDecimal meanValue[], stdValue[];
	private static BigDecimal smallValue = new BigDecimal("0.001");
	private static int n, t, valSize, kValue, precisionValue = 5;
	private static MathContext mC = new MathContext(precisionValue);
	private static boolean isFeatureWeighting;
	private static float[] weightAttr;
	
	
	TreeMap<BigDecimal, Integer>sortedDistanceValues;
	HashMap<String, BigDecimal> labelCount;
	LinkedList<Integer> inclusiveSet; 
		
	@SuppressWarnings("deprecation")
	public KNearestNeighbor (String fName, int numAttributes, int testCases, String delimiter, boolean toCondense, boolean featureWeighting) throws IOException, FileNotFoundException
	{
		
			fileName = fName;
			n = numAttributes;
			t = testCases;
			delimStr = delimiter;
			isFeatureWeighting = featureWeighting;
			
			weightAttr = new float[n];
					
			FileInputStream fstream = new FileInputStream(fileName);
            DataInputStream in = new DataInputStream(fstream);
            
            //Parse the first line to see if continuous or discrete attributes. 
            firstLine = new String[n];
            
            firstLine = in.readLine().split(delimiter);
            //firstLine[n] = "d";
            
            int i, j, lineCount = 0;
            
            for(i=0; i<n; i++)
            	weightAttr[i] = 1;
                         
            input = new String[t][n+1];
                       
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
            
            t = testCases - invalidLines;
            System.out.println(t);
            if (kValue > t)
            	kValue = t;
            
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
			
			meanValue = new BigDecimal[n];
			stdValue = new BigDecimal[n];
						
			//Pre-processing the continuous variables.         
            for(i=0; i<n; i++)
            {
				if(allCont || firstLine[i].compareTo("c") == 0) //Continuous Attribute
					//Preprocessing begins here.
					findMeanStd(i);
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
					//Normalize the values to between 0 and 1.
						input[i][j] = normalizeValue(i,j);
				}
			}
			
			inclusiveSet = new LinkedList<Integer>();
			
			if (featureWeighting)
			{
				FeatureWeights fWt = new FeatureWeights(fileName, n, t, delimStr, 3);
				weightAttr = fWt.getWeights();
			}
			
			if (toCondense)
				condenseTrainingSet();
			else
			{	
				for(i=0; i<t; i++)
					inclusiveSet.add(new Integer(i));
			}
			
			System.out.println("");
			for(i=0; i<n; i++)
				System.out.println(weightAttr[i]);
			System.out.println("");
	}	
	
	private void condenseTrainingSet() //Condensation to reduce the size of the training set.
	{
		int i;
		boolean canContinue = true;
		inclusiveSet.add(new Integer(0));
		Iterator<Integer> listIt;
		
		while(canContinue)
		{
			//System.out.println(inclusiveSet.size());
			canContinue = false;
			List <Integer>randomList = new LinkedList<Integer>();
			
			for(i=0; i<t; i++)
			{
				if(!inclusiveSet.contains(new Integer(i)))
					randomList.add(new Integer(i));
			}
			
			Collections.shuffle(randomList);
			listIt = randomList.iterator();
			
			while (listIt.hasNext())
			{
				i = listIt.next().intValue();
				//System.out.println(getOutput(joinStrings(i), 1));
				if(getOutput(joinStrings(i), 1).compareTo(input[i][n]) != 0)
				{
					inclusiveSet.add(new Integer(i));
					//System.out.println("**"+input[i][n]);
					canContinue = true;
				}
			}
		}
		
		System.out.println("Condensation done !!! inclusiveSet size = "+ inclusiveSet.size());
	}
	
	private String joinStrings(int testCase)
	{
		String finalStr = "";
		
		for(int i=0; i<n; i++)
		{
			finalStr = finalStr.concat(input[testCase][i]);
			if (i != n-1)
				finalStr = finalStr.concat(delimStr);
		}
				
		return finalStr;
	}
	
	private String normalizeValue (int testCase, int index)
	{
		return (new BigDecimal(input[testCase][index], mC).subtract(meanValue[index], mC)).divide(stdValue[index], precisionValue, RoundingMode.HALF_UP).toString();
	}
	
	private void findMeanStd(int index)
	{
		BigDecimal sum = new BigDecimal("0", mC), sqSum = new BigDecimal("0", mC);
		BigDecimal mean, std, diff;
		
		for(int i=0; i<t; i++)
			sum = sum.add(new BigDecimal(input[i][index], mC));
			
		mean = sum.divide(new BigDecimal(t, mC), precisionValue, RoundingMode.HALF_UP);
		
		for(int i=0; i<t; i++)
		{
			diff = mean.subtract(new BigDecimal(input[i][index], mC));
			sqSum = sqSum.add(diff.multiply(diff, mC));
		}
		
		sqSum = sqSum.divide(new BigDecimal(t, mC), precisionValue, RoundingMode.HALF_UP);
		stdValue[index] = new BigDecimal(Math.sqrt(sqSum.doubleValue()), mC);
		meanValue[index] = mean;
	}
	
	private void substituteMissingValue(boolean isContinuous, int testCase, int attrNum)
	{
		int i, missValues = 0;
		
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
				input[testCase][attrNum] = sum.divide(new BigDecimal(t - missValues), precisionValue, RoundingMode.HALF_DOWN).toString();
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
	
	private void computeDistances(String[] inStr)
	{
		int i, j;
		BigDecimal distValue, tempValue;
		sortedDistanceValues = new TreeMap<BigDecimal, Integer>();
		ListIterator<Integer> hIt = inclusiveSet.listIterator();
						
		while(hIt.hasNext())
		{
			i = hIt.next().intValue();
			distValue = BigDecimal.ZERO;
			for(j=0; j<n; j++)
			{
				//System.out.println(inStr[j]);
				if (firstLine[j].compareTo("d") == 0)
				{
					if (inStr[j].compareTo(input[i][j]) != 0)
					{
						if (isFeatureWeighting)
					
							distValue = distValue.add(new BigDecimal(weightAttr[j], mC));
						else
							distValue = distValue.add(new BigDecimal("1", mC));
					}
				}		
				else
				{
					tempValue = new BigDecimal(input[i][j]).subtract(new BigDecimal(inStr[j]), mC);
					tempValue = tempValue.multiply(tempValue, mC);
					if (isFeatureWeighting)
						tempValue = tempValue.multiply(new BigDecimal(weightAttr[j], mC));
					distValue = distValue.add(tempValue, mC);
				}
				//System.out.println(j);
			}
			//System.out.println(distValue);
			sortedDistanceValues.put(new BigDecimal(Math.sqrt(distValue.doubleValue()), mC), new Integer(i));
		}
	}
	
	private String getOutputClassLabel()
	{
		labelCount = new HashMap<String, BigDecimal>();
		String labelStr;
		Integer testCase;
		BigDecimal value;
		
		if (kValue > sortedDistanceValues.size())
			kValue = sortedDistanceValues.size();
		
		for(int i=0; i<kValue; i++)
		{
			value = sortedDistanceValues.firstKey();
			testCase = sortedDistanceValues.get(value);
			value = value.add(smallValue, mC);
			labelStr = input[testCase][n];
			sortedDistanceValues.remove(value);
			
			//System.out.println(testCase);
			//System.out.println(value);
			
			if(labelCount.containsKey(labelStr))
				labelCount.put(labelStr, labelCount.get(labelStr).add(BigDecimal.ONE.divide(value, precisionValue, RoundingMode.HALF_UP), mC));
			else
				labelCount.put(labelStr, BigDecimal.ONE.divide(value, precisionValue, RoundingMode.HALF_UP));
			
			while(kValue!=t && sortedDistanceValues.size() > 0 && sortedDistanceValues.firstKey().compareTo(value) == 0)
			{
				i++;
				
				value = sortedDistanceValues.firstKey();
				testCase = sortedDistanceValues.get(value);
				//System.out.println(value);
				labelStr = input[testCase][n];
				sortedDistanceValues.remove(testCase);
				
				if(labelCount.containsKey(labelStr))
					labelCount.put(labelStr, labelCount.get(labelStr).add(BigDecimal.ONE.divide(value, precisionValue, RoundingMode.HALF_UP), mC));
				else
					labelCount.put(labelStr, BigDecimal.ONE.divide(value, precisionValue, RoundingMode.HALF_UP));
			}
		}
		
		//System.out.println(labelCount);
		Iterator<String> hIt = labelCount.keySet().iterator();
		String ansLabel = "", tempLabel;
		BigDecimal maxValue = BigDecimal.ZERO, tempValue;
		
		while(hIt.hasNext())
		{
			tempLabel = hIt.next();
			tempValue = labelCount.get(tempLabel);
			
			if(tempValue.compareTo(maxValue) > 0)
			{
				maxValue = tempValue;
				ansLabel = tempLabel;
			}
		}
		
		//System.out.println(ansLabel);
		return ansLabel;
		
	}
		
	private String getOutput(String inputStr, int kVal)
	{
		String[] inStr = new String[n];
		inStr = inputStr.split(delimStr);
		kValue = kVal;
		
		int i;
		
		//Sanitize input String.
		for(i=0; i<n; i++)
		{
			if(inStr[i] == null)
			{
				System.out.println("Invalid Test String");
				return null;
			}
			
			if(inStr[i].compareTo("?") == 0)
				inStr[i] = missingValues[i];
			else
			{
				 if(firstLine[i].compareTo("c") == 0)
					inStr[i] = (new BigDecimal(inStr[i], mC).subtract(meanValue[i], mC)).divide(stdValue[i], precisionValue, RoundingMode.HALF_UP).toString();
			}
			
		}
		
		computeDistances(inStr);
		return getOutputClassLabel();
	}
	
	@SuppressWarnings("deprecation")
	public float getAccuracy(String fTestName, int validationSize, String delim, int kVal) throws IOException, FileNotFoundException
	{
		
		
		valSize = validationSize;
		
		FileInputStream fstreamTest = new FileInputStream(fTestName);
        DataInputStream inTest = new DataInputStream(fstreamTest);
        
        int lineCount = 0, correct = 0, ignoreLines = 0;
        inputTest = new String[valSize];
        
        while(lineCount < valSize)
        {
				inputTest[lineCount] = inTest.readLine();
				
				if(Array.get(inputTest[lineCount].split(delim),n).toString().compareTo("?") == 0)
				{
					lineCount++;
					ignoreLines++;
					continue;
				}
				
				if ((Array.get(inputTest[lineCount].split(delim), n).toString()).compareTo(getOutput(inputTest[lineCount], kVal)) == 0)
					correct++;
				
				lineCount++;
		}
        //System.out.println(ignoreLines);
		
		return (float)correct/(float)(valSize - ignoreLines);			
		
	}

		
	private HashMap<String, Integer> getInstances(int [] testCases, int attributeIndex)//Returns an instance TreeSet containing all instances.
	{
		HashMap<String, Integer> instMap = new HashMap<String, Integer>();
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
