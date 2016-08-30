package hamming;


import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.InternetService;
//import com.sprout.finderlib.crypto.EC_PSI_C;
import com.sprout.finderlib.crypto.PSI_C;
import com.sprout.finderlib.crypto.PrivateProtocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class InaccurateHammingRunner {
  
  public static boolean readStdIn = true;
  public static List<String> parseInput(boolean client) {
    
    ArrayList<String> res = new ArrayList<String>();
    
    if (!readStdIn) {
      String[] input1 = {"1:A", "2:G", "2.3:G", "5:-", "9:C", "111.1", "111:G"};
      String[] input2 = {"1:-", "2:C", "2.3:A", "5:A", "6:C", "7:G", "111:G"};
      //String[] input1 = {"1:A"};
      //String[] input2 = {"2:A"};
      
      if (client) {
        return Arrays.asList(input1);
      } else {
        return Arrays.asList(input2);
      }
    }
    
    
    
    Scanner in = new Scanner(System.in);
    in.useDelimiter("\n");
    
    while(in.hasNext()) {
      res.add(in.next());
    }
    
    return res;
  }
  
  public static CommunicationService setupInput(int port) {
    // Connection Setup
    CommunicationService comServ = new InternetService(port);
    
    if (!client) {
      System.out.println("Starting server on port " + port);
      comServ.start();
    } else {
      System.out.println("Starting client. Connecting to " + serverAddress + ":" + port);
      comServ.connect(serverAddress);
    }
    
   return comServ;
  }
  
  public static String[] getPositions(String[] input) {
    String[] positionsOnly = new String[input.length];
    for(int i=0; i<positionsOnly.length; i++) {
      positionsOnly[i] = input[i].replaceFirst(":.*", "");
    }
    
    return positionsOnly;
  }
  
  static boolean client = false;
  static String serverAddress;
  static int numCommon;
  static int otherSize;
  static int port;
  static int numCommonPositions;
  
  public static void main(String[] args){
    client = (args[0].equals("c") || args[0].equals("client"));
    serverAddress = args.length<=1 ? "localhost" : args[1];
    port = args.length<=2 ? 1289 : Integer.valueOf(args[2]);
    
    List<String> subs = parseInput(client);
    
    final String[] inputSubs = subs.toArray(new String[0]);
    final String[] positionsOnlySubs = getPositions(inputSubs);
    
    Thread thread = new Thread(){
      public void run() {
        CommunicationService comServ = setupInput(port);
        
        PrivateProtocol<String, Void, Integer> prot = new PSI_C(comServ, client);
        
        numCommon = prot.execute(inputSubs);
        
        comServ.write(String.valueOf(inputSubs.length));
        otherSize = Integer.valueOf(comServ.readString()); 
      }
    };
    
    Thread thread1 = new Thread(){
      public void run() {
        CommunicationService comServ = setupInput(port+1);
        
        // Run the protocol
        PrivateProtocol<String, Void, Integer> prot = new PSI_C(comServ, client);
        
        numCommonPositions = prot.execute(positionsOnlySubs);
      }
    };
    
    thread.start();
    thread1.start();
    
    try {
      thread.join();
      thread1.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
    }
    
    // Hamming distance for snps
    // total number - number of common positions - number in common
    
    int hammingDistanceSub = inputSubs.length + otherSize - numCommon - numCommonPositions;
    
    System.out.println("Hamming distance is " + hammingDistanceSub);
  }
}
