package vanetsim.scenario;

import vanetsim.gui.Renderer;
import vanetsim.gui.helpers.AttackLogWriter;

/**
 * A list of all known vehicles which was discovered through beacons. In contrast to the KnownPenalties-
 * class, an own class is used for storing the information about the vehicles. Although this means slightly
 * more overhead, it should not be a big case and allows better extensibility.<br>
 * A simple hash algorithm based on the vehicle ID is used to get better performance. The hash determines the 
 * corresponding linked list(beginnings of linked lists are found in <code>head_</code>). Known vehicles with the
 * same hash are connected together through their <code>next_</code> and <code>previous_</code> values (see 
 * KnownVehicle-class).
 */
public class KnownVehiclesList{
	
	/** How long the timeout is in milliseconds. If a vehicle wasn't updated for this time, 
	 * it is dropped from the list! */
	private static final int VALID_TIME = 1000;
	
	/** How many hash buckets will be used. Increase if you expect lots of known vehicles! */
	private static final int HASH_SIZE = 32;
	
	/** How much time has passed since beginning of the simulation. Stored here as it's really needed often. */
	private static int timePassed_ = 0;
	
	/** The array with all heads of the linked lists */
	private KnownVehicle[] head_ = new KnownVehicle[HASH_SIZE];
	
	/** The amount of items stored. */
	private int size_ = 0;
	
	/**
	 * Empty constructor.
	 */
	public KnownVehiclesList(){
		for(int i = 0; i < HASH_SIZE; ++i){
			head_[i] = null;
		}
	}
		
	/**
	 * Update a vehicle or add it if it doesn't exist yet.
	 * 
	 * @param vehicle	a reference to the vehicle
	 * @param ID		the ID of the vehicle
	 * @param x			the x coordinate
	 * @param y			the y coordinate
	 * @param speed		the speed
	 * @param sourceID	ID of the source
	 * @param isEncrypted	if Beacon was encrypted
	 * @param isARSU	if Beacon was sent from an ARSU
	 */
	public synchronized void updateVehicle(Vehicle vehicle, long ID, int x, int y, double speed, long sourceID, boolean isEncrypted, boolean isARSU){
		
		boolean found = false;
		int hash = (int)(ID % HASH_SIZE);
		if(hash < 0) hash = -hash;
		KnownVehicle next = head_[hash];
		while(next != null){
			if(next.getID() == ID){	// update of entry possible
				next.setX(x);
				next.setY(y);
				next.setLastUpdate(timePassed_+VALID_TIME);
				next.setSpeed(speed);
				next.setEncrypted_(isEncrypted);
				found = true;
				break;
			}
			next = next.getNext();
		}					
		
		if(!found){
			next = new KnownVehicle(vehicle, ID, x, y, timePassed_, speed, isEncrypted);
			next.setNext(head_[hash]);
			next.setPrevious(null);
			if(head_[hash] != null) head_[hash].setPrevious(next);
			head_[hash] = next;
			++size_;
		}
		
		if(Vehicle.isAttackerDataLogged_()){
			if(Renderer.getInstance().getAttackerVehicle() != null && this.equals(Renderer.getInstance().getAttackerVehicle().getKnownVehiclesList()) && Vehicle.isAttackerDataLogged_()){
				if(!isEncrypted || (Vehicle.isAttackerEncryptedDataLogged_() && isEncrypted)){
					if(isARSU){
						if(Renderer.getInstance().getAttackedVehicle() != null && Renderer.getInstance().getAttackedVehicle().getID() == ID) AttackLogWriter.log(Renderer.getInstance().getTimePassed() + ":Source(Attacker-RSU):" + sourceID + ":Attacked-Vehicle Data:" + Long.toHexString(ID) + ":" +  x + ":" +  y + ":"+ speed + ":" + isEncrypted );
						else AttackLogWriter.log(Renderer.getInstance().getTimePassed() + ":Source(Attacker-RSU):" + sourceID + ":Other-Vehicle Data:" + Long.toHexString(ID) + ":" +  x + ":" +  y + ":"+ speed + ":" + isEncrypted );
					}
					
					else{
						if(Renderer.getInstance().getAttackedVehicle() != null && Renderer.getInstance().getAttackedVehicle().getID() == ID) AttackLogWriter.log(Renderer.getInstance().getTimePassed() + ":Source(Attacker):"+ Long.toHexString(sourceID) + ":Attacked-Vehicle-Data:" + Long.toHexString(ID) + ":" + x + ":" +  y + ":"+ speed + ":" + isEncrypted);
						else AttackLogWriter.log(Renderer.getInstance().getTimePassed() + ":Source(Attacker):"+ Long.toHexString(sourceID) + ":Other-Vehicle-Data:" + Long.toHexString(ID) + ":" + x + ":" +  y + ":"+ speed + ":" + isEncrypted);
					}
				}
			}else AttackLogWriter.log(Renderer.getInstance().getTimePassed() + ":Any Communication:" + sourceID + ":Any-Vehicle Data:" + Long.toHexString(ID) + ":" +  x + ":" +  y + ":"+ speed + ":" + isEncrypted);
		}

	}
	
