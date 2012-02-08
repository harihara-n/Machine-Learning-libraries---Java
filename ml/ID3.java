package ml;
 
import java.io.*;
 
public class ID3
{
        public static void createDecisionTree(String filename) //Build Decision Tree for input.
        {
                FileInputStream fstream = new FileInputStream(filename);
                DataInputStream in = new DataInputStream(fstream);
 
                while(in.available()!=0)
                {
                        System.out.println(in.readLine());
                }
 
        }
}
