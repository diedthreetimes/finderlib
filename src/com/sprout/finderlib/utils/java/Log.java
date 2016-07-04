package com.sprout.finderlib.utils.java;

public class Log {

  public static void i(String tag, String string) {
    print("I", tag, string);
  }
  
  public static void d(String tag, String string) {
    print("D", tag, string);
  }
  
  public static void e(String tag, String string) {
    print("E", tag, string);
  }
  
  private static void print(String level, String tag, String string) {
    System.out.println("[" + level + "] TAG: " + string);
  }

}
