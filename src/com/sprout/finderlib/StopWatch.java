package com.sprout.finderlib;

/*
Copyright (c) 2005, Corey Goldberg

StopWatch.java is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.
*/


public class StopWatch {

private long startTime = 0;
private long stopTime = 0;
private long ellapsedTime = 0;
private boolean running = false;


public void start() {
    this.startTime = System.currentTimeMillis();
    this.running = true;
}

public void pause() {
	this.running = false;
	this.ellapsedTime += (System.currentTimeMillis() - startTime);
	this.startTime = 0;
	this.stopTime = 0;
}


public void stop() {
	if(!running){
		return;
	}
	
    this.stopTime = System.currentTimeMillis();
    this.running = false;
}

public void clear() {
	startTime = 0;
	stopTime = 0;
	ellapsedTime = 0;
}


//elaspsed time in milliseconds
public long getElapsedTime() {
    long elapsed;
    if (running) {
         elapsed = (System.currentTimeMillis() - startTime) + ellapsedTime;
    }
    else {
        elapsed = (stopTime - startTime) + ellapsedTime;
    }
    return elapsed;
}


//elaspsed time in seconds
public long getElapsedTimeSecs() {
    return getElapsedTime() / 1000;
}

}