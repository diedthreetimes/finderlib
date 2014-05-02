package com.sprout.finderlib.ui.communication;

import android.app.Activity;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.Set;

import com.sprout.finderlib.R;
import com.sprout.finderlib.R.id;
import com.sprout.finderlib.R.layout;
import com.sprout.finderlib.R.string;
import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.Device;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

//TODO: FIX the UI for this activity


/**
 * Note: 1/23/12
 * This class handles picking a device from all available sources.
 * Currently only bluetooth is supported, but in the future wifi/3g may be
 * an option. Most of the source in its current state is from the android docs
 * 
 */

/**
 * This Activity appears as a dialog. When a device is chosen
 * by the user, the MAC address along with the protocol of the device is sent back
 * to the parent Activity in the result Intent.
 */
public class DeviceList extends Activity {

  // Debugging
  private static final String TAG = "DeviceListActivity";
  private static final boolean D = true;

  // Return Intent extra
  public static String EXTRA_DEVICE_ADDRESS = "device_address";
  //TODO: add extra for other forms of communication


  // Member fields
  private ArrayAdapter<Device> mPairedDevicesArrayAdapter;
  private ArrayAdapter<Device> mNewDevicesArrayAdapter;
  private CommunicationService comService;
  /**
   * @see android.app.Activity#onCreate(Bundle)
   */
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Grab the associated communication service
    Intent intent = getIntent();
    String time = intent.getStringExtra(CommunicationService.EXTRA_SERVICE_TRANFER);

    WeakReference<CommunicationService> tmp = CommunicationService.com_transfers.remove(time);

    if(tmp == null){
      //TODO: What do we do if it is Null? Raise?
      Log.e(TAG,"Communication Service not provided");
      finish();
    }
    this.comService = tmp.get();

    if (comService == null) {
      Log.e(TAG, "Communication Service no longer available");
      finish();
    }

    // Setup the window
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.device_list);

    // Set result CANCELED in case the user backs out
    setResult(Activity.RESULT_CANCELED);

    // Initialize the button to perform device discovery
    Button scanButton = (Button) findViewById(R.id.button_scan);
    scanButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        doDiscovery();
        v.setVisibility(View.GONE);
      }
    });

    // Initialize array adapters. One for already paired devices and
    // one for newly discovered devices
    mPairedDevicesArrayAdapter = new ArrayAdapter<Device>(this, R.layout.device_name);
    mNewDevicesArrayAdapter = new ArrayAdapter<Device>(this, R.layout.device_name);

    // Find and set up the ListView for paired devices
    ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
    pairedListView.setAdapter(mPairedDevicesArrayAdapter);
    pairedListView.setOnItemClickListener(mDeviceClickListener);

    // Find and set up the ListView for newly discovered devices
    ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
    newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
    newDevicesListView.setOnItemClickListener(mDeviceClickListener);

    // Load our devices name
    Device self = comService.getSelf();
    ((TextView)findViewById(R.id.display_name)).setText(self.getName());

    // Get a set of currently paired devices
    //Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
    Set<Device> pairedDevices = comService.bondedPeers();

    // If there are paired devices, add each one to the ArrayAdapter
    if (pairedDevices.size() > 0) {
      findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
      for (Device device : pairedDevices) {
        // TODO: We could have the communication service display custom information via a custom Item maybe
        mPairedDevicesArrayAdapter.add(device);
      }
    } else {
      //TODO: Clicking this causes a crash
      // Commented out for test
      //String noDevices = getResources().getText(R.string.none_paired).toString();
      //mPairedDevicesArrayAdapter.add(noDevices);
    }


    // Added for the test only
    doDiscovery();
    scanButton.setVisibility(View.GONE);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    //TODO: Need to find a way to cancel discovery which can be costly
  }

  /**
   * Start device discover with the BluetoothAdapter
   */
  private void doDiscovery() {
    if (D) Log.d(TAG, "doDiscovery()");

    // Indicate scanning in the title
    setProgressBarIndeterminateVisibility(true);
    setTitle(R.string.scanning);

    // Turn on sub-title for new devices
    findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

    comService.discoverPeers(mDiscoveryCallback);

  }

  // The Callbacks
  private CommunicationService.Callback mDiscoveryCallback = new CommunicationService.Callback(){

    @Override
    public void onPeerDiscovered(Device peer) {
      //TODO: check if peer has been added already
      mNewDevicesArrayAdapter.add(peer);
    }  	

    @Override
    public void onDiscoveryComplete(boolean success){
      setProgressBarIndeterminateVisibility(false);
      setTitle(R.string.select_device);

      if(!success)
        Log.e(TAG, "Discovery failed!");

      //if (mNewDevicesArrayAdapter.getCount() == 0) {
      //TODO: Clicking this string causes a crash
      // commented for test
      //String noDevices = getResources().getText(R.string.none_found).toString();
      //mNewDevicesArrayAdapter.add(noDevices);
    }

    @Override
    public void onServiceDiscovered(Device peer) {
      //TODO: now that we have more up to date information we should trim back the device list. 
      // Since these peers are the ones we are actually concerned with
    }
  };

  // The on-click listener for all devices in the ListViews
  private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
    public void onItemClick(AdapterView<?> av, View v, int position, long id) {
      // Cancel discovery because it's costly and we're about to connect
      //TODO: cancel discovery

      Device device = (Device) av.getItemAtPosition(position);

      // Create the result Intent and include the MAC address
      Intent intent = new Intent();
      intent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());

      // Set result and finish this Activity
      setResult(Activity.RESULT_OK, intent);
      finish();
    }
  };
}
