package ml;

import java.util.*;
import java.io.*;
import java.math.*;
import java.lang.*;
import java.lang.reflect.*;

public class NeuralNetwork
{
	private static String input[][], thresholdVal[][];
	private static String fileName, delimStr;
	private static String firstLine[], missingValues[], inputTest[];
	private static int n, t, numSplits, numHiddenLevels, numOutputNodes, numInputNodes, maxSplits = 10, valSize;
	private static HashMap<Integer, Node> nodeMapArray[];
	private static double learningRate, localMinimaLimit = 0.01;
		
	private class Node
	{
		int nodeId;
		int nodeLevel; //level of the node - 0 is input node. 
		double nodeSum;
		double nodeOutput;
		double nodeDelta;
			
		int numAttribute; //only for input nodes. 
		String attrVal; //only for input nodes. 
		Edge fromEdges[];
		
		public Node (int nId, int nLevel)
		{
			if((nLevel == 0 || nLevel == numHiddenLevels+1) && nId != -1)
			{
				System.out.println("Wrong constuctor used for Input/Output nodes");
				System.exit(1);
			}
			
			nodeId = nId;
			nodeLevel = nLevel;
			
			if(nId == -1)
			{
				nodeSum=1;
				nodeOutput = 1;			
			}
			else
			{
				nodeSum = 0;
				nodeOutput = 0;
			}
		}
		
		public Node(int nId, int nLevel, int attrNum, String attrStr) //Only used for input nodes.
		{
			if(nLevel!=0 || nId==-1)
			{
				System.out.println("Wrong constructor - this should be used only for input nodes");
				System.exit(1);
			}
			
			nodeId = nId;
			nodeLevel = nLevel;
			numAttribute = attrNum;
			attrVal = attrStr;
			nodeSum = 0;
			nodeOutput = 0;
		}
		
		public Node(int nId, int nLevel, String attrStr)
		{
			if(nLevel!=numHiddenLevels+1)
			{
				System.out.println("The 3 argument constructor is only for output nodes");
				System.exit(1);
			}
			
			nodeId = nId;
			nodeLevel = nLevel;
			attrVal = attrStr;
			nodeSum = 0;
			nodeOutput = 0;
		}
		
		public void clearNode()
		{
			if(nodeId != -1)
			{
				nodeSum = 0;
				nodeOutput = 0;
				nodeDelta = 0;
			}
		}
		
		public void calculateOutput()
		{
			nodeOutput = 1/(1+(Math.exp(-nodeSum)));
		}
		
		public void calculateDeltaOutputNode(int testCase)
		{
			if(nodeId == -1)
				return;
			if(nodeLevel != numHiddenLevels+1)
			{
				System.out.println("This function is to calculate delta for only output nodes");
				System.exit(1);
			}
			
			double target;
			
			if(attrVal.compareTo(input[testCase][n]) == 0)
				target = 1.0;
			else
				target = 0.0;
				
			nodeDelta = nodeOutput*(1.0-nodeOutput)*(target-nodeOutput);	
		}
		
		public void calculateDeltaHiddenNode()
		{
			if(nodeId == -1)
				return;
			
			if(nodeLevel==0||nodeLevel==numHiddenLevels+1)
			{
				System.out.println("This function is only used to calculate delta for hidden nodes");
				System.exit(1);
			}	
			double summationTerm=0.0;
			
			for(int i=0; i<Array.getLength(this.fromEdges); i++)
			{
				summationTerm = summationTerm +((this.fromEdges[i].weightEdge)*(this.fromEdges[i].toNode.nodeDelta));
			}
			
			nodeDelta = summationTerm + (nodeOutput*(1.0-nodeOutput));
		}
	}
	
	private class Edge
	{
		Node fromNode;
		Node toNode;
		double weightEdge;
		
		Edge (Node from, Node to, double weight)
		{
			fromNode = from;
			toNode = to;
			weightEdge = weight;
			
		}
		
	}
	
