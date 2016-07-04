package com.sprout.finderlib.communication.android;

import com.sprout.finderlib.communication.CommunicationService;

import android.os.Handler;

/**
 * communication service for android platform
 * @author norrathep
 *
 */
public interface AndroidCommunicationService extends CommunicationService {

  public void setHandler(Handler handler);

}
