package com.sprout.finderlib.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.utils.Log;

// Input List of Entries with <Key,DataItem> for Server and <Key,""> for Client
// Output List of Strings, with first string representing Cardinality and the remaining matching data items
public class PSI_DT extends AbstractPSIProtocol<Entry<String,String>, Void, List<String>> {
  //Debugging
  private final String TAG = "PSI_C_DT";
  private final boolean D = true;
  
  
  public PSI_DT(CommunicationService s, boolean client) {
    super("PSI_DT", s, client);
  }
  
  public PSI_DT(String testName, CommunicationService s, boolean client) {
    super(testName, s, client);
  }
  
  // Client return is a list of pairs (interesction_element, associated_data)
  @Override
  protected List<String> conductClientTest(CommunicationService s, Entry<String,String>... input){
    // OFFLINE PHASE
    offlineWatch.start();
    BigInteger rc  = randomRange(q); // Secret 1
    BigInteger rc1 = randomRange(q); // Secret 2
    
    BigInteger x = g.modPow(rc, p);
    
    List<BigInteger> ais = new ArrayList<BigInteger>(); // The set {a1,a2,...,ai} aka x_i
    for( Entry<String,String> bi: input ){
      ais.add(hash(bi.getKey()).modPow(rc1, p));
    }
 
    offlineWatch.pause();

    // Wait for the server to finish offline phase
    s.readString();
    
    // ONLINE PHASE
    onlineWatch.start();
    
    s.write(x);
    s.write(ais.size());
    for( BigInteger ai : ais ){
      s.write(ai);
    }
  
    List<Entry<BigInteger,String>> tsjs = new ArrayList<Entry<BigInteger,String>>(); // The set {ts1, ts2, ..., tsj} aka ta_i
    List<BigInteger> tcis = new ArrayList<BigInteger>(); // aka tb_i
    List<BigInteger> zis = new ArrayList<BigInteger>(); // H^-1(tci)
    BigInteger y, yrc, rc_inv;
    
    // Get values from the server and process immediately 
    y = s.readBigInteger();
    yrc = y.modPow(rc, p);
    rc_inv = rc1.modInverse(q);
    for(int i = 0; i < ais.size(); i++){
      // zi = y^Rc * xi'^(1/Rc')  mod p
      zis.add(yrc.multiply((s.readBigInteger()).modPow(rc_inv, p)).mod(p));
      // H(zi)
      tcis.add(hash( zis.get(zis.size()-1) ) );
    }
    
    int size = s.readInt();
    for(int i = 0; i < size; i++){
      tsjs.add(new Entry<BigInteger, String>(s.readBigInteger(), s.readString()));
    }
    
    
    // compute the intersection on tags and the projection
    List<String> res = new ArrayList<String>();
    for (int i=0; i < tcis.size(); i++) {
      for (int j=0; j< tsjs.size(); j++) {
        if (tsjs.get(j).getKey().equals(tcis.get(i))) {
          // TODO: kj = KDF(tci) s.t. zi = tsj
          // TODO: D(tsj.getValue())
          res.add(input[i].getKey());
          res.add(tsjs.get(j).getValue());
        }
      }
    }
       
    // Send result if we want the cardinality to be mirrored
    // if(D) Log.d(TAG, "Client calculated: " + String.valueOf(sharedLengths));
    // s.write(String.valueOf(sharedLengths));
    
    onlineWatch.pause();
    
    return res;
  }
  
  // Server's return is simply the empty string
  @Override
  protected List<String> conductServerTest(CommunicationService s, Entry<String,String>... input) {
    // OFFLINE PHASE
    offlineWatch.start();
    BigInteger rs  = randomRange(q); // Secret 1
    BigInteger rs1 = randomRange(q); // Secret 2
          
    BigInteger y = g.modPow(rs, p);
  
    // The map {ks1:d_1,ks2:d2,...,ksi:di}
    // These pairs be shuffled together (could maybe save on object creation if we use PRP on indexes instead of Collections.shuffle)
    List<Entry<BigInteger,String>> ksjs = new ArrayList<Entry<BigInteger,String>>(); 
    for( Entry<String,String> pair: input ){
      Entry<BigInteger, String> new_pair =
          new Entry<BigInteger, String>(hash(pair.getKey()).modPow(rs1, p), pair.getValue());
      ksjs.add(new_pair);
    }
    
    SecureRandom r = new SecureRandom();
    Collections.shuffle(ksjs, r);
    
    offlineWatch.pause();
    s.write("Offline DONE");
    
    // ONLINE PHASE
    onlineWatch.start();
    
    List<BigInteger> ais = new ArrayList<BigInteger>(); // The set {a1,a2,...,ai} aka x_i
    BigInteger x = null;
    
    // This code has been reworked to be pipelined. The idea being, we start computation immediately 
    //   after we receive the clients data. 
    
    // Start reading client data
    x = s.readBigInteger();
    
    //List<BigInteger> xpis = new ArrayList<BigInteger>(); // will store client data before shuffling
    
    s.write(y);
    
    int size = s.readInt();
    for(int i = 0; i < size; i++){
      // Read an ai
      ais.add(s.readBigInteger());
      
      // Add our secret
      //xpis.add( ais.get(i).modPow(rs1, p) );
      s.write( ais.get(i).modPow(rs1, p) );
    }
    
    // Send back to the client
    //s.write(y);
    //for( BigInteger xpi : xpis ){
    //  s.write(xpi);
    //}
    
    BigInteger xrs = x.modPow(rs,p);
    s.write(ksjs.size());
    for(int i=0; i < ksjs.size(); i++){
      BigInteger ksj = ksjs.get(i).getKey();
      String d_j = ksjs.get(i).getValue();
      
      // This is the following calculation all mod p
      // H(x^Rs * ksj )
      BigInteger yj = xrs.multiply(ksj).mod(p);
      s.write((hash( yj )));
      
      // TODO: k_j = KDF(yj)
      // TODO: ed_j = E_{k_j}(d_j)
      // for now we just set ed_j = d_j
      String ed_j = d_j;
      s.write(ed_j);
    }
    
    
    onlineWatch.pause();
      
    List<String> ret = new ArrayList<String>();
    ret.add("");
    //ret.add(String.valueOf(xpis.size()));
    return ret;
    
    // Uncomment for symmetric protocols
    //return Integer.valueOf(s.readString());
  }

}
