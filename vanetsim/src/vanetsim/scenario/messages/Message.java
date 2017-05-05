package vanetsim.scenario.messages;

import vanetsim.gui.Renderer;
import vanetsim.scenario.Vehicle;

/**
 * An abstract class for messages.
 */
public abstract class Message{
	
	/** A reference to the renderer. */
	private static final Renderer renderer_ = Renderer.getInstance();
	
	/** The x coordinate of the destination of this message. */
	protected int destinationX_;
	
	/** The y coordinate of the destination of this message. */
	protected int destinationY_;
	
	/** The destination radius in cm. */
	protected int destinationRadius_;
	
	/** The squared destination radius in cm^2 (as it's often needed). */
	protected long destinationRadiusSquared_;
	
	/** If flooding mode is enabled. <code>true</code> if flooding of this message to all vehicles is done, 
	 * <code>false</code> if line-forwarding is done*/
	private boolean floodingMode_ = false;
	
	/** How long this message is valid. */
	protected int validUntil_;	
	
	/**
	 * Checks if the message is still valid.
	 * 
	 * @return <code>true</code> if it's valid, else <code>false</code>
	 */
	public boolean isValid(){
		if(renderer_.getTimePassed() < validUntil_) return true;
		else return false;
	}
	
	/**
	 * Gets the x coordinate of the destination.
	 * 
	 * @return the x destination
	 */
	public int getDestinationX_(){
		return destinationX_;
	}
	
	/**
	 * Gets the y coordinate of the destination.
	 * 
	 * @return the y destination
	 */
	public int getDestinationY_(){
		return destinationY_;
	}
	
	/**
	 * Returns if flooding mode is enabled on this message
	 * 
	 * @return <code>true</code> if flooding of this message to all vehicles is done, 
	 * <code>false</code> if line-forwarding is done
	 */
	public boolean getFloodingMode(){
		return floodingMode_;
	}
	
	/**
	 * Sets if flooding mode is enabled on this message
	 * 
	 * @param mode	<code>true</code> if flooding of this message to all vehicles is done, 
	 * 				<code>false</code> if line-forwarding is done
	 */
	public void setFloodingMode(boolean mode){
		floodingMode_ = mode;
	}
	
	/**
	 * Gets the destination radius. If the message arrives in this circular destination area, it 
	 * should be broadcasted to all vehicles.
	 * 
	 * @return the tolerance in cm
	 */
	public int getDestinationRadius(){
		return destinationRadius_;
	}
	
	/**
	 * Gets the squared destination radius.
	 * 
	 * @return the squared radius of the destination region in cm^2
	 */
	public long getDestinationRadiusSquared(){
		return destinationRadiusSquared_;
	}
	
	/**
	 * Executes something on a vehicle given.
	 * 
	 * @param vehicle	the vehicle
	 */
	public abstract void execute(Vehicle vehicle);
}