	private void clearAllNodes ()
	{
		for(int i=0; i<numHiddenLevels+2; i++)
		{
			Iterator<Integer> hIt = nodeMapArray[i].keySet().iterator();
			
			while(hIt.hasNext())
			{
				Node tempNode = nodeMapArray[i].get(hIt.next());
				tempNode.clearNode();
			}
			
		}
	}
	
	public NeuralNetwork (String fName, int numAttributes, int testCases, String delimiter, int limitSplits) throws IOException, FileNotFoundException
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
	
	private void createInputNodes()
	{
		int i, countNodes = 0;
		int []testCases = new int[t];
		
		for(i=0; i<t; i++)
			testCases[i] = i;
		
		HashMap <String, Integer> attrInstances;
		Iterator<String> hIt;
		nodeMapArray[0] = new HashMap<Integer, Node>();
			
		for(i=0; i<n; i++)
		{
			attrInstances = getInstances(testCases, i);	
			hIt = attrInstances.keySet().iterator();
			
			while(hIt.hasNext())
			{
				Node nodeInput = new Node(countNodes, 0, i, hIt.next());
				nodeMapArray[0].put(new Integer(countNodes), nodeInput);
				countNodes++;
			}
		}
		
		Node biasNode = new Node(-1, 0);
		nodeMapArray[0].put(new Integer(-1), biasNode);		
	}
	
	private void createHiddenNodes(int nodeLevel, int numNodes) //Used for creating Hidden and Output Nodes.
	{
		int i; int[] testCases = new int[t];
		
		for(i=0; i<t; i++)
				testCases[i] = i;
		
		HashMap<String, Integer> instancesOutputNodes = getInstances(testCases,n);
		Iterator<String>outIt = instancesOutputNodes.keySet().iterator(); 
		
		nodeMapArray[nodeLevel] = new HashMap<Integer, Node>();
		
		for(i=0; i<numNodes; i++)
		{
			if (nodeLevel != numHiddenLevels+1)
			{
					Node nodeHidden = new Node(i, nodeLevel);
					nodeMapArray[nodeLevel].put(i, nodeHidden);
			}	
			else
			{
					Node nodeOutput = new Node(i, nodeLevel, outIt.next()); 
					nodeMapArray[nodeLevel].put(i, nodeOutput);
			}
		}
		
		Iterator<Integer> hIt = nodeMapArray[nodeLevel-1].keySet().iterator();
		
		while(hIt.hasNext())
		{
			Node prevNode = nodeMapArray[nodeLevel-1].get(hIt.next());
			prevNode.fromEdges = new Edge[numNodes];
			
			for(i=0; i<numNodes; i++)
			{
				Edge edge = new Edge(prevNode, nodeMapArray[nodeLevel].get(new Integer(i)), (2*Math.random() - 1));
				prevNode.fromEdges[i] = edge;
			}
		}
		
		if(nodeLevel == numHiddenLevels+1)//Output Level
			return;
		
		Node biasNode = new Node(-1, nodeLevel);
		nodeMapArray[nodeLevel].put(new Integer(-1), biasNode);
	}
	
	public void createNeuralNetwork(int numLevels, int[] numNodesArray)
	{
		int i;
		int testCases[] = new int[t];
		
		numHiddenLevels = numLevels;
		nodeMapArray = new HashMap[numHiddenLevels+2];
	
		createInputNodes();
		
		for(i=0; i<t; i++)
			testCases[i] = i;
		
		numOutputNodes = countInstances(testCases, n);	
				
		for(i=0; i<numHiddenLevels; i++)
			createHiddenNodes(i+1, numNodesArray[i]);
		
		createHiddenNodes(i+1, numOutputNodes);		
	}
	
	private void assignInputNodeOutputs(int testCase)
	{
		Node tempNode;
		
		for(int i=0; i<n; i++)
		{
			String labelString = input[testCase][i];
			
			Iterator<Integer>hIt = nodeMapArray[0].keySet().iterator();
			
			while(hIt.hasNext())
			{
				tempNode = nodeMapArray[0].get(hIt.next());
				
				if(tempNode.numAttribute == i && tempNode.attrVal.compareTo(labelString) == 0)
				{
					tempNode.nodeOutput = 1;
					break;
				}
			}
		}
	}
	
