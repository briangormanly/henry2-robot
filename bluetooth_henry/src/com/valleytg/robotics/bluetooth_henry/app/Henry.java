package com.valleytg.robotics.bluetooth_henry.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.valleytg.robotics.bluetooth_henry.collection.DoublePoint;
import com.valleytg.robotics.bluetooth_henry.collection.World;
import com.valleytg.robotics.bluetooth_henry.service.DataRetrieval;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Point;
import android.os.SystemClock;

public class Henry extends Application {
	
	
	/**
	 * Intent to call service to update the model
	 */
	private PendingIntent mModelUpdateIntent;
	
	/**
	 * Intent for data
	 */
	private Intent dataIntent;
	
	/**
	 * Alarm managers
	 */
	private AlarmManager am;
	
	/**
	 * thread control
	 */
	private Boolean running = false;
	
	/**
	 * Bluetooth socket
	 */
	private BluetoothSocket btSocket = null;
	public BluetoothAdapter btAdapter = null;
	
	/**
	 * Henry's world
	 */
	private World world = new World();
	
	/**
	 * Tracks turret rotations for world cleanup
	 */
	private Integer turretRotations = 0;
	
	
	/*
	 * private local sensor variables
	 */
	
	// current heading as shown by compass
	private Double currentHeading = 0.0;
	
	// current heading as shown by phone compass
	private Double phoneCurrentHeading = 0.0;
	
	// current turret heading (heading adjusted for turret angle)
	private Double turretCurrentHeading = 0.0;
	
	// current slope of the angle of the turret
	// based on turrets current difference from the robots heading
	private Double turretCurrentSlope = 0.0;
	
	private String currentFrontRange = "0";
	private String currentLeftSpeed = "0";
	private String currentRightSpeed = "0";
	
	private Double[] currentDistances = new Double[11];
	private Double[] currentMagnitudes = new Double[11];
	
	
	//private String[] currentDistances = new String[11];
	//private String[] currentMagnitudes = new String[11];
	private Integer currentTurretIndex = 0;
	private Double currentX = 0.0;
	private Double currentY = 0.0;
	private Double targetX = 0.0;
	private Double targetY = 0.0;
	private String currentState = "unknown";
	
	// slopes
	
	
	private static final int REQUEST_ENABLE_BT = 1;
	private final int RECIEVE_MESSAGE = 1;        // Status  for Handler
	private StringBuilder sb = new StringBuilder();
	    
	// SPP UUID service
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	  
	// MAC-address of Bluetooth module (you must edit this line)
	private static String address = "00:12:11:23:25:58";
	
	
	/**
	 * Constructor
	 * Initialize arraylists, etc
	 */
	public Henry() {
		
		
	}
	
	@Override
	public void onCreate() {
		super.onCreate();

	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		
		// stop the data refresh and gps services
		stopServices();	
	}
	
	public void startServices(Activity thisActivity) {
		// get the current time
		long firstTime = SystemClock.elapsedRealtime() + 1000;
      
		// initialize the intents
		dataIntent = new Intent(thisActivity, DataRetrieval.class);
      
		// Create an IntentSender that will launch our services, to be scheduled
		// with the alarm manager.
		this.setmModelUpdateIntent(PendingIntent.getService(thisActivity, 0, dataIntent, 0));


		// Schedule the data refresh alarm every 100 ms
		am = (AlarmManager)getSystemService(ALARM_SERVICE);
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 1000, this.getmModelUpdateIntent());

