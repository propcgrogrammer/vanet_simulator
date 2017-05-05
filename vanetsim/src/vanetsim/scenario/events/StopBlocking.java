
package vanetsim.scenario.events;

import java.awt.Color;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;

import vanetsim.ErrorLog;
import vanetsim.localization.Messages;

/**
 * This class represents an event which ends blocking on a street ("unblock").
 */
public final class StopBlocking extends Event{
	
	/** The event which starts the blocking. */
	private final StartBlocking startBlockingEvent_; 
	
	/**
	 * Constructor.
	 * 
	 * @param time	the time in milliseconds when this event gets active
	 * @param x		the x coordinate of this event
	 * @param y		the y coordinate of this event
	 * 
	 * @throws ParseException the parse exception
	 */
	public StopBlocking(int time, int x, int y) throws ParseException{
		time_ = time;
		color_ = new Color(9,115,33);
		long dx, dy, distance, bestdistance = Long.MAX_VALUE;;
		Event event;
		StartBlocking tmpstartblock, beststartblock = null;		
		Iterator<Event> eventIterator = EventList.getInstance().getIterator();
		while(eventIterator.hasNext()){
			event = eventIterator.next();
			if(event.getTime() > time) break;		//start of a blocking can't be after end of a blocking
			if(event.getClass() == StartBlocking.class){
				tmpstartblock = (StartBlocking) event;
				if(tmpstartblock.getStopBlockingEvent() == null){
					dx = tmpstartblock.getX() - x;
					dy = tmpstartblock.getY() - y;
					distance = dx * dx + dy * dy;	//it's actually the squared distance but doesn't matter here and saves some sqrt
					if(distance < 10000000000L && distance < bestdistance){		// must be within 100000cm = 1km (squared!)
						bestdistance = distance;
						beststartblock = tmpstartblock;
					}
				}
			}
		}
		if(beststartblock != null){
			startBlockingEvent_ = beststartblock;
			startBlockingEvent_.setStopBlockingEvent(this);
		} else throw new ParseException(Messages.getString("StopBlocking.noBlockingEventFOund"),0); //$NON-NLS-1$
	}
	
	/**
	 * Gets the x coordinate.
	 * 
	 * @return the x coordinate
	 */
	public int getX(){
		return startBlockingEvent_.getX();
	}
	
	/**
	 * Gets the y coordinate.
	 * 
	 * @return the y coordinate
	 */
	public int getY(){
		return startBlockingEvent_.getY();
	}
	
	/**
	 * Returns a descriptive text for display in the GUI.
	 * 
	 * @return the text
	 * 
	 * @see vanetsim.scenario.events.Event#getText()
	 */
	public String getText(){
		return("<html>" + Messages.getString("StopBlocking.unblockingStreet") + startBlockingEvent_.getStreet().getName() + "<br>" + Messages.getString("StopBlocking.createdAt") + startBlockingEvent_.getTime() + " ms)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}
	
	/**
	 * Compare to another event.
	 * 
	 * @param other	the other event
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
			if(other.getClass() == StartBlocking.class) return 1;
			else{
				if(other.hashCode() > hashCode()) return -1;
				else if(other.hashCode() < hashCode()) return 1;
				else {
					ErrorLog.log(Messages.getString("StopBlocking.eventCompareError") , 7, StopBlocking.class.getName(), "compareTo", null);  //$NON-NLS-1$//$NON-NLS-2$
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
		ArrayList<BlockingObject> blockingObjects = startBlockingEvent_.getBlockingObjects();
		for(int i = 0; i < blockingObjects.size(); ++i){
			blockingObjects.get(i).removeFromLane();
		}
		EventList.getInstance().delCurrentBlockings(startBlockingEvent_);
	}

	/**
	 * Destroys itself.
	 * 
	 * @see vanetsim.scenario.events.Event#destroy()
	 */
	public void destroy() {
		startBlockingEvent_.setStopBlockingEvent(null);		
	}
}