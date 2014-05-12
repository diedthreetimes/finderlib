package com.sprout.finderlib.crypto;

import java.math.BigInteger;

import com.sprout.finderlib.communication.CommunicationService;

import android.util.Log;


public abstract class AbstractPSIProtocol<Params, Progress, Result> extends PrivateProtocol<Params, Progress, Result> {	
  // Debug info
  private final String TAG = "AbstractPSIProtocol";
  private final boolean D = true;

  //public keys
  protected BigInteger p, q, g;

  // t = (p-1)/q to hash into the group Z*p
  protected BigInteger t;

  public AbstractPSIProtocol(String testName, CommunicationService s, boolean client) {
    super(testName, s, client);

  }

  // Load the common inputs, p q and g which are:
  // p - a prime number
  // q - the sub prime
  // g - the base (generator)
  protected void loadSharedKeys(){
    //p = new BigInteger("b95b6c851ff243745411a0c901a14c217d429edba65b8a298534731e5c3182bf9806f592611bbf2ded9fc4a1b21acfe685112ec38d6d7c4b4bf28b5bcc636b6c4844fdcf449b002b4bc5143a32e0f7b713097b062683cc7cdaa7adfd6c49b0d897487d4e2d0c94bf0c8cafe11580cb84f14ca7922142503ee0dfc377591233c1", 16);
    //q = new BigInteger("d9ad24d2728323f368eac50bb1e1154483d820b7", 16);
    //g = new BigInteger("af3ecd5a39c2ec6fd3ebfd44a4e18a422429c3b18ec6a716968f0ea524f1e19a67f7e117211a802eaae551e4b43967b4b63a50ef6d2c31397a845456550eaa89d4fe8959e402e1484139e5ff52187882f25967ad10e294c7980dd678ebb2a592e031e75ada46d1c5af16caebcd86d06430de7e7ba6fb71590d7329ee744977dd", 16);

    p = new BigInteger("e0a67598cd1b763bc98c8abb333e5dda0cd3aa0e5e1fb5ba8a7b4eabc10ba338fae06dd4b90fda70d7cf0cb0c638be3341bec0af8a7330a3307ded2299a0ee606df035177a239c34a912c202aa5f83b9c4a7cf0235b5316bfc6efb9a248411258b30b839af172440f32563056cb67a861158ddd90e6a894c72a5bbef9e286c6b", 16);
    q = new BigInteger("e950511eab424b9a19a2aeb4e159b7844c589c4f", 16);
    g = new BigInteger("d29d5121b0423c2769ab21843e5a3240ff19cacc792264e3bb6be4f78edd1b15c4dff7f1d905431f0ab16790e1f773b5ce01c804e509066a9919f5195f4abc58189fd9ff987389cb5bedf21b4dab4f8b76a055ffe2770988fe2ec2de11ad92219f0b351869ac24da3d7ba87011a701ce8ee7bfe49486ed4527b7186ca4610a75", 16);


    //TODO: Load these keys from a file

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
