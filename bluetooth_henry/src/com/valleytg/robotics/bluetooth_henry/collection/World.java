package com.valleytg.robotics.bluetooth_henry.collection;

import java.util.ArrayList;

public class World {
	
	/**
	 * Moving Boundaries of the known world
	 */
	private Double mMaxXSpace = 480.0;
	private Double mMaxYSpcae = 800.0;
	private Double mCenterX = mMaxXSpace / 2;
	private Double mCenterY = mMaxYSpcae / 2;
	
	/**
	 * Boundaries of the known world
	 */
	private Double maxXSpace = 240.0;
	private Double minXSpace = -240.0;
	private Double maxYSpace = 240.0;
	private Double minYSpace = -240.0;
	
	private Integer mapZoomFactor = 1;
	
	/**
	 * Resolution of world
	 * increasing these values results in larger, less accurate groupings
	 * of readings.
	 */
	private Double xResolution = 5.0;
	private Double yResolution = 5.0;
	
	/**
	 * Lifespan of unproven readings
	 */
	private Integer oneHitLifeSpan = 10000; // 10 seconds
	private Integer twoHitLifeSpan = 1800000; // 1800 seconds
	
	/**
	 * Readings
	 */
	private ArrayList<Reading> readings = new ArrayList<Reading>();
	
	/**
	 * Runtime - records the time that this world was created
	 */
	private Long createTime = System.currentTimeMillis();
	
	
	/**
	 * Constructor
	 */
	public World() {
		
	}
	
	
	/**
	 * Adds reading or updates existing reading count. Based on whether there
	 * is an existing reading within the resolution threshold set in 
	 * xResolution and yResolution.  
	 * @param reading - coordinates of candidate reading.
	 */
	public void newReading(DoublePoint reading) {
		// scan for an existing reading within the set resolution
		int flag = 0;
		for(Reading existingReading : readings) {
			if(reading.x >= (existingReading.getPosition().x - xResolution) && 
					reading.x <= (existingReading.getPosition().x + xResolution) && 
					reading.y >= (existingReading.getPosition().y - yResolution) && 
					reading.y <= (existingReading.getPosition().y + yResolution) &&
					flag == 0) {
				
				// set the flag
				flag++;
				//System.out.println("$$$$$$$$$$$$$$$$$$$$ Reading match  : " + reading.x + " y " + reading.y + " xRes " + existingReading.getPosition().x + " yRes " + existingReading.getPosition().y);
				
				// Increment the hitCount of this reading
				existingReading.addHit();
			}
		}
		
		// if there were no existing readings create a new one
		if(flag == 0) {
			// create the Reading
			Reading newReading = new Reading();
			newReading.setPosition(reading);
			newReading.setHitCount(1);
			
			// add the new reading to the existing ones
			this.readings.add(newReading);
		}
	}
	
	/**
	 * Scans the existing readings to remove ones that have not been
	 * proven by obtaining a certain number of hits or lasted for a 
	 * set amount of time (see oneHitLifeSpan and twoHitLifeSpan)
	 * 
	 * Should be run periodically to collect the reading garbage.
	 */
	public void removeUnprovenReadings() {
		ArrayList<Reading> readingsToRemove = new ArrayList<Reading>();
		System.out.println("@@@@@@@@ Removing readings... @@@@@@@@@@@@@");
		for(Reading thisReading : this.readings) {
			if(thisReading.getHitCount() == 1) {
				// one or less hits check the time
				if(System.currentTimeMillis() > (thisReading.getLastHitTime() + oneHitLifeSpan)) {
					// expired
					readingsToRemove.add(thisReading);
				}
			}
			else if(thisReading.getHitCount() == 2) {
				// one or less hits check the time
				if(System.currentTimeMillis() > (thisReading.getLastHitTime() + twoHitLifeSpan)) {
					// expired
					readingsToRemove.add(thisReading);
				}
			}
		}
		
		// remove the readings that have to go
		System.out.println("Removing .. " + readingsToRemove.size() + " readings........");
		this.readings.removeAll(readingsToRemove);
	}
	

	public Double getMaxXSpace() {
		return maxXSpace;
	}

	public void setMaxXSpace(Double maxXSpace) {
		this.maxXSpace = maxXSpace;
	}

	public Double getMinXSpace() {
		return minXSpace;
	}

	public void setMinXSpace(Double minXSpace) {
		this.minXSpace = minXSpace;
	}

	public Double getMaxYSpace() {
		return maxYSpace;
	}

	public void setMaxYSpace(Double maxYSpace) {
		this.maxYSpace = maxYSpace;
	}

	public Double getMinYSpace() {
		return minYSpace;
	}

	public void setMinYSpace(Double minYSpace) {
		this.minYSpace = minYSpace;
	}

	public ArrayList<Reading> getReadings() {
		return readings;
	}

	public void setReadings(ArrayList<Reading> readings) {
		this.readings = readings;
	}



	public Double getyResolution() {
		return yResolution;
	}



	public void setyResolution(Double yResolution) {
		this.yResolution = yResolution;
	}



	public Double getxResolution() {
		return xResolution;
	}



	public void setxResolution(Double xResolution) {
		this.xResolution = xResolution;
	}


	public Integer getOneHitLifeSpan() {
		return oneHitLifeSpan;
	}


	public void setOneHitLifeSpan(Integer oneHitLifeSpan) {
		this.oneHitLifeSpan = oneHitLifeSpan;
	}


	public Integer getTwoHitLifeSpan() {
		return twoHitLifeSpan;
	}


	public void setTwoHitLifeSpan(Integer twoHitLifeSpan) {
		this.twoHitLifeSpan = twoHitLifeSpan;
	}


	public Integer getMapZoomFactor() {
		return mapZoomFactor;
	}


	public void setMapZoomFactor(Integer mapZoomFactor) {
		this.mapZoomFactor = mapZoomFactor;
	}


	public Double getmMaxXSpace() {
		return mMaxXSpace;
	}


	public void setmMaxXSpace(Double mMaxXSpace) {
		this.mMaxXSpace = mMaxXSpace;
	}


	public Double getmMaxYSpcae() {
		return mMaxYSpcae;
	}


	public void setmMaxYSpcae(Double mMaxYSpcae) {
		this.mMaxYSpcae = mMaxYSpcae;
	}


	public Double getmCenterX() {
		return mCenterX;
	}


	public void setmCenterX(Double mCenterX) {
		this.mCenterX = mCenterX;
	}


	public Double getmCenterY() {
		return mCenterY;
	}


	public void setmCenterY(Double mCenterY) {
		this.mCenterY = mCenterY;
	}


	public Long getCreateTime() {
		return createTime;
	}


	public void setCreateTime(Long createTime) {
		this.createTime = createTime;
	}

}