	private void calculateDeltaNodes(int testCase)
	{
		for(int i=numHiddenLevels+1; i>=1; i--) //Backpropagation - going from output layer to previous layers.
		{
			Iterator<Integer>hIt = nodeMapArray[i].keySet().iterator();
		
			while(hIt.hasNext())
			{
				Node tempNode = nodeMapArray[i].get(hIt.next());
				if(tempNode.nodeId == -1)
					continue;
				else
				{
					if(i==numHiddenLevels+1)
						tempNode.calculateDeltaOutputNode(testCase);
					else
						tempNode.calculateDeltaHiddenNode();
				}
			}
		}
	}
	
	private void updateEdgeWeights()
	{
		for(int i=0; i<numHiddenLevels+1; i++)
		{
			Iterator hIt = nodeMapArray[i].keySet().iterator();
			
			while(hIt.hasNext())
			{
				Node tempNode = nodeMapArray[i].get(hIt.next());
				
				for(int j=0; j<Array.getLength(tempNode.fromEdges); j++)
				{
					Edge tempEdge = tempNode.fromEdges[j];
					Node toNode = tempNode.fromEdges[j].toNode;
					
					tempEdge.weightEdge = tempEdge.weightEdge + (learningRate*toNode.nodeDelta*tempNode.nodeSum);					
				}
			}
			
			
		}
	}
	
	private double calculateError (String outputStr)
	{
		double meanSquareError = 0.0;
		Node tempNode;
		
		Iterator hIt = nodeMapArray[numHiddenLevels+1].keySet().iterator();
		
		while(hIt.hasNext())
		{
			tempNode = nodeMapArray[numHiddenLevels+1].get(hIt.next());
			//System.out.println(tempNode.attrVal);
			if(tempNode.attrVal.compareTo(outputStr) == 0)
				meanSquareError = meanSquareError + Math.pow(1.0-tempNode.nodeOutput, 2.0);
			else
				meanSquareError = meanSquareError + Math.pow(tempNode.nodeOutput, 2.0);
		}
		
		return meanSquareError/numOutputNodes;
	}
			
	public void trainNeuralNetwork (double learnRate, double errorLimit)
	{
		int epochCount = 0;
		double meanSquareError = 0.0;
		double averageErrorRate = 1.0, prevAverageErrorRate = 1.0;
		learningRate = learnRate;
		if(learningRate <= 0 || learningRate > 1)
		{
			System.out.println("Learning Rate should be between 0 and 1");
			System.exit(1);
		}
		int i, j, nodeLevel;
		
		for(i=0; i<t; i++)
		{
			clearAllNodes();
			
			//Forward Propagation
			assignInputNodeOutputs(i);
			nodeLevel = 0;
			
			while(nodeLevel < numHiddenLevels+1)
			{
				Iterator<Integer>hIt = nodeMapArray[nodeLevel].keySet().iterator();
				while(hIt.hasNext())
				{
					Node tempNode = nodeMapArray[nodeLevel].get(hIt.next());
					for(j=0; j<Array.getLength(tempNode.fromEdges); j++)
					{
						Node toNode = tempNode.fromEdges[j].toNode;
						toNode.nodeSum = toNode.nodeSum + ((tempNode.fromEdges[j].weightEdge)*(tempNode.nodeOutput));
					}
				}
				
				nodeLevel++;
				
				hIt = nodeMapArray[nodeLevel].keySet().iterator();
				while(hIt.hasNext())
				{
					Node tempNode = nodeMapArray[nodeLevel].get(hIt.next());
					
					if(tempNode.nodeId == -1)
						continue;
					else
						tempNode.calculateOutput();
				}
			}
			
			meanSquareError = meanSquareError + calculateError(input[i][n]);
						
			//Calculate Deltas for the hidden and output nodes - Backpropagation.
			calculateDeltaNodes(i);
			
			//Update the weights of the edges.
			updateEdgeWeights();
			
			if(i == t-1) //Epoch completed - go to new epoch. 
			{
				averageErrorRate = meanSquareError/t;
				
				System.out.println(averageErrorRate);
				
				if(averageErrorRate <= errorLimit && epochCount>0) //If error rate less than limiting rate.
				{
					System.out.println(epochCount+1);
					return;
				}
				
				if(Math.abs(prevAverageErrorRate - averageErrorRate) < 0.00005) //Momentum cannot go over this hill.
				{
					System.out.println(epochCount+1);
					return;
					}
				
				//Check for local minima.
				if (epochCount>0 && Math.abs(prevAverageErrorRate - averageErrorRate) < localMinimaLimit)
					learningRate = 2*learnRate;  //Momentum
				else
					learningRate = learnRate;
								
				i = -1;
				epochCount++;
				prevAverageErrorRate = averageErrorRate;
				meanSquareError = 0.0;
			}
		}
	}
	
