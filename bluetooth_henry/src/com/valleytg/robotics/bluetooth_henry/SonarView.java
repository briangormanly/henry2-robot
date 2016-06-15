package com.valleytg.robotics.bluetooth_henry;

import java.util.ArrayList;
import java.util.List;

import com.valleytg.robotics.bluetooth_henry.app.Henry;
import com.valleytg.robotics.bluetooth_henry.collection.Point;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.view.View;

public class SonarView extends View {
	private static final String TAG = "DrawView";

	List<Point> points = new ArrayList<Point>();
	Paint paint = new Paint();
	
	Integer reading0x = 480;
	Integer reading0y = 425;
	Integer reading1x = 480;
	Integer reading1y = 140;
	Integer reading2x = 360;
	Integer reading2y = 0;
	Integer reading3x = 120;
	Integer reading3y = 0;
	Integer reading4x = 0;
	Integer reading4y = 140;
	Integer reading5x = 0;
	Integer reading5y = 425;
	Integer reading6x = 0;
	Integer reading6y = 280;
	Integer reading7x = 0;
	Integer reading7y = 0;
	Integer reading8x = 240;
	Integer reading8y = 0;
	Integer reading9x = 480;
	Integer reading9y = 0;
	Integer reading10x = 480;
	Integer reading10y = 280;
	Integer readingoptx = 0;
	Integer readingopty = 0;
	
	// define the x ratios for the lines that need it
	Double ratio10 = .85714285;
	Double ratio1 = .64343163;
	Double ratio9 = .49180327;
	Double ratio2 = .54298642;
	Double ratio3 = .54298642;
	Double ratio7 = .49180327;
	Double ratio4 = .64343163;
	Double ratio6 = .85714285;
	
	Double slope0 = 0.0;
	Double slope1 = 0.0;
	Double slope2 = 0.0;
	Double slope3 = 0.0;
	Double slope4 = 0.0;
	Double slope5 = 0.0;
	Double slope6 = 0.0;
	Double slope7 = 0.0;
	Double slope8 = 0.0;
	Double slope9 = 0.0;
	Double slope10 = 0.0;
	Double slopeopt = 0.0;
	
	Integer originX = 240;
	Integer originY = 425;
	
	int yinter10 = 570;
    int yinter1 = 710;
    int yinter9 = 850;
    int yinter2 = 1275;

    int yinter3 = -425;
    int yinter7 = 0; // really 0!
    int yinter4 = 140;
    int yinter6 = 280;
	
	
	Handler updateHandler;
	
	Henry app;
	
