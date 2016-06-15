package com.valleytg.robotics.bluetooth_henry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.UUID;

import com.valleytg.robotics.bluetooth_henry.app.Henry;
import com.valleytg.robotics.bluetooth_henry.app.Henry.ConnectedThread;
  
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
  
public class MainActivity extends Activity {
  private static final String TAG = "bluetooth2";
  
  Henry app;
    
  //Button btnOn, btnOff;
  TextView txtHeading;
  TextView txtFrontRange;
  TextView txtLeftSpeed;
  TextView txtRightSpeed;
  TextView txtDistances;
  TextView txtMagnitudes;
  TextView txtTurretIndex;
  TextView txtTurretHeading;
  TextView txtTurretSlope;
  TextView txtRobotSlope;
  TextView txtX;
  TextView txtY;
  TextView txtTargetX;
  TextView txtTargetY;
  TextView txtCurrentState;
  
  Button btnRoaming;
  Button btnWaiting;
  Button btnTraveling;
  Button btnSonar;
  Button btnMap;
  
  /**
	 * Timer that manages screen refresh
	 */
	private Handler ticketTimer = new Handler();
  
  private static final int DIALOG_X = 1;
  private static final int DIALOG_Y = 2;
  
  Handler h;

  private int sendX = 0;
  private int sendY = 0;
    
  private static final int REQUEST_ENABLE_BT = 1;
  final int RECIEVE_MESSAGE = 1;        // Status  for Handler
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private StringBuilder sb = new StringBuilder();
   
  ConnectedThread mConnectedThread;
    
  // SPP UUID service
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  
  // MAC-address of Bluetooth module (you must edit this line)
  private static String address = "00:12:11:23:25:58";
    
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  
    setContentView(R.layout.activity_main);
    
    // get the application
    this.app = (Henry) getApplication();
  
    //btnOn = (Button) findViewById(R.id.btnOn);                  // button LED ON
    //btnOff = (Button) findViewById(R.id.btnOff);                // button LED OFF
    txtHeading = (TextView) findViewById(R.id.txtHeading);      // for display the received data from the Arduino
    txtFrontRange = (TextView) findViewById(R.id.txtFrontRange);
    txtLeftSpeed = (TextView) findViewById(R.id.txtLeftSpeed);
    txtRightSpeed = (TextView) findViewById(R.id.txtRightSpeed);
    txtDistances = (TextView) findViewById(R.id.txtDistances);
    txtMagnitudes = (TextView) findViewById(R.id.txtMagnitudes);
    txtTurretIndex = (TextView) findViewById(R.id.txtTurretIndex);
    txtTurretHeading = (TextView) findViewById(R.id.txtTurretCurrentHeading);
    txtTurretSlope = (TextView) findViewById(R.id.txtTurretCurrentSlope);
    txtRobotSlope = (TextView) findViewById(R.id.txtRobotSlope);
    txtX = (TextView) findViewById(R.id.txtX);
    txtY = (TextView) findViewById(R.id.txtY);
    txtTargetX = (TextView) findViewById(R.id.txtTargetX);
    txtTargetY = (TextView) findViewById(R.id.txtTargetY);
    txtCurrentState = (TextView) findViewById(R.id.txtCurrentState);
    
    btnRoaming = (Button) findViewById(R.id.btnRoaming);
    btnWaiting = (Button) findViewById(R.id.btnWaiting);
    btnTraveling = (Button) findViewById(R.id.btnTraveling);
    btnSonar = (Button) findViewById(R.id.btnSonar);
    btnMap = (Button) findViewById(R.id.btnMap);
    
    // button listeners 
    this.btnTraveling.setOnClickListener(new View.OnClickListener() {
		
		public void onClick(View v) {
			
			
			mConnectedThread.write("ST2");
			mConnectedThread.write("CX8"); 
        	mConnectedThread.write("CY30");

		}
	});
    
    
    
    
   
    
    btnRoaming.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
            
