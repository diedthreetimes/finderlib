package com.sprout.finderlib.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.spongycastle.math.ec.ECPoint;

import com.sprout.finderlib.communication.CommunicationService;

import android.util.Log;

/**
 * This class behaves just like PSI_C but uses elliptic curves
 * @author skyf
 *
 */

//TODO: See note in AbstractECProtocol about refactoring this

public class EC_PSI_C extends AbstractECProtocol <String, Void, Integer > {
	// Debugging
    private final String TAG = "EC_PSI_C";
    private final boolean D = true;
	
	public EC_PSI_C(CommunicationService s, boolean client) {
		super("EC_PSI_C", s, client);
	}
	public EC_PSI_C(String testName, CommunicationService s, boolean client) {
		super(testName, s, client);
	}
    
    @Override
	protected Integer conductClientTest(CommunicationService s, String... input){
    	// OFFLINE PHASE
    	offlineWatch.start();
    	BigInteger rc  = randomRange(n); // Secret 1
    	BigInteger rc1 = randomRange(n); // Secret 2
    	
    	ECPoint x = g.multiply(rc);
    	
    	List<ECPoint> ais = new ArrayList<ECPoint>(); // The set {a1,a2,...,ai}
    	for( String marker: input ){
    		ais.add(hash(marker).multiply(rc1)); // TODO: this maps h(m) to a point on the curve and mutliplys by rc. is this correct? efficent?
    	}
   
    	offlineWatch.pause();
    	//Log.i(TAG, "Client offline phase completed in " + offlineWatch.getElapsedTime() + " miliseconds.");
    	
    	// Wait for the server to finish offline phase
    	s.readString();
    	
    	// ONLINE PHASE
    	onlineWatch.start();
    	
    	
    	// This code has been pipelined (see note in server code)
    	
    	s.write(x);
    	for( ECPoint ai : ais ){
    		s.write(ai);
    	}
    
    	List<BigInteger> tsjs = new ArrayList<BigInteger>(); // The set {ts1, ts2, ..., tsj}
    	List<BigInteger> tcis = new ArrayList<BigInteger>(); //Will store the clients processed set
    	ECPoint y, yrc;
    	BigInteger rc_inv;	
    	
    	// Get values from the server and process immediately 
    	y = s.readECPoint();
    	yrc = y.multiply(rc);
    	rc_inv = rc1.modInverse(n);
    	for(int i = 0; i < ais.size(); i++){
    		// This is the following calculation all mod p
    		// H(y^Rc * bi^(1/Rc') )
    		tcis.add(hash( yrc.add((s.readECPoint().multiply(rc_inv)))));
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
    	
    	return sharedLengths;
    }
    
	
	//TODO: Implment from here
    @Override
    protected Integer conductServerTest(CommunicationService s, String... input) {
    	// OFFLINE PHASE
    	offlineWatch.start();
    	BigInteger rs  = randomRange(n); // Secret 1
    	BigInteger rs1 = randomRange(n); // Secret 2
    	    	
    	ECPoint y = g.multiply(rs);
    	
    	List<ECPoint> ksjs = new ArrayList<ECPoint>(); // The set {ks1,ks2,...,ksi}
    	for( String marker: input ){
    		ksjs.add(hash(marker).multiply(rs1));
    	}
    	
    	SecureRandom r = new SecureRandom();
    	Collections.shuffle(ksjs, r);
    	
    	offlineWatch.pause();
    	//Log.i(TAG, "Server offline phase completed in " + offlineWatch.getElapsedTime() + " miliseconds.");
    	s.write("Offline DONE");
    	
    	// ONLINE PHASE
    	onlineWatch.start();
    	
    	List<ECPoint> ais = new ArrayList<ECPoint>(); // The set {a1,a2,...,ai}
    	ECPoint x = null;
    	
    	// This code has been reworked to be pipelined. The idea being, we start computation immediately 
    	//   after we receive the clients data. 
    	
    	// Start reading client data
    	x = s.readECPoint();
    	
    	List<ECPoint> bis = new ArrayList<ECPoint>(); // will store client data before shuffling
    	
    	for(int i = 0; i < ksjs.size(); i++){
    		// Read an ai
    		ais.add(s.readECPoint());
    		
    		// Add our secret
    		bis.add( ais.get(i).multiply(rs1) );
    	}
    	Collections.shuffle(bis, r);
    	
    	// Send back to the client
    	s.write(y);
    	for( ECPoint bi : bis ){
    		s.write(bi);
    	}
    	
    	ECPoint xrs = x.multiply(rs);
    	for(ECPoint ksj : ksjs){
    		// This is the following calculation all mod p
    		// H(x^Rs * ksj )
    		s.write((hash( xrs.add(ksj) )));
    	}
    	
    	
    	onlineWatch.pause();
        //Log.i(TAG, "Server online phase completed in " + onlineWatch.getElapsedTime()+ " miliseconds.");
    	
		return Integer.valueOf(s.readString());
    }

}
