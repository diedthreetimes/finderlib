package com.sprout.finderlib.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sprout.finderlib.communication.CommunicationService;

import com.sprout.finderlib.utils.Log;

public class PSI_C_P extends PSI_C_DT {

  private final String TAG = "PSI_C_P";
  private final boolean D = false;
  
  private final int TAG_LENGTH = 120;
  
  public PSI_C_P(CommunicationService s, boolean client) {
    super("PSI_C_P", s, client);
  }
  
  public PSI_C_P(String testName, CommunicationService s, boolean client) {
    super(testName, s, client);
  }
  
  private String generateTag(SecureRandom rand) {
    if (rand == null) {
      rand = new SecureRandom();
    }
    
    // TODO: Better performance if we do something like base64 encode
    return (new BigInteger(TAG_LENGTH, rand)).toString(Character.MAX_RADIX);
  }
  
  @Override
  protected List<String> conductClientTest(CommunicationService s, Entry<String,String>... input) {
  
    // First round of PSI-CA-DT
    List<String> Tp = super.conductClientTest(s, input);
    Tp.remove(0);
    
    // Second round of PSI-CA-DT
    // Small wrapper to get around identical input types for server and client
    List<Entry<String,String>> tp = new ArrayList<Entry<String,String>>();
    for (String str : Tp) {
      tp.add(new Entry<String, String>(str, ""));
    }
    ///  end small wrapper
    
    if (D) Log.d(TAG, "Running second round");
    List<String> Dp = super.conductClientTest(s, tp.toArray(new Entry[0]));
    
    if (D) Log.d(TAG, "Finsihed second round");
    // frequency computation and return omitted.
    return Dp;
  }
  
  @Override
  protected List<String> conductServerTest(CommunicationService s, Entry<String,String>... input) {
    // TAG GENERATION
    offlineWatch.start();
    
    List<Entry<String,String>> mp = new ArrayList<Entry<String, String>>();
    List<Entry<String,String>> mt = new ArrayList<Entry<String, String>>();
   
    Map<String,String> marks = new HashMap<String, String>();
    SecureRandom rand = new SecureRandom();
    for( Entry<String,String> pair: input ){
      if (!marks.containsKey(pair.getValue())) {
        String tag = generateTag(rand);
        mp.add( new Entry<String,String>(pair.getKey(), tag) );
        mt.add( new Entry<String,String>(tag, pair.getValue()) );
        marks.put(pair.getValue(), tag);
      } else {
        mt.add( new Entry<String,String>(marks.get(pair.getValue()), pair.getValue()) );
      }
    }
    
    offlineWatch.pause();
    
    // First round of PSI-CA-DT    
    super.conductServerTest(s, mp.toArray(new Entry[0]));
    
    if (D) Log.d(TAG, "Running second round");
    
    // Second round of PSI-CA-DT
    super.conductServerTest(s, mt.toArray(new Entry[0]));
    
    if (D) Log.d(TAG, "Finsihed second round");
    
    
    return new ArrayList<String>();
  }
}
