package com.sprout.finderlib.communication;

import java.util.HashMap;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

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
// However, a timeout may increase the robustness, in the case where we cache bad data. Which happens a lot
// TODO: Implement a rolling timeout of a couple of minutes. Essentially pick a small random amount of time, and for each device add this time to their timeout clock.
//  This will allow only a few devices to expire at any given round of discovery. We can then spend a few extra cycles to reinspect these devices, in case
//  their services have changed.
public class BluetoothDeviceCache {
  
  private static final String TAG = BluetoothDeviceCache.class.getSimpleName();
  private static boolean D = true;
  
  private HashMap<BluetoothDevice, Entry> cache;
  private long timeout; // How long do entries stay valid, in ms
  
  private static long FUZZY_TIMEOUT = 120*1000; // Two minutes
  
  public BluetoothDeviceCache() {
    cache = new HashMap<BluetoothDevice, Entry>();
    this.timeout = -1;
  }
  
  public BluetoothDeviceCache(long timeout) {
    if (D) Log.d(TAG, "DeviceCache initialized with " + timeout + " second timeout.");
    cache = new HashMap<BluetoothDevice, Entry>();
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
      // Past timeout
      if (timeout > 0 && cache.get(device).time < (now() - timeout)) {
        if (D) Log.d(TAG, "Device: " + device + " has expired");
        cache.remove(device);
        return false;
      }
      // Past fuzzy timeout
      else if (cache.get(device).fuzzy && cache.get(device).time < (now() - FUZZY_TIMEOUT)) {
        if (D) Log.d(TAG, "Fuzzy device: " + device + " has expired");
        cache.remove(device);
        return false;
      }
      
      return true;
    }
  }
  
  /**
   * @see add(BluetoothDevice device, boolean present, boolean fuzzy)
   * @param device
   * @param present
   */
  public void add(BluetoothDevice device, boolean present) {
    add(device, present, false);
  }
  
  /**
   * Add a device to the cache.
   * 
   * @param device
   * @param present Is our app present on the device
   * @param fuzzy If true present is considered to be a non-confident result and should be retried
   */
  public void add(BluetoothDevice device, boolean present, boolean fuzzy) {
    if (D) Log.d(TAG, "Device: " + device + " added to the cache: " + present + " fuzzy: " + fuzzy);
    
    if (D && contains(device)) {
      Log.w(TAG, "Device already present in the cache with " + appPresent(device));
    }
    cache.put(device, new Entry(now(), present, fuzzy));
  }
  
  /**
   * 
   * @param device
   * @return true if device is in our cache and has a matching UUID
   */
  public boolean appPresent(BluetoothDevice device) {
    return cache.get(device).present;
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
  
  public class Entry {
    long time;
    boolean present;
    boolean fuzzy;
    
    Entry(long time, boolean present, boolean fuzzy) {
      this.time = time;
      this.present = present;
      this.fuzzy = fuzzy;
    }
  }
}
