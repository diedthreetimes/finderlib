package com.sprout.finderlib.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sprout.finderlib.communication.BluetoothService;

import android.util.Log;

/**
 * Partialy-Homomorphic encryption version of PSI-C based on masked hamming distance
 * @author skyf
 *
 */

public class PSI_C_Homo extends PSI_C {
	
	// Debugging
    private final String TAG = "Homomorphic PSI-C";
    private final boolean D = false;
    
    private SecureRandom rand;
    
    public void onCreate(){
    	Log.e(TAG, "+++ ON CREATE +++ ");
    	super.onCreate();
    	rand = new SecureRandom();
    }
	
    //TODO: Think about handling the actual encryption here
    //        Also think about moving this somewhere else
    // For now this class stores one encryption
    class Encryption {
    	private BigInteger c1;
    	private BigInteger c2;
    	
    	Encryption(BigInteger one, BigInteger two){
    		c1 = one;
    		c2 = two;
    	}
    	
    	public boolean isEncryptionOfZero(BigInteger key){
    		return c1.modPow(key, p).equals(c2);
    	}
    	
    	/**
    	 * Add other to self
    	 * @param o other
    	 * @return self
    	 */
    	//TODO: should this be unmutable?
    	public Encryption plus(Encryption o){
    		c1 = c1.multiply(o.c1).mod(p);
    		c2 = c2.multiply(o.c2).mod(p);
    		return this;
    	}
    	
    	/**
    	 * Multiply other to self
    	 * @param o other
    	 * @return self
    	 */
    	public Encryption mult(BigInteger o){
    		c1 = c1.modPow(o,p);
    		c2 = c2.modPow(o,p);
    		return this;
    	}
    }
    
 // Actually perform the test (these will be overides from a testing base class (and can be the same function)
    protected String conductClientTest(BluetoothService s, List<String> inputs){
    	// OFFLINE PHASE
    	offlineWatch.start();
    	BigInteger x  = randomRange(q); // Secret 1
    	
    	BigInteger y = g.modPow(x, p);
    	
    	// C = (c1, c2)
    	List<Encryption> Cs = new ArrayList<Encryption>(); // The set of encryptions of inputs
    	
    	BigInteger h,r,c1,c2;
    	for( String input: inputs ){
    		h = q.subtract(hash(input)).mod(q);
    		
    		// Encrypt h
    		
    		r = new BigInteger(160, rand);
    		// g^r
    		c1 = g.modPow(r,p);
    		
    		// y^r * g ^ h
    		c2 = y.modPow(r, p).multiply(g.modPow(h,p));
    		
    		Cs.add(new Encryption(c1,c2));
    	}
   
    	offlineWatch.pause();
    	//Log.i(TAG, "Client offline phase completed in " + offlineWatch.getElapsedTime() + " miliseconds.");
    	
    	// Wait for the server to finish offline phase
    	s.readString();
    	
    	// ONLINE PHASE
    	onlineWatch.start();
    	
    	
    	s.write(y);
    	for( Encryption e : Cs ){
    		s.write(e.c1);
    		s.write(e.c2);
    	}
    	
    	//onlineWatch.pause();
    	//Log.i(TAG, "Client send phase completed in " + onlineWatch.getElapsedTime() + " miliseconds.");
    	//onlineWatch.start();
    	
    	int numCommon = 0;
    	
    	// Get values from the server and process
    	for(int i = 0; i < Cs.size(); i++){

    		if (new Encryption(s.readBigInteger(), s.readBigInteger()).isEncryptionOfZero(x))
    				numCommon++;
    	}
    	
    	// Send result
    	if(D) Log.d(TAG, "Client calculated: " + String.valueOf(numCommon));
    	s.write(String.valueOf(numCommon));
    	
    	onlineWatch.pause();
        //Log.i(TAG, "Client online phase completed in " + onlineWatch.getElapsedTime() + " miliseconds.");
    	
		return String.valueOf(numCommon);
    }
    
    protected String conductServerTest(BluetoothService s, List<String> inputs) {
    	// OFFLINE PHASE
    	offlineWatch.start();
    	// Generate the randoms for later along with the first half of the encryption 
    	List<BigInteger> rs = new ArrayList<BigInteger>();  // Randomness for encryption later
    	List<BigInteger> r1s = new ArrayList<BigInteger>(); // Randomness for masking later
    	List<BigInteger> c1s = new ArrayList<BigInteger>(); // First half of encryption 
    	List<BigInteger> ghs = new ArrayList<BigInteger>();  // Hashed input

    	BigInteger r, h;
    	
    	for( String input: inputs ){
    		r = new BigInteger(160, rand);
    		h = hash(input);
    		
    		rs.add(r);
    		r1s.add(new BigInteger(80, rand));
    		c1s.add( g.modPow(r,p) );
    		ghs.add(g.modPow(h,p));
    	}
    	
    	offlineWatch.pause();
    	//Log.i(TAG, "Server offline phase completed in " + offlineWatch.getElapsedTime() + " miliseconds.");
    	s.write("Offline DONE");
    	
    	List<Encryption> es = new ArrayList<Encryption>(); // The set of return encryptions
    	BigInteger y = null;
    	
    	// This code has been reworked to be pipelined. The idea being, we start computation immediately 
    	//   after we receive the clients data. 
    	
    	// Start reading client data
    	y = s.readBigInteger();
    	
    	// ONLINE PHASE
    	onlineWatch.start();
    
    	BigInteger c2;
    	Encryption se, ce;
    	
    	for(int i = 0; i < ghs.size(); i++){
    		// Compute our c2 and encryption
    		c2 = y.modPow(rs.get(i), p).multiply(ghs.get(i));
    		se = new Encryption(c1s.get(i), c2);
    		
    		// Read a client encryption
    		ce = new Encryption(s.readBigInteger(), s.readBigInteger());
    		
    		// Compute difference and mask
    		es.add( ce.plus(se).mult(r1s.get(i)) );
    	}
    	Collections.shuffle(es, rand);
    	
    	// Send back to the client
    	for( Encryption e: es ){
    		s.write(e.c1);
    		s.write(e.c2);
    	}
    	
    	onlineWatch.pause();
        //Log.i(TAG, "Server online phase completed in " + onlineWatch.getElapsedTime()+ " miliseconds.");
    	
        // Return the result from the client
		return s.readString();
    }
}
