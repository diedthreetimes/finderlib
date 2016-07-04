package com.sprout.finderlib.utils.android;

/**
 * android Log wrapper
 * TODO: implements the rest of functions
 * @author norrathep
 *
 */
public class Log {

  public static void i(String tag, String string) {
	android.util.Log.i(tag, string);
  }
  
  public static void d(String tag, String string) {
	  android.util.Log.d(tag, string);
  }
  
  public static void e(String tag, String string) {
	  android.util.Log.e(tag, string);
    print("E", tag, string);
  }
  
  private static void print(String level, String tag, String string) {
	  android.util.Log.println(1, tag, string); // TODO: what's this?
  }

}
