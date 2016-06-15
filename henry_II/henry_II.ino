
// includes
#include "memory.h"
#include "robot.h"
#include "world.h"
#include <Servo.h> 
#include <Wire.h>
#include <LSM303.h>

// Max incoming serial message size
#define maxMessageSize 32

/**
 * Memory 
 */
Memory * pMyMemory;

/**
 * Robot 
 */
Robot * pMyRobot;

/**
 * World
 */
World * pMyWorld;

/* Possible current states of the robot
 * - ROAMING : Robot is moving and mapping while avoiding obsticles with no
 *  direct distination.
 * - WAITING : Robot is on but not moving.  Awaiting change of state.
 * - TRAVELING : Robot is traveling to a destination in 2D space.
 * - AT_DESTINATION : Robot has reached the x,y co-ordinate or goal location.
 */
enum STATES
{
  WAITING = 0,
  ROAMING = 1,
  TRAVELING = 2,
  AT_DESTINATION = 3
};

// contians the current state of the robot
int currentState;

/**
 * Buffer that holds incoming data on the 
 * serial line.
 */
char  readSerialBuffer[maxMessageSize];



typedef struct Timer 
{
  unsigned long start;
  unsigned long timeout;
};

char TimerExpired ( struct Timer * timer ) 
{
  if ( millis () > timer->start + timer->timeout ) 
    return true;

  return false;    
}

void TimerStart ( struct Timer * timer ) 
{
  timer->start = millis ( );
}

// Timer to manage the delay before checking turn necessity 
Timer timerTurn = { 
  0, 350 };

// timer to manage the delay on head rotation
Timer timerRotateHead = { 
  0, 100 };

// timer that manages how often check if ending a turn
Timer timerForward = {
  0, 50};

// timer that keeps the current heading current.
Timer timerHeading = {
  0, 20};
  
// timer to retreive the averages of the heading
Timer timerHeadingAverage = {
  0, 110};

// timer to time analog range finder value colletion
Timer timerRange = {
  0, 175};

// timer to send the android device data
Timer timerTransmit = {
  0, 100};

// timer to check recieved data on serial communiction
Timer timerRecieve = {
  0, 350};

// timer to get the encoder data
Timer timerEncoder = {
  0,80};

// timer to update the world view
Timer timerWorld = {
  0, 200};

unsigned long startTurn;
float currentMag = 0.0;



void setup() {

  // initialize the robot hardware
  //myRobot = Robot();

  // set baud rate for the serial connection
  Serial.begin(115200);

  // initialize the memory
  pMyMemory= new Memory();

  // initialize the robot hardware
  pMyRobot = new Robot();

  // initialize the world
  pMyWorld = new World();

  //tone(pMyRobot->piezoPin, 500, 500);                                  // Play tone for 1/2 second
  delay(1000);                                                         // Delay to finish tone                                                // Open serial connection

  pMyRobot->servoLeft.attach(pMyRobot->servoLeftPin);                  // Attach left signal to pin 13
  pMyRobot->servoRight.attach(pMyRobot->servoRightPin);                // Attach right signal to pin 12
  pMyRobot->servoTurret.attach(pMyRobot->servoTurretPin);              // Attach turret signal to pin 12

  pMyRobot->maneuver(0, 0);                                  // Stay still for 1 second
  delay(1000);
  pMyRobot->turret(0);                                       // Set turret to 0 degrees
  delay(1000);

  // look to see if the motors are centered
  pMyRobot->maneuver(0,0);
  delay(1000);

  // set the initial state to waiting
  currentState = WAITING;

}