	/**
	 * Checks if a vehicle is too old so that it can be removed. Note that this function is not synchronized! You need to make
	 * sure that no other thread uses any function on this object at the same time!
	 */
	public void checkOutdatedVehicles(){
		//int timeout = timePassed_ - VALID_TIME;
		int timeout = timePassed_;
		KnownVehicle next;
		for(int i = 0; i < HASH_SIZE; ++i){
			next = head_[i];
			while(next != null){
				if(next.getLastUpdate() < timeout){ // remove!
					if(next.getNext() != null) next.getNext().setPrevious(next.getPrevious());
					if(next.getPrevious() != null) next.getPrevious().setNext(next.getNext());
					else { //it is the head!
						head_[i] = next.getNext();
					}
					--size_;
				}
				next = next.getNext();	// still works as we didn't change it. Garbage Collector will remove it now.
			}
		}		
	}
	
	/**
	 * Finds the nearest known vehicle to a destination.
	 * 
	 * @param vehicleX		the x coordinate of the calling vehicle
	 * @param vehicleY		the y coordinate of the calling vehicle
	 * @param destX			the x coordinate of the destination
	 * @param destY			the y coordinate of the destination
	 * @param maxDistance	the maximum distance the nearest vehicle max have from the calling vehicle
	 * 
	 * @return the nearest vehicle or <code>null</code> if the calling vehicle is the nearest
	 */
	public Vehicle findNearestVehicle(int vehicleX, int vehicleY, int destX, int destY, int maxDistance){
		double tmpDistance, bestDistance;
		long dx = vehicleX - destX;
		long dy = vehicleY - destY;
		long maxDistanceSquared = (long)maxDistance * maxDistance;
		bestDistance = dx * dx + dy * dy;		// Pythagorean theorem but without costly sqrt because it's unnecessary
		KnownVehicle bestVehicle = null;
		KnownVehicle next;
		for(int i = 0; i < HASH_SIZE; ++i){
			next = head_[i];
			while(next != null){
				dx = next.getX() - destX;
				dy = next.getY() - destY;
				tmpDistance = dx * dx + dy * dy;
				if(tmpDistance < bestDistance){
					dx = next.getX() - vehicleX;
					dy = next.getY() - vehicleY;
					if((dx * dx + dy *dy) < maxDistanceSquared){	// needs to be inside maximum distance
						bestDistance = tmpDistance;
						bestVehicle = next;
					}
				}
				next = next.getNext();
			}
		}		
		if(bestVehicle != null) return bestVehicle.getVehicle();
		else return null;
	}
	
	/**
	 * Gets an hashed array with known vehicles (array length depends on the HASH_SIZE). You can iterate through 
	 * all known vehicles by using <code>getNext()</code> until you get to a <code>null</code> element on all 
	 * elements of this array
	 * 
	 * @return the array with known vehicles
	 */
	public KnownVehicle[] getFirstKnownVehicle(){
		return head_;
	}
	
	/**
	 * Gets the amount of known vehicles stored.
	 * 
	 * @return the size
	 */
	public int getSize(){
		return size_;
	}
	
	/**
	 * Sets the time passed since simulation start.
	 * 
	 * @param time the new time in milliseconds
	 */
	public static void setTimePassed(int time){
		timePassed_ = time;
	}
	
	/**
	 * Clears everything from this data structure.
	 */
	public void clear(){
		head_ = new KnownVehicle[HASH_SIZE];
		size_ = 0;
	}
}