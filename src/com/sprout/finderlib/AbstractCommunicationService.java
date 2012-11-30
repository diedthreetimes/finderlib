package com.sprout.finderlib;

import java.math.BigInteger;

import org.spongycastle.asn1.nist.NISTNamedCurves;
import org.spongycastle.math.ec.ECPoint;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public abstract class AbstractCommunicationService implements
		CommunicationService {

	protected Handler mHandler;
	protected int mState;
	protected Context mContext;
	
	private static final String TAG = "AbstractCommunicationService";
    private static final boolean D = false;
	
	
    /**
    * Constructor. Prepares a new Service object.
    * @param context  The UI Activity Context
    * @param handler  A Handler to send messages back to the UI Activity
    */
    public AbstractCommunicationService(Context context, Handler handler) {
        mContext = context;
        mState = STATE_NONE;
        mHandler = handler;
    }
    
    
	@Override
    // TODO: Work out communicating messages to multiple handlers
    // This allows an activity to start a connection, and then hand it off
    //   to another activity or service
	public synchronized void setHandler(Handler handler) {
		mHandler = handler;
	}

	/**
     * Set the current state of the connection
     * @param state  An integer defining the current connection state
     */
    protected synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    
	@Override
    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

	public void write(String buffer) {
    	write(buffer.getBytes());
    	if(D) Log.d(TAG, "Write: " + buffer);
    }
    
    public void write(BigInteger out){
    	write(out.toByteArray());
    }
    
    public void write(ECPoint out){
    	write(out.getEncoded());
    }

    public BigInteger readBigInteger(){
    	return new BigInteger(read());
    }
    
    public ECPoint readECPoint(){
    	//TODO: This probably doesn't belong here
    	return NISTNamedCurves.getByName("P-224").getCurve().decodePoint(read());
    }
    
    /**
     * Read a string from Connected Thread
     * @see #read()
     */
	public String readString() {
		return new String(read());
	}

}
