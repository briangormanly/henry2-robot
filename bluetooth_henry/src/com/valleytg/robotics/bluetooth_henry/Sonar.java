package com.valleytg.robotics.bluetooth_henry;

import com.valleytg.robotics.bluetooth_henry.app.Henry;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

public class Sonar extends Activity {

	SonarView drawView;
	
	Henry app;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // get the application
        this.app = (Henry) getApplication();
        
        // Set full screen view
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                         WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        drawView = new SonarView(this);
        setContentView(drawView);
        drawView.requestFocus();
    }
    
    
   
    
}