void loop() {
  /**
   * Task scheduler.
   * All tasks are managed here.  No task should use a delay
   * under any circumstances.
   *
   * The current state of the robot defines which tasks will be
   * run.
   */
  switch(currentState) {
  case WAITING:
    allStop();
    break;

  case ROAMING:

    // see if we need to turn
    if ( TimerExpired ( & timerTurn ) )
    {
      taskTurn ( ); 
      TimerStart ( & timerTurn );
    }

    // rotate the turret
    if ( TimerExpired ( & timerRotateHead ) ) 
    {
      taskRotateHead( );
      TimerStart ( & timerRotateHead );
    }

    // go forward 
    if(TimerExpired(& timerForward)) 
    {
      taskForward();
      TimerStart (& timerForward);
    }

    break;

  case TRAVELING:
    // see if we need to turn
    if ( TimerExpired ( & timerTurn ) )
    {
      taskTurn ( ); 
      TimerStart ( & timerTurn );
    }

    // rotate the turret
    if ( TimerExpired ( & timerRotateHead ) ) 
    {
      taskRotateHead( );
      TimerStart ( & timerRotateHead );
    }

    // go forward 
    if(TimerExpired(& timerForward)) 
    {
      taskForward();
      TimerStart (& timerForward);
    }


    break;

  case AT_DESTINATION:
    allStop();
    break;
  }

  // the following tasks always run


  // get the current compass heading
  if ( TimerExpired ( & timerHeading ) ) 
  {
    taskGetHeading();
    TimerStart ( & timerHeading );
  }
  
  // get the current compass heading average
  if ( TimerExpired ( & timerHeadingAverage ) ) 
  {
    taskGetHeadingAverage();
    TimerStart ( & timerHeadingAverage );
  }

  // get the current opitical range finder distance
  if ( TimerExpired ( & timerRange ) ) 
  {
    taskGetRange();
    TimerStart ( & timerRange );
  }

  // transmit data to the android device
  if ( TimerExpired ( & timerTransmit ) ) 
  {
    taskTransmitData();
    TimerStart ( & timerTransmit );
  }

  // recieve data from the android device
  if ( TimerExpired ( & timerRecieve ) ) 
  {
    taskRecieveData();
    TimerStart ( & timerRecieve );
  }

  // check the encoders for rotation
  if ( TimerExpired ( & timerEncoder ) ) 
  {
    taskCheckEncoders();
    TimerStart ( & timerEncoder );
  }

  // update the robots current location
  if ( TimerExpired ( & timerWorld ) ) 
  {
    taskUpdateLoction();
    TimerStart ( & timerWorld );
  }

}

/**
 * Stop all servos!
 */
void allStop(void) {
  pMyMemory->leftSpeed = 0;
  pMyMemory->rightSpeed = 0;

  pMyRobot->maneuver(pMyMemory->leftSpeed, pMyMemory->rightSpeed);
}


void taskForward(void) 
{

  float absTime = currentMag;
  if(absTime < 0) {
    absTime = -absTime;
  }
  float totalTime = startTurn + absTime;
  //Serial.println(totalTime);
  //Serial.println(millis());

  if(totalTime > millis()) {
    // keep turning
    //Serial.println('T');
  }
  else {

    // increament the straight count
    pMyMemory->straightCount++;

    // add the straight count to the speed
    int boost = (pMyMemory->straightCount * 5);
    if(boost > 100) {
      boost = 100;
    }

    pMyMemory->leftSpeed = 50 + boost;
    pMyMemory->rightSpeed = 50 + boost;

    pMyRobot->maneuver(pMyMemory->leftSpeed, pMyMemory->rightSpeed);
    //pMyRobot->maneuver(200, 200);


    //Serial.println('F');
  }
}

/**
 * Get the current heading of the LSM303DLHC compass.
 */
void taskGetHeading() {
  pMyRobot->getHeading();
  //pMyRobot->calibrateCompass();

}

/**
 * Get the runing average of the LSM303DLHC compass
 */
void taskGetHeadingAverage() {
  pMyMemory->currentHeading = pMyRobot->getHeadingAverage();
}

/**
 * Get the value of the front range finder.
 */
void taskGetRange() {
  pMyMemory->forwardRange = analogRead(pMyRobot->analogRangePin);

}

/**
 *  Check the status of the wheel encoders and update the counts as necessary.
 */
void taskCheckEncoders() {
  pMyRobot->checkEncoders();
}

/**
 *  Use the current heading and encoder counts to update the robots precieved location
 * in x,y space.
 */
void taskUpdateLoction() {
  // update the current x,y in the world.
  pMyWorld->updateXY(pMyMemory->currentHeading, pMyRobot->leftEncoderCount, pMyRobot->lastLeftEncoderCount, pMyRobot->rightEncoderCount, pMyRobot->lastRightEncoderCount);

  // set the last Encoder count to the current.
  pMyRobot->lastLeftEncoderCount = pMyRobot->leftEncoderCount;
  pMyRobot->lastRightEncoderCount = pMyRobot->rightEncoderCount;

}

