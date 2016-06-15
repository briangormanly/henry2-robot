#include "memory.h"

Memory::Memory() {

  // initialize the values of the sequence array
  // sequence starts with the turret facing the r right
  sequence[0] = 0;
  sequence[1] = 2;
  sequence[2] = 4;
  sequence[3] = 6;
  sequence[4] = 8;
  sequence[5] = 10;
  sequence[6] = 9;
  sequence[7] = 7;
  sequence[8] = 5;
  sequence[9] = 3;
  sequence[10] = 1; 
  
  /* on hold */
  sequenceValueToIndex[0] = 0;
  sequenceValueToIndex[1] = 10;
  sequenceValueToIndex[2] = 1;
  sequenceValueToIndex[3] = 9;
  sequenceValueToIndex[4] = 2;
  sequenceValueToIndex[5] = 8;
  sequenceValueToIndex[6] = 3;
  sequenceValueToIndex[7] = 7;
  sequenceValueToIndex[8] = 4;
  sequenceValueToIndex[9] = 6;
  sequenceValueToIndex[10] = 5;  
      
  // set the number of positions for the turret
  elements = 11;
      
  // initialize the currentDistances to all 0's
  for(int i=0; i<elements; i++) {
    currentDistances[i] = 0;
  }
      
  // initialize the currentMagnitudes to all 0's
  for(int i=0; i<elements; i++) {
    currentMagnitudes[i] = 0.0;
  }
  
  // initialize the currentAveragedMagnitudes to all 0's
  for(int i=0; i<elements; i++) {
    currentAveragedMagnitudes[i] = 0.0;
  }
        
  // set the index and sign for the turret
  turretIndex = 0;
  sign = 1;
      
  // Pre-calculate degrees per adjustment of turret servo
  degreesTurret = 180/(sizeof(sequence)/sizeof(int)-1); // 18
      
  // Set up initial turret angle
  theta = -degreesTurret;
  
  straightCount = 0;
      
  // initialize the current heading
  currentHeading = 0;
  
  // initialize the value of the forward range finder
  forwardRange = 0;
  
  // set the initial wheel speeds
  leftSpeed = 50;
  rightSpeed = 50;
  
}

// destructor implementation
Memory::~Memory(void)
{
  degreesTurret = 0;
}



/**
 * gets the magnitude associated with a registered distance.
 * mag = 1 / distance
 * possible range 1 to .002??
 */
float Memory::determineMagnitude(int distance, int theta) 
{
  // initialize the magnitude
  float magnitude = 0.000;
  
  // set the magnitude scaled to the distance of the ping response
  magnitude = (float) 10 / distance;
  
  // scale the magnitude based on the offset determined by the position of the turret.
  float offset = determineOffset(theta);
  if(offset > 0) {
    magnitude = (float) magnitude / offset;
  }
  
  return magnitude;
}



/**
 * Offset is determined by halfing the degrees the turret is +/- from 90 (center)
 */
float Memory::determineOffset(int theta)
{
  // set offest to assume 90 degrees center theta
  float offset = 0.000;
  
  // check the theta
  if(theta > 100) {
    offset = (float) (theta - 90) / 2;
  }
  else if (theta < 90) {
    offset = (float) (90 - theta) / 2;
  }
  else {
     offset = 1; 
  }
  return offset;
}


float Memory::determineDirection() 
{
  
  float leftWeight = determineLeftWeight();
  float rightWeight = determineRightWeight();
  float centerWeight = determineCenterWeight();
  
  float difference = 0.0;
  
  /*
  float testDiff = 0.0;
  if(leftWeight > rightWeight) {
    testDiff = leftWeight - rightWeight;
  }
  else if(rightWeight > leftWeight) {
    testDiff = rightWeight - leftWeight; 
  }
  
  
  if(centerWeight > leftWeight && centerWeight > rightWeight) {
    // see witch side is higher and steer away
    if(leftWeight > rightWeight) {
      difference = centerWeight + leftWeight;
      
    }
    else {
      difference = centerWeight + rightWeight;
      difference = -difference;
    }
    
  }
  */
  if(leftWeight > rightWeight) {
    // left has larger magnitude
    difference = (leftWeight + centerWeight) - rightWeight;
  }
  else if(rightWeight > leftWeight) {
    // right has larger magnitude
    difference = (rightWeight + centerWeight) - leftWeight;
    difference = -difference;
  }
  else {
    // everything is the same... keep going forward
    difference = 0.0;
  }
  
  
  return difference * 1500;
}

float Memory::determineLeftWeight() 
{
  return (float) (currentMagnitudes[5] + currentMagnitudes[6] + currentMagnitudes[4]  + currentMagnitudes[7] + currentMagnitudes[3]);
}

float Memory::determineRightWeight() 
{
  return (float) (currentMagnitudes[0] + currentMagnitudes[10] + currentMagnitudes[1]  + currentMagnitudes[9] + currentMagnitudes[2]);
}

float Memory::determineCenterWeight()
{
  float rangeMag = 0;
  rangeMag = rangeMag * .7;
  
  if(rangeMag <= 0) {
     rangeMag = 0;
  }
  
  return (float) (rangeMag + currentMagnitudes[8]);
}







/**
 * LEGACY - used for the non-potential-feilds total method. 
 *
 * Looks through the magnitude array and finds the angle that has the 
 * lowest magnitude and returns the lowest inext that points in that 
 * direction. 
 */
int Memory::determineLowestMagnitudePath() {
  float lowestValue = 0;
  int lowestIndex = -1;
  for(int i=0; i<elements; i++) {
    if(currentDistances[i] > lowestValue) {
      lowestValue = currentDistances[i];
      lowestIndex = i;
    }
  }
  
  return lowestIndex;
}


/**
 * Detirmine the average of the 
 
 * ON HOLD
 */
float Memory::determineAverageMagnitude(int theta, int index)
{
  int p1 = -1;
  int p2 = -1;
  int p3 = -1;

  if(averageBlock[index] == 0) {
    // start block average, manual
    p1 = 0;
    p2 = 10;
    
  }
  else if(averageBlock[index] == 10) {
    // end block average, manual
    p1 = 5;
    p2 = 6;
  }
  else {
    // middle block average (p1 = index, p2 = 10-index, p3 = 10-index+1)
    p1 = averageBlock[index];
    p2 = 10 - averageBlock[index];
    p3 = 10 - averageBlock[index] + 1;
  } 
  
  
}
