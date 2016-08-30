package psica;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.InternetService;
import com.sprout.finderlib.crypto.PSI_C;
import com.sprout.finderlib.crypto.PrivateProtocol;

/**
 * Simple 2-Party PSICA protocols.
 *
 */
public class Runner {
  
  public static String[] parseInput(boolean client) {
    String[] input1 = {"1:A", "2:G", "2.3:G", "5:-", "9:C", "111.1", "111:G"};
    String[] input2 = {"1:-", "2:C", "2.3:A", "5:A", "6:C", "7:G", "111:G"};
    
    if (client) {
      return input1;
    } else {
      return input2;
    }
  }
  
  public static CommunicationService setupInput(int port, boolean c) {
    // Connection Setup
    CommunicationService comServ = new InternetService(port);
    
    if (!c) {
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
  
  static String serverAddress;
  static int numCommon;
  static int otherSize;
  static int port;
  static int numCommonPositions;
  
  public static void main(String[] args){
    serverAddress = "localhost" ;
    port = 1299;
    
    
    Thread clientThread = new Thread(){
      public void run() {
        final String[] clientInput = parseInput(true);
        CommunicationService comServ = setupInput(port, true);
        
        PrivateProtocol<String, Void, Integer> prot = new PSI_C(comServ, true);
        
        numCommon = prot.execute(clientInput);
      }
    };
    
    Thread serverThread = new Thread(){
      public void run() {
        final String[] serverInput = parseInput(false);
        CommunicationService comServ = setupInput(port, false);
        
        // Run the protocol
        PrivateProtocol<String, Void, Integer> prot = new PSI_C(comServ, false);
        
        prot.execute(serverInput);
      }
    };
    
    clientThread.start();
    serverThread.start();
    
    try {
      clientThread.join();
      serverThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
    }
    
    System.out.println("Cardinality is " + numCommon);
  }
}