void taskTurn ( void ) 
{
  float thisMag = pMyMemory->determineDirection();
  //Serial.println(thisMag);
  
  // check to see if there is an attractive force
  if(currentState == TRAVELING) {
    float targetResult = pMyWorld->goTowardsTargetXY(pMyMemory->currentHeading);
    if(targetResult > 0) {
      thisMag += targetResult;
    }
    else {
      // right
      thisMag += targetResult;
    }
  }
  
  
  //Serial.println(thisMag);
  if(thisMag > 40) {
    pMyMemory->straightCount = 0;
    startTurn = millis();
    currentMag = thisMag;

    // go right CW
    if(currentMag > 300) {

      //set speet
      pMyMemory->leftSpeed = 50;
      pMyMemory->rightSpeed = -50;

      // turn hard
      pMyRobot->maneuver(pMyMemory->leftSpeed, pMyMemory->rightSpeed);
      currentMag = currentMag / 3;
    }
    else if(currentMag > 170) {
      
      //set speet
      pMyMemory->leftSpeed = 100;
      pMyMemory->rightSpeed = 0;

      pMyRobot->maneuver(pMyMemory->leftSpeed, pMyMemory->rightSpeed);
      currentMag = currentMag / 1.5;
    }
    else {
      
      // light turn
      pMyMemory->leftSpeed = 100;
      pMyMemory->rightSpeed = 25;

      pMyRobot->maneuver(pMyMemory->leftSpeed, pMyMemory->rightSpeed);
    }
  }
  else if(thisMag < -40) {
    pMyMemory->straightCount = 0;
    startTurn = millis();
    currentMag = thisMag;

    // go left CCW  
    if(currentMag < -300) {
      
      //set speet
      pMyMemory->leftSpeed = -50;
      pMyMemory->rightSpeed = 50;

      pMyRobot->maneuver(pMyMemory->leftSpeed, pMyMemory->rightSpeed);

      currentMag = currentMag / 3;
    }
    else if(currentMag < -170) {
 
      //set speed
      pMyMemory->leftSpeed = 0;
      pMyMemory->rightSpeed = 100;

      pMyRobot->maneuver(pMyMemory->leftSpeed, pMyMemory->rightSpeed);

      currentMag = currentMag / 1.5;
    }
    else {
      
      //set speet
      pMyMemory->leftSpeed = 25;
      pMyMemory->rightSpeed = 100;

      pMyRobot->maneuver(pMyMemory->leftSpeed, pMyMemory->rightSpeed);
    }
  }
  else {
    pMyRobot->maneuver(pMyMemory->leftSpeed, pMyMemory->rightSpeed);
  }
}

void taskRotateHead(void) 
{

  // get the current distance reading and store it in the correct position of the currentDistances
  pMyMemory->currentDistances[pMyMemory->turretIndex] = pMyRobot->cmDistance();

  // get the magitude of the distance
  pMyMemory->currentMagnitudes[pMyMemory->turretIndex] = 
    pMyMemory->determineMagnitude(pMyMemory->currentDistances[pMyMemory->turretIndex], pMyMemory->theta);
    
  // check the turret index for overflow and reset
  if(pMyMemory->turretIndex == 10)                                      // If turret at max, go back to zero 
  {
    pMyMemory->turretIndex = -1;
  }
  
  // increment the turret position
  pMyMemory->turretIndex++;
  
  // get the next turret position in degrees 
  pMyMemory->theta = pMyMemory->sequence[pMyMemory->turretIndex] * pMyMemory->degreesTurret; 

  // move the turret to the next position
  pMyRobot->turret(pMyMemory->theta);
  
  
}


void taskRecieveData(void) {
  // set the array index pointer
  short index = 0;

  char inChar;
  if(Serial.available() > 0)
  {
    while(Serial.available() > 0 && index < maxMessageSize-1)
    {
      inChar = Serial.read();
      readSerialBuffer[index] = inChar;
      index++;
      readSerialBuffer[index] = '\0'; // Add a null at the end
    }
  }

  // get the size of our current buffer
  int bufferSize = sizeof(readSerialBuffer) / sizeof(int);

  // debug the string
  //for(int i=0; i<bufferSize; i++) {
  //  Serial.print(readSerialBuffer[i]);
  //}

  /* 
   *we have the data, parse it for the information we want
   */
  // check for new robot state
  for(int i=0; i<bufferSize; i++) {
    // check for the first character 
    if(readSerialBuffer[i] == 'S') {
      i++;
      // check for the second character
      if(readSerialBuffer[i] == 'T') {
        i++;
        
        int value = (int) readSerialBuffer[i];
        
        if(value == 48) {
          currentState = WAITING;
        }
        else if(value == 49) {
          currentState = ROAMING;
        }

        else if(value == 50) {
          currentState = TRAVELING;
          // temp hard code destination
          pMyWorld->targetX = 5.0;
          pMyWorld->targetY = 30.0;
        }


      }
    }
    else if(readSerialBuffer[i] == 'C') {
      i++;
      // check for the second character
      if(readSerialBuffer[i] == 'X') {
        i++;

        int value = (int) readSerialBuffer[i];

        // going to have to deal with getting more then one character
        // going to have to deal with converting the 'chars' retrieved into ints for the current x,y destinations.
        // for now i am hard coding a destination in the change to state traveling.

      }
      else if(readSerialBuffer[i] == 'Y') {

      }
    }
  }

  // clear the buffer
  for(int i=0; i<bufferSize; i++) {
    readSerialBuffer[i] = '\0';
  }
}


