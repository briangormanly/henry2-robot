#include "robot.h"
#include "Arduino.h"

// Constructor
Robot::Robot(void) {
  // set all the pins
  servoLeftPin = 13;
  servoRightPin = 12;
  servoTurretPin = 11;
  
  leftEncoderPin = 8;
  rightEncoderPin = 9;
  
  pingPin = 10;
  piezoPin = 4;
  
  analogRangePin = 3;
  
  leftEncoderCount = 0;
  rightEncoderCount = 0;
  lastLeftEncoderCount = 0;
  lastRightEncoderCount = 0;
  leftEncoderLastHigh = false;
  rightEncoderLastHigh = false;
  
  // heading values to average out
  headingCounter = 0;
  totalHeadingReadings = 20;
  for(int i=0; i<totalHeadingReadings; i++) {
    headings[i] = 0;
  }
  
  // set the amount of ms to turn a wheel 1 degree
  msPerTurnDegree = 6;

  rtAngle = 900;
  ccwLim = 1400;
  usTocm = 29;
  
  // set up the compass
  compass = new LSM303();
  LSM303::vector running_min = {2047, 2047, 2047}, running_max = {-2048, -2048, -2048};
  
  // create possible states enumeration
  
  
  // initialize the compass
  Wire.begin();
  compass->init(LSM303DLHC_DEVICE, LSM303_SA0_A_AUTO);
  compass->enableDefault();
  compass->writeAccReg(LSM303_CTRL_REG1_A, 0x27); // Bump accelerometer from 50 hz to 400 hz
  
  // Lowest possible gain value that doesn't overflow and return -4096
  compass->setMagGain(LSM303::magGain_25);
  
  // Calibration values. Use the Calibrate example program to get the values for
  // your compass.

  
  // after returning the compass 
  compass->m_min.x = -449; compass->m_min.y = -440; compass->m_min.z = -405;
  compass->m_max.x = 223; compass->m_max.y = 321; compass->m_max.z = 389;

}

// destructor implementation
Robot::~Robot(void)
{
  servoLeftPin = 0;
  servoRightPin = 0;
  servoTurretPin = 0;
  pingPin = 0;
  piezoPin = 0;
  
  msPerTurnDegree = 0;
  
  rtAngle = 0;
  ccwLim = 0;
  usTocm = 0;  
}


/*
 * Control BOE Shield-Bot servo direction, speed, set and forget version.
 * Parameters: speedLeft - left servo speed
 *             speedRight - right servo speed
 *             Backward  Linear  Stop  Linear   Forward
 *             -200      -100......0......100       200
 */ 
void Robot::maneuver(int speedLeft, int speedRight)
{
  // Call maneuver with just 1 ms blocking; servos will keep going indefinitely.
  maneuver(speedLeft, speedRight, 0);              
}

/*
 * Control BOE Shield-Bot servo direction, speed and maneuver duration.   
 * Parameters: speedLeft - left servo speed
 *             speedRight - right servo speed
 *             Backward  Linear  Stop  Linear   Forward
 *             -200      -100......0......100       200
 *             msTime - time to block code execution before another maneuver
 * Source:     http://learn.parallax.com/ManeuverFunction
 */ 
void Robot::maneuver(int speedLeft, int speedRight, int msTime)
{
  servoLeft.writeMicroseconds(1500 + speedLeft);   // Set Left servo speed
  servoRight.writeMicroseconds(1500 - speedRight); // Set right servo speed
  if(msTime==-1)                                   // if msTime = -1
  {                                  
    servoLeft.detach();                            // Stop servo signals
    servoRight.detach();   
  }
  //delay(msTime);                                   // Delay for msTime
}


/*
 * Position the horn of a Parallax Standard Servo
 * Parameter: degreeVal in a range from 90 to -90 degrees. 
 */ 
void Robot::turret(int degreeVal)
{
  servoTurret.writeMicroseconds(ccwLim - rtAngle + (degreeVal * 10));
}

/*
 * Get cm distance measurment from Ping Ultrasonic Distance Sensor
 * Returns: distance measurement in cm.   
 */ 
int Robot::cmDistance()
{
  int distance = 0;                                // Initialize distance to zero    
  do                                               // Loop in case of zero measurement
  {
    int us = ping(pingPin);                        // Get Ping))) microsecond measurement
    distance = convert(us, usTocm);                // Convert to cm measurement
    delay(3);                                      // Pause before retry (if needed)
  }
  while(distance == 0);      //?????????                   
  return distance;                                 // Return distance measurement
}

