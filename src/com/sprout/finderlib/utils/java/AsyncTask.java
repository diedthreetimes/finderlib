package com.sprout.finderlib.utils.java;

public abstract class AsyncTask<Params, Progress, Result> {

  protected abstract Result doInBackground(Params... params);
  
  // TODO: Make this work like androids aysnctask but just with threads
  public Result execute(Params... params) {
    return doInBackground(params);
  }

}
