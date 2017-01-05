package com.atc.twocircles;

import java.io.IOException;
import java.io.File;

import processing.core.*;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

public class TwoCircles extends PApplet {
	
	float rightRotate;
	float leftRotate;
	float centroidScale;
	
	float circleCentreX01;
	float circleCentreY01;
	float circleCentreX02;
	float circleCentreY02;
	float radius01;
	float radius02;

	float counterTest;
	float counterInc;

	float angle01;
	float angle02;
	
	private static final String TAG = "TwoCircles";
	private PdUiDispatcher dispatcher;
	
	private PdService pdService = null;
	
	public int sketchWidth() { return this.displayWidth; }
	public int sketchHeight() { return this.displayHeight; }
	
	public String sketchRenderer() {
		return PApplet.OPENGL;
	}

	// Run PD as background service
	private final ServiceConnection pdConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pdService = ((PdService.PdBinder)service).getService();
			try {
				initPd();
				loadPatch();
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				finish();
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// this method will never be called
		}
			
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Bind service
		bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
		
		// Don't let display sleep
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void initPd() throws IOException {
		int sampleRate = AudioParameters.suggestSampleRate();
		pdService.initAudio(sampleRate,  0,  2,  10.0f);
		pdService.startAudio();
		
		dispatcher = new PdUiDispatcher();
		PdBase.setReceiver(dispatcher);
		
		// rotation value 01 from pd
		dispatcher.addListener("rightRotate", new PdListener.Adapter() {
			@Override
			public void receiveFloat(String source, float x) {
				 rightRotate = x;
			}
		});
		
		// rotation value 02 from pd
		dispatcher.addListener("leftRotate", new PdListener.Adapter() {
			@Override
			public void receiveFloat(String source, float x) {
				 leftRotate = x;
			}
		});

		// used to modulate background colour
		dispatcher.addListener("centroidScale", new PdListener.Adapter() {
			@Override
			public void receiveFloat(String source, float x) {
				 centroidScale = x;
			}
		});

	
	}

	
	private void loadPatch() throws IOException {
		File dir = getFilesDir();
		IoUtils.extractZipResource(getResources().openRawResource(R.raw.twocirclespd), dir, true);
		File patchFile = new File(dir, "twocircles.pd");
		PdBase.openPatch(patchFile.getAbsolutePath());
	}
		
	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(pdConnection);
	}

	
    public void setup() {
    	smooth(16);
    	
    	// Set orientation
    	orientation(PORTRAIT);
    	
    	// these are 0 as using rotate and translate
    	circleCentreX01 = 0;
    	circleCentreY01 = 0;
    	circleCentreX02 = 0;
    	circleCentreY02 = 0;

    	// Set circle sizes
    	radius01 = 500;  
    	radius02 = 500;

    	// angle / spacing of lines making up circles
    	angle01 = 3;
    	angle02 = 7;
    }

    public void draw() {
    	
    	// Map PD input
    	float backgroundCol0101 = map(centroidScale,0,1,200,255);
    	float backgroundCol0102 = map(centroidScale,0,1,50,150);
    	
    	float backgroundCol0201 = map(centroidScale,0,1,200,255);
    	float backgroundCol0202 = map(centroidScale,0,1,50,150);

    	// Set background colours
    	noStroke();
    	beginShape();
    	fill(255,backgroundCol0102,backgroundCol0101);
    	vertex(0,0);
    	vertex(displayWidth,0);
    	fill(backgroundCol0201,255,backgroundCol0202);
    	vertex(displayWidth,displayHeight);
    	vertex(0,displayHeight);
    	endShape(CLOSE);
    	
    	// First circle
    	strokeWeight(4);
    	stroke(0);
    	// Set position
    	translate(displayWidth/2-20, displayHeight/2+20); 
    	// Rotate shape
    	pushMatrix();
    	rotate(radians(leftRotate));
    	// Draw shape
    	lineDraw(angle01, radius01, circleCentreX01, circleCentreY01);
    	popMatrix();
    	
    	// Second circle
    	// Offset amount
    	translate(50, -50);
    	// rotate
    	pushMatrix();
    	rotate(radians(rightRotate));
    	
    	stroke(255);
    	// Draw shape
    	lineDraw(angle02, radius02, circleCentreX02, circleCentreY02);
    	popMatrix();
    }
    
    // Function to draw circles
    void lineDraw(float angle, float radius, float centreX, float centreY) {
    	  for (float i = -180; i < 180; i = i+angle) {
    	    line((centreX + cos(radians(i))*(radius)), 
    	    (centreY + sin(radians(i))*(radius)), 
    	    (centreX + cos(radians(-i))*(radius)), 
    	    (centreY + sin(radians(-i))*(radius)));
    	  }
    	}
}