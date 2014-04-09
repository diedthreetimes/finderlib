package com.sprout.finderlib.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class WifiService extends AbstractCommunicationService {
	
	static final String TAG = "WifiService";
	static final boolean D = true;
	
	private WifiP2pManager mManager;
	private Channel mChannel;
	private BroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
	
	private WifiP2pDevice device;
	
	private static final int SOCKET_TIMEOUT = 5000;
	private static final int MAX_RETRY = 2;
	
	private int mNumTries;
    private AcceptThread mSecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
	
	public WifiService(Context context, Handler handler) {
		super(context, handler);
		mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(context, context.getMainLooper(), null);

	    mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, handler, null);
	    
	    mIntentFilter = new IntentFilter();
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
	    
	    if(mManager == null) Log.d(TAG, "mManager is null"); 	    
	    if(mChannel == null) Log.d(TAG, "mChannel is null"); // In this case p2p is probably not supported
	}
	
	//TODO: Work out using states and threads!!
	@Override
	public synchronized void start() {
		if(D) Log.d(TAG, "Start");
		resume();
		
		mNumTries = 0;
	}

	@Override
	public synchronized void connect(String address, boolean secure) {
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = address;
		if(secure)
			config.wps.setup = WpsInfo.PBC; // TODO: Does this provide authentication
		
		
		if(D) Log.d(TAG, "Attempting connection with: " + address);
		
		mManager.connect(mChannel, config, new ActionListener() {

		    @Override
		    public void onSuccess() {
		        //TODO: success logic
		    	
		    	if(D) Log.d(TAG, "Invitation sent");
		    }

		    @Override
		    public void onFailure(int reason) {
		        //TODO: failure logic
		    	
		    	if(D) Log.d(TAG, "Invitation failed");
		    }
		});

	}

	@Override
	public synchronized void stop() {
		
		//TODO: Stop discovery
		// Only supported in api 16
		// stopPeerDiscovery (WifiP2pManager.Channel c, WifiP2pManager.ActionListener listener)
		
		if(D) Log.d(TAG, "Stop");
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
	}
	
	@Override
	public void pause() {
		if(D) Log.d(TAG, "Pause");
		mContext.unregisterReceiver(mReceiver);
	}

	@Override
	public void resume() {
		if(D) Log.d(TAG, "Resume");
		mContext.registerReceiver(mReceiver, mIntentFilter);
	}
	
	public void discoverPeers(final Callback callback){
		
		((WiFiDirectBroadcastReceiver) mReceiver).setCallback(callback);
		
		//TODO: Call fragments onInitiateDiscovery to ensure appropriate dialogs
		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				if(D) Log.d(TAG, "Discovery Success");
				callback.onDiscoveryComplete(true);
			}

			@Override
			public void onFailure(int reasonCode) {
				if(D) Log.d(TAG, "Discovery Failure");
				callback.onDiscoveryComplete(false);
			}
		});
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
	public Set<Device> bondedPeers() {
		// WiFi does not have a concept of bonded peers
		//   For secure connections this should change.
		
		//TODO: Retrieve the remembered groups here
		return new HashSet<Device>();
	}

	@Override
	public Device getSelf() {
		if(device != null)
			return new Device(device);
		else
			return new Device("info miss...","info miss..."); // TODO: Test this 
	}
	
	/**
	 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
	 */
	public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

	    private WifiP2pManager manager;
	    private Channel channel;
	    private Handler handler;
	    private Callback callback;
	    private PeerListListener peerListListener;
	    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, Handler handler, Callback callback) {
	        super();
	        this.manager = manager;
	        this.channel = channel;
	        this.handler = handler;
	        this.callback = callback;

	        this.peerListListener = new PeerListListener() {
	    	public void onPeersAvailable (WifiP2pDeviceList peers){
	    		
	    		if(D) Log.d(TAG, "Peers Returned");
	    			List<Device> ans = new ArrayList<Device>();
	    			for(WifiP2pDevice device : peers.getDeviceList()){
	    				Log.d(TAG, "Device address: " + device.deviceAddress);
	    				Log.d(TAG, "Device name: " + device.deviceName);
	    				
	    				ans.add(new Device(device));
	    			}
	    			
	    			WiFiDirectBroadcastReceiver.this.callback.onDiscovery(ans);
	    		// TODO: FIll out with a device list parcel //handler.sendMessage();
	    	}
	    	
	    };

	    }
	    
	    public void setCallback(Callback callback){
	    	this.callback = callback;
	    }

	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();

	        if(D) Log.d(TAG, "Broadcast intent recieved");
	        
	        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
	        	int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
	            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
	                // Wifi Direct is enabled //TODO: Update STATE
	            	Log.d(TAG,"P2P is enabled!");
	            } else {
	                // Wi-Fi Direct is not enabled // TODO: Update STATE
	            	Log.d(TAG,"P2P is disabled!");
	            }
	        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
	        	// request available peers from the wifi p2p manager. This is an
	            // asynchronous call and the calling activity is notified with a
	            // callback on PeerListListener.onPeersAvailable()
	            if (manager != null && peerListListener != null) {
	            	Log.d(TAG,"REQUESTING PEERS");
	                manager.requestPeers(channel, peerListListener);
	            }
	        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
	        	
	        	Log.d(TAG, "P2P Connection Changed");
	            // Respond to new connection or disconnections
	        	NetworkInfo networkInfo = (NetworkInfo) intent
	                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

	        	WifiP2pInfo wifiInfo = (WifiP2pInfo) intent
	        			.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
	        	
	        	Log.d(TAG, networkInfo.toString());
	        	Log.d(TAG, wifiInfo.toString());
	        	
	            if (networkInfo.isConnected()) {
	            	Log.d(TAG, "Requesting Connection Info");
	                // we are connected with the other device, request connection                                 
	                // info to find group owner IP                                                                
		        manager.requestConnectionInfo(channel, new ConnectionInfoListener(){
					@Override
					public void onConnectionInfoAvailable(WifiP2pInfo info) {
						// TODO: Pass connection info to the handler?
						
						// After the group negotiation, we assign the group owner as the server                                                                                        
				        if (info.groupFormed && info.isGroupOwner) {
				            if(D) Log.d(TAG, "Server Started");
				       
				            //TODO: Make port configurable
				            startAcceptThread(8988);
				            setState(STATE_LISTEN);
				        } else if (info.groupFormed) {
				            // The other device acts as the client.                                                                             
				            if(D) Log.d(TAG, "Client Started");
				            
				            String host = info.groupOwnerAddress.getHostAddress();
				            startConnectThread(host, 8988, true);
				        }
				        else {
				        	Log.e(TAG, "Group not formed");
				        }

						
					}
		        });
	            } else {
	                // It's a disconnect
	            	//TODO: Handle disconnect
	            	Log.e(TAG, "Possible disconnect");
	                //activity.resetData();
	            }

	        	
	        	// Here if we want we can print various network information
	        	Log.d(TAG,"P2P connection changed");
	        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
	            Log.d(TAG,"P2P this device changed");
	        	// Respond to this device's wifi state changing
	        	WifiService.this.device = (WifiP2pDevice) intent.getParcelableExtra(
	                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
	        }
	    }

	}
	
	private synchronized void startAcceptThread(int port) {
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
	
	protected synchronized void retry() {
    	if(D) Log.d(TAG, "retry");
    	
    	if(D) Log.d(TAG, "Retrying in state: " + getState());
    	
    	if(mState == STATE_CONNECTED) return;
    	
    	signalFailed();
		start();
		return;
		// TODO: How do we rety a WiFi connection
    	/* if(mNumTries >= MAX_RETRY){
    	
    		signalFailed();
    		start();
    		return;
    	}
    	
    	startAcceptThread();
    	
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
    	*/
    }
	
	/**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The ip address to connect to
     * @param port the port to connect to 
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void startConnectThread(String address, int port, boolean secure) {
    	//TODO: Why is the connect thread called twice!!
        if (D) Log.d(TAG, "connect to: " + device);

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
    
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(Socket socket, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

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
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, socket.getInetAddress().toString());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
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
                            connected(socket,
                                    mSocketType);
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
                    	Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);                        Log.e(TAG, e.getStackTrace().toString());
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
        private boolean mForwardRead = true;
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
	
}