            mConnectedThread.write("ST1");    // Send "1" via Bluetooth
            //Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
          }
        });
    
    btnWaiting.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
            
            mConnectedThread.write("ST0");    // Send "0" via Bluetooth
            //Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
          }
        });
    
    btnSonar.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
            
            // start the sonar activity
        	Intent intent = new Intent(MainActivity.this, Sonar.class);
			startActivity(intent);
          }
        });
    
    btnMap.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
            
            // start the sonar activity
        	Intent intent = new Intent(MainActivity.this, WorldMap.class);
			startActivity(intent);
          }
        });

    btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
    checkBTState();

  }
  
  public void refresh() {
	  txtHeading.setText("Heading: " + this.app.getCurrentHeading());            // update TextView
      txtFrontRange.setText("Front Range: " + this.app.getCurrentFrontRange());
      txtLeftSpeed.setText("Left Motor Speed: " + this.app.getCurrentLeftSpeed());
      txtRightSpeed.setText("Right Motor Speed: " + this.app.getCurrentRightSpeed());
      txtDistances.setText("");
      for(int i=0; i<this.app.getCurrentDistances().length; i++) {
    	  txtDistances.append(this.app.getCurrentDistances()[i] + " ");
      }
      txtMagnitudes.setText("");
      for(int i=0; i<this.app.getCurrentMagnitudes().length; i++) {
    	  txtMagnitudes.append(this.app.getCurrentMagnitudes()[i] + " ");
      }
      
      DecimalFormat twoDForm = new DecimalFormat("#.##");
      
      txtTurretIndex.setText("Turret Position: " + this.app.getCurrentTurretIndex());
      txtTurretHeading.setText("Turret Heading: " + this.app.getTurretCurrentHeading());
      txtTurretSlope.setText("T Slope: " + Double.valueOf(twoDForm.format(this.app.getTurretCurrentSlope())));
      txtRobotSlope.setText("R Slope: " + Double.valueOf(twoDForm.format(this.app.getCurrentRobotHeadingSlope())));
      txtX.setText("X : " + this.app.getCurrentX());
      txtY.setText("Y : " + this.app.getCurrentY());
      txtTargetX.setText("Target X : " + this.app.getTargetX());
      txtTargetY.setText("Target Y : " + this.app.getTargetY());
      txtCurrentState.setText("Current State: " + this.app.getCurrentState());
  }
  
  /**
	 * Starts the process of sending location updates, periodically to the server
	 */
	public void startUpdatingTicketView() {
      ticketTimer.removeCallbacks(sendLocation);
      ticketTimer.postDelayed(sendLocation, 100);
	}
	
	/**
	 * Ends the process of periodically sending location updates to the server
	 */
	public void endUpdatingTicketView() {
		 ticketTimer.removeCallbacks(sendLocation);
	}
	
	
	private Runnable sendLocation = new Runnable() {
		   public void run() {
			   // refresh the data
			   // getList();
			   
			   // update the view to the model data
			   MainActivity.this.refresh();
			   
			   // log that it happened
			   //System.out.println("TicketActivtiy view refresh complete");
			   
			   // call this thread again
			   ticketTimer.postDelayed(this, 100);
		   }
	};
    
  @Override
  public void onResume() {
    super.onResume();
  
    Log.d(TAG, "...onResume - try connect...");
    
    // Set up a pointer to the remote node using it's address.
    BluetoothDevice device = btAdapter.getRemoteDevice(address);
    
    // Two things are needed to make a connection:
    //   A MAC address, which we got above.
    //   A Service ID or UUID.  In this case we are using the
    //     UUID for SPP.
    try {
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
    }
    
    // Discovery is resource intensive.  Make sure it isn't going on
    // when you attempt to connect and pass your message.
    btAdapter.cancelDiscovery();
    
    // Establish the connection.  This will block until it connects.
    Log.d(TAG, "...Connecting...");
    try {
      btSocket.connect();
      Log.d(TAG, "....Connection ok...");
    } catch (IOException e) {
      try {
        btSocket.close();
      } catch (IOException e2) {
        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
      }
    }
      
    // Create a data stream so we can talk to server.
    Log.d(TAG, "...Create Socket...");
    
    if(mConnectedThread == null) {
	    mConnectedThread = app.new ConnectedThread(btSocket);
	    mConnectedThread.start();
    }
    
    // start the ticket updates
    startUpdatingTicketView();
  }
  
  @Override
  public void onPause() {
    super.onPause();
  
    Log.d(TAG, "...In onPause()...");
    
    endUpdatingTicketView();
  }
  
  
    
  @Override
protected void onDestroy() {
	// TODO Auto-generated method stub
	super.onStop();
	Log.d(TAG, "...In onDestroy()...");
	
	try     {
	      btSocket.close();
	    } catch (IOException e2) {
	      errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
	    }
}

private void checkBTState() {
    // Check for Bluetooth support and then check to make sure it is turned on
    // Emulator doesn't support Bluetooth and will return null
    if(btAdapter==null) { 
      errorExit("Fatal Error", "Bluetooth not support");
    } else {
      if (btAdapter.isEnabled()) {
        Log.d(TAG, "...Bluetooth ON...");
      } else {
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
      }
    }
  }
  
  private void errorExit(String title, String message){
    Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
    finish();
  }
  
  
}