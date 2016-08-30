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

public class Runner {
  
  public static boolean readStdIn = true;
  static boolean skipDeletions = true;
  static boolean skipInsertions = true;
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
  static int numCommonIns;
  static int otherSizeIns;
  static int numCommonSubs;
  static int otherSizeSubs;
  static int port;
  static int numCommonPositions;
  static int numCommonPositionsIns;
  static int numCommonPositionsSubs;
  
  public static void main(String[] args){
    client = (args[0].equals("c") || args[0].equals("client"));
    serverAddress = args.length<=1 ? "localhost" : args[1];
    port = args.length<=2 ? 1289 : Integer.valueOf(args[2]);
    
    List<String> allinput = parseInput(client);
    
    List<String> subs = parseSubs(allinput);
    
    
    List<String> ins = parseIns(allinput);
    
    final String[] inputSubs = subs.toArray(new String[0]);
    final String[] positionsOnlySubs = getPositions(inputSubs);
    
    final String[] inputIns = ins.toArray(new String[0]);
    final String[] positionsOnlyIns = getPositions(inputIns);
    
    Thread thread = new Thread(){
      public void run() {
        CommunicationService comServ = setupInput(port);
        
        PrivateProtocol<String, Void, Integer> prot = new PSI_C(comServ, client);
        
        numCommonSubs = prot.execute(inputSubs);
        
        comServ.write(String.valueOf(inputSubs.length));
        otherSizeSubs = Integer.valueOf(comServ.readString()); 
      }
    };
    
    Thread thread1 = new Thread(){
      public void run() {
        CommunicationService comServ = setupInput(port+1);
        
        // Run the protocol
        PrivateProtocol<String, Void, Integer> prot = new PSI_C(comServ, client);
        
        numCommonPositionsSubs = prot.execute(positionsOnlySubs);
      }
    };
    
    Thread thread2 = new Thread(){
      public void run() {
        CommunicationService comServ = setupInput(port+2);
        
        PrivateProtocol<String, Void, Integer> prot = new PSI_C(comServ, client);
        
        numCommonIns = prot.execute(inputIns);
        
        comServ.write(String.valueOf(inputIns.length));
        otherSizeIns = Integer.valueOf(comServ.readString()); 
      }
    };
    
    Thread thread3 = new Thread(){
      public void run() {
        CommunicationService comServ = setupInput(port+3);
        
        // Run the protocol
        PrivateProtocol<String, Void, Integer> prot = new PSI_C(comServ, client);
        
        numCommonPositionsIns = prot.execute(positionsOnlyIns);
      }
    };
    
    thread.start();
    thread1.start();
    
    if (!skipInsertions) {
      thread2.start();
      thread3.start();
    }
    
    try {
      thread.join();
      thread1.join();
      
      if (!skipInsertions) {
        thread2.join();
        thread3.join();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
    }
    
    int otherSize = otherSizeSubs + otherSizeIns;
    int numCommonPositions = numCommonPositionsSubs + numCommonPositionsIns;
    int numCommon = numCommonSubs + numCommonIns;
    
    int numUncommonPositions = allinput.size() + otherSize - 2*numCommonPositions;
 
    int editDistance = (allinput.size() + otherSize - 2 * numCommon + numUncommonPositions) / 2;
    
    // Hamming distance for snps
    // total number - number of common positions - number in common
    
    // Hamming distance for ins
    // number of positions that match - number of insertions that match
    int hammingDistanceSub = inputSubs.length + otherSizeSubs - numCommonSubs - numCommonPositionsSubs;
    int hammingDistanceIns = numCommonPositionsIns - numCommonIns;
    int hammingDistance = hammingDistanceIns + hammingDistanceSub;
    
    if (skipInsertions) {
      hammingDistance = hammingDistanceSub;
    }
    
    if (!skipDeletions) {
      // Note, this is a less accurate approximation of the edit distance than the version in edit_distance.Runner
      // If a deletion occurs at the same place as an SNP or sub this will cause inaccuracy.
      System.out.println("Edit distance is " + editDistance);
    }
    System.out.println("Hamming distance is " + hammingDistance);
  }

  private static List<String> parseIns(List<String> allinput) {
    ArrayList<String> res = new ArrayList<String>();
    
    for (String s : allinput) {
      if (s.contains(".")) {
        res.add(s);
      } else if (!skipDeletions && s.contains("-")) {
        res.add(s);
      }
    }
    
    return res;
  }

  private static List<String> parseSubs(List<String> allinput) {
    ArrayList<String> res = new ArrayList<String>();
    
    for (String s : allinput) {
      if (!s.contains(".") && !s.contains("-")) {
        res.add(s);
      }
    }
    
    return res;
  }
}
