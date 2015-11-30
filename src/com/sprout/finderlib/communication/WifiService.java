package com.sprout.finderlib.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * WifiService using network discovery service
 * 
 * OK 2 important TODO's here
 * 2) where do we unregister the registered service?
 * technically, we should unregister it when
 * WifiService is stopped, which means when in the app?
 * Ideas:
 * unregister it once the main activity is destroyed
 * or when we no longer connect to the wifi
 * or when DiscoveryService is stopped (then we need to move registerService)
 * 3) implement retry but how?
 * @author norrathep
 *
 */
public class WifiService extends AbstractCommunicationService {

  private static final boolean D = false;
  private static final boolean V = false;

  private static final String TAG = WifiService.class.getSimpleName();

  // Maximum reconnect attempts
  private static final int MAX_RETRY = 2;

  private AcceptThread mSecureAcceptThread;
  private ConnectThread mConnectThread;
  private ConnectedThread mConnectedThread;
  
  private int mNumTries; //TODO: retry this...
  
  /**
   * NSD components 
   */

  public static final String SERVICE_TYPE = "_http._tcp.";

  NsdManager mNsdManager;
  NsdManager.ResolveListener mResolveListener;
  NsdManager.DiscoveryListener mDiscoveryListener;
  NsdManager.RegistrationListener mRegistrationListener;

  NsdServiceInfo mService;

  private static final int PORT = 8997;
  public static final String SERVICE_NAME = "VVDFGSW";
  public String mServiceName = SERVICE_NAME;

//  
//  private int nsdState = NsdManager.NSD_STATE_DISABLED;
//
//  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//      @Override
//      public void onReceive(Context context, Intent intent) {
//          String action = intent.getAction();
//          Log.i(TAG, "in onReceive with action "+action);
//          if (NsdManager.ACTION_NSD_STATE_CHANGED.equals(action)) {
//            Log.i(TAG, "setting nsd state");
//            nsdState = intent.getIntExtra(NsdManager.EXTRA_NSD_STATE, NsdManager.NSD_STATE_DISABLED);
//          }
//      }
//  };
  
  public WifiService(Context context, Handler handler) {
    super(context, handler);

    mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    
    // TODO: when are we going to unregister this?
//    mContext.registerReceiver(mReceiver, new IntentFilter());
  }

  @Override
  public boolean isEnabled() {
    // TODO: not sure why not working
//    Log.i(TAG, "in isEnabled: state is "+nsdState);
//    return (nsdState == NsdManager.NSD_STATE_ENABLED ? true : false);
    
    // should be enough
    WifiManager wifi = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
    return wifi.isWifiEnabled();
  }
  
  private boolean registered = false;

  @Override
  public void start() {
    start(false);
  }

  /**
   * Start the communication service. Specifically start AcceptThread to begin a
   * session in listening (server) mode. Called by the Activity onResume() */
  @Override
  public synchronized void start(boolean secure) {
    if(D) Log.d(TAG, "start");
    
    if(!registered) {
      registerService(PORT);
      registered = true;
    }

    resume();

    setState(STATE_LISTEN);
    startAcceptThread(PORT);

    mNumTries = 0;
  }
  
  @Override
  public synchronized void connect(String address, boolean secure) {
    Log.i(TAG, "Connecting to addr: "+address);
    
    startConnectThread(address, PORT, secure);
  }
  

  /**
   * Start the ConnectThread to initiate a connection to a remote device.
   * @param device  The ip address to connect to
   * @param port the port to connect to 
   * @param secure Socket Security type - Secure (true) , Insecure (false)
   */
  public synchronized void startConnectThread(String address, int port, boolean secure) {
    //TODO: Why is the connect thread called twice!!
//    if (D) Log.d(TAG, "connect to: " + device);

    // Don't throw out connections if we are already connected
    if( mState == STATE_CONNECTING || mConnectedThread != null ){
      return;
    }

    mNumTries++;
    // Commented out for test
    //        // Cancel any thread attempting to make a connection
    //        if (mState == STATE_CONNECTING) {
    //            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
    //        }
    //
    //        // Cancel any thread currently running a connection
    //        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

    // Start the thread to connect with the given device
    setState(STATE_CONNECTING);

    mConnectThread = new ConnectThread(address, port, secure);
    mConnectThread.start();        
  }
  