/*
 * Converts microsecond Ping))) round trip measurement to a useful value.
 * Parameters: us - microsecond value from Ping))) echo time measurement.
 *             scalar - 29 for us to cm, or 74 for us to in.
 * Returns:    distance measurement dictated by the scalar.   
 */ 
int Robot::convert(int us, int scalar)
{
    return us / scalar / 2;                        // Echo round trip time -> cm
}

/*
 * Initiate and capture Ping))) Ultrasonic Distance Sensor's round trip echo time.
 * Parameter: pin - Digital I/O pin connected to Ping)))
 * Returns:   duration - The echo time in microseconds 
 * Source:    Ping by David A. Mellis, located in File -> Examples -> Sensors
 * To-Do:     Double check timing against datasheet
 */ 
long Robot::ping(int pin)
{
  long duration;                                   // Variables for calculating distance
  pinMode(pin, OUTPUT);                            // I/O pin -> output
  digitalWrite(pin, LOW);                          // Start low
  delayMicroseconds(2);                            // Stay low for 2 us
  digitalWrite(pin, HIGH);                         // Send 5 us high pulse
  delayMicroseconds(5);                            
  digitalWrite(pin, LOW);                          
  pinMode(pin, INPUT);                             // Set I/O pin to input 
  duration = pulseIn(pin, HIGH, 25000);            // Measure echo time pulse from Ping)))
  return duration;                                 // Return pulse duration
}

int Robot::getHeadingAverage() {

  // setup
  int lastReading = 0;
  int newReading = 0;
  int totalHeadings = 0;
  int avgHeading = 0;
  
  // loop through all the readings
  for(int i=0; i<totalHeadingReadings; i++) {
    // get the reading
    newReading = headings[i];
    
    // make sure we have an old reading
    if(i==0) {
      lastReading = newReading;
    }
    
    if((newReading + 180) < lastReading) {
      newReading = newReading + 360;
    }
    if((newReading - 180) > lastReading) {
      newReading = newReading - 360;
    }
    
    lastReading = newReading;
    totalHeadings = totalHeadings + newReading;

  }
  
  // get the average and make sure we do not end up over 360
  avgHeading = (totalHeadings / totalHeadingReadings) % 360;
  
  // check for negative value
  if(avgHeading < 0) {
    avgHeading = -1 * avgHeading; 
  }

  return avgHeading; 
}

void Robot::getHeading() {
  compass->read();
  // check to see if we need to reset the counter
  if(headingCounter > totalHeadingReadings) {
    headingCounter = 0;
  }
  
  // get the compass reading and add 500 to it
  headings[headingCounter] = compass->heading((LSM303::vector){0,-1,0});

  // increment the counter
  headingCounter++;
  
}

void Robot::calibrateCompass() {
  compass->read();
  
  Serial.print("Compass Readings : ");
  Serial.print("X: ");
  Serial.print((int)compass->m.z);
  Serial.print(" Y: ");
  Serial.print((int)compass->m.z);
  Serial.print(" Z: ");
  Serial.println((int)compass->m.z);
  
  running_min.x = min(running_min.x, compass->m.x);
  running_min.y = min(running_min.y, compass->m.y);
  running_min.z = min(running_min.z, compass->m.z);

  running_max.x = max(running_max.x, compass->m.x);
  running_max.y = max(running_max.y, compass->m.y);
  running_max.z = max(running_max.z, compass->m.z);
  
  Serial.print("M min ");
  Serial.print("X: ");
  Serial.print((int)running_min.x);
  Serial.print(" Y: ");
  Serial.print((int)running_min.y);
  Serial.print(" Z: ");
  Serial.print((int)running_min.z);

  Serial.print(" M max ");  
  Serial.print("X: ");
  Serial.print((int)running_max.x);
  Serial.print(" Y: ");
  Serial.print((int)running_max.y);
  Serial.print(" Z: ");
  Serial.println((int)running_max.z);
}

void Robot::checkEncoders() {
  // get the readings of the encoders
  pinMode(leftEncoderPin, INPUT);
  pinMode(rightEncoderPin, INPUT);
  
  boolean leftEncoder = digitalRead(leftEncoderPin);
  boolean rightEncoder = digitalRead(rightEncoderPin);
  
  // check to see if there has been a change
  if(leftEncoder != leftEncoderLastHigh) {
    leftEncoderLastHigh = leftEncoder;
    leftEncoderCount++;
  }
  if(rightEncoder != rightEncoderLastHigh) {
    rightEncoderLastHigh = rightEncoder;
    rightEncoderCount++;
  }
}
