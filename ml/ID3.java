/* Constructing the ID3 Decision Tree for Machine Learning problems */
/* Author: Harihara K Narayanan - Grad Student, Texas A&M University */

package ml;
 
import java.io.*;
import java.lang.reflect.Array;
import java.lang.Integer;
import java.lang.Long;
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
import java.awt.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
 
public class ID3
{
		private static String[][]input;
		private static int n,t; //number of attributes and number of test_cases;
		private static String fileName;
		private static TreeNode root;
		private static LinkedList unusedAttr = new LinkedList();
		private static int gObjectX;
		private static int gObjectY;
		private static int nodeDiameter;
		private static int minX = Integer.MAX_VALUE, maxX = 0, maxY=0;
		private static int numColors = 9;
		private static int legendX;
		private static int legendY;
			
		private static Graphics gObject;
		
		public ID3(String fName, int numAttributes, int testCases)
		{
			fileName = fName;
			n = numAttributes;
			t = testCases;
		}
		
		private int countInstances (int[] testCases, int attributeIndex) //Counts the number of unique values of a particular attribute over the given testcases. 
		{
			TreeSet ts = new TreeSet();
			
			for(int i = 0; i<Array.getLength(testCases); i++)
				ts.add(input[testCases[i]][attributeIndex]);
			
			return ts.size();		
		}
		
