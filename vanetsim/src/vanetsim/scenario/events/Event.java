package vanetsim.scenario.events;

import java.awt.Color;

/**
 * An abstract class for events.
 */
public abstract class Event implements Comparable<Event>{
	
	/** The time in milliseconds when this event gets active. */
	protected int time_;
	
	/** The color for display of this event. */
	protected Color color_;
	
	/**
	 * Returns a descriptive text for display in the GUI.
	 * 
	 * @return the text
	 */
	public abstract String getText();
	
	/**
	 * Destroys itself.
	 */
	public abstract void destroy();
	
	/**
	 * This function should be called when the event is due. The event here does all needed
	 * actions.
	 */
	public abstract void execute();
	
	/**
	 * Gets the time when this event will be fired.
	 * 
	 * @return the time
	 * 
	 * @see vanetsim.scenario.events.Event#getTime()
	 */
	public int getTime(){
		return time_;
	}
	
	/**
	 * Gets the color of the descriptive text for display in the GUI.
	 * 
	 * @return the text color
	 * 
	 * @see vanetsim.scenario.events.Event#getTextColor()
	 */
	public Color getTextColor() {
		return color_;
	}
}