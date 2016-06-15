package com.valleytg.robotics.bluetooth_henry.collection;

public class Reading {
	
	/**
	 * Cartesian coordinates of this reading
	 */
	private DoublePoint position;
	
	/**
	 * Number of times there has been a successful reading at this
	 * location.  Indicates confidence in accuracy.
	 */
	private Integer hitCount = 0;
	
	private Long createTime = System.currentTimeMillis();
	
	private Long lastHitTime = System.currentTimeMillis();

	public DoublePoint getPosition() {
		return position;
	}

	public void setPosition(DoublePoint position) {
		this.position = position;
	}

	public Integer getHitCount() {
		return hitCount;
	}

	public void setHitCount(Integer hitCount) {
		this.hitCount = hitCount;
	}
	
	public void addHit() {
		this.hitCount++;
		
		// update the last hit time
		this.updateLastHitTime();
	}

	public Long getCreateTime() {
		return createTime;
	}

	public Long getLastHitTime() {
		return lastHitTime;
	}

	public void setLastHitTime(Long lastHitTime) {
		this.lastHitTime = lastHitTime;
	}
	
	public void updateLastHitTime() {
		this.lastHitTime = System.currentTimeMillis();
	}
	
}
