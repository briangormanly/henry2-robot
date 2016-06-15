package com.valleytg.robotics.bluetooth_henry.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.valleytg.robotics.bluetooth_henry.MainActivity;
import com.valleytg.robotics.bluetooth_henry.app.Henry;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class DataRetrieval extends Service {
	
	
	/**
	 * Application layer
	 */
	Henry app;
	

	public DataRetrieval() {
		Log.i("","DataRefreshService Constructor");

	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
    public void onCreate() {
		Log.i("","Data Refresh Service.onCreate().");
	    
	    // get the application
	    this.app = (Henry)getApplication();
	    
	    super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
    	Log.i("","DataRefresh.onStart().");
        
        // get the application
        this.app = (Henry)getApplication();

        // call the threads to get the local or remote data
        if(!this.app.getRunning()) {
        	DataThread dataThread = new DataThread(this.app.getBtSocket());
	        dataThread.execute();
        }
        
        super.onStart(intent, startId);
    }

    @Override
	public void onDestroy() {
		// TODO Auto-generated method stub
    	//dataTimer.removeCallbacks(getData);
		super.onDestroy();
	}
    
    

	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		//dataTimer.removeCallbacks(getData);
		super.finalize();
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("","DataRefresh.onStartCommand().");
            
		// check to see if the local thread is not already running
		if(!this.app.getRunning()) {
			// call the threads to get the local or remote data
            DataThread dataThread = new DataThread(this.app.getBtSocket());
	        dataThread.execute();
        }
        else {
        	Log.i("","Data thread already running, attempted from onStartCommand");
        }
            
        return Service.START_STICKY;
    }
	
	
	
	// Threads

		class DataThread extends AsyncTask<Void, Void, Void> {
			
			private final BluetoothSocket mmSocket;
	        private final InputStream mmInStream;
	        private final OutputStream mmOutStream;
	        
	        private StringBuilder sb = new StringBuilder();
	      
	        public DataThread(BluetoothSocket socket) {
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

			@Override
			protected Void doInBackground(Void... unused) {
				try {
					Looper.myLooper().prepare();
				}
				catch(Exception e) {
					// Looper only needs to be created if the thread is new, if reusing the thread we end up here
				}
				
				// set the local running
				DataRetrieval.this.app.setRunning(true);
				Log.i("", "data flag running set");
				
				try {
					
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
		                        DataRetrieval.this.app.setCurrentHeading(sb.substring((startHeading + 2), endHeading));

		                        // get the start and end indexes of the front range
		                        int startFrontRange = sb.indexOf("FR");
		                        int endFrontRange = sb.indexOf("/FR");
		                        
		                        // get the front range
		                        DataRetrieval.this.app.setCurrentFrontRange(sb.substring((startFrontRange + 2), endFrontRange));
		                        
		                        // get the start and end indexes of the left wheel speed
		                        int startLeftSpeed = sb.indexOf("LS");
		                        int endLeftSpeed = sb.indexOf("/LS");
		                        
		                        // set the left wheel speed
		                        DataRetrieval.this.app.setCurrentLeftSpeed(sb.substring((startLeftSpeed + 2), endLeftSpeed));
		                        
		                        // get the start and end indexes of the right wheel speed
		                        int startRightSpeed = sb.indexOf("RS");
		                        int endRightSpeed = sb.indexOf("/RS");
		                        
		                        // set the right wheel speed
		                        DataRetrieval.this.app.setCurrentRightSpeed(sb.substring((startRightSpeed + 2), endRightSpeed));
		                        
		                        // get the start and end indexes of the turret position
		                        int startTurretPosition = sb.indexOf("TI");
		                        int endTurretPosition = sb.indexOf("/TI");
		                        
		                        // set the right wheel speed
		                        DataRetrieval.this.app.setCurrentTurretIndex(sb.substring((startTurretPosition + 2), endTurretPosition));
		                        
		                        // get the current distances array
		                        int startDistances = sb.indexOf("CD");
		                        int endDistances = sb.indexOf("/CD");
		                        
		                        // get all the distances delinated by _
		                        String all_distances = sb.substring((startDistances + 2), endDistances);
		                        
		                        DataRetrieval.this.app.setCurrentDistances(all_distances.split("_"));
		                        
		                        // get the current magnitudes array
		                        int startMagnitudes = sb.indexOf("CM");
		                        int endMagnitudes = sb.indexOf("/CM");
		                        
		                        // get all the distances delinated by _
		                        String all_magnitudes = sb.substring((startMagnitudes + 2), endMagnitudes);
		                        
		                        DataRetrieval.this.app.setCurrentMagnitudes(all_magnitudes.split("_"));
		                        
		                        // get the start and end indexes of the x coordinate
		                        int startX = sb.indexOf("XP");
		                        int endX = sb.indexOf("/XP");
		                        
		                        // set the left wheel speed
		                        DataRetrieval.this.app.setCurrentX(sb.substring((startX + 2), endX));
		                        
		                        // get the start and end indexes of the y coordinate
		                        int startY = sb.indexOf("YP");
		                        int endY = sb.indexOf("/YP");
		                        
		                        // set the left wheel speed
		                        DataRetrieval.this.app.setCurrentY(sb.substring((startY + 2), endY));
		                        
		                        // get the start and end indexes of the target x coordinate
		                        int startTargetX = sb.indexOf("XT");
		                        int endTargetX = sb.indexOf("/XT");
		                        
		                        // set the left wheel speed
		                        DataRetrieval.this.app.setTargetX(sb.substring((startTargetX + 2), endTargetX));
		                        
		                        // get the start and end indexes of the target y coordinate
		                        int startTargetY = sb.indexOf("YT");
		                        int endTargetY = sb.indexOf("/YT");
		                        
		                        // set the left wheel speed
		                        DataRetrieval.this.app.setTargetY(sb.substring((startTargetY + 2), endTargetY));
		                        
		                        // get the start and end indexes of the current state
		                        int startCS = sb.indexOf("CS");
		                        int endCS = sb.indexOf("/CS");
		                        
		                        // set the left wheel speed
		                        DataRetrieval.this.app.setCurrentState(sb.substring((startCS + 2), endCS));
		                        
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
		        catch(Exception e) {
		        	e.printStackTrace();
		        	DataRetrieval.this.app.setRunning(false);
		        	Log.i("", "Data flag running un-set by error");
		        	// no tickets anywhere local, call remote
					
		        	return null;
		        }
		        
				return null;
			}
			
			/* Call this from the main activity to send data to the remote device */
	        public void write(String message) {
	            //Log.d(TAG, "...Data to send: " + message + "...");
	            byte[] msgBuffer = message.getBytes();
	            try {
	            	mmOutStream.write(msgBuffer);
	            } 
	            catch (IOException e) {
	                //Log.d(TAG, "...Error data send: " + e.getMessage() + "...");     
	            }
	        }
	      
	        /* Call this from the main activity to shutdown the connection */
	        public void cancel() {
	            try {
	                mmSocket.close();
	            } catch (IOException e) { }
	        }
			
			protected void onPostExecute(Void unused) {
				
				DataRetrieval.this.app.setRunning(false);
				Log.i("", "Data flag running un-set");
				
		    }
		}

}
