package com.sprout.finderlib;

import java.math.BigInteger;

import org.spongycastle.math.ec.ECPoint;

import android.os.Handler;


public interface CommunicationService {
	// Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_STOPPED = 4;   // we're shutting things down
    public static final int STATE_RETRY = 5;      // we are going to retry, but first we listen
    
    // Message types sent to mHandler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_FAILED = 6;
    
    // Key names sent to mHandler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    public void setHandler(Handler handler);
    public int getState();
    
    public void start();
    public void connect(String address, boolean secure);
    public void stop();
    public void pause();
    public void resume();
    
    public void write(byte[] out);
    public void write(String buffer);
    public void write(BigInteger out);
    public void write(ECPoint out);
    public byte [] read();
    public BigInteger readBigInteger();
    public ECPoint readECPoint();
    public String readString();
    
    /**
     * Sets the readLoop flag to signify the behavior of socket reads
     * @param flag The desired behavior of the read loop. \
     *     True[on] signifies that all messages will be passed to the handler as they arrive. 
     * @see ConnectedThread#setReadLoop()
     */
    public void setReadLoop(boolean flag);
    
    
  
}
