package com.sprout.finderlib;

import java.math.BigInteger;

import android.util.Log;


public abstract class AbstractPSIProtocol extends PrivateProtocol {
	// Debug info
	private final String TAG = "AbstractPSIProtocol";
	private final boolean D = true;
	
    //public keys
    protected BigInteger p, q, g;

    // t = (p-1)/q to hash into the group Z*p
    protected BigInteger t;
    
 	// Load the common inputs, p q and g which are:
 	// p - a prime number
 	// q - the sub prime
 	// g - the base (generator)
    protected void loadSharedKeys(){
 		p = new BigInteger("b95b6c851ff243745411a0c901a14c217d429edba65b8a298534731e5c3182bf9806f592611bbf2ded9fc4a1b21acfe685112ec38d6d7c4b4bf28b5bcc636b6c4844fdcf449b002b4bc5143a32e0f7b713097b062683cc7cdaa7adfd6c49b0d897487d4e2d0c94bf0c8cafe11580cb84f14ca7922142503ee0dfc377591233c1", 16);
 		q = new BigInteger("d9ad24d2728323f368eac50bb1e1154483d820b7", 16);
 		g = new BigInteger("af3ecd5a39c2ec6fd3ebfd44a4e18a422429c3b18ec6a716968f0ea524f1e19a67f7e117211a802eaae551e4b43967b4b63a50ef6d2c31397a845456550eaa89d4fe8959e402e1484139e5ff52187882f25967ad10e294c7980dd678ebb2a592e031e75ada46d1c5af16caebcd86d06430de7e7ba6fb71590d7329ee744977dd", 16);
 	
 		//TODO: Load these keys from a file
 		// TODO: Find a way to generate them?
 		
 		if(D) Log.d(TAG, "P: " + p);
 		if(D) Log.d(TAG, "q: " + q);
 		if(D) Log.d(TAG, "g: " + g);
 		
 		t = (p.subtract(BigInteger.ONE).divide(q));
 	}
    
    // H, as in the PCI-C protocol. Here the input is a string, and the output is an element in Z*p
 	protected BigInteger hash(String input){
 		return hash(input.getBytes(), (byte)0).mod(p).modPow(t, p);
 	}
 	// H', as in the PCI-C protocol. The input is an integer, and the output is any <160 bit integer
 	protected BigInteger hash(BigInteger input){
 		return hash(input.toByteArray(), (byte)1);
 	}
 	
}
