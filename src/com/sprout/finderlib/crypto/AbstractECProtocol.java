package com.sprout.finderlib.crypto;

import java.math.BigInteger;

import org.spongycastle.asn1.nist.NISTNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.math.ec.ECPoint;

import com.sprout.finderlib.communication.CommunicationService;

import com.sprout.finderlib.utils.Log;

/**
 * This is the base fo many elliptic curve based protocols.
 * @author skyf
 *
 */

//TODO: We should refator this, as most of the underlying protocols work for any group
//        We should add an interface to groups, and allow the protocol to depend on any group
//        In this way we can have PSI_C_EC and PSI_C_Elgamal depend on PSI_C (which could be abstract)

public abstract class AbstractECProtocol<Params, Progress, Result> extends PrivateProtocol<Params, Progress, Result> {
	// Debug info
	private final String TAG = "AbstractECProtocol";
	private final boolean D = true;

	 //public keys
    protected ECPoint g;
    protected BigInteger n;
    
    
	public AbstractECProtocol(String testName, CommunicationService s, boolean client) {
		super(testName, s, client);
	}
	
	@Override
	protected void loadSharedKeys() {
    	X9ECParameters x9 = NISTNamedCurves.getByName("P-224"); // or whatever curve you want to use
    	g = x9.getG();
    	n = x9.getN();
    	 	
 		//TODO: Load these keys from a file
 		// TODO: Find a way to generate them?
 		
 		if(D) Log.d(TAG, "n: " + n);
 		if(D) Log.d(TAG, "g: " + g);

	}

    // H, as in the PCI-C protocol. Here the input is a string, and the output is an element in the curve
 	protected ECPoint hash(String input){
 		return g.multiply(hash(input.getBytes(), (byte)0).mod(n));
 	}
 	// H', as in the PCI-C protocol. The input is a ECPoint, and the output is any <160 bit integer
 	protected BigInteger hash(ECPoint input){
 		//TODO: Is getEncoded correct!!
 		return hash(input.getEncoded(), (byte)1);
 	}
 	
}
