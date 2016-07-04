package com.sprout.finderlib.communication.android;


import com.sprout.finderlib.communication.AbstractCommunicationService;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * abstract communication service for android platform
 * make use of android handler
 * @author norrathep
 *
 */
public abstract class AbstractAndroidCommunicationService extends AbstractCommunicationService 
implements AndroidCommunicationService  {

  protected Handler mHandler;
  protected Context mContext;

  private static final String TAG = "AbstractAndroidCommunicationService";


  @SuppressWarnings("unused")
  private AbstractAndroidCommunicationService() {} // void arg is not allowed here
  
  /**
   * Constructor. Prepares a new Service object.
   * @param context  The UI Activity Context
   * @param handler  A Handler to send messages back to the UI Activity
   */
  public AbstractAndroidCommunicationService(Context context, Handler handler) {
    mContext = context;
    mState = STATE_NONE;
    mHandler = handler;
  }


  @Override
  // TODO: Work out communicating messages to multiple handlers
  // This allows an activity to start a connection, and then hand it off
  //   to another activity or service
  public synchronized void setHandler(Handler handler) {
    mHandler = handler;
  }

  /**
   * Set the current state of the connection
   * @param state  An integer defining the current connection state
   */
  protected synchronized void setState(int state) {
	  super.setState(state);

    // Give the new state to the Handler so the UI Activity can update
    mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
  }

  /**
   * Indicate that the connection attempt failed and notify the UI Activity.
   */
  protected void connectionFailed() {
    // Send a failure message back to the Activity
    Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
    Bundle bundle = new Bundle();
    bundle.putString(TOAST, "Unable to connect device");
    msg.setData(bundle);
    mHandler.sendMessage(msg);

    super.connectionFailed();
  }

  /**
   * Indicate that the connection was lost and notify the UI Activity.
   */
  protected void connectionLost() {
    // Send a failure message back to the Activity
    Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
    Bundle bundle = new Bundle();
    bundle.putString(TOAST, "Device connection was lost");
    msg.setData(bundle);
    mHandler.sendMessage(msg);

    super.connectionLost();

  }

  protected void signalFailed(){
    Message msg = mHandler.obtainMessage(MESSAGE_FAILED);
    mHandler.sendMessage(msg);
  }

}