  @Override
  public synchronized void stop() {
    Log.d(TAG, "Stop: registered = "+registered);
    pause();

    setState(STATE_STOPPED);

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
    
    if(registered) {
      //TODO: definitely need more testing.
      // it could cause an error if mRegistrationListener is not active
      // maybe putting try catch here is a good idea just in case?
      Log.i(TAG, "unregistering");
      mNsdManager.unregisterService(mRegistrationListener);
      registered = false;
    }
  }
  
  @Override
  public synchronized void pause() {
  }

  @Override
  public synchronized void resume() {
  }

  @Override
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

  @Override
  public byte[] read() {
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

  @Override
  public void setReadLoop(boolean flag) {
    ConnectedThread r;
    // Synchronize a copy of the ConnectedThread
    synchronized (this) {
      if (mState != STATE_CONNECTED) return;
      r = mConnectedThread;
    }
    // Perform the write unsynchronized
    r.setReadLoop(flag);
  }

  @Override
  public boolean getReadLoop() {
    boolean ret = false; // The default
    synchronized (this) {
      if (mState == STATE_CONNECTED)
        ret = mConnectedThread.mForwardRead;
    }
    return ret;
  }

  @Override
  public Set<Device> bondedPeers() {
    // WiFi does not have a concept of bonded peers
    //   For secure connections this should change.

    //TODO: Retrieve the remembered groups here
    return new HashSet<Device>();
  }

  @Override
  public void discoverPeers(final Callback callback) {
      Log.i(TAG, "start discovering peers");
      mResolveListener = new NsdManager.ResolveListener() {
  
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed" + errorCode);
        }
  
        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
            
            if (serviceInfo.getServiceName().equals(mServiceName)) {
                Log.d(TAG, "Same IP.");
                return;
            }
            mService = serviceInfo;

            Device peer = new Device(serviceInfo);
            
            callback.onServiceDiscovered(peer);
        }
    };
    
