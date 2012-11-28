package com.sprout.finderlib;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;

/**
 * This class does all the work for conducting the secure communication
 * with another user. It does not handle any communication directly, but instead
 * relies on a Object that implements an BluetoothService's connected thread api
 * @author skyf
 *
 */

// At the moment we use the built in BigInteger implementation to do our calculations.
// TODO: Benchmark an openssl/gmp version
public class PSI_C extends AbstractPSIProtocol {
	// Debugging
    private final String TAG = "PSI_C";
    private final boolean D = false;
    
    // Actually perform the test (these will be overides from a testing base class (and can be the same function)
    protected String conductClientTest(BluetoothService s, List<String> input){
    	// OFFLINE PHASE
    	offlineWatch.start();
    	BigInteger rc  = randomRange(q); // Secret 1
    	BigInteger rc1 = randomRange(q); // Secret 2
    	
    	BigInteger x = g.modPow(rc, p);
    	
    	List<BigInteger> ais = new ArrayList<BigInteger>(); // The set {a1,a2,...,ai}
    	for( String marker: input ){
    		ais.add(hash(marker).modPow(rc1, p));
    	}
   
    	offlineWatch.pause();
    	//Log.i(TAG, "Client offline phase completed in " + offlineWatch.getElapsedTime() + " miliseconds.");
    	
    	// Wait for the server to finish offline phase
    	s.readString();
    	
    	// ONLINE PHASE
    	onlineWatch.start();
    	
    	
    	// This code has been pipelined (see note in server code)
    	
    	s.write(x);
    	for( BigInteger ai : ais ){
    		s.write(ai);
    	}
    
    	List<BigInteger> tsjs = new ArrayList<BigInteger>(); // The set {ts1, ts2, ..., tsj}
    	List<BigInteger> tcis = new ArrayList<BigInteger>(); //Will store the clients processed set
    	BigInteger y, yrc, rc_inv;
    	
    	// Get values from the server and process immediately 
    	y = s.readBigInteger();
    	yrc = y.modPow(rc, p);
    	rc_inv = rc1.modInverse(q);
    	for(int i = 0; i < ais.size(); i++){
    		// This is the following calculation all mod p
    		// H(y^Rc * bi^(1/Rc') )
    		tcis.add(hash( yrc.multiply((s.readBigInteger()).modPow(rc_inv, p)).mod(p) ) );
    	}
    	
    	for(int i = 0; i < ais.size(); i++){
    		tsjs.add(s.readBigInteger());
    	}
    	
    	// tcis = tcis ^ tsjs (intersection)
    	tcis.retainAll(tsjs);
    	
    	int sharedLengths = tcis.size();
    	
    	// Send result
    	if(D) Log.d(TAG, "Client calculated: " + String.valueOf(sharedLengths));
    	s.write(String.valueOf(sharedLengths));
    	
    	onlineWatch.pause();
        //Log.i(TAG, "Client online phase completed in " + onlineWatch.getElapsedTime() + " miliseconds.");
    	
		return String.valueOf(sharedLengths);
    }
    
    protected String conductServerTest(BluetoothService s, List<String> input) {
    	// OFFLINE PHASE
    	offlineWatch.start();
    	BigInteger rs  = randomRange(q); // Secret 1
    	BigInteger rs1 = randomRange(q); // Secret 2
    	    	
    	BigInteger y = g.modPow(rs, p);
    	
    	List<BigInteger> ksjs = new ArrayList<BigInteger>(); // The set {ks1,ks2,...,ksi}
    	for( String marker: input ){
    		ksjs.add(hash(marker).modPow(rs1, p));
    	}
    	
    	SecureRandom r = new SecureRandom();
    	Collections.shuffle(ksjs, r);
    	
    	offlineWatch.pause();
    	//Log.i(TAG, "Server offline phase completed in " + offlineWatch.getElapsedTime() + " miliseconds.");
    	s.write("Offline DONE");
    	
    	// ONLINE PHASE
    	onlineWatch.start();
    	
    	List<BigInteger> ais = new ArrayList<BigInteger>(); // The set {a1,a2,...,ai}
    	BigInteger x = null;
    	
    	// This code has been reworked to be pipelined. The idea being, we start computation immediately 
    	//   after we receive the clients data. 
    	
    	// Start reading client data
    	x = s.readBigInteger();
    	
    	List<BigInteger> bis = new ArrayList<BigInteger>(); // will store client data before shuffling
    	
    	for(int i = 0; i < ksjs.size(); i++){
    		// Read an ai
    		ais.add(s.readBigInteger());
    		
    		// Add our secret
    		bis.add( ais.get(i).modPow(rs1, p) );
    	}
    	Collections.shuffle(bis, r);
    	
    	// Send back to the client
    	s.write(y);
    	for( BigInteger bi : bis ){
    		s.write(bi);
    	}
    	
    	BigInteger xrs = x.modPow(rs,p);
    	for(BigInteger ksj : ksjs){
    		// This is the following calculation all mod p
    		// H(x^Rs * ksj )
    		s.write((hash( xrs.multiply(ksj).mod(p) )));
    	}
    	
    	
    	onlineWatch.pause();
        //Log.i(TAG, "Server online phase completed in " + onlineWatch.getElapsedTime()+ " miliseconds.");
    	
		return s.readString();
    }
}
