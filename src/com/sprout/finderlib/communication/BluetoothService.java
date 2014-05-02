package com.sprout.finderlib.communication;

/**
 * This service is largely just pulled from the android docs and handles
 * estabilishing and listening for bluetooth connections
 */

//TODO: benchmark (figure out what is slowing down) this new method of encoding vs string encoding (this may be a GC issue)
//          This may also be due to the overhead of using objectstream see http://stackoverflow.com/questions/2251051/performance-issue-using-javas-object-streams-with-sockets
//TODO: Double check that secure implies encryption and not just authentication

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.spongycastle.asn1.nist.NISTNamedCurves;
import org.spongycastle.math.ec.ECPoint;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected. It does not
 * howerver, handle discovery.
 */
public class BluetoothService extends AbstractCommunicationService {	
  // Debugging
  private static final String TAG = "BluetoothService";
  private static final boolean D = true;

  // Name for the SDP record when creating server socket
  private static final String NAME_SECURE = "GenomicTestSecure";
  private static final String NAME_INSECURE = "GenomicTestInsecure";

  // Unique UUID for this application
  private static final UUID MY_UUID_SECURE =
      UUID.fromString("6bc03610-4155-11e1-b86c-0800200c9a66");
  private static final UUID MY_UUID_INSECURE = // Secure threads are not allowed but left for debugging
      UUID.fromString("cf32cad0-cfcb-11e3-9c1a-0800200c9a66");

  // Maximum reconnect attempts
  private static final int MAX_RETRY = 2;

  // Member fields
  private final BluetoothAdapter mAdapter;
  private AcceptThread mSecureAcceptThread;
  private ConnectThread mConnectThread;
  private ConnectedThread mConnectedThread;

  private int mNumTries;
  private BluetoothDevice mDevice;
  private boolean mSecure;


  /**
   * Constructor. Prepares a new Bluetooth session.
   * @param context  The UI Activity Context
   * @param handler  A Handler to send messages back to the UI Activity
   */
  public BluetoothService(Context context, Handler handler) {
    super(context, handler);
    mAdapter = BluetoothAdapter.getDefaultAdapter();

    // Register for broadcasts when a device is discovered or finished
    mIntentFilter = new IntentFilter();
    mIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
    mIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
    mIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    mIntentFilter.addAction(BluetoothDevice.ACTION_UUID);
  }


  /**
   * Start the communication service. Specifically start AcceptThread to begin a
   * session in listening (server) mode. Called by the Activity onResume() */
  @Override
  public synchronized void start(boolean secure) {
    if (D) Log.d(TAG, "start");

    mSecure = secure;

    resume();

    startAcceptThread(secure);

    mNumTries = 0;

    setState(STATE_LISTEN);
  }

  private synchronized void startAcceptThread(boolean secure) {
    // Cancel any thread attempting to make a connection
    if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

    // Cancel any thread currently running a connection
    if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

    // Start the thread to listen on a BluetoothServerSocket
    if (mSecureAcceptThread == null) {
      mSecureAcceptThread = new AcceptThread(secure);
      mSecureAcceptThread.start();
    }
  }

  protected synchronized void retry() {
    if(D) Log.d(TAG, "retry");

    if(D) Log.d(TAG, "Retrying in state: " + getState());

    if(mState == STATE_CONNECTED) return;

    //TODO: Does this logic belong here
    if(mNumTries >= MAX_RETRY){
      signalFailed();
      start(mSecure);
      return;
    }

    startAcceptThread(mSecure);

    setState(STATE_RETRY);

    int sleep = (int) (Math.random()*1000 + 100);
    if(D) Log.d(TAG, "Sleeping: " + sleep);
    try {
      Thread.sleep(sleep);
    } catch (InterruptedException e) {
      Log.e(TAG, "Sleep interupted");
    } //TODO: This blocks the UI!

    if(D) Log.d(TAG, "Waking up: " + getState());

    // TODO: make this less strict
    if( mState != STATE_CONNECTING && mState != STATE_CONNECTED && mConnectedThread == null && mConnectThread == null)
      connect(mDevice, mSecure);
  }




