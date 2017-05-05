package vanetsim.scenario.events;

import vanetsim.map.Street;
import vanetsim.scenario.LaneObject;

public class BlockingObject extends LaneObject{
	
	/**
	 * Instantiates a new blocking object.
	 * 
	 * @param lane the lane number (1..n)
	 * @param direction the direction
	 * @param street the street
	 * @param position the position on the street
	 */
	public BlockingObject(int lane, boolean direction, Street street, double position){
		curLane_ = lane;
		curDirection_ = direction;
		curStreet_ = street;
		if(curLane_ < 1) curLane_ = 1;
		else if (curLane_ > curStreet_.getLanesCount()) curLane_ = curStreet_.getLanesCount();
		curPosition_ = position;
		curStreet_.addLaneObject(this, curDirection_);
	}
	
	/**
	 * Remove this BlockingObject from it's current lane.
	 */ 
	public void removeFromLane(){
		curStreet_.delLaneObject(this, curDirection_);
	}
}