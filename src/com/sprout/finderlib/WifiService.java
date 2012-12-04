package com.sprout.finderlib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Handler;
import android.util.Log;

public class WifiService extends AbstractCommunicationService {
	
	static final String TAG = "WifiService";
	static final boolean D = true;
	
	private WifiP2pManager mManager;
	private Channel mChannel;
	private BroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
	
	private WifiP2pDevice device;
	
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
	}
	
	//TODO: Work out using states and threads!!
	@Override
	public synchronized void start() {
		resume();
	}

	@Override
	public synchronized void connect(String address, boolean secure) {
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = address;
		mManager.connect(mChannel, config, new ActionListener() {

		    @Override
		    public void onSuccess() {
		        //TODO: success logic
		    }

		    @Override
		    public void onFailure(int reason) {
		        //TODO: failure logic
		    }
		});

	}

	@Override
	public synchronized void stop() {
		pause();
	}
	
	@Override
	public void pause() {
		mContext.unregisterReceiver(mReceiver);
	}

	@Override
	public void resume() {
		mContext.registerReceiver(mReceiver, mIntentFilter);
	}
	
	public void discoverPeers(final Callback callback){
		
		((WiFiDirectBroadcastReceiver) mReceiver).setCallback(callback);
		
		//TODO: Call fragments onInitiateDiscovery to ensure appropriate dialogs
		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				callback.onDiscoveryComplete(true);
			}

			@Override
			public void onFailure(int reasonCode) {
				callback.onDiscoveryComplete(false);
			}
		});
	}

	@Override
	public void write(byte[] out) {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] read() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setReadLoop(boolean flag) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public Set<Device> bondedPeers() {
		// WiFi does not have a concept of bonded peers
		//   For secure connections this should change. 
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
	            // Respond to new connection or disconnections
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

}