  /**
   * Connect using the already configured security method.
   */
  @Override
  public void connect(String address) {
    connect(address, mSecure);
  }

  public synchronized void connect(String address, boolean secure){
    BluetoothDevice device = mAdapter.getRemoteDevice(address);
    connect(device, secure);
  }
  /**
   * Start the ConnectThread to initiate a connection to a remote device.
   * @param device  The BluetoothDevice to connect
   * @param secure Socket Security type - Secure (true) , Insecure (false)
   */
  public synchronized void connect(BluetoothDevice device, boolean secure) {
    if (D) Log.d(TAG, "connect to: " + device);

    // Don't throw out connections if we are already connected
    if( mState == STATE_CONNECTING || mConnectedThread != null ){
      return;
    }
    
    mNumTries++;
    mDevice = device;
    mSecure = secure;
    
    // Cancel any thread attempting to make a connection
    if (mState == STATE_CONNECTING) {
      if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
    }

    // Cancel any thread currently running a connection
    if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

    // Start the thread to connect with the given device
    mConnectThread = new ConnectThread(device, secure);
    mConnectThread.start();
    setState(STATE_CONNECTING);
  }

  /**
   * Start the ConnectedThread to begin managing a Bluetooth connection
   * @param socket  The BluetoothSocket on which the connection was made
   * @param device  The BluetoothDevice that has been connected
   */
  public synchronized void connected(BluetoothSocket socket, BluetoothDevice
      device, final String socketType) {
    if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

    // Cancel the thread that completed the connection
    if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

    // Cancel any thread currently running a connection
    if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

    // Cancel the accept thread because we only want to connect to one device
    if (mSecureAcceptThread != null) {
      if(D) Log.d(TAG, "Canceling the accept thread");
      mSecureAcceptThread.cancel();
      mSecureAcceptThread = null;
    }

    // Start the thread to manage the connection and perform transmissions
    mConnectedThread = new ConnectedThread(socket, socketType);
    mConnectedThread.start();

    // Send the name of the connected device back to the UI Activity
    Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
    Bundle bundle = new Bundle();
    bundle.putString(DEVICE_NAME, device.getName());
    msg.setData(bundle);
    mHandler.sendMessage(msg);

    setState(STATE_CONNECTED);
  }

  private boolean active = false; // Boolean indicating the status of the broadcast receiver
  
  // TODO: Will the activating another activity cause these events to be missed? Probably.
  public synchronized void pause() {
    if (active)
      mContext.unregisterReceiver(mReceiver);
    
    active = false;
  }
  public synchronized void resume() {
    if (!active)
      mContext.registerReceiver(mReceiver, mIntentFilter);
    
    active = true;
  }


  /**
   * Stop all threads
   */
  public synchronized void stop() {
    pause();
    if (D) Log.d(TAG, "stop");
    setState(STATE_STOPPED);

    // Make sure we're not doing discovery anymore
    if (mAdapter != null) {
      mAdapter.cancelDiscovery();
    }

    if (mConnectThread != null) {
      mConnectThread.cancel();
      mConnectThread = null;
    }

    if (mConnectedThread != null) {
      mConnectedThread.cancel();
      mConnectedThread = null;
    }

    if (mSecureAcceptThread != null) {
      mSecureAcceptThread.cancel();
      mSecureAcceptThread = null;
    }
  }

  /**
   * Write to the ConnectedThread in an unsynchronized manner
   * 
   * This does not add message boundries!!
   * @param out The bytes to write
   * @see ConnectedThread#write(byte[])
   */
  public void write(byte[] out) {
    // Create temporary object
    ConnectedThread r;
    // Synchronize a copy of the ConnectedThread
    synchronized (this) {
      if (mState != STATE_CONNECTED) return;
      r = mConnectedThread;
    }
    // Perform the write unsynchronized
    r.write(out);
  }





