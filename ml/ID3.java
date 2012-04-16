/* Constructing the ID3 Decision Tree for Machine Learning problems */
/* Author: Harihara K Narayanan - Grad Student, Texas A&M University */

package ml;
 
import java.io.*;
import java.lang.reflect.Array;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Math;
import java.lang.NumberFormatException;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
 
public class ID3
{
		private static String[][]input;
		private static String[] inputTest;
		private static int n,t; //number of attributes and number of test_cases;
		private static int valSize;
		private static int maxDepthNode = 1;
		private static String fileName;
		private static String[] firstLine;
		private static TreeNode root;
		private static LinkedList unusedAttr = new LinkedList();
		private static int gObjectX;
		private static int gObjectY;
		private static int nodeDiameter;
		private static int minX = Integer.MAX_VALUE, maxX = 0, maxY=0;
		private static int numColors = 9;
		private static int legendX;
		private static int legendY;
		private static int numSplits;
		private static int maxSplits = 10;
		private static String[][] thresholdVal;
		
		private static float pessError = (float)0.5; //pessimistic error for post pruning.
				
		private static Graphics gObject;
		
		public ID3(String fName, int numAttributes, int testCases, String delimiter, int limitSplits) throws IOException, FileNotFoundException
		{
			fileName = fName;
			n = numAttributes;
			t = testCases;
			numSplits = limitSplits;
					
			FileInputStream fstream = new FileInputStream(fileName);
            DataInputStream in = new DataInputStream(fstream);
            
            //Parse the first line to see if continuous or discrete attributes. 
            firstLine = new String[n];
            
            firstLine = in.readLine().split(delimiter);
            
            int i, j, lineCount = 0;
                         
            for(i=0; i<n; i++)
				unusedAttr.add(new Integer(i));
                                
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
						input[i][j] = makeContinuous(input[i][j], j);
					
				}
						
			}
		}	
		
		public float findAccuracy(String fTestName, int validationSize, String delim) throws IOException, FileNotFoundException
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
		
		private String maxOutputLabel(TreeNode node)
		{
			HashMap<String, Integer> instMap = getInstances(node.testCases, n);
			
			Iterator it = instMap.keySet().iterator();
			Integer max = Integer.MIN_VALUE, val;
			String answer = ""; String label;
			while(it.hasNext())
			{
				label = it.next().toString();
				val = instMap.get(label);
				
				if (val.compareTo(max) > 0)
				{
					max = val;
					answer = label;
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
		
		private double getChiSquareValue (int dof) //Chi-Squared values for pre-pruning (threshold).
		{
			switch(dof)
			{
				case 1: return 3.841;
				case 2: return 5.991;
				case 3: return 7.815;
				case 4: return 9.488;
				case 5: return 11.070;
				case 6: return 12.592;
				case 7: return 14.067;
				case 8: return 15.507;
				case 9: return 16.919;
				case 10: return 18.307;
				case 11: return 19.675;
				case 12: return 21.026;
				case 13: return 22.362;
				case 14: return 23.685;
				case 15: return 24.996;
				case 16: return 26.296;
				case 17: return 27.587;
				case 18: return 28.869;
				case 19: return 30.144;
				case 20: return 31.410;
				default: return (3*dof)/2;
			}
		}
		
		private Color getNodeColor(int attr) //get the node color depending upon its splitAttribute.
		{
			Color nodeColor;
			switch(attr)
			{
				case 0: nodeColor = Color.BLUE; break;
				case 1: nodeColor = Color.YELLOW; break;
				case 2: nodeColor = Color.RED; break;
				case 3: nodeColor = Color.GREEN; break;
				case 4: nodeColor = Color.CYAN; break;
				case 5: nodeColor = Color.PINK; break;
				case 6: nodeColor = Color.GRAY; break;
				case 7: nodeColor = Color.ORANGE; break;
				case 8: nodeColor = Color.MAGENTA; break;
				default: nodeColor = Color.BLACK;
			}
			
			return nodeColor;
		}
		
		public int returnNumNodes()
		{
			int count = 0;
			TreeNode temp;
			Stack<TreeNode> stNode = new Stack<TreeNode>();
			stNode.push(root);
			
			while(!stNode.isEmpty())
			{
				temp = stNode.pop();
				count++;
				
				if(temp.childPtr == null)
					continue;
				else
				{
					for(int i=0; i<Array.getLength(temp.childPtr); i++)
						stNode.push(temp.childPtr[i]);
				}
			}
			
			return count;	
				
		}
		
		private void findNodesPosition (TreeNode node) //Calculate the node positions.
		{
				int widthVal = 0;
				if (node.childPtr == null)
					return;
				int childSize = Array.getLength(node.childPtr);
				boolean isEven = false;
				float mid = 0;
				if(childSize%2 == 0)
				{
						isEven = true;
						mid = (float)(childSize-1)/2;
				}
				
				if(childSize != 0)
					widthVal = 2*(maxDepthNode - node.depthNode + 1)/childSize;
				
				for(int i=0; i<childSize; i++)
				{
					node.childPtr[i].posY = node.posY + 2*nodeDiameter;
					
					if (isEven)
						node.childPtr[i].posX = (int)(((float)i - mid)*widthVal*nodeDiameter) + (node.posX);
					else
						node.childPtr[i].posX = ((i-(childSize/2))*widthVal*nodeDiameter) + (node.posX);
					
					if(minX > node.childPtr[i].posX)
						minX = node.childPtr[i].posX;
						
					if(maxX < node.childPtr[i].posX)
						maxX = node.childPtr[i].posX;
						
					if(maxY < node.childPtr[i].posY)
						maxY = node.childPtr[i].posY;
						
					findNodesPosition(node.childPtr[i]);
				}
					
		}
		
		private void drawNode(TreeNode node, int translate)
		{
			String legendEntry;
			
			gObject.drawOval(node.posX, node.posY, nodeDiameter, nodeDiameter);
						
			if(node.childPtr == null)
				return;
				
			Color nodeColor = getNodeColor(node.splitAttribute%numColors);
			int numBrighter = node.splitAttribute/numColors;
			numBrighter = numBrighter*3;
			
			while(numBrighter>0)
			{
					nodeColor = nodeColor.darker();
					numBrighter--;
			}
			
			gObject.setColor(nodeColor);
			gObject.fillOval(node.posX-1, node.posY-1, nodeDiameter+1, nodeDiameter+1);
			
			gObject.drawOval(legendX, legendY, nodeDiameter*2/3, nodeDiameter*2/3); //Fill in the legend entry.
			gObject.fillOval(legendX-1, legendY-1, nodeDiameter*2/3, nodeDiameter*2/3);
			gObject.setColor(Color.WHITE);
			legendEntry = new String("Attr.#").concat(new Integer(node.splitAttribute +1).toString());
			gObject.drawString(legendEntry, legendX + (nodeDiameter*3/2), legendY+(nodeDiameter/2));
			
			legendY += (nodeDiameter*3/2);			
			
			for(int i=0; i<Array.getLength(node.childPtr); i++)
			{
				node.childPtr[i].posX = node.childPtr[i].posX + translate;
				gObject.drawLine(node.posX + (nodeDiameter/2), node.posY + (nodeDiameter/2), node.childPtr[i].posX + (nodeDiameter/2), node.childPtr[i].posY+(nodeDiameter/2));	
				drawNode(node.childPtr[i], translate);
			}	
			
		}
				
		public void drawDecisionTree(String outFile, int NodeSize) throws FileNotFoundException, IOException //Draw Decision Tree to file.
		{
			nodeDiameter = NodeSize;
			root.posY = 3*nodeDiameter/2;
			root.posX = 500;
					
			outFile = outFile.concat(".png");
			File f = new File(outFile);
			
			Font font = new Font("Arial", Font.PLAIN, 20);
			
			int minAdjust = 0;
			int translate, newCenter;
			boolean moveRight;
			
			if(!f.exists())
				f.createNewFile();
			
			findNodesPosition(root);
			maxX = Math.max(maxX, root.posX);
			minX = Math.min(minX, root.posX);
			
			translate = 5-minX;
			maxX = translate + maxX;
						
			legendX = maxX + (nodeDiameter*3/2);
			legendY = root.posY;
			
			gObjectX = legendX + 8*nodeDiameter;
			gObjectY = maxY + 2*nodeDiameter; 
			BufferedImage bImage = new BufferedImage(gObjectX, gObjectY, BufferedImage.TYPE_INT_RGB);
			gObject = bImage.getGraphics();
			gObject.setFont(font);
			
			root.posX += translate;
			drawNode(root, translate);
			ImageIO.write(bImage, "png", f);
				
		}
		
		private String getOutput(String str) //Output a class label based on decisiontree.
		{
			String[] inStr = new String[n];
			inStr = str.split(",");
			
			int i;
			
			for(i=0; i<n; i++)
			{
				if(inStr[i] == null)
				{
					System.out.println("Invalid Test String");
					return null;
				}
				
				if(firstLine[i].compareTo("c") == 0)
					inStr[i] = makeContinuous(inStr[i], i);
			}
				
			if(root == null)
			{
				System.out.println("Training has to be performed first");
				return null;
			}
			
			boolean found = false;
			TreeNode temp = root;
			
			while(temp.childPtr != null)
			{
						found = false;
						for(i=0; i<Array.getLength(temp.childPtr); i++)
						{
							if (temp.childPtr[i].parentLabel.compareTo(inStr[temp.splitAttribute]) == 0)
							{
								temp = temp.childPtr[i];
								found = true;
								break;
							}
						}
						
						if(!found)
						{
							return maxOutputLabel(temp);
						}
			}
			
			HashMap<String, Integer> instMap = getInstances(temp.testCases, n);
			
			Iterator<String> instIt = instMap.keySet().iterator();
			int max = 0, val;
			String label, answer = "";
			
			while(instIt.hasNext())
			{
				label = instIt.next();
				val = instMap.get(label).intValue();
				
				if(val > max)
				{
					max = val;
					answer = label;
				} 
			}
			
			return answer;
									
		}
		
		private String makeContinuous(String value, int attrIndex)
		{
			for(int i=0; i<numSplits-1; i++)
			{
				if (value.compareTo(thresholdVal[attrIndex][i]) <= 0)
					return new Integer(i+1).toString();
			}
			
			return new Integer(numSplits).toString();
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
		
		public void postPruneTree() //Postpruning
		{
			int wrongCases, i;
			
			if(root == null)
			{
				System.out.println("First construct the tree before post-pruning");
				System.exit(1);
			}
			
			TreeNode node = root;
			Stack<TreeNode> nodeStack = new Stack();
			nodeStack.push(node);
			
			while(!nodeStack.empty())
			{
				TreeNode tempNode = nodeStack.pop();
				if(tempNode.childPtr == null)
					continue;
				
				wrongCases = Array.getLength(tempNode.testCases) - (getInstances(tempNode.testCases, n).get(tempNode.maxLabel));
				float preSplitError = (float)(wrongCases + pessError)/(float)Array.getLength(tempNode.testCases);
				float postSplitError = 0;
				
				wrongCases = 0;
				
				for(i=0; i<Array.getLength(tempNode.childPtr); i++)
					wrongCases = wrongCases + Array.getLength(tempNode.childPtr[i].testCases) - (getInstances(tempNode.childPtr[i].testCases, n).get(tempNode.childPtr[i].maxLabel)); ;
					
				postSplitError = (float)(wrongCases + (float)(pessError*i))/(float)Array.getLength(tempNode.testCases);
				
				if(postSplitError > preSplitError)
				{
						tempNode.childPtr = null; continue;
				}
				
				for(i=0; i<Array.getLength(tempNode.childPtr); i++)
						nodeStack.push(tempNode.childPtr[i]);
			}
		}
		
		public void createDecisionTree(boolean isPrePruning) //Build Decision Tree for input.
        {
                int i,j;                
                int[] cases = new int[t];
                for(i=0;i<t;i++)
					cases[i] = i;
                
                //Start constructing the tree.
                Stack<TreeNode> treeStack = new Stack<TreeNode>();
                TreeNode node = new TreeNode(cases);
                root = node;
                root.depthNode = 1;
                              
                root.parentPtr = null;
                root.maxLabel = maxOutputLabel(root);
                HashMap<String, Integer> tempMap;
                Iterator<String> tempIt, tempIt2;
                String mostCommonLabel = "";
                
                ArrayList tempList;
                int tempSize;
				int[] tempArray;	
				String label;
                treeStack.push(node);
                
                while(!treeStack.isEmpty() && unusedAttr.size()>0)
                {
					TreeNode tempNode = treeStack.pop();
					
					if(countInstances(tempNode.testCases, n) == 1)
					{
						tempNode.childPtr = null;
						continue;
					}
						
					tempNode.findSplitAttribute();
					unusedAttr.remove(new Integer(tempNode.splitAttribute));
					
					tempMap = getInstances(tempNode.testCases, tempNode.splitAttribute);
					tempSize = tempMap.size();
					
					tempIt2 = tempMap.keySet().iterator();
					int max = 0, val; String key;
					
					if(isPrePruning)
					{
						float chiSquareThreshold = (float)getChiSquareValue((countInstances(tempNode.testCases,n)-1)*(countInstances(tempNode.testCases,tempNode.splitAttribute)));
						float chiSquare = 1;
										
						tempIt2 = tempMap.keySet().iterator();
						while(tempIt2.hasNext())
							chiSquare = (chiSquare*(tempMap.get(tempIt2.next())))/(float)(Array.getLength(tempNode.testCases));
												
						chiSquare = chiSquare*(float)(Array.getLength(tempNode.testCases));
						
						if(chiSquare > chiSquareThreshold) //Stop expanding the node further - Statistically split is not significant.
							continue;
					}
					
					while(tempIt2.hasNext())
					{
						key = tempIt2.next();
						val = tempMap.get(key);
						if(max < val)
						{
							max = val;
							mostCommonLabel = key;
						}
					} 
					
					tempNode.childPtr = new TreeNode [tempSize]; //Create as many child nodes as there are categories in the splitAttribute.					
					tempIt = tempMap.keySet().iterator();

					for(i=0; i<tempSize; i++) 
					{
							tempList = new ArrayList();
							label = tempIt.next();
							
							for(j=0; j<Array.getLength(tempNode.testCases); j++)
							{
								if(input[tempNode.testCases[j]][tempNode.splitAttribute].compareTo("?") == 0)
								{
									if (label.compareTo(mostCommonLabel) == 0)
										tempList.add(tempNode.testCases[j]);
									continue;	
								}		
								    
								if (input[tempNode.testCases[j]][tempNode.splitAttribute].compareTo(label) == 0)
									tempList.add(tempNode.testCases[j]);
							}
								
							tempArray = new int[tempList.size()];
							
							for(j=0; j<tempList.size(); j++)
								tempArray[j] = new Integer(tempList.get(j).toString()).intValue();
								
							
							TreeNode tempNode2 = new TreeNode(tempArray);
							tempNode2.parentLabel = label;
							tempNode2.maxLabel = maxOutputLabel(tempNode2);

							//System.out.println(label);
							tempNode2.depthNode = tempNode.depthNode + 1;
							if(tempNode2.depthNode > maxDepthNode)
								maxDepthNode = tempNode2.depthNode;
							tempNode.childPtr[i] = tempNode2;
							tempNode2.parentPtr = tempNode;
							treeStack.push(tempNode2);
					}
										
				}
         }
         
         //public void pruneDecisionTree(int validationSize) Prunes the decision tree based on the first 
         
         private class TreeNode //Decision Tree data structure.
		 {
			int[] testCases;
			int splitAttribute;
			
			int posX, posY; //Coordinates of the node's center - for drawing.
			int depthNode; //Depth of this Node.
			
			TreeNode childPtr[];
			TreeNode parentPtr;
			
			String parentLabel;
			String maxLabel;
			
			TreeNode(int[] cases)
			{
				int size = Array.getLength(cases);
				testCases = new int[size];			
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
				float max = Long.MIN_VALUE;
				float val;
				int finalAttribute = -1, attr;
				ListIterator lIt = unusedAttr.listIterator();
				
				while(lIt.hasNext())
				{
					attr = new Integer(lIt.next().toString()).intValue();
					val = getInformationGain(testCases, attr);
					if (val>max)
					{
						max = val;
						finalAttribute = attr;
					}
				}
				
				splitAttribute = finalAttribute;
			}
			
		}
}
