package com.sprout.finderlib.crypto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sprout.finderlib.communication.CommunicationService;

import android.util.Log;

public class APSI extends AbstractPSIProtocol <String, Void, List<String> > {

	// Debugging
    private final String TAG = "APSI";
    private final boolean D = false;
	
	// Public keys
	protected BigInteger N, e;
	
	
	public APSI(CommunicationService s, boolean client) {
		super("APSI", s, client);
	}
	
	public APSI(String testName, CommunicationService s, boolean client) {
		super(testName, s, client);
	}
	
	
	// TODO: This class may belong somewhere else (and have non static methods
	class CA {
		
		private BigInteger N,e;
		
		private BigInteger p,q,d;
		
		
		public CA(){
			// TODO: Load from file
			
			p = new BigInteger("00dba2d30dfc225ffcd894015d8971" +
					"6c2693e7d35c051670eb850337a41f" + 
			        "719855ebc0839747651487a4f178cd" +
			        "3f5c17cccb66f7baa8f8f54c3c2021" + 
			        "9a95f37f41", 16);
			q = new BigInteger("00d10e691f38413dc6ca084a403059" +
					"de7934422b44436ffc8b4b35572e24" + 	
					"e5df78615bfabc7251f1e050bb5a75" +
					"598e0d957c9ae96457442a43db9130" +
					"4c64d11e9b",16);
			
			
			N = p.multiply(q);
			
			e = BigInteger.valueOf(3);
			
			// e*d = 1 mod (p-1)(q-1)
			d = e.modInverse(p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE)));
		}
		
		public BigInteger sign(BigInteger m){
			return m.modPow(d, N);
		}

		public BigInteger getN() {
			return N;
		}

		public BigInteger getE() {
			return e;
		}
	}
	
	private CA ca;
	
	@Override
	protected List<String> conductClientTest(CommunicationService s, String... input) {
		List<BigInteger> sigs = new ArrayList<BigInteger>();
		for(String i: input){
			sigs.add(ca.sign(hash(i)));
		}
		offlineWatch.start();
		List<BigInteger> rci = new ArrayList<BigInteger>();
		List<BigInteger> ai = new ArrayList<BigInteger>();
		
		BigInteger rc;
		for(BigInteger sigma : sigs){
			// rc <- Zn/2
			rc = randomRange(N.divide(BigInteger.valueOf(2)));
			rci.add(rc);

			// ai = sig * g^rc
			ai.add(sigma.multiply(g.modPow(rc, N)).mod(N));
		}
		offlineWatch.pause();
		
		// Wait for the server to finish offline phase
    	s.readString();
		
    	// Online phase start
		onlineWatch.start();
		
		s.write(String.valueOf(ai.size()));
		int serverSize = Integer.valueOf(s.readString());
		
		
		List<BigInteger> tcis = new ArrayList<BigInteger>();
		List<BigInteger> tsjs = new ArrayList<BigInteger>();
		
		BigInteger Y = s.readBigInteger();
		
		
		//TODO: This needs to be (truly) threaded write could block
		for(int i = 0; i < ai.size(); i++){
			s.write(ai.get(i));
		}
		
		for(int i = 0; i < ai.size(); i++){
			//tsci = h'(ai' * Y^-rc)		
			tcis.add(hash(s.readBigInteger().multiply(Y.modPow(rci.get(i).negate(),N)).mod(N)));
		}
		
		// For APSI (specifically for Personalized Medicine) we only time 
		onlineWatch.pause();
		
		for(int i = 0; i < serverSize; i++){
			tsjs.add(s.readBigInteger());
		}
		
		// tcis = tcis ^ tsjs (intersection)
    	tcis.retainAll(tsjs);
    	
    	int sharedLengths = tcis.size();
		
    	//TODO: Currently this just returns the length!!!!!
    	//        These needs to be fixed
    	
		// Send result
    	if(D) Log.d(TAG, "Client calculated: " + String.valueOf(sharedLengths));
    	s.write(String.valueOf(sharedLengths));
    	
    	//onlineWatch.pause();
    	
    	
    	List<String> ret = new ArrayList<String>();
    	ret.add( String.valueOf(sharedLengths));
    	
		return ret;
	}

	@Override
	protected List<String> conductServerTest(CommunicationService s, String... input) {
		offlineWatch.start();
		BigInteger rs = randomRange(N.divide(BigInteger.valueOf(2)));
		BigInteger Y = g.modPow(BigInteger.valueOf(2).multiply(e).multiply(rs), N);
		
		List<BigInteger> tsjs = new ArrayList<BigInteger>();
		for(String i: input){
			// tsj = H'(H(i)^rs)
			tsjs.add(hash(hash(i).modPow(BigInteger.valueOf(2).multiply(rs),N).mod(N)));
		}
		
		offlineWatch.pause();
		
    	s.write("Offline DONE");
		
		onlineWatch.start();
		s.write(String.valueOf(input.length));
		int clientSize = Integer.valueOf(s.readString());
		
		s.write(Y);
		for(int i =0; i < clientSize; i++){
			// ai' = ai^2e*rs
			s.write(s.readBigInteger().modPow(BigInteger.valueOf(2).multiply(e).multiply(rs), N));
		}
		
		// For Personalized medicine we only care about this tie
		onlineWatch.pause();
		
		// Send tsj
		for(BigInteger tsj: tsjs)
			s.write(tsj);
		
		// onlineWatch.pause();
		
		List<String> ret = new ArrayList<String>();
		ret.add(s.readString());
		
		return ret;
	}
	
	// Full domain hash
	protected BigInteger hash(String input){
		//TODO: Fix this hash
 		return hash(input.getBytes(), (byte)0).mod(N);
 	}
	
	protected void loadSharedKeys(){
		ca = new CA();
		N = ca.getN();
		e = ca.getE();
		
		g = new BigInteger("af3ecd5a39c2ec6fd3ebfd44a4e18a422429c3b18ec6a716968f0ea524f1e19a67f7e117211a802eaae551e4b43967b4b63a50ef6d2c31397a845456550eaa89d4fe8959e402e1484139e5ff52187882f25967ad10e294c7980dd678ebb2a592e031e75ada46d1c5af16caebcd86d06430de7e7ba6fb71590d7329ee744977dd", 16);
		g = g.modPow(BigInteger.valueOf(4), N);
	}

}