  /**
   * Read from the ConnectedThread in an unsynchronized manner
   * Note, this is a blocking call
   * @return the bytes read
   * @see ConnectedThread#read()
   */
  public byte [] read() {
    // Create temporary object
    ConnectedThread r;
    // Synchronize a copy of the ConnectedThread
    synchronized (this) {
      if (mState != STATE_CONNECTED) return null;
      r = mConnectedThread;
    }

    // Perform the read unsynchronized and parse
    byte[] readMessage = r.read();

    if(D) Log.d(TAG, "Read: " + new String(readMessage));
    return readMessage;
  }


  /**
   * Sets the readLoop flag to signify the behavior of socket reads
   * @param flag The desired behavior of the read loop. \
   *     True[on] signifies that all messages will be passed to the handler as they arrive. 
   * @see ConnectedThread#setReadLoop()
   */
  public void setReadLoop(boolean flag){
    // TODO: We should be able to set this before we are connected
    Log.i(TAG, "Setting the read loop to " + flag);
    ConnectedThread r;
    // Synchronize a copy of the ConnectedThread
    synchronized (this) {
      if (mState != STATE_CONNECTED) return;
      r = mConnectedThread;
    }
    // Perform the write unsynchronized
    r.setReadLoop(flag);
  }
  
  public boolean getReadLoop() {
    boolean ret = false; // The default
    synchronized (this) {
      if (mState == STATE_CONNECTED)
        ret = mConnectedThread.mForwardRead;
    }
    return ret;
  }

  /**
   * This thread runs while listening for incoming connections. It behaves
   * like a server-side client. It runs until a connection is accepted
   * (or until cancelled).
   */
  private class AcceptThread extends Thread {
    // The local server socket
    private final BluetoothServerSocket mmServerSocket;
    private String mSocketType;

