#ifndef ROBOT_H
#define ROBOT_H 
#include <Servo.h> 
#include <Wire.h>
#include <LSM303.h>

class Robot
{
  
  public:
    // Servo object instances
    Servo servoLeft;                                   
    Servo servoRight;
    Servo servoTurret; 
  
    // I/O Pin constants
    int servoLeftPin;                      
    int servoRightPin;
    int servoTurretPin;
    int pingPin;
    int piezoPin;
    //int led1Pin;
    //int led2Pin;
    
    // encoder input
    double leftEncoderPin;
    double rightEncoderPin;
    
    int analogRangePin;
    
    // encoder counts
    int leftEncoderCount;
    int lastLeftEncoderCount;
    int lastRightEncoderCount;
    int rightEncoderCount;
    boolean leftEncoderLastHigh;
    boolean rightEncoderLastHigh;
    
    // heading values to average out
    int headings[20];
    int headingCounter;
    int totalHeadingReadings;
    
    // constant value representing the amount of time it takes the wheels to turn 1 degree
    int msPerTurnDegree;
    
    // turret information
    int ccwLim;           
    int rtAngle;
    
    // Ping))) conversion constant
    int usTocm; 
    
    // compass
    LSM303 * compass;
    LSM303::vector running_min;
    LSM303::vector running_max;
    

    // Constructor
    Robot();
    
    // destructor
    ~Robot();
    
    void maneuver(int speedLeft, int speedRight);
    void maneuver(int speedLeft, int speedRight, int msTime);
    void turret(int degreeVal);
    int cmDistance();
    int convert(int us, int scalar);
    long ping(int pin);
    void getHeading();
    int getHeadingAverage();
    void calibrateCompass();
    
    // Checks to see if the encoders have advanced and modifies the count
    void checkEncoders();
};
#endif

