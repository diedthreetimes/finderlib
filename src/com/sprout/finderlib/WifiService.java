package com.sprout.finderlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Handler;

public class WifiService extends AbstractCommunicationService {
	
	private WifiP2pManager mManager;
	private Channel mChannel;
	private BroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
	
	public WifiService(Context context, Handler handler){
		super(context, handler);
		mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
	    mChannel = mManager.initialize(context, context.getMainLooper(), null);
	    mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, handler);
	    
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
		// TODO Auto-generated method stub

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
	
	//TODO: Find a way to notify app about discovery process
	public void discoverPeers(){
		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
	       
			}

			@Override
			public void onFailure(int reasonCode) {
	        
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
	
	/**
	 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
	 */
	public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

	    private WifiP2pManager manager;
	    private Channel channel;
	    private Handler handler;

	    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, Handler handler) {
	        super();
	        this.manager = manager;
	        this.channel = channel;
	        this.handler = handler;
	    }

	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();

	        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
	        	int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
	            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
	                // Wifi Direct is enabled
	            } else {
	                // Wi-Fi Direct is not enabled
	            }
	        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
	            // Call WifiP2pManager.requestPeers() to get a list of current peers
	        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
	            // Respond to new connection or disconnections
	        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
	            // Respond to this device's wifi state changing
	        }
	    }

	}

}
