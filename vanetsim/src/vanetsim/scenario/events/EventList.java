package vanetsim.scenario.events;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;


/**
 * This class stores all events.
 */
public final class EventList{

	/** The only instance of this class (singleton). */
	private static final EventList INSTANCE = new EventList();

	/** A <code>PriorityQueue</code> storing all coming events. */
	private final PriorityQueue<Event> allEvents_ = new PriorityQueue<Event>(16);

	/** An <code>ArrayList</code> storing the currently active blocking events. */
	private final ArrayList<StartBlocking> currentBlockings_ = new ArrayList<StartBlocking>(16);

	/**
	 * Empty, private constructor in order to disable instancing.
	 */
	private EventList(){
	}	

	/**
	 * Gets the single instance of this EventList.
	 * 
	 * @return single instance of this EventList
	 */
	public static EventList getInstance(){
		return INSTANCE;
	}

	/**
	 * Adds an event.
	 * 
	 * @param event the event
	 */
	public void addEvent(Event event){
		allEvents_.add(event);
	}

	/**
	 * Removes an event.
	 * 
	 * @param event the event
	 */
	public void delEvent(Event event){
		event.destroy();
		allEvents_.remove(event);
	}

	/**
	 * Removes all events.
	 */
	public void clearEvents(){
		allEvents_.clear();
		currentBlockings_.clear();
	}

	/**
	 * Gets an iterator over all events.
	 * 
	 * @return the iterator
	 */
	public Iterator<Event> getIterator(){
		return allEvents_.iterator();
	}


	/**
	 * Gets the list with all blocking events. Note that this is not optimal for application performance
	 * as new a complete new ArrayList is created (applies only if there are lots of events).
	 * 
	 * @return the an array list with all blocking events.
	 */
	public ArrayList<StartBlocking> getAllBlockingsArrayList(){
		Iterator<Event> events = allEvents_.iterator();
		ArrayList<StartBlocking> result = new ArrayList<StartBlocking>(16);
		Event event;
		while(events.hasNext()){
			event = events.next();
			if(event.getClass() == StartBlocking.class){
				result.add((StartBlocking)event);
			}
		}
		return result;
	}

	/**
	 * Gets the list with the currently active events.
	 * 
	 * @return the current events array list
	 */
	public ArrayList<StartBlocking> getCurrentBlockingsArrayList(){
		return currentBlockings_;
	}

	/**
	 * Adds an event to the list of currently active events.
	 * 
	 * @param event the event
	 */
	public void addCurrentBlockings(StartBlocking event){
		currentBlockings_.add(event);
	}
	
	/**
	 * Removes an event from the list of currently active events.
	 * 
	 * @param event the event
	 */
	public void delCurrentBlockings(StartBlocking event){
		currentBlockings_.remove(event);
	}

	/**
	 * Process next event(s).
	 * 
	 * @param time the current absolute time of the simulation
	 */
	public void processEvents(int time){
		Event tmpEvent;
		while(allEvents_.size() > 0){
			tmpEvent = allEvents_.peek();	//don't remove item yet
			if(tmpEvent.getTime() <= time){	//it's time to fire this event!
				allEvents_.poll();		//now we can remove it!
				tmpEvent.execute();		//execute event				 
			} else break;		//event is not due yet => all others in this queue won't be either so we can stop looking!
		}
	}
}