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

public class WorldMapView extends View {

	Henry app;
	
	Paint paint = new Paint();
	ArrayList<Reading> staleReadings = new ArrayList<Reading>();
	
	boolean isScreenDirtry = false;
	
	Handler updateHandler2;
	
	public WorldMapView(Context context) {
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
		
		// get the robots coordinates
		DoublePoint robotCoords = new DoublePoint();
		robotCoords.x = app.getCurrentX();
		robotCoords.y = app.getCurrentY();
		
		// get the robots location
		DoublePoint robotLocation = this.convertCoordinateToPixel(robotCoords);
		
		// put the robots location on the screen
		paint.setColor(Color.RED);
		canvas.drawCircle((float) (robotLocation.x / app.getWorld().getMapZoomFactor()), (float) (robotLocation.y / app.getWorld().getMapZoomFactor()), 5, paint);
		
		//System.out.println("Robot coords: " + (float) robotCoords.x + ", " + (float) robotCoords.y + ", " + 5);
		//System.out.println("Robot location: " + (float) robotLocation.x + ", " + (float) robotLocation.y + ", " + 5);
		//System.out.println("**Robot heading: " + this.app.getCurrentHeading() + " **Current turret heading: " + this.app.getTurretCurrentHeading() + " **Current robot heading slope: " + app.getCurrentRobotHeadingSlope());
		
		// create a line 35px long signifying the direction of the robot
		DoublePoint robotDirection = app.findRemotePoint(robotLocation.x, robotLocation.y, app.getCurrentRobotHeadingSlope(), 25.0);
		DoublePoint turretDirection = app.findRemotePoint(robotLocation.x, robotLocation.y, app.getTurretCurrentSlope(), 25.0);
		
		canvas.drawLine((float) (robotDirection.x / app.getWorld().getMapZoomFactor()), (float) (robotDirection.y / app.getWorld().getMapZoomFactor()), 
				(float) (robotLocation.x / app.getWorld().getMapZoomFactor()), (float) (robotLocation.y / app.getWorld().getMapZoomFactor()), paint);
		
		paint.setColor(Color.GREEN);
		
		canvas.drawLine((float) (turretDirection.x / app.getWorld().getMapZoomFactor()), (float) (turretDirection.y / app.getWorld().getMapZoomFactor()), 
				(float) (robotLocation.x / app.getWorld().getMapZoomFactor()), (float) (robotLocation.y / app.getWorld().getMapZoomFactor()), paint);
		
		// Copy the readings to avoid concurrent modification problems
		staleReadings.clear();
		staleReadings.addAll(this.app.getWorld().getReadings());
		
		//add the current readings to the screen
		for(Reading reading : staleReadings) {
		
			// add a point for the reading
			// put the robots location on the screen
			paint.setColor(Color.BLACK);
			
			// get the pixel location for this reading
			DoublePoint thisPixel = convertCoordinateToPixel(reading.getPosition());
			canvas.drawCircle((float) (thisPixel.x / app.getWorld().getMapZoomFactor()), (float) (thisPixel.y / app.getWorld().getMapZoomFactor()), 2, paint);
		}
		
		/*
		Point point1 = new Point();
		point1.x = 30;
	    point1.y = 20;
	    points.add(point1);
	    
	    Point point2 = new Point();
	    point2.x = 35;
	    point2.y = 25;
	    points.add(point2);
	    
	    Point point3 = new Point();
	    point3.x = 40;
	    point3.y = 40;
	    points.add(point3);
	    
	    for (Point point : points) {
		    Log.d(TAG, "point: " + point);
	        canvas.drawCircle(point.x, point.y, 5, paint);
	        // Log.d(TAG, "Painting: "+point);
	    }
	    
	    */
	}
	
	
	private DoublePoint convertCoordinateToPixel(DoublePoint coordinate) {
		DoublePoint returnPoint = new DoublePoint();
		
		// get the x value
		returnPoint.x = (app.getWorld().getMaxXSpace() * app.getWorld().getMapZoomFactor()) + coordinate.x;

		// get the y value 
		returnPoint.y = (app.getWorld().getMaxYSpace() * app.getWorld().getMapZoomFactor()) + coordinate.y;
		
		//System.out.println("Map zoom factor is: " + app.getWorld().getMapZoomFactor());
		//System.out.println("Calculated return point x : " + returnPoint.x);
		//System.out.println("Calculated return point y : " + returnPoint.y);
		
		// do check for boundaries 
		if(returnPoint.x  <= 0 || returnPoint.x > ((app.getWorld().getMaxXSpace() * 2) * app.getWorld().getMapZoomFactor()) || 
				returnPoint.y  <= 0 || returnPoint.y > ((app.getWorld().getMaxYSpace() * 2) * app.getWorld().getMapZoomFactor())) {
			// Boundary violation increase the MapZoomFactor and mark the screen as dirty
			app.getWorld().setMapZoomFactor((app.getWorld().getMapZoomFactor() + 1));
			
			// mark this screen as dirty... needs to be redrawn
			this.isScreenDirtry = true;
		}
		
		return returnPoint;
	}
	
	
	Runnable RecurringTask = new Runnable() {
		  public void run() {
		    // Do whatever you want
			WorldMapView.this.invalidate();

		    // Call this method again 
		    updateHandler2.postDelayed(this, 1000);
		  }
	};
}
