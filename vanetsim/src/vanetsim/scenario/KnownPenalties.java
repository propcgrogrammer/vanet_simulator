package vanetsim.scenario;

import vanetsim.gui.Renderer;
import vanetsim.map.Street;

/**
 * Stores all known penalties for streets. The streets are stored together with their direction and a penalty
 * in cm. Arrays are directly used here (in contrast to the KnownVehiclesList) as this allows easier and faster
 * usage in the routing algorithm. Extensibility is not a major concern here.
 * <br><br>
 * Note for developers: You need to make sure, that all used arrays always have the same size!
 */
public class KnownPenalties{

	/** The vehicle this data structure belongs to. */
	private final Vehicle vehicle_;

	/** The streets which have penalties. */
	private Street[] streets_;

	/** An array with directions corresponding to the streets. <code>1</code> means from endNode to startNode, 
	 * <code>0</code> means both directions and <code>-1</code> means from startNode to endNode */
	private int[] directions_;

	/** The penalties values. Stored in cm */
	private int[] penalties_ ;

	/** How long this entry will be valid. Measured in milliseconds from simulation start. */
	private int[] validUntil_;
	
	/** An array to store if a route update is necessary if this route is removed */
	private boolean[] routeUpdateNecessary_;

	/** The current size of the list. */
	private int size = 0;

	/**
	 * Constructor.
	 * 
	 * @param vehicle	the vehicle this data structure belongs to.
	 */
	public KnownPenalties(Vehicle vehicle){
		vehicle_ = vehicle;
		// just presize so that resizing isn't needed that often
		streets_ = new Street[2];
		directions_ = new int[2];
		penalties_ = new int[2];
		validUntil_ = new int[2];
		routeUpdateNecessary_ = new boolean[2];
	}

	/**
	 * Updates or adds a penalty. If a penalty already existed, the values for penalty and validUntil are
	 * overwritten! If the penalty is new or differs from the last one, a new route calculation is initiated.
	 * 
	 * @param street 		the street
	 * @param direction 	the direction. <code>1</code> means from endNode to startNode, <code>0</code> means
	 * 						both directions and <code>-1</code> means from startNode to endNode
	 * @param penalty		the penalty in cm
	 * @param validUntil	how long this entry will be valid. Measured in millseconds from simulation start
	 */
	public synchronized void updatePenalty(Street street, int direction, int penalty, int validUntil){
		boolean found = false;
		boolean otherPenaltyValue = false;
		for(int i = 0; i < streets_.length; ++i){
			if(streets_[i] == street && directions_[i] == direction){	// update existing value
				found = true;
				if(penalties_[i] != penalty) otherPenaltyValue = true;
				penalties_[i] = penalty;
				validUntil_[i] = validUntil;
				break;
			}
		}

		if(!found){			
			if(size < streets_.length){	// arrays are still large enough
				streets_[size] = street;
				directions_[size] = direction;
				penalties_[size] = penalty;
				validUntil_[size] = validUntil;
			} else {
				// create larger arrays and insert element
				Street[] newArray = new Street[size + 2];
				System.arraycopy (streets_,0,newArray,0,size);
				newArray[size] = street;
				streets_ = newArray;

				int[] newArray2 = new int[size + 2];
				System.arraycopy (directions_,0,newArray2,0,size);
				newArray2[size] = direction;
				directions_ = newArray2;

				newArray2 = new int[size + 2];
				System.arraycopy (penalties_,0,newArray2,0,size);
				newArray2[size] = penalty;
				penalties_ = newArray2;

				newArray2 = new int[size + 2];
				System.arraycopy (validUntil_,0,newArray2,0,size);
				newArray2[size] = validUntil;
				validUntil_ = newArray2;
				
				boolean[] newArray3 = new boolean[size + 2];
				System.arraycopy (newArray3,0,newArray3,0,size);
				newArray3[size] = false;
				routeUpdateNecessary_ = newArray3;
			}
			++size;
		}

		// a really new information has arrived!
		if(!found || otherPenaltyValue){			
			// search if this new information affects the route of the vehicle as route calculation is quite costly
			Street[] routeStreets = vehicle_.getRouteStreets();
			boolean[] routeDirections = vehicle_.getRouteDirections();
			int i = vehicle_.getRoutePosition() + 1;	// increased by 1 because a penalty on the street on which the vehicle currently is isn't very helful
			found = false;
			for(;i < routeStreets.length; ++i){
				if(routeStreets[i] == street){
					if(routeDirections[i]){		// from startNode to endNode
						if(direction < 1){
							found = true;
							break;
						}
					} else {		// from endNode to startNode
						if(direction > -1){
							found = true;
							break;
						}
					}
				}				
			}
			// the route is affected => recalculate it!
			if(found){
				routeUpdateNecessary_[size-1] = true;
				vehicle_.calculateRoute(true, true);
			}	
		}
	}

	/**
	 * Check for outdated entries and remove them. Note that this function is not synchronized! You need to make
	 * sure that no other thread uses any function on this object at the same time!
	 */
	public void checkValidUntil(){
		int timeout = Renderer.getInstance().getTimePassed();
		boolean updateRoute = false;
		
		for(int i = size - 1; i > -1; --i){	// going backwards because it's easier for deletion!
			if(validUntil_[i] < timeout){
				// check if route might be affected if we remove this
				if(routeUpdateNecessary_[i]) updateRoute = true;

				// Don't really remove. Just make the size smaller and copy everything to the front. The data left is some kind of garbage
				// but that doesn't matter...
				--size;
				System.arraycopy(streets_,i+1,streets_,i,size-i);
				System.arraycopy(directions_,i+1,directions_,i,size-i);
				System.arraycopy(penalties_,i+1,penalties_,i,size-i);
				System.arraycopy(validUntil_,i+1,validUntil_,i,size-i);
				System.arraycopy(routeUpdateNecessary_,i+1,routeUpdateNecessary_,i,size-i);
			}
		}
		// if one was removed, a recalculation is necessary
		if(updateRoute){
			vehicle_.calculateRoute(true, true);
		}

	}
	/**
	 * Gets all streets with known penalties.
	 * 
	 * @return an array with all streets
	 */

	public Street[] getStreets(){
		return streets_;
	}
	/**
	 * Gets an array with the directions corresponding to the getStreets()-function.
	 * <code>1</code> in the array means from endNode to startNode, <code>0</code> means
	 * both directions and <code>-1</code> means from startNode to endNode
	 * 
	 * @return an array with all directions
	 */

	public int[] getDirections(){
		return directions_;
	}

	/**
	 * Gets an array with the penalties corresponding to the getStreets()-function.
	 * Measured in cm. 
	 * 
	 * @return an array with all penalties
	 */
	public int[] getPenalties(){
		return penalties_;
	}

	/**
	 * Gets the amount of known penalties stored.
	 * 
	 * @return the size
	 */
	public int getSize(){
		return size;
	}

	/**
	 * Clears everything from this data structure.
	 */
	public void clear(){
		streets_ = new Street[2];	// just presize so that resizing isn't needed that often
		directions_ = new int[2];
		penalties_ = new int[2];
		validUntil_ = new int[2];
		routeUpdateNecessary_ = new boolean[2];
		size = 0;
	}
}