    // set up listener
    mDiscoveryListener = new NsdManager.DiscoveryListener() {

      @Override
      public void onDiscoveryStarted(String regType) {
          Log.d(TAG, "Service discovery started");
          callback.onDiscoveryStarted();
      }

      @Override
      public void onServiceFound(NsdServiceInfo service) {
          if (!service.getServiceType().equals(SERVICE_TYPE)) {
              Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
          } else if (service.getServiceName().equals(mServiceName)) {
              Log.d(TAG, "Same machine: " + mServiceName);
          } else {
            Log.d(TAG, "Service discovery success " + service);
            setTimeOut(DISCOVERY_IDLE_TIMEOUT, callback);
            if (service.getServiceName().contains(SERVICE_NAME)){
              Log.d(TAG, "resolving the host");
              mNsdManager.resolveService(service, mResolveListener);
            } else {
                Log.d(TAG, "not applicable");
                // TODO: dummy address since it cant resolve
                callback.onPeerDiscovered(new Device(service.getServiceName(), "..."));
            }
          }
      }

      @Override
      public void onServiceLost(NsdServiceInfo service) {
          Log.e(TAG, "service lost" + service);
          if (mService == service) {
              mService = null;
          }
      }
      
      @Override
      public void onDiscoveryStopped(String serviceType) {
          Log.i(TAG, "Discovery stopped: " + serviceType);        
      }

      @Override
      public void onStartDiscoveryFailed(String serviceType, int errorCode) {
          Log.e(TAG, "Discovery failed: Error code:" + errorCode);
          mNsdManager.stopServiceDiscovery(this);
          callback.onDiscoveryComplete(false);
      }

      @Override
      public void onStopDiscoveryFailed(String serviceType, int errorCode) {
          Log.e(TAG, "Discovery failed: Error code:" + errorCode);
          mNsdManager.stopServiceDiscovery(this);
          callback.onDiscoveryComplete(false);
      }
    };
    setTimeOut(DISCOVERY_IDLE_TIMEOUT, callback);
    mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
  }
  
  private final long DISCOVERY_IDLE_TIMEOUT = 10*1000; // 10 secs is enough?
  private Handler handler = new Handler();
  
  /**
   * remove all runnables and set timeout runnable
   * @param delayMillis
   * @param callback
   */
  private void setTimeOut(long delayMillis, final Callback callback) {
    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(new Runnable() {
      
      @Override
      public void run() {
        Log.i(TAG, "Havent discovered any valid service for "+DISCOVERY_IDLE_TIMEOUT+" ms. Now stopping it");
        callback.onDiscoveryComplete(true);
        
      }
    }, delayMillis);
    
  }

  @Override
  public void stopDiscovery() {
    try {
      mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    } catch(Exception e) {
      Log.e(TAG, e.getMessage());
      Log.e(TAG, "Maybe you call DiscoveryService.run() many times or you are being discovered?");
    }
  }

  @Override
  public Device getSelf() {
    NsdServiceInfo serviceInfo  = new NsdServiceInfo();
    serviceInfo.setPort(PORT);
    serviceInfo.setServiceName(mServiceName);
    serviceInfo.setServiceType(SERVICE_TYPE);
    return new Device(serviceInfo);
  }

  @Override
  protected void retry() {
    if(D) Log.d(TAG, "retry");

    if(D) Log.d(TAG, "Retrying in state: " + getState());

    if(mState == STATE_CONNECTED) return;
    signalFailed();
    start();
    return;
    
  }
  
  /**
   * This thread runs while listening for incoming connections. It behaves
   * like a server-side client. It runs until a connection is accepted
   * (or until cancelled).
   */
  private class AcceptThread extends Thread {
    // The local server socket
    private final ServerSocket mmServerSocket;
    private String mSocketType;

    public AcceptThread(boolean secure, int port) {
      ServerSocket tmp = null;
      mSocketType = secure ? "Secure":"Insecure";

      // Create a new listening server socket
      try {
        if (secure) {
          // We should use something like this here SSLServerSocket.startHandshake
          tmp = new ServerSocket(port);
        } else {
          tmp = new ServerSocket(port);
        }
      }
      catch (BindException e) {
        Log.e(TAG, "Server Socket Failed");
        Log.e(TAG, "listen() failed could not bind to port: " + port, e);
      }
      catch (IOException e) {
        Log.e(TAG, "Server Socket Failed");
        Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
      }

      mmServerSocket = tmp;
    }

    public void run() {
      if(D) Log.i(TAG, "BEGIN mAcceptThread Socket Type: " + mSocketType + this );
      setName("AcceptThread" + mSocketType);

      Socket socket = null;

      // Listen to the server socket if we're not connected
      while (mState != STATE_CONNECTED) {
        try {

          if(mmServerSocket == null){
            Log.e(TAG, "Accet socket did not bind");
            break;
          }
          // This is a blocking call and will only return on a
          // successful connection or an exception
          socket = mmServerSocket.accept();

        } catch (IOException e) {
          Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
          break;
        }

        // If a connection was accepted
        if (socket != null) {
          synchronized (WifiService.this) {
            switch (mState) {
            case STATE_LISTEN:
            case STATE_CONNECTING:
              // Situation normal. Start the connected thread.
              if(D) Log.i(TAG,"Socket accepted start connected thread");
              connected(socket, mSocketType);
              break;
            case STATE_NONE:
            case STATE_CONNECTED:
              // Either not ready or already connected. Terminate new socket.
              try {
                if(D) Log.d(TAG, "Already connected, closing socket");
                socket.close();
              } catch (IOException e) {
                Log.e(TAG, "Could not close unwanted socket", e);
              }
              return; 
            }
          }
        }
        else if(D) Log.d(TAG, "Accept socket returned null");
      }
      if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

    }

    public void cancel() {
      if (D) Log.d(TAG, "Accept thread cancel. Socket Type" + mSocketType);
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
    private final Socket mmSocket;
    private String mSocketType;
    private String host;
    int port;

    public ConnectThread(String host, int port, boolean secure) {
      Socket tmp = null;
      mSocketType = secure ? "Secure" : "Insecure";

      this.host = host;
      this.port = port;

      // Initialize a socket

      try {
        if (secure) {
          //TODO: use SSL here
          tmp = new Socket();
          tmp.bind(null);
        } else {
          tmp = new Socket();
          tmp.bind(null);
        }
      } catch (IOException e) {
        Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
      }
      mmSocket = tmp;
    }

    public void run() {
      Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
      setName("ConnectThread" + mSocketType);

      // Always cancel discovery) because it will slow down a connection
      //TODO: Cancel discovery?

      // Make a connection to the Socket
      try {
        // This is a blocking call and will only return on a
        // successful connection or an exception              
        mmSocket.connect((new InetSocketAddress(host, port)));//, SOCKET_TIMEOUT);
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
      synchronized (WifiService.this) {
        mConnectThread = null;
      }

      // Start the connected thread
      connected(mmSocket, mSocketType);
      Log.i(TAG, "END mConnectThread SocketType:" + mSocketType);
    }

    public void cancel() {
      if(D) Log.d(TAG, "Canceling connect thread");
      if (mmSocket != null) {
        if (mmSocket.isConnected()) {
          try {
            mmSocket.close();
          } catch (IOException e) {
            Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);                        
            Log.e(TAG, e.getStackTrace().toString());
          }
        }
      }
    }
  }

  /**
   * This thread runs during a connection with a remote device.
   * It handles all incoming and outgoing transmissions.
   */
  private class ConnectedThread extends Thread {
    private final Socket mmSocket;
    private final DataInputStream mmInStream;
    private final DataOutputStream mmOutStream;
    private boolean mForwardRead = false;
    private BlockingQueue<byte []> mMessageBuffer;

    public ConnectedThread(Socket socket, String socketType) {
      Log.d(TAG, "create ConnectedThread: " + socketType);
      mmSocket = socket;
      DataInputStream tmpIn = null;
      DataOutputStream tmpOut = null;
      mMessageBuffer = new LinkedBlockingQueue<byte []>(); // TODO: add a capacity here to prevent doS

      if(!socket.isConnected())
        Log.e(TAG,"Connected thread passed closed socket");

      // Get the BluetoothSocket input and output streams
      try {
        //TODO: Investigate performance difference when using buffered input stream (probably nothing for network)            
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
        return null; //TODO: Raise here?
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
        } 
        //catch(EOFException e){
        //Log.e(TAG, "Less bytes then expected", e);             
        //} 
        catch (IOException e) {
          Log.e(TAG, "disconnected", e);
          connectionLost();
          break;
        }
      }

      Log.i(TAG, "END mConnectedThread");
    }

    public void cancel() {
      if(D) Log.d(TAG, "Canceling connected thread");
      try {
        mmSocket.close();
      } catch (IOException e) {
        Log.e(TAG, "close() of connect socket failed", e);
      }
    }
  }

  public void connected(Socket socket, String mSocketType) {
    if (D) Log.d(TAG, "connected, Socket Type:" + mSocketType);

    // Cancel the thread that completed the connection
    if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

    // Cancel any thread currently running a connection
    if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

    // Cancel the accept thread because we only want to connect to one device
    if (mSecureAcceptThread != null) {
      mSecureAcceptThread.cancel();
      mSecureAcceptThread = null;
    }

    // Start the thread to manage the connection and perform transmissions
    mConnectedThread = new ConnectedThread(socket, mSocketType);
    mConnectedThread.start();

    // Send the name of the connected device back to the UI Activity
    Message msg = mHandler.obtainMessage(MESSAGE_DEVICE);
    Bundle bundle = new Bundle();
    bundle.putSerializable(DEVICE, new Device("Unkown name", socket.getInetAddress().toString()));
    msg.setData(bundle);
    mHandler.sendMessage(msg);

    setState(STATE_CONNECTED);
    
  }
  
  private synchronized void startAcceptThread(int port) {
    Log.i(TAG, "starting accept thread");
    // Cancel any thread attempting to make a connection
    if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

    // Cancel any thread currently running a connection
    if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

    // Start the thread to listen on a BluetoothServerSocket
    if (mSecureAcceptThread == null) {
      mSecureAcceptThread = new AcceptThread(true, port);
      mSecureAcceptThread.start();
    }
  }
  

  public void registerService(int port) {
      Log.i(TAG, "Registering service at port "+port);
      mRegistrationListener = new NsdManager.RegistrationListener() {
  
        @Override
        public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
            mServiceName = NsdServiceInfo.getServiceName();
            Log.i(TAG, "registered name: "+mServiceName);
        }
        
        @Override
        public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
            Log.i(TAG, "registration failed: "+arg0.toString());
        }
  
        @Override
        public void onServiceUnregistered(NsdServiceInfo arg0) {
          Log.i(TAG, "Successfully unregister: "+arg0.toString());
        }
        
        @Override
        public void onUnregistrationFailed(NsdServiceInfo arg0, int errorCode) {
          Log.i(TAG, "Unregistration failed: "+arg0.toString());
        }
        
      };
      NsdServiceInfo serviceInfo  = new NsdServiceInfo();
      serviceInfo.setPort(port);
      serviceInfo.setServiceName(mServiceName);
      serviceInfo.setServiceType(SERVICE_TYPE);
      
      mNsdManager.registerService(
              serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
      
  }

}
