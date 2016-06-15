package com.valleytg.robotics.bluetooth_henry;

import java.util.ArrayList;

import com.valleytg.robotics.bluetooth_henry.app.Henry;
import com.valleytg.robotics.bluetooth_henry.collection.DoublePoint;
import com.valleytg.robotics.bluetooth_henry.collection.Reading;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.view.View;

public class MovingWorldMapView extends View {

	Henry app;
	
	Paint paint = new Paint();
	ArrayList<Reading> staleReadings = new ArrayList<Reading>();
	
	boolean isScreenDirtry = false;
	
	Handler updateHandler2;
	
	/**
	 * Tracks the actual x,y location of the robot not associated with 
	 * the screen location in pixels
	 */
	DoublePoint robotLocation = new DoublePoint();
	
	/**
	 * Will hold the difference between the robots location in Cartesian space
	 * and the readings laction in Carte
	 */
	DoublePoint difference = new DoublePoint();

	/**
	 * Holds the current readings pixel location
	 */
	DoublePoint readingPixel = new DoublePoint();
	
	
	//DoublePoint screenShift = new DoublePoint();
	
	public MovingWorldMapView(Context context) {
	    super(context);
	    setFocusable(true);
	    setFocusableInTouchMode(true);
	    
	    // get the application
	    app = (Henry) ((Activity) context).getApplication();
	    
	    paint.setColor(Color.BLUE);
	    paint.setAntiAlias(true);
	    
	    updateHandler2 = new Handler();
	    // Do this first after 1 second
	    updateHandler2.postDelayed(RecurringTask, 1000);
	}
	
	
	
	@Override
	public void onDraw(Canvas canvas) {
		
		// reset the dirty flag
		this.isScreenDirtry = false;
		
		// get and set the robots coordinates
		robotLocation.x = app.getCurrentX();
		robotLocation.y = app.getCurrentY();
		
		// the robots screen location is always the center of the screen
		paint.setColor(Color.RED);
		canvas.drawCircle(app.getWorld().getmCenterX().floatValue(), app.getWorld().getmCenterY().floatValue(), 5, paint);

		// create a line 35px long signifying the direction of the robot
		DoublePoint robotDirection = app.findRemotePoint(app.getWorld().getmCenterX(), app.getWorld().getmCenterY(), app.getCurrentRobotHeadingSlope(), 35.0);
		DoublePoint turretDirection = app.findRemoteTurretPoint(app.getWorld().getmCenterX(), app.getWorld().getmCenterY(), app.getTurretCurrentSlope(), 30.0);
		
		// draw the line representing the direction of the robot
		canvas.drawLine((float) (robotDirection.x), (float) (robotDirection.y), app.getWorld().getmCenterX().floatValue(), app.getWorld().getmCenterY().floatValue(), paint);
		
		// change the paint color to green for the turret
		paint.setColor(Color.GREEN);
		
		// draw the line representing the direction of the turret
		canvas.drawLine((float) (turretDirection.x), (float) (turretDirection.y), app.getWorld().getmCenterX().floatValue(), app.getWorld().getmCenterY().floatValue(), paint);
		
		// Copy the readings to avoid concurrent modification problems
		staleReadings.clear();
		staleReadings.addAll(this.app.getWorld().getReadings());
		
		//add the current readings to the screen
		for(Reading reading : staleReadings) {
		
			// add a point for the reading
			// put the robots location on the screen
			paint.setColor(Color.BLACK);

			/**
			 * Get the difference between the robots actual Cartesian coordinate 
			 * location and the reading Cartesian coordinate location.  This difference
			 * will be the pixel difference from the center of the screen to the 
			 * displayed location of the reading
			 */
			difference.x = robotLocation.x - reading.getPosition().x;
			difference.y = robotLocation.y - reading.getPosition().y;
			
			// use the difference to detirmine and set the pixel location of this reading
			readingPixel.x = app.getWorld().getmCenterX() - difference.x;
			readingPixel.y = app.getWorld().getmCenterY() - difference.y;
			
			// check to see if the pixel is in the range we would want to draw on the screen
			if(readingPixel.x > 0 && readingPixel.x < 401 && readingPixel.y > 0 && readingPixel.y < 801) {
				if(reading.getHitCount() > 1){
					paint.setColor(Color.BLUE);
				}
				else if(reading.getHitCount() > 2){
					paint.setColor(Color.GREEN);
				}
				canvas.drawCircle((float) readingPixel.x, (float) readingPixel.y, 2, paint);
			}
		}

	}
	
	
	
	
	Runnable RecurringTask = new Runnable() {
		  public void run() {
		    // Do whatever you want
			MovingWorldMapView.this.invalidate();

		    // Call this method again 
		    updateHandler2.postDelayed(this, 500);
		  }
	};
}
