package com.sprout.finderlib.communication;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
  public static final int MESSAGE_DEVICE = 4;
  public static final int MESSAGE_TOAST = 5;
  public static final int MESSAGE_FAILED = 6;
  public static final int MESSAGE_DISABLED = 7;
  public static final int MESSAGE_ENABLED = 8;

  public static final String EXTRA_SERVICE_TRANFER = "com.sprout.finderlib.communicationservice.service_transfer";
  public static final Map<String, WeakReference<CommunicationService>> com_transfers = new HashMap<String, WeakReference<CommunicationService>>();

  // Key names sent to mHandler
  public static final String DEVICE = "device_serialized";
  public static final String TOAST = "toast";

  public void setHandler(Handler handler);
  public int getState();
  public boolean isEnabled();

  /**
   * start discovery process
   */
  public void start();
  public void start(boolean secure);
  public void connect(Device device);
  public void connect(String address);
  public void connect(Device device, boolean secure);
  public void connect(String address, boolean secure);
  public void stop();
  /**
   * pause discovery process
   */
  public void pause();
  public void resume();

  // TODO: Add methods for reading and writing collections

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
  public boolean getReadLoop();

  public Set<Device> bondedPeers();
  public void discoverPeers(Callback callback);
  
  public void stopDiscovery();


  public Device getSelf();

  /**
   * A interface to provide callbacks for certain events.
   * This is an alternative interface to the passed Handler which allows inter-thread communication
   */
  //TODO: Provide a way for the device list to access this object (Think about making it static)
  //TODO: Think about which method signature (or maybe both) makes sense
  abstract class Callback {
    /**
     * Called when any peer of the medium is discovered. These peers are not guaranteed to belong to the same application.
     * For many subclasses, this method may return results quicker then {@link #onServiceDiscovered(Device)}
     */
    public abstract void onPeerDiscovered(Device peer);
    public void onPeerDiscovered(Collection<Device> peers){
      for(Device device: peers)
        onPeerDiscovered(device);
    }

    /**
     * Called when peer is found running the same service. (Where a "service" is specific to the subclass)
     */
    public abstract void onServiceDiscovered(Device peer);
    public void onServiceDiscovered(Collection<Device> peers){
      for(Device device: peers)
        onPeerDiscovered(device);
    }


    public void onDiscoveryComplete(boolean success){

    }

    public void onDiscoveryStarted(){

    }
  }


}
