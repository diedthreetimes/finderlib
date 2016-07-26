package com.sprout.finderlib.crypto;

public class Entry<Key, Value> {
  
  private final Key K;
  private final Value V;
  
  public Entry(Key key, Value value) {
    K = key;
    V = value;
  }

  @Override
  public String toString() {
    return K.toString() + " : " + V.toString();
  }
  
  @Override
  public int hashCode() {
    return K.hashCode();
  }
  
  public Key getKey() {
    return K;
  }
  
  public Value getValue() {
    return V;
  }
}
