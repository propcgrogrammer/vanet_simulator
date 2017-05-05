package vanetsim.map.OSM;

import vanetsim.map.Node;

/**
 * A helper class for processing OpenStreetMap-Nodes.
 */
public final class OSMNode{
	
	/** The longitude representing the origin of the map. */
	private static double longitudeMiddle_ = 1.0;
	
	/** The correction in x direction which is used for all OSM_Nodes. */
	private static double correctionX_ = 0;
	
	/** The correction in y direction which is used for all OSM_Nodes. */
	private static double correctionY_ = 0;
	
	/** The latitude of this node. */
	private final double latitude_;
	
	/** The longitude of this node. */
	private final double longitude_;
	
	/** flag for traffic signals */
	private final boolean hasTrafficSignal_;
	
	/**
	 * Instantiates a new helper node.
	 * 
	 * @param latitude		the latitude
	 * @param longitude	the longitude
	 */
	public OSMNode(double latitude, double longitude){
		latitude_ = latitude;
		longitude_ = longitude;
		
		hasTrafficSignal_ = false;
	}
	
	/**
	 * Instantiates a new helper node.
	 * 
	 * @param latitude		the latitude
	 * @param longitude	the longitude
	 * @param hasTrafficSignal if node has a traffic signal
	 */
	public OSMNode(double latitude, double longitude, boolean hasTrafficSignal){
		latitude_ = latitude;
		longitude_ = longitude;
		
		hasTrafficSignal_ = hasTrafficSignal;
	}
	
	/**
	 * Sets the corrections. Valid for all OSM_Nodes so it's static!
	 * 
	 * @param longitudeMiddle	the longitude to use the middle for all conversions from WGS84 to UTM
	 * @param correctionX		the correction value for the x coordinate (minimum x value you have)
	 * @param correctionY		the correction value for the y coordinate (maximum y value you have)
	 */
	public static void setCorrections(double longitudeMiddle, double correctionX, double correctionY){
		longitudeMiddle_ = longitudeMiddle;
		correctionX_ = correctionX;
		correctionY_ = correctionY;
	}
	
	
	/**
	 * Gets a real node (node with coordinates in cm) associated to this node.
	 * 
	 * @return the ready-to-use real node
	 */
	public Node getRealNode(){
		//convert coordinates
		double[] result = new double[2];
		OSMLoader.getInstance().WGS84toUTM(result,longitude_, latitude_, false, longitudeMiddle_, false);
		
		//make corrections
		int x = (int)Math.round((result[0] - correctionX_) * 100);
		int y = (int)Math.round((correctionY_ - result[1]) * 100);
		return new Node(x,y, hasTrafficSignal_);
	}
}