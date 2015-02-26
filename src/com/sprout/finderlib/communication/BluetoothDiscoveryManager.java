package com.sprout.finderlib.communication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sprout.finderlib.communication.CommunicationService.Callback;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;

public class BluetoothDiscoveryManager extends BroadcastReceiver {
  
  private static final String TAG = BluetoothDiscoveryManager.class.getSimpleName();
  private static final boolean V = true;
  
  private static final long UUID_FETCH_TIMEOUT = 10*1000;// 10s may not be enough;
  
  //The BroadcastReceiver that listens for discovered devices and
  // changes the title when discovery is finished
  IntentFilter mIntentFilter;
  private List<BluetoothDevice> discoveredDevices = new ArrayList<BluetoothDevice>();
  private Set<String> returnedUUIDs = new HashSet<String>();
  //TODO: Think about ways to remove the need for returnedUUIDs 
  // (this probably entails only returning device once regardless of security setting)
  
  private BluetoothDeviceCache uuidCache = new BluetoothDeviceCache();
  Callback callback;
  
  BluetoothService service;

  BluetoothDiscoveryManager(BluetoothService service) {
    // Register for broadcasts when a device is discovered or finished
    mIntentFilter = new IntentFilter();
    mIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
    mIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
    mIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    mIntentFilter.addAction(BluetoothDevice.ACTION_UUID);
    mIntentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
    
    this.service = service;
  }
  
  public void clearCache() {
    uuidCache.clear();
  }
  
  
  private Handler handler = new Handler();
  private void setTimeout(long delayMillis) {
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        if (V) {
          Log.w(TAG,
              "Timeout expired with " +
                  discoveredDevices.size() + " still without UUID");
        }
        
        if (callback != null)
          callback.onDiscoveryComplete(true);
      }
    }, delayMillis);
  }
  
  private void cancelTimeout() {
    handler.removeCallbacksAndMessages(null);
  }
  
  private void markDevice(BluetoothDevice device) {
    if(V) Log.d(TAG, "Device: " + device + " removed");
    discoveredDevices.remove(device);

    if (discoveredDevices.size() == 0) {
      if(callback != null)
        callback.onDiscoveryComplete(true);
      
      cancelTimeout();
    }
  }

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

    } else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
      returnedUUIDs.clear();
      discoveredDevices.clear();

      if(callback != null)
        callback.onDiscoveryStarted();
    }
    else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
      // We may need to wait until discovery is completed before we can query for the UUID

      // TODO: We may want some callback here eventually
      //   For now we use only the callback once all devices have been added
      //if(callback != null)
      //  callback.onDiscoveryComplete(true);

      for (BluetoothDevice device : new ArrayList<BluetoothDevice>(discoveredDevices)) {

        if (uuidCache.contains(device)) {
          
          if (V) Log.d(TAG, "Device " + device + " found in cache: " + uuidCache.appPresent(device));
          
          if (uuidCache.appPresent(device)) {
            if(callback != null)
              callback.onServiceDiscovered(new Device(device));
          }
          
          markDevice(device);
        } else { // We don't have the UUIDs for this device in the cache
          Log.i(TAG, "Getting Services for " + device.getName() + ", " + device);
          if(!device.fetchUuidsWithSdp()) {
            Log.e(TAG,"SDP Failed for " + device.getName());
          }
        }
      }
      
      if (discoveredDevices.size() != 0) {
        setTimeout(UUID_FETCH_TIMEOUT);
      }
    } else if(BluetoothDevice.ACTION_UUID.equals(action)) {
      BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
      Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

      if (uuidExtra == null) {     
        uuidExtra = device.getUuids();

        if (uuidExtra == null) {
          Log.e(TAG, "UUID could not be retrieved for device: " + device);

          // It is possible this result happens in error. So mark it as a fuzzy result.
          uuidCache.add(device, false, true);
                   
          markDevice(device);
          return;
        }
      } 

      boolean present = false; // Is our app present on the device?
      for (int i=0; i<uuidExtra.length; i++) {
        String uuid = uuidExtra[i].toString();

        if(V) Log.d(TAG, "Device : " + device.toString() + " Serivce: " + uuid);

        // If we haven't already returned this UUID and it matches our UUID
        if (( (service.mSecure && uuid.equals(BluetoothService.MY_UUID_SECURE.toString())) ||
                (!service.mSecure && uuid.equals(BluetoothService.MY_UUID_INSECURE.toString())) ) ) {

          if (!returnedUUIDs.contains(device.toString()+uuid)) {
            if(V) Log.i(TAG, "Device: " + device.getName() + ", " + device + ", Service: " + uuid);
  
            if(callback != null)
              callback.onServiceDiscovered(new Device(device));
  
            returnedUUIDs.add(device.toString()+uuid);
          }
          
          present = true;
        }
      }
      
      // Remember the result of this fetch
      uuidCache.add(device, present);

      markDevice(device);
    }
    // TODO: This branch isn't related to discovery so we should probably put into a different
    //    receiver. For now we just leave here as it does very little
    else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
      // TODO: We may need to listen for these changes, even when we are "stopped"
      //   To do that we probably need to use a different intent filter while stopped
      switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
        case BluetoothAdapter.STATE_OFF:
          if (service.getState() != BluetoothService.STATE_STOPPED) {
            // TODO: start the connection
          }

          service.mHandler.sendMessage(service.mHandler.obtainMessage(BluetoothService.MESSAGE_DISABLED));
          break;
        case BluetoothAdapter.STATE_ON:
          if ( service.getState() != BluetoothService.STATE_STOPPED) {
            // TODO: resume the connection
          }

          service.mHandler.sendMessage(service.mHandler.obtainMessage(BluetoothService.MESSAGE_ENABLED));
          break;
        case -1:
          Log.e(TAG, "No state provided");
          break;
        default:
          // Just ignore the action
          break;
      } 
    }
  } 
}
