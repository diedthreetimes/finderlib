package com.sprout.finderlib.communication;

import java.util.HashMap;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.util.Pair;

/**
 * This class helps alleviate some unnecessary UUID inspection by caching the results of
 * earlier executions.
 * 
 * A side affect of this is that devices with newly installed applications may not be identified.
 * Further, devices who have uninstalled our app may still be returned. 
 * 
 * Ultimately we would like this cache to be persistent, and time dependent to reduce
 * the impact of these issues. However, for now it is neither as for our use neither are
 * immediate problems.
 *
 */
// We have the machinery to be time dependent, but it is unclear what the correct timeout is or if it is needed at all.
// However, a timeout may increase the robustness, in the case where we cache bad data.
public class BluetoothDeviceCache {
  
  private static final String TAG = BluetoothDeviceCache.class.getSimpleName();
  private static boolean D = true;
  
  private HashMap<BluetoothDevice, Pair<Long, Boolean>> cache;
  private long timeout; // How long do entries stay valid, in ms
  
  public BluetoothDeviceCache() {
    cache = new HashMap<BluetoothDevice, Pair<Long, Boolean>>();
    this.timeout = -1;
  }
  
  public BluetoothDeviceCache(long timeout) {
    if (D) Log.d(TAG, "DeviceCache initialized with " + timeout + " second timeout.");
    cache = new HashMap<BluetoothDevice, Pair<Long, Boolean>>();
    this.timeout = timeout * 1000;
  }
  
  /**
   * 
   * @param device
   * @return true if device is in the cache
   */
  public boolean contains(BluetoothDevice device) {
    if (!cache.containsKey(device))
      return false;
    else {
      if (timeout > 0 && cache.get(device).first < (now() - timeout)) {
        if (D) Log.d(TAG, "Device: " + device + " has expired");
        cache.remove(device);
        return false;
      }
      
      return true;
    }
  }
  
  /**
   * Add a device to the cache.
   * 
   * @param device
   * @param present Is our app present on the device
   */
  public void add(BluetoothDevice device, boolean present) {
    if (D) Log.d(TAG, "Device: " + device + " added to the cache: " + present);
    cache.put(device, new Pair<Long, Boolean>(now(), present));
  }
  
  /**
   * 
   * @param device
   * @return true if device is in our cache and has a matching UUID
   */
  public boolean appPresent(BluetoothDevice device) {
    return cache.get(device).second;
  }
  
  /**
   * Clear the cache
   */
  public void clear() {
    if (D) Log.d(TAG, "Clear...");
    cache.clear();
  }

  private long now() {
    return System.currentTimeMillis();
  }
}