		// tell the log!!!
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~Services have been started!!!!!!!!!!!!~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
      
	}
	
	/**
	 * Alarm Manager Services - Stop Services
	 * This method is called to stop both the data refresh and gps reporting services
	 */
	public void stopServices() {
		// stop updating the data model and gps
		
		AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmManager.cancel(this.getmModelUpdateIntent());
		
		alarmManager = null;
		
		this.setmModelUpdateIntent(null);
		
		// tell the log!!!
		System.out.println("******************************Services have been stopped!!!!!!!!!!!!********************************************");
		
	}
	
	/**
	 * Given a Point in Cartesian space, distance from the original point
	 * and the slope of the line the two points will be on.  This method
	 * locates the Point in Cartesian coordinates at distance from the 
	 * original coordinates passed.
	 * 
	 * Note this function assumes 1.3 cm distance from any x or y whole number 
	 * coordinate to the next.
	 * 
	 * @param currentX - Robots current x location
	 * @param currentY - Robots current y location
	 * @param slope - slope of the line that we wish to find the new point on.
	 * 	This will most likely be the slope of the current turret angle or the 
	 *  slope of the current sensor reading.
	 * @param distance - The distance in centimeters of the new point from the
	 *  current position.
	 *  
	 * @return Point with x and y members representing the new point.
	 */
	public DoublePoint findRemotePoint(Double currentX, Double currentY, Double slope, Double distance) {
		DoublePoint returnPoint = new DoublePoint();
		
		// get the adjusted distance measurement.  This is necessary because the 
		// robots Cartesian coordinate system is based on 1.3 cm between whole 
		// numbers.
		Double adjustedDistance = distance / 1.3;
		
		Double dvx = 1.0;
		Double dvy = slope;
		
		Double magnitude = Math.pow((Math.pow(dvx, 2) + Math.pow(dvy, 2)), .5);
		
		Double ndx = dvx / magnitude;
		Double ndy = dvy / magnitude;
		
		//returnPoint.x = currentX + (adjustedDistance * ndx);
		//returnPoint.y = currentY + (adjustedDistance * ndy);
		
		// experimental attempt to take orientation into account
		// if the current heading is in the top half of grid or bottom.
		if(this.currentHeading > 270 || (this.currentHeading > 0 && this.currentHeading < 90)) {
			// add the values
			returnPoint.x = currentX + (adjustedDistance * ndx);
			returnPoint.y = currentY + (adjustedDistance * ndy);
		}
		else {
			// subtract them.
			returnPoint.x = currentX - (adjustedDistance * ndx);
			returnPoint.y = currentY - (adjustedDistance * ndy);
		}
		
		return returnPoint;
	}
	
	
	public DoublePoint findRemoteTurretPoint(Double currentX, Double currentY, Double slope, Double distance) {
		DoublePoint returnPoint = new DoublePoint();
		
		// get the adjusted distance measurement.  This is necessary because the 
		// robots Cartesian coordinate system is based on 1.3 cm between whole 
		// numbers.
		Double adjustedDistance = distance / 1.3;
		
		Double dvx = 1.0;
		Double dvy = slope;
		
		Double magnitude = Math.pow((Math.pow(dvx, 2) + Math.pow(dvy, 2)), .5);
		
		Double ndx = dvx / magnitude;
		Double ndy = dvy / magnitude;
		
		//returnPoint.x = currentX + (adjustedDistance * ndx);
		//returnPoint.y = currentY + (adjustedDistance * ndy);
		
		// experimental attempt to take orientation into account
		// if the current heading is in the top half of grid or bottom.
		if(this.turretCurrentHeading > 270 || (this.turretCurrentHeading > 0 && this.turretCurrentHeading < 90)) {
			// add the values
			returnPoint.x = currentX + (adjustedDistance * ndx);
			returnPoint.y = currentY + (adjustedDistance * ndy);
		}
		else {
			// subtract them.
			returnPoint.x = currentX - (adjustedDistance * ndx);
			returnPoint.y = currentY - (adjustedDistance * ndy);
		}
		
		return returnPoint;
	}
	
	
	
	
	public Double getCurrentRobotHeadingSlope() {
		Double slope;
		
		if(this.getCurrentHeading() == 90 || this.getCurrentHeading() == 270) {
			slope = 200.0;
		}
		else {
			double radians = Math.toRadians(this.getCurrentHeading());
			slope = Math.tan(radians);
		}
		

		return slope;
	}
	
	
	
	public PendingIntent getmModelUpdateIntent() {
		return mModelUpdateIntent;
	}

	public void setmModelUpdateIntent(PendingIntent mModelUpdateIntent) {
		this.mModelUpdateIntent = mModelUpdateIntent;
	}
	
	public Boolean getRunning() {
		return running;
	}

	public void setRunning(Boolean running) {
		this.running = running;
	}

	public Intent getDataIntent() {
		return dataIntent;
	}

	public void setDataIntent(Intent dataIntent) {
		this.dataIntent = dataIntent;
	}

	public AlarmManager getAm() {
		return am;
	}

	public void setAm(AlarmManager am) {
		this.am = am;
	}

	public Double getCurrentHeading() {
		return currentHeading;
	}

	public void setCurrentHeading(String currentHeading) {
		// convert to double
		try {
			this.currentHeading = Double.parseDouble(currentHeading);
		}
		catch(Exception e) {
			this.currentHeading = 0.0;
		}
	}
	
	public Double getPhoneCurrentHeading() {
		return phoneCurrentHeading;
	}

	public void setPhoneCurrentHeading(String phoneCurrentHeading) {
		// convert to double
		try {
			this.phoneCurrentHeading = Double.parseDouble(phoneCurrentHeading);
		}
		catch(Exception e) {
			this.phoneCurrentHeading = 0.0;
		}
	}
	
	public Double getTurretCurrentHeading() {
		return turretCurrentHeading;
	}

	public void setTurretCurrentHeading(String turretCurrentHeading) {
		// convert to double
		try {
			this.turretCurrentHeading = Double.parseDouble(turretCurrentHeading);
		}
		catch(Exception e) {
			this.turretCurrentHeading = 0.0;
		}
	}
	
	/**
	 * Set the current heading of the turret based on the current compass value of the robot
	 * passed.
	 * @param currentHeading - compass heading value to use to adjust for turret angle.
	 */
	public void setTurretCurrentHeading(Double currentHeading) {
		switch(this.getCurrentTurretIndex()) {
		case(0):
			this.turretCurrentHeading = (currentHeading + 90) % 360;
			break;
			
		case(1):
			this.turretCurrentHeading = (currentHeading + 54) % 360;
			break;
			
		case(2):
			this.turretCurrentHeading = (currentHeading + 18) % 360;
			break;
			
		case(3):
			this.turretCurrentHeading = (currentHeading - 18) % 360;
			break;
			
		case(4):
			this.turretCurrentHeading = (currentHeading - 54) % 360;
			break;
			
		case(5):
			this.turretCurrentHeading = (currentHeading - 90) % 360;
			break;
			
		case(6):
			this.turretCurrentHeading = (currentHeading - 72) % 360;
			break;
			
		case(7):
			this.turretCurrentHeading = (currentHeading - 36) % 360;
			break;
			
		case(8):
			this.turretCurrentHeading = currentHeading;
			break;
			
		case(9):
			this.turretCurrentHeading = (currentHeading + 36) % 360;
			break;
			
		case(10):
			this.turretCurrentHeading = (currentHeading + 72) % 360;
			break;
		}
	}
	
	public void setTurretCurrentSlope() {

		double radians = Math.toRadians(this.getTurretCurrentHeading());
		//System.out.format("The tangent of %.1f degrees is %.4f%n", this.getCurrentHeading(), Math.tan(radians));
		
		this.turretCurrentSlope = Math.tan(radians);
		
	}

	public Double getTurretCurrentSlope() {
		
		return this.turretCurrentSlope;
	}
	
	
	public String getCurrentFrontRange() {
		return currentFrontRange;
	}

	public void setCurrentFrontRange(String currentFrontRange) {
		this.currentFrontRange = currentFrontRange;
	}

	public String getCurrentLeftSpeed() {
		return currentLeftSpeed;
	}

	public void setCurrentLeftSpeed(String currentLeftSpeed) {
		this.currentLeftSpeed = currentLeftSpeed;
	}

	public String getCurrentRightSpeed() {
		return currentRightSpeed;
	}

	public void setCurrentRightSpeed(String currentRightSpeed) {
		this.currentRightSpeed = currentRightSpeed;
	}

	public Double[] getCurrentDistances() {
		return currentDistances;
	}

	public void setCurrentDistances(String[] currentDistances) {
		for(int i=0; i<currentDistances.length; i++) {
			try {
				this.currentDistances[i] = Double.parseDouble(currentDistances[i]);
			}
			catch(Exception e) {
				this.currentDistances[i] = 0.0;
			}
		}
	}

	public Double[] getCurrentMagnitudes() {
		return currentMagnitudes;
	}

	public void setCurrentMagnitudes(String[] currentMagnitudes) {
		for(int i=0; i<currentMagnitudes.length; i++) {
			try {
				this.currentMagnitudes[i] = Double.parseDouble(currentMagnitudes[i]);
			}
			catch(Exception e) {
				this.currentMagnitudes[i] = 0.0;
			}
		}
	}

	public Integer getCurrentTurretIndex() {
		return currentTurretIndex;
	}


	/**
	 * not only advances the turret index, but also updates the turret heading,
	 * turret slope and gets the position of the distance sensed along this line
	 * and adds the reading to the world view.
	 * 
	 * @param currentTurretIndex
	 * 
	 */
	public void setCurrentTurretIndex(String currentTurretIndex) {
		
		try{
			this.currentTurretIndex = Integer.parseInt(currentTurretIndex);
			
			if(!Henry.this.currentState.equals("0")) {
				// when we update the turret index we should update the turretheading as well
				// currently using the robot compass reading
				this.setTurretCurrentHeading(this.getCurrentHeading());
				
				// now that we have the current heading of the turret we will convert it into
				// a slope using its tangent.
				this.setTurretCurrentSlope();
				
				synchronized (this) {
					// get the remote point for the distance of the current turret position
					DoublePoint reading = this.findRemoteTurretPoint(currentX, currentY, this.getTurretCurrentSlope(), this.getCurrentDistances()[this.currentTurretIndex]);
					
					// add the point to the world
					this.world.newReading(reading);
				}
	
				// mark a full rotation if the turret is in the last spot.
				if(this.currentTurretIndex == 10) {
					turretRotations++;
					this.world.removeUnprovenReadings();
				}
			}
			
		}
		catch(Exception e) {
			this.currentTurretIndex = 0;
		}
	}

	public Double getCurrentX() {
		return currentX;
	}

	public void setCurrentX(String currentX) {
		try {
			this.currentX = Double.parseDouble(currentX);
		}
		catch(Exception e) {
			this.currentX = 0.0;
		}
	}

	public Double getCurrentY() {
		return currentY;
	}

	public void setCurrentY(String currentY) {
		try {
			this.currentY = Double.parseDouble(currentY);
		}
		catch(Exception e) {
			this.currentY = 0.0;
		}
	}

	public Double getTargetX() {
		return targetX;
	}

	public void setTargetX(String targetX) {
		try {
			this.targetX = Double.parseDouble(targetX);
		}
		catch(Exception e) {
			this.targetX = 0.0;
		}
	}

	public Double getTargetY() {
		return targetY;
	}

	public void setTargetY(String targetY) {
		try {
			this.targetY = Double.parseDouble(targetY);
		}
		catch(Exception e) {
			this.targetY = 0.0;
		}
	}

	public String getCurrentState() {
		return currentState;
	}

	public void setCurrentState(String currentState) {
		this.currentState = currentState;
	}

	public BluetoothAdapter getBtAdapter() {
		return btAdapter;
	}

	public void setBtAdapter(BluetoothAdapter btAdapter) {
		this.btAdapter = btAdapter;
	}

	public BluetoothSocket getBtSocket() {
		return btSocket;
	}

	public void setBtSocket(BluetoothSocket btSocket) {
		this.btSocket = btSocket;
	}

	public StringBuilder getSb() {
		return sb;
	}

	public void setSb(StringBuilder sb) {
		this.sb = sb;
	}

	public static String getAddress() {
		return address;
	}

	public static void setAddress(String address) {
		Henry.address = address;
	}

	public static int getRequestEnableBt() {
		return REQUEST_ENABLE_BT;
	}

	public int getRECIEVE_MESSAGE() {
		return RECIEVE_MESSAGE;
	}

	public static UUID getMyUuid() {
		return MY_UUID;
	}
	
	
	
	
	
	public World getWorld() {
		return world;
	}

	public void setWorld(World world) {
		this.world = world;
	}





	public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
      
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
      
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
      
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
      
        public void run() {
            byte[] buffer = new byte[128];  // buffer store for the stream
            int bytes; // bytes returned from read()
 
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                	
                	
                	bytes = mmInStream.read(buffer);
                	byte[] readBuf = (byte[]) buffer;
                    String strIncom = new String(readBuf, 0, bytes);                 // create string from bytes array
                    sb.append(strIncom);                                                // append string
                    int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                    if (endOfLineIndex > 0) {  
                    	// add the current string to eol to a local string
                        String sbprint = sb.substring(0, endOfLineIndex);
                        
                        // get the start and end indexes of the heading
                        int startHeading = sb.indexOf("HE");
                        int endHeading = sb.indexOf("/HE");
                        
                        // set the heading
                        Henry.this.setCurrentHeading(sb.substring((startHeading + 2), endHeading));

                        // get the start and end indexes of the front range
                        int startFrontRange = sb.indexOf("FR");
                        int endFrontRange = sb.indexOf("/FR");
                        
                        // get the front range
                        Henry.this.currentFrontRange = sb.substring((startFrontRange + 2), endFrontRange);
                        
                        // get the start and end indexes of the left wheel speed
                        int startLeftSpeed = sb.indexOf("LS");
                        int endLeftSpeed = sb.indexOf("/LS");
                        
                        // set the left wheel speed
                        Henry.this.currentLeftSpeed = sb.substring((startLeftSpeed + 2), endLeftSpeed);
                        
                        // get the start and end indexes of the right wheel speed
                        int startRightSpeed = sb.indexOf("RS");
                        int endRightSpeed = sb.indexOf("/RS");
                        
                        // set the right wheel speed
                        Henry.this.currentRightSpeed = sb.substring((startRightSpeed + 2), endRightSpeed);
                        
                        // get the start and end indexes of the turret position
                        int startTurretPosition = sb.indexOf("TI");
                        int endTurretPosition = sb.indexOf("/TI");
                        
                        // set the turret index
                        Henry.this.setCurrentTurretIndex(sb.substring((startTurretPosition + 2), endTurretPosition));
                        
                        // get the current distances array
                        int startDistances = sb.indexOf("CD");
                        int endDistances = sb.indexOf("/CD");
                        
                        // get all the distances delinated by _
                        String all_distances = sb.substring((startDistances + 2), endDistances);

                        String[] tempDistances = all_distances.split("_");
                        Henry.this.setCurrentDistances(tempDistances);

                        // get the current magnitudes array
                        int startMagnitudes = sb.indexOf("CM");
                        int endMagnitudes = sb.indexOf("/CM");
                        
                        // get all the distances delinated by _
                        String all_magnitudes = sb.substring((startMagnitudes + 2), endMagnitudes);
                        
                        String[] tempMagnitudes = all_magnitudes.split("_");
                        Henry.this.setCurrentMagnitudes(tempMagnitudes);
                        
                        // get the start and end indexes of the x coordinate
                        int startX = sb.indexOf("XP");
                        int endX = sb.indexOf("/XP");
                        
                        // set the x co-ordinate
                        Henry.this.setCurrentX(sb.substring((startX + 2), endX));
                        
                        // get the start and end indexes of the y coordinate
                        int startY = sb.indexOf("YP");
                        int endY = sb.indexOf("/YP");
                        
                        // set the y co-ordinate
                        Henry.this.setCurrentY(sb.substring((startY + 2), endY));
                        
                        // get the start and end indexes of the target x coordinate
                        int startTargetX = sb.indexOf("XT");
                        int endTargetX = sb.indexOf("/XT");
                        
                        // set the left wheel speed
                        Henry.this.setTargetX(sb.substring((startTargetX + 2), endTargetX));
                        
                        // get the start and end indexes of the target y coordinate
                        int startTargetY = sb.indexOf("YT");
                        int endTargetY = sb.indexOf("/YT");
                        
                        // set the left wheel speed
                        Henry.this.setTargetY(sb.substring((startTargetY + 2), endTargetY));
                        
                        // get the start and end indexes of the current state
                        int startCS = sb.indexOf("CS");
                        int endCS = sb.indexOf("/CS");
                        
                        // set the current state
                        Henry.this.currentState = sb.substring((startCS + 2), endCS);
                        
                        // output what we have
                        //System.out.println("recv: " + sbprint);
                        sb.delete(0, sb.length());   
                    }
                	
                	
                	
                	
                    // Read from the InputStream
                            // Get number of bytes and message in "buffer"
                    //h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } 
                catch (IOException e) {
                	System.out.println("There was a problem parsing the incoming data!");
                	e.printStackTrace();
                    break;
                }
                catch (Exception e) {
                	System.out.println("There was a problem parsing the incoming data!");
                	e.printStackTrace();
                    break;
                }
            }
        }
      
        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            System.out.println("...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
            	System.out.println("...Error data send: " + e.getMessage() + "...");     
              }
        }
      
        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}