    public AcceptThread(boolean secure) {
      BluetoothServerSocket tmp = null;
      mSocketType = secure ? "Secure":"Insecure";

      // Create a new listening server socket
      try {
        if (secure) {
          tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
              MY_UUID_SECURE);
        } else {
          tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
              NAME_INSECURE, MY_UUID_INSECURE);
        }
      } catch (IOException e) {
        Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
      }
      mmServerSocket = tmp;
    }

    public void run() {
      if(D) Log.i(TAG, "BEGIN mAcceptThread Socket Type: " + mSocketType + this );
      setName("AcceptThread" + mSocketType);

      BluetoothSocket socket = null;

      // Listen to the server socket if we're not connected
      while (mState != STATE_CONNECTED) {
        try {
          // This is a blocking call and will only return on a
          // successful connection or an exception
          socket = mmServerSocket.accept();
        } catch (IOException e) {
          Log.e(TAG, "Socket Type: " + mSocketType + " accept() failed", e);
          break;
        }

        // If a connection was accepted
        if (socket != null) {
          synchronized (BluetoothService.this) {
            switch (mState) {
            case STATE_LISTEN:
            case STATE_CONNECTING:
              // Situation normal. Start the connected thread.
              connected(socket, socket.getRemoteDevice(),
                  mSocketType);
              break;
            case STATE_NONE:
            case STATE_CONNECTED:
              // Either not ready or already connected. Terminate new socket.
              try {
                socket.close();                             
              } catch (IOException e) {
                Log.e(TAG, "Could not close unwanted socket", e);
              }

              //TODO: Should we really be returning here?
              return;              
            }
          }
        }
      }
      if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

    }

    public void cancel() {
      if (D) Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
      try {
        mmServerSocket.close();
      } catch (IOException e) {
        Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
      }
    }
  }

  /**
   * This thread runs while attempting to make an outgoing connection
   * with a device. It runs straight through; the connection either
   * succeeds or fails.
   */
  private class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private String mSocketType;

    public ConnectThread(BluetoothDevice device, boolean secure) {
      mmDevice = device;
      BluetoothSocket tmp = null;
      mSocketType = secure ? "Secure" : "Insecure";

      // Get a BluetoothSocket for a connection with the
      // given BluetoothDevice
      try {
        if (secure) {
          tmp = device.createRfcommSocketToServiceRecord(
              MY_UUID_SECURE);
        } else {
          tmp = device.createInsecureRfcommSocketToServiceRecord(
              MY_UUID_INSECURE);
        }
      } catch (IOException e) {
        Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
      }
      mmSocket = tmp;
    }

    public void run() {
      Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
      setName("ConnectThread" + mSocketType);

      // Always cancel discovery because it will slow down a connection
      mAdapter.cancelDiscovery();

      // Make a connection to the BluetoothSocket
      try {
        // This is a blocking call and will only return on a
        // successful connection or an exception
        mmSocket.connect();
      } catch (IOException e) {
        // Close the socket
        try {
          mmSocket.close();
        } catch (IOException e2) {
          Log.e(TAG, "unable to close() " + mSocketType +
              " socket during connection failure", e2);
        }
        connectionFailed();
        return;
      }

      // Reset the ConnectThread because we're done
      synchronized (BluetoothService.this) {
        mConnectThread = null;
      }

      // Start the connected thread
      connected(mmSocket, mmDevice, mSocketType);
    }

    public void cancel() {
      try {
        mmSocket.close();
      } catch (IOException e) {
        Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
      }
    }
  }

  /**
   * This thread runs during a connection with a remote device.
   * It handles all incoming and outgoing transmissions.
   */
  private class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final DataInputStream mmInStream;
    private final DataOutputStream mmOutStream;
    private boolean mForwardRead = false; // This is a much more sane default for the kind of work we are doing
    private BlockingQueue<byte []> mMessageBuffer;

    public ConnectedThread(BluetoothSocket socket, String socketType) {
      Log.d(TAG, "create ConnectedThread: " + socketType);
      mmSocket = socket;
      DataInputStream tmpIn = null;
      DataOutputStream tmpOut = null;
      mMessageBuffer = new LinkedBlockingQueue<byte []>(); // TODO: add a capacity here to prevent doS

      // Get the BluetoothSocket input and output streams
      try {
        tmpIn = new DataInputStream( socket.getInputStream() );
        tmpOut = new DataOutputStream( socket.getOutputStream() );

      } catch (StreamCorruptedException e) {
        Log.e(TAG, "object streams corrupt", e);
      } catch (IOException e) {
        Log.e(TAG, "temp sockets not created", e);
      }

      mmInStream = tmpIn;
      mmOutStream = tmpOut; 
    }



    /**
     * Sets the readLoop flag to signify the behavior of socket reads
     * 
     * Some cycles may be required before the change takes into affect.
     * As a result, a packet may be processed incorrectly.
     *   
     * True(on) indicates that all messages will be passed to the handler as they arive.
     * False(off) indicates that messages will only be read on demand via {@link #read()}
     * @param flag The desired behavior of the read loop.
     */
    public void setReadLoop(boolean flag) {
      mForwardRead = flag; 
    }

    /**
     * Read from the ConnectedThread in an unsynchronized manner
     * 
     * This is a blocking call and will only return data if the readLoop flag is false
     * @return the bytes read
     * @see ConnectedThread#read()
     */
    public byte[] read() {
      // read should not be used if packets are being read directly off the wire
      if(mForwardRead){
        Log.e(TAG,"Can not perform syncrhomous read with readloop set");
        return null;
        // throw new Exception("Can't call read in serial mode");
      }

      try {
        return mMessageBuffer.take();
      } catch (InterruptedException e) {
        Log.e(TAG, "Message Read Interupted");
        return null;
      }
    }

    /**
     * Write to the connected OutStream.
     * @param buffer  The bytes to write
     */
    public void write(byte[] buffer) {
      try {
        mmOutStream.writeInt(buffer.length);
        mmOutStream.write(buffer);               

        // Share the sent message back to the UI Activity
        mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
        .sendToTarget();
      } catch (IOException e) {
        Log.e(TAG, "Exception during write", e);
      }
    }

    public void run() {
      Log.i(TAG, "BEGIN mConnectedThread");

      int bytes;

      // Keep listening to the InputStream while connected
      while (true) {
        try {
          // Read from the InputStream
          bytes = mmInStream.readInt();	
          byte[] buffer = new byte[bytes]; // TODO: This is a little dangerous	

          mmInStream.readFully(buffer, 0, bytes);

          if(mForwardRead) {
            mHandler.obtainMessage(MESSAGE_READ, buffer.length, -1, buffer)
            .sendToTarget();
          }
          else {
            try {

              mMessageBuffer.put(buffer);
            } catch (InterruptedException e) {
              Log.e(TAG, "Message add interupted.");
              //TODO: possibly throw here
            }
          }
        } catch (IOException e) {
          Log.i(TAG, "disconnected");
          connectionLost();
          break;
        }
      }
    }

    public void cancel() {
      try {
        mmSocket.close();
      } catch (IOException e) {
        Log.e(TAG, "close() of connect socket failed", e);
      }
    }
  }


  //TODO: Move this discovery code to the top of the class
  Callback callback;

  // The BroadcastReceiver that listens for discovered devices and
  // changes the title when discovery is finished
  private IntentFilter mIntentFilter;
  private List<BluetoothDevice> discoveredDevices;
  private Set<String> returnedUUIDs;

  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      // When discovery finds a device
      if (BluetoothDevice.ACTION_FOUND.equals(action)) {
        // Get the BluetoothDevice object from the Intent
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        // If it's already paired, skip it, because it's been listed already
        if (device.getBondState() != BluetoothDevice.BOND_BONDED && callback != null) {
          callback.onPeerDiscovered(new Device(device));
        }

        discoveredDevices.add(device);

        // When discovery is finished, change the Activity title
      } else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
        if (discoveredDevices == null) {
          discoveredDevices = new ArrayList<BluetoothDevice>();
        }
        
        if (returnedUUIDs == null) {
          returnedUUIDs = new HashSet<String>();
        }

        returnedUUIDs.clear();
        discoveredDevices.clear();

        if(callback != null)
          callback.onDiscoveryStarted();
      }
      else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
        // We may need to wait until discovery is completed before we can query for the UUID

        if(callback != null)
          callback.onDiscoveryComplete(true);

        for (BluetoothDevice device : discoveredDevices) {
          Log.i(TAG, "Getting Services for " + device.getName() + ", " + device);
          if(!device.fetchUuidsWithSdp()) {
            Log.e(TAG,"SDP Failed for " + device.getName());
          }
        }
      } else if(BluetoothDevice.ACTION_UUID.equals(action)) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

        if (uuidExtra == null) {     
          uuidExtra = device.getUuids();

          if (uuidExtra == null) {
            Log.e(TAG, "UUID could not be retrieved for device: " + device.getName());
            return;
          }
        } 
        
        for (int i=0; i<uuidExtra.length; i++) {
          String uuid = uuidExtra[i].toString();
          
          // If we haven't already returned this UUID and it matches our UUID
          if (!returnedUUIDs.contains(uuid) && 
              ( (mSecure && uuid.equals(BluetoothService.MY_UUID_SECURE.toString())) ||
                (!mSecure && uuid.equals(BluetoothService.MY_UUID_INSECURE.toString())) ) ) {
            
            Log.i(TAG, "Device: " + device.getName() + ", " + device + ", Service: " + uuid);

            if(callback != null)
              callback.onServiceDiscovered(new Device(device));
            
            returnedUUIDs.add(uuid);
          }

        }
      }
    }
  };

  public void discoverPeers(Callback callback) {
    this.callback = callback;

    // If we're already discovering, stop it
    if (mAdapter.isDiscovering()) {
      mAdapter.cancelDiscovery();
    }

    // Request discover from BluetoothAdapter
    mAdapter.startDiscovery();
  }
  
  public void stopDiscovery() {
    this.callback = null;
    
    mAdapter.cancelDiscovery();
  }

  public Set<Device> bondedPeers() {
    Set<Device> ans = new HashSet<Device>();
    for(BluetoothDevice device: mAdapter.getBondedDevices()){
      ans.add(new Device(device));
    }
    return ans;
  }

  public Device getSelf() {
    return new Device(mAdapter.getName(), mAdapter.getAddress());
  }
}