		private HashMap<String, Integer> getInstances(int [] testCases, int attributeIndex)//Returns an instance TreeSet containing all instances.
		{
			HashMap<String, Integer> instMap = new HashMap();
			int occ;
			
			for(int i = 0; i<Array.getLength(testCases); i++)
			{
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
				fraction = new BigDecimal(hIt.next().toString()).divide(new BigDecimal(Array.getLength(testCases)), 2, RoundingMode.HALF_DOWN);
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
		
		/*private void buildNodeMap(TreeNode rootNode)
		{
			int hmKey, hmValue;
			while(rootNode.childPtr != null)
			{
				for(int i=0; i<Array.getLength(rootNode.childPtr); i++)
				{
					hmKey = rootNode.childPtr[i].depthNode;
					if (nodeMap.containsKey(hmKey))
					{
						hmValue = nodeMap.get(hmKey);
						nodeMap.put(val, hmValue);
					}
					else
						nodeMap.put(hmKey, 1);
						
					buildNodeMap(rootNode.childPtr[i]);
				}
			}
		}
		
		private int getWidthTree(int depth) //Returns the maximum width in the tree.
		{
				buildNodeMap(root);
				int max = 0, val;
				
				for(int i=1; i<=depth; i++)
				{
					val = nodeMap.get(i);
					
					if(val > max)
						max = val;
				}
				
				return max;
		}
		
		private int getDepthTree (TreeNode rootNode) //Returns the depth of the tree
		{
			int max = rootNode.depthNode, val;
			
			while(rootNode.childPtr != null)
			{
				for(int i=0; i<Array.getLength(rootNode.childPtr); i++)
				{
					val = getDepthTree(rootNode.childPtr[i]);
					
					if (val > max)
						max = val;
				}
			}
			
			return max;
			
		}*/
		
				
		private void findNodesPosition (TreeNode node) //Calculate the node positions.
		{
				//gObject.drawOval(node.posX, node.posY, nodeDiameter, nodeDiameter);
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
				
				for(int i=0; i<childSize; i++)
				{
					node.childPtr[i].posY = node.posY + 2*nodeDiameter;
					
					if (isEven)
						node.childPtr[i].posX = (int)(((float)i - mid)*2*nodeDiameter) + (node.posX);
					else
						node.childPtr[i].posX = ((i-(childSize/2))*2*nodeDiameter) + (node.posX);
					
					if(minX > node.childPtr[i].posX)
						minX = node.childPtr[i].posX;
						
					if(maxX < node.childPtr[i].posX)
						maxX = node.childPtr[i].posX;
						
					if(maxY < node.childPtr[i].posY)
						maxY = node.childPtr[i].posY;
					
					//gObject.drawLine(node.posX + (nodeDiameter/2), node.posY + (nodeDiameter/2), node.childPtr[i].posX + (nodeDiameter/2), node.childPtr[i].posY+(nodeDiameter/2));	
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
			
			while(numBrighter>0)
			{
					nodeColor = nodeColor.brighter();
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
			
			/*if(maxX > (gObjectX - nodeDiameter)) //If true, then no adjustment required.
				minAdjust+=1;
			if(minX < 0)
				minAdjust+=2;
				
			if(minAdjust == 0 || minAdjust == 3) //Do no translation.
				translate = 0; 
			else
			{
				if(minAdjust == 1)
					translate = -(maxX-(gObjectX-nodeDiameter));
				else
					translate = minX;
					
				maxX+=translate; minX+=translate;	
			}
			
			newCenter = (maxX + minX)/2;
			
			if((maxX + (newCenter - (gObjectX/2)) < (gObjectX - nodeDiameter)) && (minX + (newCenter - (gObjectX/2))) > 0)
				translate = translate + (newCenter - (gObjectX/2));*/
			root.posX += translate;
			drawNode(root, translate);
			ImageIO.write(bImage, "png", f);
				
		}
		
		public String getOutput(String str) //Output a class label based on decisiontree.
		{
			String[] inStr = str.split(",");
			int i;
			
			if(Array.getLength(inStr) != n)
			{
				System.out.println("Invalid input String - Check if the number of provided parameters are correct");
				return null;
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
							System.out.println("Invalid value for the "+temp.splitAttribute+" attribute");
							return null;
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
		
		public void createDecisionTree() throws FileNotFoundException, IOException //Build Decision Tree for input.
        {
                FileInputStream fstream = new FileInputStream(fileName);
                DataInputStream in = new DataInputStream(fstream);
                
                //Parse the first line to see if continuous or discrete attributes. 
                //To be done later.
 
                int i, j, lineCount = 0;
                for(i=0; i<n; i++)
					unusedAttr.add(new Integer(i));
                                
                input = new String[t][n+1];
                String line;
                
                while(lineCount<t)
                {
					input[lineCount] = (in.readLine()).split(",");
					if (Array.getLength(input[lineCount]) != n+1) //number of attributes provided in the line is incorrect. 
					{
						System.out.println("Invalid data entry");
						System.exit(0);
					}	
					lineCount++;
                }
                
                int[] cases = new int[t];
                for(i=0;i<t;i++)
					cases[i] = i;
                
                //Start constructing the tree.
                Stack<TreeNode> treeStack = new Stack<TreeNode>();
                TreeNode node = new TreeNode(cases);
                root = node;
                root.depthNode = 1;
                //nodeMap.put(1,1);

                
                node.parentPtr = null;
                HashMap<String, Integer> tempMap;
                Iterator<String> tempIt;
                
                ArrayList tempList;
                int tempSize;
				int[] tempArray;	
				String label;
                treeStack.push(node);
                
                while(!treeStack.isEmpty() && unusedAttr.size()>0)
                {
					TreeNode tempNode = treeStack.pop();
					
					if(countInstances(tempNode.testCases, n) == 1)
						continue;
										
					tempNode.findSplitAttribute();
					unusedAttr.remove(new Integer(tempNode.splitAttribute));
					
					tempMap = getInstances(tempNode.testCases, tempNode.splitAttribute);
					tempSize = tempMap.size();
					tempNode.childPtr = new TreeNode [tempSize]; //Create as many child nodes as there are categories in the splitAttribute.					
					tempIt = tempMap.keySet().iterator();

					
					for(i=0; i<tempSize; i++) 
					{
							tempList = new ArrayList();
							label = tempIt.next();

							for(j=0; j<Array.getLength(tempNode.testCases); j++)
							{
								if (input[tempNode.testCases[j]][tempNode.splitAttribute].compareTo(label) == 0)
									tempList.add(tempNode.testCases[j]);
							}
							
							tempArray = new int[tempList.size()];
							
							for(j=0; j<tempList.size(); j++)
								tempArray[j] = new Integer(tempList.get(j).toString()).intValue();
								
							
							TreeNode tempNode2 = new TreeNode(tempArray);
							tempNode2.parentLabel = label;
							tempNode2.depthNode = tempNode.depthNode + 1;
							tempNode.childPtr[i] = tempNode2;
							tempNode2.parentPtr = tempNode;
							treeStack.push(tempNode2);
							
					}
										
				}
         }
         
         public void pruneDecisionTree(int validationSize) //Prunes the decision tree based on the first 
         
         private class TreeNode //Decision Tree data structure.
		{
			int[] testCases;
			int splitAttribute;
			
			int posX, posY; //Coordinates of the node's center - for drawing.
			int depthNode; //Depth of this Node.
			
			TreeNode childPtr[];
			TreeNode parentPtr;
			
			String parentLabel;
			
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
