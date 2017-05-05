package vanetsim.scenario.events;

import java.awt.Color;
import java.text.ParseException;
import java.util.ArrayList;

import vanetsim.ErrorLog;
import vanetsim.localization.Messages;
import vanetsim.map.MapHelper;
import vanetsim.map.Street;

/**
 * This class represents an event which starts blocking on a street.
 */
public final class StartBlocking extends Event{
	
	/** The affected street. */
	private final Street affectedStreet_; 
	
	/** The position on the affected street (measured from StartPoint). */
	private final double affectedStreetPos_;
	
	/** The affected street direction. <code>0</code> for both directions, <code>1</code> for startNode to endNode, <code>1</code> for endNode to startNode */
	private final int affectedDirection_;
	
	/** The amount of affected lanes. Will be used for both directions. */
	private final int affectedLanes_;
	
	/** The event connected to this which ends blocking. */
	private StopBlocking stopBlockingEvent_ = null;
	
	/** The blocking objects. */
	private ArrayList<BlockingObject> blockingObjects_ = null;
	
	/**
	 * Constructor.
	 * 
	 * @param time the time in milliseconds when this event gets active
	 * @param x 		the x coordinate of this event
	 * @param y 		the y coordinate of this event
	 * @param direction The affected street direction. <code>0</code> for both directions,
	 * <code>1</code> for startNode to endNode, <code>1</code> for endNode to startNode
	 * @param lanes 	The amount of affected lanes (should be at least 1). Will be used for both directions.
	 * Use <code>Integer.MAX_VALUE</code> to use all lanes.
	 * 
	 * @throws ParseException an exception indicating that creation wasn't successful
	 */
	public StartBlocking(int time, int x, int y, int direction, int lanes) throws ParseException{
		time_ = time;
		color_ = Color.red;
		affectedDirection_ = direction;
		affectedLanes_ = lanes;
		int[] nearestpoint = new int[2];
		affectedStreet_ = MapHelper.findNearestStreet(x,y, 10000, new double[1], nearestpoint);	// search in 100m radius for the starting street
		if(affectedStreet_ != null){
			// position on street measured from startNode to nearestpoint
			long tmp1 = affectedStreet_.getStartNode().getX() - nearestpoint[0];
			long tmp2 = affectedStreet_.getStartNode().getY() - nearestpoint[1];
			affectedStreetPos_ = Math.sqrt(tmp1 * tmp1 + tmp2 * tmp2); 	// Pythagorean theorem: a^2 + b^2 = c^2	
		} else throw new ParseException(Messages.getString("StartBlocking.snappingFailed"),0); //$NON-NLS-1$
	}
	
	/**
	 * Returns a descriptive text for display in the GUI.
	 * 
	 * @return the text
	 * 
	 * @see vanetsim.scenario.events.Event#getText()
	 */
	public String getText(){
		String tmp;
		if(affectedDirection_ == 0) tmp = Messages.getString("StartBlocking.both"); //$NON-NLS-1$
		else if(affectedDirection_ == 1) tmp = Messages.getString("StartBlocking.fromStartNode"); //$NON-NLS-1$
		else tmp = Messages.getString("StartBlocking.fromEndNode"); //$NON-NLS-1$
		return("<html>" + Messages.getString("StartBlocking.blockingStreet") + affectedStreet_.getName() + "<br>" + Messages.getString("StartBlocking.direction") + tmp + Messages.getString("StartBlocking.lanes") + affectedLanes_);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}
	
	
	/**
	 * Compare to another event.
	 * 
	 * @param other the other event
	 * 
	 * @return <code>-1</code> if <code>other</code> is larger, <code>1</code> if it's smaller and 0 if both are equal.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Event other){
		if(other == this) return 0;
		else if(other.getTime() > time_) return -1;
		else if (other.getTime() < time_) return 1;
		else{
			if(other.getClass() == StopBlocking.class) return -1;
			else{
				if(other.hashCode() > hashCode()) return -1;
				else if(other.hashCode() < hashCode()) return 1;
				else {
					ErrorLog.log(Messages.getString("StartBlocking.eventCompareError") , 7,StartBlocking.class.getName(), "compareTo", null);  //$NON-NLS-1$//$NON-NLS-2$
					return 0;
				}
			}
		}
	}
	

	
	/**
	 * Execute the task.
	 * 
	 * @see vanetsim.scenario.events.Event#execute()
	 */
	public void execute(){
		EventList.getInstance().addCurrentBlockings(this);
		if(blockingObjects_ == null) blockingObjects_ = new ArrayList<BlockingObject>(2);
		int lanes = Math.min(affectedLanes_, affectedStreet_.getLanesCount()+1);	//don't create more blockings than street has lanes.
		if(affectedDirection_ == 0 || affectedDirection_ == 1){
			for(int i = 1; i <= lanes; ++i){
				blockingObjects_.add(new BlockingObject(i, true, affectedStreet_, affectedStreetPos_));
			}
		}
		if(!affectedStreet_.isOneway() && (affectedDirection_ == 0 || affectedDirection_ == -1)){
			for(int i = 1; i <= lanes; ++i){
				blockingObjects_.add(new BlockingObject(i, false, affectedStreet_, affectedStreetPos_));
			}
		}
	}

	/**
	 * Destroys itself.
	 * 
	 * @see vanetsim.scenario.events.Event#destroy()
	 */
	public void destroy() {
	}
	
	/**
	 * Gets the x coordinate.
	 * 
	 * @return the x coordinate
	 */
	public int getX(){
		return (int) Math.round(-0.5 + affectedStreet_.getStartNode().getX() + ((affectedStreet_.getEndNode().getX() - affectedStreet_.getStartNode().getX())*affectedStreetPos_ / affectedStreet_.getLength()));		// -0.5 to surely stay within int!
	}
	
	/**
	 * Gets the y coordinate.
	 * 
	 * @return the y coordinate
	 */
	public int getY(){
		return (int) Math.round(-0.5 + affectedStreet_.getStartNode().getY() + ((affectedStreet_.getEndNode().getY() - affectedStreet_.getStartNode().getY())*affectedStreetPos_ / affectedStreet_.getLength()));
	}
	
	/**
	 * Gets the event which ends this blocking.
	 * 
	 * @return the event or <code>null</code> if this event exists forever
	 */
	public StopBlocking getStopBlockingEvent(){
		return stopBlockingEvent_;
	}
	
	/**
	 * Sets the event which ends this blocking.
	 * 
	 * @param event the event which ends blocking
	 */
	public void setStopBlockingEvent(StopBlocking event){
		stopBlockingEvent_ = event;
	}
	
	/**
	 * Gets the street where blocking is done.
	 * 
	 * @return the street affected by this event
	 */
	public Street getStreet(){
		return affectedStreet_;
	}	
	
	/**
	 * Gets the affected direction.
	 * 
	 * @return <code>0</code> to indicate that this event is for both directions,<code>1</code> that it's only from
	 * startNode to endNode, <code>1</code> that it's only from endNode to startNode
	 * */
	public int getAffectedDirection(){
		return affectedDirection_;
	}
	
	/**
	 * Gets the blocking objects.
	 * 
	 * @return the blocking objects
	 */
	public ArrayList<BlockingObject> getBlockingObjects(){
		return blockingObjects_;
	}
	
	/**
	 * Gets the amount of lanes (per direction) affected by this event.
	 * 
	 * @return the amount of lanes
	 */
	public int getAffectedLanes(){
		return affectedLanes_;
	}
}