	private String getOutput(String inputStr)
	{
			clearAllNodes();
			String[] inStr = new String[n];
			inStr = inputStr.split(delimStr);
			
			int i,j;
			
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
						inStr[i] = makeDiscrete(inStr[i], i);
				}
				
				input[0][i] = inStr[i];
			}
			
			assignInputNodeOutputs(0);
			
			int nodeLevel = 0;
			Iterator<Integer>hIt;
			
			while(nodeLevel < numHiddenLevels+1)
			{
				hIt = nodeMapArray[nodeLevel].keySet().iterator();
				while(hIt.hasNext())
				{
					Node tempNode = nodeMapArray[nodeLevel].get(hIt.next());
					for(j=0; j<Array.getLength(tempNode.fromEdges); j++)
					{
						Node toNode = tempNode.fromEdges[j].toNode;
						toNode.nodeSum = toNode.nodeSum + ((tempNode.fromEdges[j].weightEdge)*(tempNode.nodeOutput));
					}
				}
				
				nodeLevel++;
				
				hIt = nodeMapArray[nodeLevel].keySet().iterator();
				while(hIt.hasNext())
				{
					Node tempNode = nodeMapArray[nodeLevel].get(hIt.next());
					
					if(tempNode.nodeId == -1)
						continue;
					else
						tempNode.calculateOutput();
				}
			}
			
			hIt = nodeMapArray[numHiddenLevels+1].keySet().iterator();
			double maxOutputValue = -1; String answer="";
			
			while(hIt.hasNext())
			{
				Node tempNode = nodeMapArray[numHiddenLevels+1].get(hIt.next());
				
				if(maxOutputValue < tempNode.nodeOutput)
				{
					maxOutputValue = tempNode.nodeOutput;
					answer = tempNode.attrVal;
				}
			}
			
			return answer;	
	}
	
	public float getAccuracy(String fTestName, int validationSize, String delim) throws IOException, FileNotFoundException
		{
			valSize = validationSize;
			
			FileInputStream fstreamTest = new FileInputStream(fTestName);
            DataInputStream inTest = new DataInputStream(fstreamTest);
            
            int lineCount = 0, correct = 0, ignoreLines = 0;
            inputTest = new String[valSize];
            
            String tempInput;
            
            while(lineCount < valSize)
            {
					inputTest[lineCount] = inTest.readLine();
					
					if(Array.get(inputTest[lineCount].split(delim),n).toString().compareTo("?") == 0)
					{
						lineCount++;
						ignoreLines++;
						continue;
					}
					
					if ((Array.get(inputTest[lineCount].split(delim), n).toString()).compareTo(getOutput(inputTest[lineCount])) == 0)
						correct++;
					
					lineCount++;
			}
			
			return (float)correct/(float)(valSize - ignoreLines);			
			
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
	
	private int countInstances (int[] testCases, int attributeIndex) //Counts the number of unique values of a particular attribute over the given testcases. 
	{
			TreeSet ts = new TreeSet();
			
			for(int i = 0; i<Array.getLength(testCases); i++)
			{
				if(input[testCases[i]][attributeIndex].compareTo("?") != 0)
					ts.add(input[testCases[i]][attributeIndex]);
			}
			return ts.size();		
	}
		
}		