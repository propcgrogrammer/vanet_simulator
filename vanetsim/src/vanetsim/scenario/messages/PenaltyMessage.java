package vanetsim.scenario.messages;

import vanetsim.map.Street;
import vanetsim.scenario.Vehicle;

/**
 * A message which indicates some kind of traffic jam through assigning a penalty to the street on which the jam is.
 */
public class PenaltyMessage extends Message{
	
	/** The penalty street. */
	private final Street penaltyStreet_;
	
	/** The direction. <code>1</code> means from endNode to startNode, <code>0</code> means
	 * 	both directions and <code>-1</code> means from startNode to endNode */
	private final int penaltyDirection_;
	
	/** A value for the penalty in cm. */
	private final int penaltyValue_;
	
	/** Until when this penalty is valid. */
	private final int penaltyValidUntil_;
	
	/**
	 * Instantiates a new penalty message.
	 * 
	 * @param destinationX		the x coordinate of the destination of the message
	 * @param destinationY		the y coordinate of the destination of the message
	 * @param destinationRadius	the radius of the destination area in cm
	 * @param validUntil		how long the message is valid in ms (measured from simulation start)
	 * @param penaltyStreet		the penalty street
	 * @param penaltyDirection	the direction to which the penalty corresponds. <code>1</code> means from endNode to startNode, 
	 * 							<code>0</code> means both directions and <code>-1</code> means from startNode to endNode
	 * @param penaltyValue		the penalty value in cm
	 * @param penaltyValidUntil	how long the penalty is valid in ms (measured from simulation start)
	 */
	public PenaltyMessage(int destinationX, int destinationY, int destinationRadius, int validUntil, Street penaltyStreet, int penaltyDirection, int penaltyValue, int penaltyValidUntil){
		destinationX_ = destinationX;
		destinationY_ = destinationY;
		destinationRadius_ = destinationRadius;
		destinationRadiusSquared_ = (long)destinationRadius * destinationRadius;
		validUntil_ = validUntil;
		penaltyStreet_ = penaltyStreet;
		penaltyDirection_ = penaltyDirection;
		penaltyValue_ = penaltyValue;
		penaltyValidUntil_ = penaltyValidUntil;
	}
	
	/**
	 * Executes the message by adding a new penalty value to the known penalties of the vehicle given. 
	 * 
	 * @param vehicle	the vehicle on which this operation is done
	 * 
	 * @see vanetsim.scenario.messages.Message#execute(vanetsim.scenario.Vehicle)
	 */
	public void execute(Vehicle vehicle){
		vehicle.getKnownPenalties().updatePenalty(penaltyStreet_, penaltyDirection_, penaltyValue_, penaltyValidUntil_);
	}
}