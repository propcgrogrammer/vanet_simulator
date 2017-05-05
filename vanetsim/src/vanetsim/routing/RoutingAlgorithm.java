package vanetsim.routing;

import java.util.ArrayDeque;
//import java16.util.ArrayDeque;

import vanetsim.map.Node;
import vanetsim.map.Street;

/**
 * An interface for routing algorithms.
 */
public interface RoutingAlgorithm{
	
	/**
	 * Gets a routing result.
	 * 
	 * @param mode				You can handle over a mode for the routing algo here. <code>0</code> must be implemented by every algorithm.
	 * @param direction			<code>0</code>=don't care about direction, <code>-1</code>=from startNode to endNode, <code>1</code>=from endNode to startNode
	 * @param startX			the x coordinate of the start point
	 * @param startY			the y coordinate of the start point
	 * @param startStreet		the street on which the start point lies
	 * @param startStreetPos	the position measured in cm from the startNode of the <code>startStreet</code>
	 * @param targetX			the x coordinate of the target point
	 * @param targetY			the y coordinate of the target point
	 * @param targetStreet		the street on which the target point lies
	 * @param targetStreetPos	the position measured in cm from the startNode of the <code>targetStreet</code>
	 * @param penaltyStreets	an array with all streets which have penalties.
	 * @param penaltyDirections	an array with directions corresponding to penaltyStreets. <code>1</code> in the array means from endNode to startNode, 
	 * 							<code>0</code> means both directions and <code>-1</code> means from startNode to endNode
	 * @param penalties			an array with all penalties measured in cm.
	 * @param penaltySize		how many penalties exist.
	 * @param additionalVar		an additional variable specific to the routing algorithm.
	 *
	 * @return An <code>ArrayDeque</code> for returning the result. The first element will be the start node and the last will be the end node of the routing.
	 */
	public abstract ArrayDeque<Node> getRouting(int mode, int direction, int startX, int startY, Street startStreet, double startStreetPos, int targetX, int targetY, Street targetStreet, double targetStreetPos, Street[] penaltyStreets, int[] penaltyDirections, int[] penalties, int penaltySize, int additionalVar);
}