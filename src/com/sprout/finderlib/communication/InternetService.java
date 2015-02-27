package com.sprout.finderlib.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class InternetService extends AbstractCommunicationService {
  
  int port;
  
  SocketChannel connected;
  DataInputStream mmInStream;
  DataOutputStream mmOutStream;
  
  public InternetService(int port) {
    super();
    
    this.port = port;
  }

  @Override
  public void start(boolean secure) {
    ServerSocketChannel ssc;
    try {
      ssc = ServerSocketChannel.open();
      ssc.socket().bind(new InetSocketAddress(port) );
      
      connected = ssc.accept();
      
      setupStreams();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  @Override
  public void connect(String address, boolean secure) {
    try {
      connected = SocketChannel.open();
      connected.connect(new InetSocketAddress(address, port));
     
      setupStreams();
    
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private void setupStreams() {
    try {
      mmInStream = new DataInputStream( connected.socket().getInputStream());
      mmOutStream = new DataOutputStream( connected.socket().getOutputStream());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void stop() {
    try {
      connected.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void write(byte[] out) {
    try {
      mmOutStream.writeInt(out.length);
      mmOutStream.write(out);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

 
  @Override
  public byte[] read() {
    try {
      int bytes = mmInStream.readInt();
      // This could be done faster using a byte buffer
      byte[] buffer = new byte[bytes];
      
      mmInStream.readFully(buffer, 0, bytes);
      
      return buffer;   
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    return null;
  }
  
  // Unsupported methods
  @Override
  protected void retry() {
    
    // Random backoff and then attempt connect again.
    // For now we just singalFailed
    
    signalFailed();
  }
  
  @Override
  public Device getSelf() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public void setReadLoop(boolean flag) {
    // No readloop
  }

  @Override
  public boolean getReadLoop() {
    return false;
  }

  @Override
  public Set<Device> bondedPeers() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void discoverPeers(Callback callback) {
    // TODO Auto-generated method stub

  }

  @Override
  public void stopDiscovery() {
    // TODO Auto-generated method stub

  }
  
  @Override
  public void pause() {
    // TODO Auto-generated method stub

  }

  @Override
  public void resume() {
    // TODO Auto-generated method stub

  }
  
  @Override
  public boolean isEnabled() {
    return true;
  }
}
