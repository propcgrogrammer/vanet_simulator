package vanetsim.scenario;

/**
 * A known vehicle (discovered by receiving a beacon). The variables represent what is known and might
 * differ from the real ones if it hasn't been updated for some time!
 */
public class KnownVehicle{
	
	/** The vehicle associated. */
	private final Vehicle vehicle_;
	
	/** The ID of the vehicle. */
	private final long ID_;
	
	/** The current x coordinate. */
	private int x_;

	/** The current y coordinate. */
	private int y_;	
	
	/** The current speed. */
	private double speed_;
	
	/** If the beacon is encrypted */
	private boolean isEncrypted_;
	
	/** The time when the vehicle was last updated in milliseconds. */
	private int lastUpdate_;
	
	/** Link to the previous object. */
	protected KnownVehicle previous_;
	
	/** Link to the next object. */
	protected KnownVehicle next_;
	
	/**
	 * Instantiates a new known vehicle.
	 * 
	 * @param vehicle	the vehicle
	 * @param ID		the ID of the vehicle
	 * @param x 		the x coordinate
	 * @param y			the y coordinate
	 * @param time		the current time
	 * @param speed		the current speed
	 * @param isEncrypted	if Beacon was encrypted
	 */
	public KnownVehicle(Vehicle vehicle, long ID, int x, int y, int time, double speed, boolean isEncrypted){
		vehicle_ = vehicle;
		ID_ = ID;
		x_ = x;
		y_ = y;
		speed_ = speed;
		lastUpdate_ = time;
		isEncrypted_ = isEncrypted;
	}
	
	
	/**
	 * Updates the x coordinate.
	 * 
	 * @param x		the x coordinate
	 */
	public void setX(int x){
		x_ = x;
	}
	
	/**
	 * Updates the y coordinate.
	 * 
	 * @param y		the y coordinate
	 */
	public void setY(int y){
		y_ = y;
	}
	
	/**
	 * Updates the last modification time.
	 * 
	 * @param time	the current time
	 */
	public void setLastUpdate(int time){
		lastUpdate_ = time;
	}
	
	/**
	 * Updates the speed.
	 * 
	 * @param speed	the current speed
	 */
	public void setSpeed(double speed){
		speed_ = speed;
	}
	
	/**
	 * Gets the x coordinate.
	 * 
	 * @return the x coordinate
	 */
	public int getX(){
		return x_;
	}
	
	/**
	 * Gets the y coordinate.
	 * 
	 * @return the y coordinate
	 */
	public int getY(){
		return y_;
	}
	
	/**
	 * Gets the ID.
	 * 
	 * @return the ID
	 */
	public long getID(){
		return ID_;
	}
	
	/**
	 * Gets the speed
	 * 
	 * @return the speed
	 */
	public double getSpeed(){
		return speed_;
	}
	
	/**
	 * Gets the vehicle.
	 * 
	 * @return the vehicle
	 */
	public Vehicle getVehicle(){
		return vehicle_;
	}
	
	/**
	 * Gets when this vehicle was last updated.
	 * 
	 * @return the last update time in milliseconds
	 */
	public int getLastUpdate(){
		return lastUpdate_;
	}
	
	/**
	 * Returns the KnownVehicle after this one.
	 * 
	 * @return the next
	 */
	public KnownVehicle getNext() {
		return next_;
	}

	/**
	 * Returns the KnownVehicle before this one.
	 * 
	 * @return the previous
	 */
	public KnownVehicle getPrevious() {
		return previous_;
	}

	/**
	 * Sets the KnownVehicle after this one.
	 * 
	 * @param next	the object which comes after this one
	 */
	public void setNext(KnownVehicle next) {
		next_ = next;
	}

	/**
	 * Sets the KnownVehicle before this one.
	 * 
	 * @param previous	the object which comes before this one
	 */
	public void setPrevious(KnownVehicle previous) {
		previous_ = previous;
	}


	public boolean isEncrypted_() {
		return isEncrypted_;
	}


	public void setEncrypted_(boolean isEncrypted_) {
		this.isEncrypted_ = isEncrypted_;
	}
}