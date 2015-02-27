package com.sprout.finderlib.communication;

import java.util.Set;

public class InternetService extends AbstractCommunicationService {

  @Override
  public boolean isEnabled() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void start(boolean secure) {
    // TODO Auto-generated method stub

  }

  @Override
  public void connect(String address, boolean secure) {
    // TODO Auto-generated method stub

  }

  @Override
  public void stop() {
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
  public boolean getReadLoop() {
    // TODO Auto-generated method stub
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
  public Device getSelf() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected void retry() {
    // TODO Auto-generated method stub

  }

}
