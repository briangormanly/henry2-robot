#ifndef MEMORY_H
#define MEMORY_H

class Memory
{
  public:
  
    // Aequence of turret positions.
    int sequence[11];
    
    // lookup to get the index of the sequence array when you are looking for a particular value.
    int sequenceValueToIndex[11];
    
    // keeps track of which block to average for each turret position.
    int averageBlock[11];
    
    // Holds the number of turret positions
    int elements;
    
    // Array that contains the current known distances in cm reported by the ping))) sensor
    // in each turret position.
    int currentDistances[11];
    
    // Array that contains the computed magnitudes of obsticles at each turret position.
    float currentMagnitudes[11];
    
    // Array contains the average of the surrounding positions (total 2 or 3 positions)
    // end spots (0, 11) are only averaged against thier 1 nieghbor
    // all other spots are averaged with the neighbor to each side.
    float currentAveragedMagnitudes[11];
    
    // turret index
    int turretIndex; 
    
    // Sign of increments
    int sign;   
    
    // holds the degress per turret movement. 
    int degreesTurret; // 18

    // holds the turret angle    
    int theta;  
    
    // current speed of the left wheel
    int leftSpeed;
    
    // current speed of the right wheel
    int rightSpeed;
    
    // keeps track of the distance the robot has gone straight.
    int straightCount;
    
    // keeps track of the current heading
    int currentHeading;
    
    // keeps track of the current range measured by the forward range finder
    int forwardRange;
    
    // keep track of individual compass reading to obtain average.

    // constant representing when an object is too close (legacy)    
    //const int tooCloseCm = 30;  
    
    // Memory constructor
    Memory(void);
    
    // destructor
    ~Memory(void);

    // get the magnitude
    float determineMagnitude(int distance, int theta);
    
    // returns the lowest value index
    int determineLowestMagnitudePath(void);
    
    float determineDirection(void);
    
  private:
    // gets the value of the offset from the center
    float determineOffset(int theta);
    
    float determineAverageMagnitude(int theta, int index);
    
    float determineLeftWeight();
    
    float determineRightWeight();
    
    float determineCenterWeight();
};

#endif