	public SonarView(Context context) {
	    super(context);
	    setFocusable(true);
	    setFocusableInTouchMode(true);
	    
	    // get the application
	    app = (Henry) ((Activity) context).getApplication();

	
	    paint.setColor(Color.BLUE);
	    paint.setAntiAlias(true);
	    
	    updateHandler = new Handler();
	    // Do this first after 1 second
	    updateHandler.postDelayed(RecurringTask, 90);
	    
	    // determine the slopes for the us positions
	    slope0 = ((425.0  - (double) originY) / (480.0 - (double) originX));
	    System.out.println("0 slope: " + slope0);
	    
	    slope10 = ((280.0  - (double) originY) / (480.0 - (double) originX));
	    System.out.println("10 slope: " + slope10);
	    
	    slope1 = ((140.0  - (double) originY) / (480.0 - (double) originX));
	    System.out.println("1 slope: " + slope1);
	    
	    slope9 = ((0.0  - (double) originY) / (480.0 - (double) originX));
	    System.out.println("9 slope: " + slope9);
	    
	    slope2 = ((0.0  - (double) originY) / (360.0 - (double) originX));
	    System.out.println("2 slope: " + slope2);
	    
	    slope8 = ((0.0 - (double) originY) / (240.0 - (double) originX));
	    System.out.println("8 slope: " + slope8);
	    
	    slope3 =  ((0.0 - (double)originY) / (120.0 - (double)originX));
	    System.out.println("3 slope: " + slope3);
	    
	    slope7 = ((0.0 - (double) originY) / (0.0 - (double) originX));
	    System.out.println("7 slope: " + slope7);
	    
	    slope4 = ((140.0 - (double) originY) / (0.0 - (double) originX));
	    System.out.println("4 slope: " + slope4);
	    
	    slope6 = ((280.0 - (double) originY) / (0.0 - (double) originX));
	    System.out.println("6 slope: " + slope6);
	    
	    slope5 = ((425.0 - (double) originY) / (0.0 - (double) originX));
	    System.out.println("5 slope: " + slope5);

	}
	
	
	
	
	@Override
	public void onDraw(Canvas canvas) {
		
		// get the turret postion and work into the line display
		int lineX = 0;
		int lineY = 0;
		
		int locationX = 0;
		int locationY = 0;
		
		//System.out.println(app.getCurrentTurretIndex());
		
		if(app.getCurrentTurretIndex() == 0) {
			lineX = 480;
			lineY = 425;

		}
		else if(app.getCurrentTurretIndex() == 10) {
			lineX = 480;
			lineY = 280;
			
		}
		else if(app.getCurrentTurretIndex() == 1) {
			lineX = 480;
			lineY = 140;

		}
		else if(app.getCurrentTurretIndex() == 9) {
			lineX = 480;
			lineY = 0;

		}
		else if(app.getCurrentTurretIndex() == 2) {
			lineX = 360;
			lineY = 0;
		}
		else if(app.getCurrentTurretIndex() == 8) {
			lineX = 240;
			lineY = 0;
		}
		else if(app.getCurrentTurretIndex() == 3) {
			lineX = 120;
			lineY = 0;
		}
		else if(app.getCurrentTurretIndex() == 7) {
			lineX = 0;
			lineY = 0;
		}
		else if(app.getCurrentTurretIndex() == 4) {
			lineX = 0;
			lineY = 140;
		}
		else if(app.getCurrentTurretIndex() == 6) {
			lineX = 0;
			lineY = 280;
		}
		else if(app.getCurrentTurretIndex() == 5) {
			lineX = 0;
			lineY = 425;
		}
		
		canvas.drawRect(190, 415, 290, 435, paint);
		System.out.println(lineX + ", " + lineY + ", " + originX + ", " + originY);
		canvas.drawLine(lineX, lineY, originX, originY, paint);
		
		
		
		if(app.getCurrentDistances()[0].intValue() < 240) {
			reading0x = 240 + (app.getCurrentDistances()[0].intValue());
			reading0y = 425;
			
			// show the object
			canvas.drawCircle(reading0x, reading0y, 15, paint);
		}
		
		if(app.getCurrentDistances()[10].intValue() < 450) {
			reading10x = (int) (240 + (app.getCurrentDistances()[10].intValue() * ratio10));
			reading10y = (int) (slope10 * reading10x + yinter10);
			
			// show the object
			canvas.drawCircle(reading10x, reading10y, 15, paint);
		}
		
		if(app.getCurrentDistances()[1].intValue() < 450) {
			reading1x = (int) (240 + (app.getCurrentDistances()[1].intValue() * ratio1));
			reading1y = (int) (slope1 * reading1x + yinter1);
			
			// show the object
			canvas.drawCircle(reading1x, reading1y, 15, paint);
		}
		
		if(app.getCurrentDistances()[9].intValue() < 450) {
			reading9x = (int) (240 + (app.getCurrentDistances()[9].intValue() * ratio9));
			reading9y = (int) (slope9 * reading9x + yinter9);
			
			// show the object
			canvas.drawCircle(reading9x, reading9y, 15, paint);
		}
		
		if(app.getCurrentDistances()[2].intValue() < 450) {
			reading2x = (int) (240 + (app.getCurrentDistances()[2].intValue() * ratio2));
			reading2y = (int) (slope2 * reading2x + yinter2);
			
			// show the object
			canvas.drawCircle(reading2x, reading2y, 15, paint);
		}
		
		if(app.getCurrentDistances()[8].intValue() < 450) {
			reading8x = 240;
			reading8y = 425 - app.getCurrentDistances()[8].intValue();
			
			// show the object
			canvas.drawCircle(reading8x, reading8y, 15, paint);
		}
		
		if(app.getCurrentDistances()[3].intValue() < 450) {
			reading3x = (int) (240 - (app.getCurrentDistances()[3].intValue() * ratio3));
			reading3y = (int) (slope3 * reading3x + yinter3);
			
			// show the object
			canvas.drawCircle(reading3x, reading3y, 15, paint);
		}
		
		if(app.getCurrentDistances()[7].intValue() < 450) {
			reading7x = (int) (240 - (app.getCurrentDistances()[7].intValue() * ratio7));
			reading7y = (int) (slope7 * reading7x + yinter7);
			
			// show the object
			canvas.drawCircle(reading7x, reading7y, 15, paint);
		}
		
		if(app.getCurrentDistances()[2].intValue() < 450) {
			reading4x = (int) (240 - (app.getCurrentDistances()[2].intValue() * ratio4));
			reading4y = (int) (slope4 * reading4x + yinter4);
			
			// show the object
			canvas.drawCircle(reading4x, reading4y, 15, paint);
		}
		
		if(app.getCurrentDistances()[6].intValue() < 450) {
			reading6x = (int) (240 - (app.getCurrentDistances()[6].intValue() * ratio6));
			reading6y = (int) (slope6 * reading6x + yinter6);
			
			// show the object
			canvas.drawCircle(reading6x, reading6y, 15, paint);
		}
		if(app.getCurrentDistances()[5].intValue() < 240) {
			reading5x = 240 - app.getCurrentDistances()[5].intValue();
			reading5y = 425;
			
			// show the object
			canvas.drawCircle(reading5x, reading5y, 15, paint);
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
	
	Runnable RecurringTask = new Runnable() {
		  public void run() {
		    // Do whatever you want
			SonarView.this.invalidate();

		    // Call this method again 
		    updateHandler.postDelayed(this, 90);
		  }
	};
}