void taskTransmitData(void)
{
  // start the xml
  Serial.print("HE");
  Serial.print(pMyMemory->currentHeading);
  Serial.print("/HE");
  Serial.print("FR");
  Serial.print(pMyMemory->forwardRange);
  Serial.print("/FR");
  Serial.print("LS");
  Serial.print(pMyMemory->leftSpeed);
  Serial.print("/LS");
  Serial.print("RS");
  Serial.print(pMyMemory->rightSpeed);
  Serial.print("/RS");
  Serial.print("TI");
  Serial.print(pMyMemory->turretIndex);
  Serial.print("/TI");
  Serial.print("CD");
  outputCurrentDistances();
  Serial.print("/CD");
  Serial.print("CM");
  outputCurrentMagnitudes();
  Serial.print("/CM");
  Serial.print("XP");
  Serial.print(pMyWorld->x);
  Serial.print("/XP");
  Serial.print("YP");
  Serial.print(pMyWorld->y);
  Serial.print("/YP");
  Serial.print("XT");
  Serial.print(pMyWorld->targetX);
  Serial.print("/XT");
  Serial.print("YT");
  Serial.print(pMyWorld->targetY);
  Serial.print("/YT");
  Serial.print("CS");
  Serial.print(currentState);
  Serial.print("/CS");

  Serial.println();

}


void taskTransmitDataXML(void)
{
  // start the xml
  Serial.print("<transmission>");
  Serial.print("<heading>");
  Serial.print(pMyMemory->currentHeading);
  Serial.print("</heading>");
  Serial.print("<forwardRange>");
  Serial.print(pMyMemory->forwardRange);
  Serial.print("</forwardRange>");
  Serial.print("<leftSpeed>");
  Serial.print(pMyMemory->leftSpeed);
  Serial.print("</leftSpeed>");
  Serial.print("<rightSpeed>");
  Serial.print(pMyMemory->rightSpeed);
  Serial.print("</rightSpeed>");
  Serial.print("<turretIndex>");
  Serial.print(pMyMemory->turretIndex);
  Serial.print("</turretIndex>");
  Serial.print("<currentDistances>");
  outputCurrentDistances();
  Serial.print("</currentDistances>");
  Serial.print("<currentMagnitudes>");
  outputCurrentMagnitudes();
  Serial.print("</currentMagnitudes>");
  Serial.print("</transmission>");
  Serial.println();

  //Serial.println(pMyMemory->currentHeading);

  //outputCurrentDistances();

}

/**
 * Outputs the current distances in the correct order to a serial device
 */
void outputCurrentDistances() 
{
  Serial.print(pMyMemory->currentDistances[0]);
  Serial.print("_");
  Serial.print(pMyMemory->currentDistances[1]);
  Serial.print("_");
  Serial.print(pMyMemory->currentDistances[2]);
  Serial.print("_");
  Serial.print(pMyMemory->currentDistances[3]);
  Serial.print("_");
  Serial.print(pMyMemory->currentDistances[4]);
  Serial.print("_");
  Serial.print(pMyMemory->currentDistances[5]);
  Serial.print("_");
  Serial.print(pMyMemory->currentDistances[6]);
  Serial.print("_");
  Serial.print(pMyMemory->currentDistances[7]);
  Serial.print("_");
  Serial.print(pMyMemory->currentDistances[8]);
  Serial.print("_");
  Serial.print(pMyMemory->currentDistances[9]);
  Serial.print("_");
  Serial.print(pMyMemory->currentDistances[10]);
}

/**
 * Outputs the current magnitudes in the correct order to a serial device
 */
void outputCurrentMagnitudes() 
{
  Serial.print(pMyMemory->currentMagnitudes[0], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentMagnitudes[1], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentMagnitudes[2], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentMagnitudes[3], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentMagnitudes[4], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentMagnitudes[5], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentMagnitudes[6], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentMagnitudes[7], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentMagnitudes[8], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentMagnitudes[9], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentMagnitudes[10], 4);
}

/**
 * Outputs the current averaged magnitudes in the correct order to a serial device
 * this is current not in use.
 */
void outputCurrentAveragedMagnitudes() 
{
  Serial.print(pMyMemory->currentAveragedMagnitudes[0], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentAveragedMagnitudes[10], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentAveragedMagnitudes[1], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentAveragedMagnitudes[9], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentAveragedMagnitudes[2], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentAveragedMagnitudes[8], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentAveragedMagnitudes[3], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentAveragedMagnitudes[7], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentAveragedMagnitudes[4], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentAveragedMagnitudes[6], 4);
  Serial.print("_");
  Serial.print(pMyMemory->currentAveragedMagnitudes[5], 4);
}

