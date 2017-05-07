package vanetsim.map;

import java.util.ArrayList;

import vanetsim.gui.Renderer;
import vanetsim.gui.helpers.PrivacyLogWriter;
import vanetsim.scenario.Vehicle;
import vanetsim.scenario.RSU;
import vanetsim.simulation.WorkerThread;

/**
 * A region stores all objects in a specific part of the map. It stores streets, nodes and vehicles.
 */
public final class Region{
	
	/** An empty vehicle array to prevent unnecessary object creation on <code>toArray()</code> operation. */
	private static final Vehicle[] EMPTY_VEHICLE = new Vehicle[0];
	
	/** The position on the x axis (in relation to all other regions => does not correspond to map coordinates!). */
	private final int x_;

	/** The position on the y axis (in relation to all other regions => does not correspond to map coordinates!). */
	private final int y_;
	
	/** The coordinate representing the left boundary of this region */
	private final int leftBoundary_;
	
	/** The coordinate representing the right boundary of this region */
	private final int rightBoundary_;
	
	/** The coordinate representing the upper boundary of this region */
	private final int upperBoundary_;
	
	/** The coordinate representing the lower boundary of this region */
	private final int lowerBoundary_;

	/** An array storing all nodes in this region. */
	private Node[] nodes_ = new Node[0];	// This has a little bit overhead while loading compared to an ArrayList but requires less memory and is faster when iterating
	
	/** An array storing all the Road-Side-Units in this region. */
	private RSU[] rsus_ = new RSU[0];	// This has a little bit overhead while loading compared to an ArrayList but requires less memory and is faster when iterating
	
	/** An array storing all mix nodes. Within a defined distance, no communication is allowed (and beacon-IDs are changed). */
	private Node[] mixZoneNodes_ = new Node[0];
	
	/** An array storing all streets in this region. */
	private Street[] streets_ = new Street[0];		// This has a little bit overhead while loading compared to an ArrayList but requires less memory and is faster when iterating

	/** An <code>ArrayList</code> storing all vehicles in this region. */
	private ArrayList<Vehicle> vehicles_;	// changes relatively often so use ArrayList here

	/** The simulation requests an array for the vehicles which is cached here. */
	private Vehicle[] vehiclesArray_;
	
	/** The worker thread this region is associated with. */
	private WorkerThread thread_ = null;
	
	/** The number of this region in the worker thread. */
	private int numberInThread_ = -1;

	/** <code>true</code> to indicate that the vehicles have changed since the last call to getVehicleArray() */
	private boolean vehiclesDirty_ = true;

	public ArrayList<String> xxx = new ArrayList<String>();
	public ArrayList<String> yyy = new ArrayList<String>();
	public ArrayList<String> nnn = new ArrayList<String>();
	
	/**
	 * Constructor for a region.
	 * 
	 * @param x				the position on the x axis of the new region
	 * @param y				the position on the y axis of the new region
	 * @param leftBoundary	the coordinate of the left boundary
	 * @param rightBoundary	the coordinate of the right boundary
	 * @param upperBoundary	the coordinate of the upper boundary
	 * @param lowerBoundary	the coordinate of the lower boundary
	 */
	public Region(int x, int y, int leftBoundary, int rightBoundary, int upperBoundary, int lowerBoundary){
		vehicles_ = new ArrayList<Vehicle>(1);
		x_ = x;
		y_ = y;
		leftBoundary_ = leftBoundary;
		rightBoundary_ = rightBoundary;
		upperBoundary_ = upperBoundary;
		lowerBoundary_ = lowerBoundary;
	}
	
	/**
	 * Function to add a node to this region.
	 * 
	 * @param node 		the node to add
	 * @param doCheck 	<code>true</code> if a check should be made if this node already exists; else <code>false</code> to skip the test
	 * 
	 * @return the node added (might be different from <code>node</code> if it already existed and <code>check</code> was true)
	 */
	public Node addNode(Node node, boolean doCheck){
		if(doCheck){
			Node curNode, foundNode = null;
			int x = node.getX();   //cache to save function calls
			int y = node.getY();
			for(int i = 0; i < nodes_.length; ++i){
				curNode = nodes_[i];
				if(curNode.getX() == x && curNode.getY() == y){
					foundNode = curNode;
					break;
				}
			}
			if(foundNode != null) return foundNode;
		}
		Node[] newArray = new Node[nodes_.length+1];
		System.arraycopy (nodes_,0,newArray,0,nodes_.length);
		newArray[nodes_.length] = node;
		nodes_ = newArray;
		return node;
	}

	/**
	 * Delete a node.
	 * 
	 * @param node the node
	 */
	public void delNode(Node node){
		for(int i = 0; i < nodes_.length; ++i){
			if(nodes_[i] == node){
				Node[] newArray = new Node[nodes_.length-1];
				if(i > 0){
					System.arraycopy (nodes_,0,newArray,0,i);
					System.arraycopy (nodes_,i+1,newArray,i,nodes_.length-i-1);
				} else System.arraycopy (nodes_,1,newArray,0,nodes_.length-1);
				nodes_ = newArray;
			}			
		}
	}

	/**
	 * Function to add a Road-Side-Units to this region.
	 * 
	 * @param rsu the RSU to add
	 * 
	 */
	public void addRSU(RSU rsu){
		RSU[] newArray = new RSU[rsus_.length+1];
		System.arraycopy (rsus_,0,newArray,0,rsus_.length);
		newArray[rsus_.length] = rsu;
		rsus_ = newArray;
	}

	/**
	 * Delete a Road-Side-Unit.
	 * 
	 * @param rsu the RSU to delete
	 */
	public void delRSU(RSU rsu){
		for(int i = 0; i < rsus_.length; ++i){
			if(rsus_[i] == rsu){
				RSU[] newArray = new RSU[rsus_.length-1];
				if(i > 0){
					System.arraycopy (rsus_,0,newArray,0,i);
					System.arraycopy (rsus_,i+1,newArray,i,rsus_.length-i-1);
				} else System.arraycopy (rsus_,1,newArray,0,rsus_.length-1);
				rsus_ = newArray;
			}			
		}
	}

	/**
	 * Function to add a street to this region. This also checks if it is intersecting with other streets in this region
	 * and sets the appropriate flag on the streets!
	 * 
	 * @param street the street to add
	 * @param doCheck 	<code>true</code> if a check should be made if this street already exists; else <code>false</code> to skip the test
	 */
	public void addStreet(Street street, boolean doCheck){
		boolean foundstreet = false;
		boolean createBridges = false;
		if(Map.getInstance().getReadyState() == true) createBridges = true;
		if(streets_.length > 0 && (doCheck || createBridges)){
			Street otherStreet;
			int color1, color2;
			for(int i = 0; i < streets_.length; ++i){
				otherStreet = streets_[i];
				if((street.getStartNode() == otherStreet.getStartNode() || street.getStartNode() == otherStreet.getEndNode()) && (street.getEndNode() == otherStreet.getEndNode() ||  street.getEndNode() == otherStreet.getStartNode())) foundstreet = true;
				if(createBridges){
					color1 = street.getDisplayColor().getRGB();
					color2 = otherStreet.getDisplayColor().getRGB();
					//check to which street we should add the bridge
					if(color1 != color2){
						if(color1 < color2) MapHelper.calculateBridges(otherStreet, street);
						else MapHelper.calculateBridges(street, otherStreet);
					} else {
						if(street.getBridgePaintLines() != null || street.getBridgePaintPolygons() != null) MapHelper.calculateBridges(street, otherStreet);	//add bridge to street which already has a bridge
						else if(otherStreet.getBridgePaintLines() != null || otherStreet.getBridgePaintPolygons() != null) MapHelper.calculateBridges(otherStreet, street);
						else if(street.getSpeed() > otherStreet.getSpeed()) MapHelper.calculateBridges(otherStreet, street);		//decide on speed
						else MapHelper.calculateBridges(street, otherStreet);
					}	
				}				
			}
		}
		if(!doCheck || !foundstreet){
			Street[] newArray = new Street[streets_.length+1];
			System.arraycopy (streets_,0,newArray,0,streets_.length);
			newArray[streets_.length] = street;
			streets_ = newArray;
		}
	}
	
	
	/**
	 * Checks all streets in this region for possible bridges.
	 */
	public void checkStreetsForBridges(){
		if(streets_.length > 0){
			Street firstStreet, secondStreet;
			int color1, color2, size = streets_.length;
			for(int i = 0; i < size; ++i){
				firstStreet = streets_[i];
				for(int j = i+1; j < size; ++j){
					secondStreet = streets_[j];
					color1 = firstStreet.getDisplayColor().getRGB();
					color2 = secondStreet.getDisplayColor().getRGB();
					//check to which street we should add the bridge
					if(color1 != color2){
						if(color1 < color2) MapHelper.calculateBridges(secondStreet, firstStreet);
						else MapHelper.calculateBridges(firstStreet, secondStreet);
					} else {
						if(firstStreet.getBridgePaintLines() != null || firstStreet.getBridgePaintPolygons() != null) MapHelper.calculateBridges(firstStreet, secondStreet);	//add bridge to street which already has a bridge
						else if(secondStreet.getBridgePaintLines() != null || secondStreet.getBridgePaintPolygons() != null) MapHelper.calculateBridges(secondStreet, firstStreet);
						else if(firstStreet.getSpeed() > secondStreet.getSpeed()) MapHelper.calculateBridges(secondStreet, firstStreet);		//decide on speed
						else MapHelper.calculateBridges(firstStreet, secondStreet);
					}
				}
			}
		}
	}

	/**
	 * Delete a street.
	 * 
	 * @param street the street
	 */
	public void delStreet(Street street){
		for(int i = 0; i < streets_.length; ++i){
			if(streets_[i] == street){
				Street[] newArray = new Street[streets_.length-1];
				if(i > 0){
					System.arraycopy (streets_,0,newArray,0,i);
					System.arraycopy (streets_,i+1,newArray,i,streets_.length-i-1);
				} else System.arraycopy (streets_,1,newArray,0,streets_.length-1);
				streets_ = newArray;
			}			
		}
	}

	/**
	 * Function to add a vehicle to this region.
	 * 
	 * @param vehicle vehicle to add
	 * @param doCheck 	<code>true</code> if a check should be made if this vehicle already exists; else <code>false</code> to skip the test
	 */
	public synchronized void addVehicle(Vehicle vehicle, boolean doCheck){
		if(doCheck){
			if(!vehicles_.contains(vehicle)){
				vehicles_.add(vehicle);
				if(thread_ != null) thread_.addChangedRegion(numberInThread_);
				vehiclesDirty_ = true;
			}
		} else {
			vehicles_.add(vehicle);
			if(thread_ != null) thread_.addChangedRegion(numberInThread_);
			vehiclesDirty_ = true;
		}
	}

	/**
	 * Function to delete a vehicle from this region.
	 * 
	 * @param vehicle the vehicle to remove
	 */
	public synchronized void delVehicle(Vehicle vehicle){
		vehicles_.remove(vehicle);
		if(thread_ != null) thread_.addChangedRegion(numberInThread_);
		vehiclesDirty_ = true;
	}

	/**
	 * Function to get the x axis position of this region.
	 * 
	 * @return x axis position
	 */
	public int getX(){
		return x_;
	}

	/**
	 * Function to get the y axis position of this region.
	 * 
	 * @return y axis position
	 */
	public int getY(){
		return y_;
	}

	/**
	 * This function should be called before starting simulation. All nodes calculate if they are junctions and
	 * and what their priority streets are. Furthermore, mixing zones are generated.
	 */
	public void calculateJunctions(){
		if(Renderer.getInstance().isAutoAddMixZones()) mixZoneNodes_ = new Node[0];
		
		for(int i = 0; i < nodes_.length; ++i){
			nodes_[i].calculateJunction();		
			
			//Mix zones are only added if autoAddMixZones is activated
			if(Renderer.getInstance().isAutoAddMixZones()){
				if(nodes_[i].getJunction() != null){
					Node[] newArray = new Node[mixZoneNodes_.length+1];
					System.arraycopy (mixZoneNodes_,0,newArray,0,mixZoneNodes_.length);
					newArray[mixZoneNodes_.length] = nodes_[i];
					nodes_[i].setMixZoneRadius(Vehicle.getMixZoneRadius());
					mixZoneNodes_ = newArray;
					if(Vehicle.isEncryptedBeaconsInMix_()){
						RSU tmpRSU = new RSU(nodes_[i].getX(),nodes_[i].getY(), Vehicle.getMixZoneRadius(), true);
						Map.getInstance().addRSU(tmpRSU);
						nodes_[i].setEncryptedRSU_(tmpRSU);
					}
				}
			}
			if(nodes_[i].getJunction() != null && nodes_[i].getJunction().getNode().getTrafficLight_() == null && nodes_[i].isHasTrafficSignal_()) new TrafficLight(nodes_[i].getJunction());
		}
		
		prepareLogs(nodes_);
	}
	
	/**
	 * adds mix zone at the location of "node" if no mix zone on this location already exists
	 */
	public void addMixZone(Node node, int radius){
		boolean found = false;
		int x = node.getX();
		int y = node.getY();
		
		for(int i = 0; i < mixZoneNodes_.length;i++){		
			if(x == mixZoneNodes_[i].getX() && y == mixZoneNodes_[i].getY()) found = true;
		}
		
		if(!found){
			Node[] newArray = new Node[mixZoneNodes_.length+1];
			System.arraycopy (mixZoneNodes_,0,newArray,0,mixZoneNodes_.length);
			newArray[mixZoneNodes_.length] = node;
			node.setMixZoneRadius(radius);
			mixZoneNodes_ = newArray;	
			if(Vehicle.isEncryptedBeaconsInMix_()){
				RSU tmpRSU = new RSU(node.getX(),node.getY(), node.getMixZoneRadius(), true);
				Map.getInstance().addRSU(tmpRSU);
				node.setEncryptedRSU_(tmpRSU);
			}
		}
	}
	

	/**
	 * deletes mix zone at the node
	 * 
	 *  @param node Node where mix zone is placed on
	 */
	public void deleteMixZone(Node node){
		for(int i = 0; i < mixZoneNodes_.length; ++i){
			if(mixZoneNodes_[i] == node){
				Node[] newArray = new Node[mixZoneNodes_.length-1];
				if(i > 0){
					System.arraycopy (mixZoneNodes_,0,newArray,0,i);
					System.arraycopy (mixZoneNodes_,i+1,newArray,i,mixZoneNodes_.length-i-1);
				} else System.arraycopy (mixZoneNodes_,1,newArray,0,mixZoneNodes_.length-1);
				mixZoneNodes_ = newArray;
			}			
		}
		Map.getInstance().delRSU(node.getX(), node.getY());
		node.setEncryptedRSU_(null);
	}
	
	
	/**
	 * Method to prepare the log files. Calculates all intersections beetween streets and mix-zones
	 */
	public void prepareLogs(Node[] nodes){
		String coordinates[] = null;

		Node node = null;
		for(int k = 0; k < nodes.length; ++k){
			node = nodes[k];
			
			if(node.getMixZoneRadius()>0){
				coordinates = getIntersectionPoints(node, this); 
				
				
				if(coordinates != null){
					String[] xxx2 = coordinates[0].split(":");
					String[] yyy2 = coordinates[1].split(":");
		
					for(int i = 5; i < xxx2.length;i++){
						xxx.add(xxx2[i]);
						yyy.add(yyy2[i]);
						nnn.add("" + (i-4));
					}
				}
				
				
				for(String s:coordinates) PrivacyLogWriter.log(s);
			}
		}
	}
	
	
	/**
	 * Gets intersection points between mix-zone and streets. Before evoking check if this node is a mix-zone. Note that the performance of this function is very bad. It has to be evoked before the
	 * start of the simulation!
	 * 
	 * @return string array with to intersection String in form of "x1:x2:x3"
	 */
	/*
	public String[] getIntersectionPoints(Node mixNode, Region r) {
		String[] returnArray = new String[2];
		returnArray[0] = "Mix-Zone(x):Node ID:" + mixNode.getNodeID();
		returnArray[1] = "Mix-Zone(y):Node ID:" + mixNode.getNodeID();
		
		//we need to check all streets:
		Street[] streets;
		Street street;
		
		double y1 = -1;
		double x1 = -1;
		double y2 = -1;
		double x2 = -1;
		double m = -1;
		double t = -1;
		
		double xNode = -1;
		double yNode = -1;
		double radius = -1;
		
		double resultForSqrt = -1;
		double result = -1;
		
		//blacklist to avoid double values on two lane Motorways
		ArrayList<Street> blackList = new ArrayList<Street>();
		
		streets = getStreets();
		for(int k = 0; k < streets.length; ++k){	
			street = streets[k];
			boolean blackListed = false;
					
			if(street.getLanesCount() > 1){
				if(blackList.contains(street)){							
					blackListed = true;
				}	
				if(!blackListed) blackList.add(street);
			}

					if(true){
						//now let's do some magic
						y1 = street.getEndNode().getY();
						x1 = street.getEndNode().getX();
						y2 = street.getStartNode().getY();
						x2 = street.getStartNode().getX();
						xNode = mixNode.getX();
						yNode = mixNode.getY();
						
						m = ((y1-y2)/(x1-x2));
					
						t = y1 - (m*x1);
					
						radius = mixNode.getMixZoneRadius();
						if(street.getStartNode().getX() == 38191) System.out.println("y1:x1:y2:x2:yNode:xNode:m:t:::" + y1 + ":" + x1 + ":" + y2 + ":" + x2 + ":" + yNode + ":" + xNode + ":" + m + ":" + t);
					//	BigInteger term1 = BigInteger.valueOf(xNode).pow(4).negate();
						//BigInteger term2 = BigInteger.valueOf(xNode).pow(2);
					//	BigInteger term3 = BigInteger.valueOf((long)((2*yNode) + (radius*radius)-(2*t))).multiply(term2);
					//	BigInteger term4 = BigInteger.valueOf(yNode).pow(2);
					//	BigInteger term5 = BigInteger.valueOf((long)((2*yNode*t) + (radius*radius) - (t*t)));
						
					//	BigInteger ergebnis = term1.add(term3).subtract(term4).add(term5);
						//


					//	System.out.println("term1" + (long)(xNode*xNode*xNode*xNode));
						//System.out.println("term2" +  (xNode*xNode) * ((2*yNode) + (radius*radius)-(2*t)) + "-" + ((yNode * yNode) + (2*yNode*t) + (radius*radius) - (t*t)));
						//System.out.println((xNode*xNode)*((2*yNode) + (radius*radius)-(2*t)) - (yNode * yNode) + (2*yNode*t) + (radius*radius) - (t*t));
					/*
						System.out.println("x1=" + x1);
						System.out.println("y1=" + y1);
						System.out.println("x2=" + x2);
						System.out.println("y2=" + y2);
						System.out.println("xNode=" + xNode);
						System.out.println("yNode=" + yNode);
						System.out.println("radius=" + radius);
						System.out.println("m=" + m);
						System.out.println("t=" + t);
					
						//resultForSqrt=(long) (-(xNode*xNode*xNode*xNode) + (xNode*xNode)*((2*yNode) + (radius*radius)-(2*t)) - (yNode * yNode) + (2*yNode*t) + (radius*radius) - (t*t));
						//resultForSqrt=(-(xNode*xNode)*(m*m) + (2 * xNode * m)*(yNode - t) + (m*m)*(radius*radius)-(yNode*yNode) + 2*yNode*t + (radius*radius) - (t*t));
						resultForSqrt = (-xNode*xNode*m*m + 2*xNode*m*(yNode - t) + m*m*radius*radius - yNode*yNode + 2*yNode*t + radius*radius - t*t);
						//resultForSqrt=( + *((2*yNode) + (radius*radius)-(2*t)) - (yNode * yNode) + (2*yNode*t) + (radius*radius) - (t*t));
						if(street.getStartNode().getX() == 38191) System.out.println("resultForSqrt:" + resultForSqrt);
						//resultForSqrt = ergebnis.longValue();
					//	System.out.println(resultForSqrt);
						//no solution
						if(resultForSqrt < 0){
						//	System.out.println("kleiner 0");
						}
						//one solution
						else if(resultForSqrt > 0){
							//if(street.getStartNode().getX() == 38191) System.out.println("gr��er");
							//result = ((xNode*(yNode - t + 1)) - Math.sqrt(resultForSqrt))/((xNode*xNode) + 1);
							//result = (Math.sqrt(resultForSqrt) + (xNode+m*(yNode - t)))/((xNode*xNode) + 1);
							result =  -(Math.sqrt(resultForSqrt) - xNode + m*(t - yNode))/(Math.pow(m,2) + 1);
							//if(street.getStartNode().getX() == 38191) System.out.println(result);
							//if((result >= x1 && result <= x2) || (result <= x1 && result >= x2)){
							double dx1= xNode - x1;
							double dy1 = yNode - y1;
							double distanceSquared1 = dx1 * dx1 + dy1 * dy1;
							
							double dx2= xNode - x2;
							double dy2 = yNode - y2;
							double distanceSquared2 = dx2 * dx2 + dy2 * dy2;
							
						//	if((result >= x1 && result <= x2) || (result <= x1 && result >= x2)){
							if(((distanceSquared1 <= radius*radius) || (distanceSquared2 <= radius*radius)) && ((distanceSquared1 > radius*radius) || (distanceSquared2 > radius*radius)) ){
									//if(result > xNode) returnArray[0] += ":" + String.valueOf((int)result - radius);
									//else returnArray[0] += ":" + String.valueOf(radius + (int)result);
									//if(x1 < 0) {
									//	returnArray[0] += ":" + String.valueOf(-(int)result);
									//	returnArray[1] += ":" + String.valueOf(-(int)((m*result + t)));
									//}
									//else {
										returnArray[0] += ":" + String.valueOf((int)result);
										returnArray[1] += ":" + String.valueOf((int)((m*result + t)));
									//}

									
								
							if(street.getStartNode().getX() == 38191) System.out.println(Map.getInstance().getMapHeight()-(m*result + t));
							if(street.getStartNode().getX() == 38191) System.out.println(Map.getInstance().getMapHeight());
							}

						}
						//two solutions
						/*
						else if(resultForSqrt == 0){
							if(street.getStartNode().getX() == 38191) System.out.println("gleich");
							result = -(Math.sqrt(resultForSqrt) - xNode + m*(t - yNode))/(Math.pow(m,2) + 1);

							if((result >= x1 && result <= x2) || (result <= x1 && result >= x2)){
							//	returnArray[0] += ":" + String.valueOf((int)result);
								//returnArray[1] += ":" + String.valueOf((int)(m*result + t));
								
							//	if(y1 < 0){
							//		returnArray[0] += ":" + String.valueOf(-(int)result);
								//	returnArray[1] += ":" + String.valueOf(-(int)(m*result + t));
							//	}
							//	else{
									returnArray[0] += ":" + String.valueOf((int)result);
									returnArray[1] += ":" + String.valueOf((int)((Map.getInstance().getMapHeight()-(m*result + t))));
							//	}

							}
							
							result =  -(Math.sqrt(resultForSqrt) + xNode + m*(t - yNode))/(Math.pow(m,2) + 1);

							if((result >= x1 && result <= x2) || (result <= x1 && result >= x2)){
							//	returnArray[0] += ":" + String.valueOf((int)result);
							//	returnArray[1] += ":" + String.valueOf((int)(m*result + t));
								
							//	if(y1 < 0){
								//	returnArray[0] += ":" + String.valueOf(-(int)result);
							//		returnArray[1] += ":" + String.valueOf(-(int)(m*result + t));
							//	}
							//	else{
									returnArray[0] += ":" + String.valueOf((int)result);
									returnArray[1] += ":" + String.valueOf((int)(Map.getInstance().getMapHeight()-(m*result + t)));
							//	}

							}
						}
						
					}					
				}
		//	}
	//	}

		
		return returnArray;
	}
	*/
	/*
	public String[] getIntersectionPoints(Node mixNode, Region r) {
		String[] returnArray = new String[2];
		returnArray[0] = "Mix-Zone(x):Node ID:" + mixNode.getNodeID();
		returnArray[1] = "Mix-Zone(y):Node ID:" + mixNode.getNodeID();
		
		//we need to check all streets:
		Street[] streets;
		Street street;

		double a1 = -1;
		double a2 = -1;
		double b1= -1;
		double b2 = -1;

		double resultForSqrt = -1;
		double result = -1;
		
		double xNode = -1;
		double yNode = -1;
		double radius = -1;
		
		double m = -1;
		
		double t = -1;
	
		
		streets = getStreets();
		for(int k = 0; k < streets.length; ++k){	
			street = streets[k];
			
			a1 = street.getEndNode().getX();
			a2 = street.getEndNode().getY();
			b1= street.getStartNode().getX();
			b2 = street.getStartNode().getY();
			
			xNode = mixNode.getX();
			yNode = mixNode.getY();
			
			radius = mixNode.getMixZoneRadius();
			
			m = ((a2-b2)/(a1-b1));
			
			t = a2 - (m*a1);
		
			
			resultForSqrt =  (radius*radius) * ( ((b1 - a1) * (b1 - a1)) + ((b2 - a2) * (b2 - a2)) ) - ( (a1 * (b2-a2) - a2*(b1-a1)) * (a1 * (b2-a2) - a2*(b1-a1)) );
			
			if(resultForSqrt < 0){
				
			}
			else if(resultForSqrt == 0){
				result =  (-a1*(b1-a1) - a2 *(b2 - a2)) / (((b1-a1)*(b1-a1)) + ((b2-a2) * (b2-a2)));

				returnArray[0] += ":" + String.valueOf((int)result);
				returnArray[1] += ":" + String.valueOf((int)((m*result + t)));
			}
			else{

			}
		}
		
		return returnArray;
	}
	*/
	
	public String[] getIntersectionPoints(Node mixNode, Region region12) {
		Region[][] regions = Map.getInstance().getRegions();
		
		String[] returnArray = new String[2];
		returnArray[0] = "Mix-Zone(x):Node ID:" + mixNode.getNodeID() + ":Radius:" + mixNode.getMixZoneRadius();
		returnArray[1] = "Mix-Zone(y):Node ID:" + mixNode.getNodeID() + ":Radius:" + mixNode.getMixZoneRadius();
		
		//we need to check all streets:
		Street[] streets;
		Street street;
		
		double y1 = -1;
		double x1 = -1;
		double y2 = -1;
		double x2 = -1;
		double m = -1;
		double t = -1;
		
		double xNode = -1;
		double yNode = -1;
		double r = -1;
		
		//double resultForSqrt = -1;
		double result = -1;
		double result1 = -1;
		double result2 = -1;
		
		//blacklist to avoid double values on two lane Motorways
		ArrayList<Street> blackList = new ArrayList<Street>();
		ArrayList<Street> blackList2 = new ArrayList<Street>();
		boolean blackListed = false;
		boolean blackListed2 = false;
		
		for(int i = 0; i < regions.length; i++){
			for(int j = 0; j < regions[i].length;j++){
				
				
				streets = regions[i][j].getStreets();
				for(int k = 0; k < streets.length; k++){	
					street = streets[k];
					blackListed = false;
					blackListed2 = false;
					if(street.getLanesCount() > 1){
						if(blackList.contains(street)){							
							blackListed = true;
						}	
						if(!blackListed) blackList.add(street);
					}
					
					if(blackList2.contains(street)){							
						blackListed2 = true;
					}	
					if(!blackListed2) blackList2.add(street);
					
							if(!blackListed && !blackListed2){
								//now let's do some magic
								y1 = street.getEndNode().getY();
								x1 = street.getEndNode().getX();
								y2 = street.getStartNode().getY();
								x2 = street.getStartNode().getX();
								xNode = mixNode.getX();
								yNode = mixNode.getY();
								
								m = ((y1-y2)/(x1-x2));
							
								t = y1 - (m*x1);
							
								r = mixNode.getMixZoneRadius();
							//	BigInteger term1 = BigInteger.valueOf(xNode).pow(4).negate();
								//BigInteger term2 = BigInteger.valueOf(xNode).pow(2);
							//	BigInteger term3 = BigInteger.valueOf((long)((2*yNode) + (radius*radius)-(2*t))).multiply(term2);
							//	BigInteger term4 = BigInteger.valueOf(yNode).pow(2);
							//	BigInteger term5 = BigInteger.valueOf((long)((2*yNode*t) + (radius*radius) - (t*t)));
								
							//	BigInteger ergebnis = term1.add(term3).subtract(term4).add(term5);
								//


							//	System.out.println("term1" + (long)(xNode*xNode*xNode*xNode));
								//System.out.println("term2" +  (xNode*xNode) * ((2*yNode) + (radius*radius)-(2*t)) + "-" + ((yNode * yNode) + (2*yNode*t) + (radius*radius) - (t*t)));
								//System.out.println((xNode*xNode)*((2*yNode) + (radius*radius)-(2*t)) - (yNode * yNode) + (2*yNode*t) + (radius*radius) - (t*t));
							
								//resultForSqrt=(long) (-(xNode*xNode*xNode*xNode) + (xNode*xNode)*((2*yNode) + (radius*radius)-(2*t)) - (yNode * yNode) + (2*yNode*t) + (radius*radius) - (t*t));
								
								//System.out.println("wurzel:" + resultForSqrt);
								//resultForSqrt=(-(xNode*xNode)*(m*m) + (2 * xNode * m)*(yNode - t) + (m*m)*(radius*radius)-(yNode*yNode) + 2*yNode*t + (radius*radius) - (t*t));
								//resultForSqrt = (-xNode*xNode*m*m + 2*xNode*m*(yNode - t) + m*m*radius*radius - yNode*yNode + 2*yNode*t + radius*radius - t*t);
								//resultForSqrt=( + *((2*yNode) + (radius*radius)-(2*t)) - (yNode * yNode) + (2*yNode*t) + (radius*radius) - (t*t));
								//resultForSqrt = ergebnis.longValue();
							//	System.out.println(resultForSqrt);
								//no solution
								if(street.getName().equals("aaaa")) System.out.println("wuhuhahahah" + (-yNode*yNode + 2*xNode*yNode*m - xNode*xNode*m*m + r*r + m*m*r*r + 2*yNode*t - 2*xNode*m*t - t*t));
								if(street.getName().equals("aaaa")) System.out.println("wuhuhahahah2" + yNode + " " + xNode + " " + m + " " + r + " " + t);
								

								if((-yNode*yNode + 2*xNode*yNode*m - xNode*xNode*m*m + r*r + m*m*r*r + 2*yNode*t - 2*xNode*m*t - t*t) < 0){
								//	System.out.println("kleiner 0");
								}
								//two solution
								else if((-yNode*yNode + 2*xNode*yNode*m - xNode*xNode*m*m + r*r + m*m*r*r + 2*yNode*t - 2*xNode*m*t - t*t) > 0){
									
									//result1 =  -(Math.sqrt(resultForSqrt) - xNode + m*(t - yNode))/(Math.pow(m,2) + 1);
									//result1 = (xNode*(yNode - t + 1) - Math.sqrt(resultForSqrt)) / ((xNode * xNode) + 1);
									//result1 = (xNode*(yNode - t + 1) + Math.sqrt(resultForSqrt)) / ((xNode * xNode) + 1);
									result1 = (xNode + yNode*m - m*t - Math.sqrt(-yNode*yNode + 2*xNode*yNode*m - xNode*xNode*m*m + r*r + m*m*r*r + 2*yNode*t - 2*xNode*m*t - t*t))/(1 + m*m);
									result2 = (xNode + yNode*m - m*t + Math.sqrt(-yNode*yNode + 2*xNode*yNode*m - xNode*xNode*m*m + r*r + m*m*r*r + 2*yNode*t - 2*xNode*m*t - t*t))/(1 + m*m);
									
									double dx1= xNode - x1;
									double dy1 = yNode - y1;
									double distanceSquared1 = dx1 * dx1 + dy1 * dy1;
									
									double dx2= xNode - x2;
									double dy2 = yNode - y2;
									double distanceSquared2 = dx2 * dx2 + dy2 * dy2;
									if(street.getName().equals("aaaa")) System.out.println("distancesqu1alex" + distanceSquared1);
									if(street.getName().equals("aaaa")) System.out.println("distancesqu1alex" + distanceSquared2);
									

								//	if((result >= x1 && result <= x2) || (result <= x1 && result >= x2)){
									//if(((distanceSquared1 <= r*r) || (distanceSquared2 <= r*r)) && ((distanceSquared1 > r*r) || (distanceSquared2 > r*r)) ){
										if(street.getName().equals("aaaa")) System.out.println("da bin ich noch");

										if((result1 >= x1 && result1 <= x2) || (result1 <= x1 && result1 >= x2)){
											returnArray[0] += ":" + String.valueOf((int)result1);
											returnArray[1] += ":" + String.valueOf((int)((m*result1 + t)));
											/*
											System.out.println(xNode + "/" + yNode);
									
											System.out.println("x: " + result1);
											System.out.println("y: " + (m*result1 + t));
											*/
										}


										if((result2 >= x1 && result2 <= x2) || (result2 <= x1 && result2 >= x2)){
											returnArray[0] += ":" + String.valueOf((int)result2);
											returnArray[1] += ":" + String.valueOf((int)((m*result2 + t)));
											
											/*
											System.out.println(xNode + "/" + yNode);
									
											System.out.println("x: " + result2);
											System.out.println("y: " + (m*result2 + t));
										*/	
										}

									//}
								//}
											//if(result > xNode) returnArray[0] += ":" + String.valueOf((int)result - radius);
										

								}
								
								//one solutions
								
								else if((-yNode*yNode + 2*xNode*yNode*m - xNode*xNode*m*m + r*r + m*m*r*r + 2*yNode*t - 2*xNode*m*t - t*t) == 0){
									//result = (xNode *(yNode - t + 1))/((xNode*xNode)+1);
									result = (xNode + yNode*m - m*t)/(1 + m*m);

									double dx1= xNode - x1;
									double dy1 = yNode - y1;
									double distanceSquared1 = dx1 * dx1 + dy1 * dy1;
									
									double dx2= xNode - x2;
									double dy2 = yNode - y2;
									double distanceSquared2 = dx2 * dx2 + dy2 * dy2;
									
									//if((result >= x1 && result <= x2) || (result <= x1 && result >= x2)){
									if(((distanceSquared1 <= r*r) || (distanceSquared2 <= r*r)) && ((distanceSquared1 > r*r) || (distanceSquared2 > r*r)) ){
										//System.out.println(xNode + "/" + yNode);
										returnArray[0] += ":" + String.valueOf((int)result);
										returnArray[1] += ":" + String.valueOf((int)(((m*result + t))));
								
										//System.out.println("x: " + result);
										//System.out.println("y: " + (m*result + t));
									}

								}
								
							}					
						}
				//	}
			//	}

				
			}
		}
		
		
		return returnArray;
		
	}
	
	
	
	/**
	 * clear all mix zones from this region.
	 */
	public void clearMixZones(){
		for(Node mixNode : mixZoneNodes_) deleteMixZone(mixNode);
		mixZoneNodes_ = new Node[0];
	}
	
	/**
	 * clear all RSUs from this region.
	 */
	public void clearRSUs(){
		rsus_ = new RSU[0];
	}

	/**
	 * This function should be called before initializing a new scenario to delete all vehicles.
	 */
	public void cleanVehicles(){
		vehicles_ = new ArrayList<Vehicle>(1);
		for(int i = 0; i < streets_.length; ++i){
			streets_[i].clearLanes();
		}
		vehiclesDirty_ = true;
	}
	
	/**
	 * This function deletes all traffic lights in this region
	 */
	public void clearTrafficLights(){
		for(int i = 0; i < nodes_.length; i++){
			if(nodes_[i].getJunction() != null){
				nodes_[i].getJunction().delTrafficLight();
			}
		}
	}

	/**
	 * Returns all nodes in this region.
	 * 
	 * @return an array containing all nodes
	 */	
	public Node[] getNodes(){
		return nodes_;
	}
	
	/**
	 * Returns all mix zone nodes in this region.
	 * 
	 * @return an array containing all nodes
	 */	
	public Node[] getMixZoneNodes(){
		return mixZoneNodes_;
	}

	/**
	 * Returns all Road-Side-Units in this region.
	 * 
	 * @return an array containing all RSUs
	 */	
	public RSU[] getRSUs() {
		return rsus_;
	}
	
	/**
	 * Returns all streets in this region.
	 * 
	 * @return an array containing all streets
	 */
	public Street[] getStreets(){
		return streets_;
	}

	/**
	 * Used to return the <code>ArrayList</code> of all vehicles. Note that it can not guaranteed, that no
	 * changes are made after you received this.
	 * 
	 * @return the <code>ArrayList</code> containing all vehicles
	 */
	public ArrayList<Vehicle> getVehicleArrayList(){
		return vehicles_;
	}


	/**
	 * Creates an array as a copy of the vehicle <code>ArrayList</code> to prevent problems during simulation caused by
	 * changing the <code>ArrayList</code> while reading it in another thread. The array is cached so that new ones are only
	 * created when needed.
	 * 
	 * @return the array copy of all vehicles in this region or an empty array if there are no elements
	 */
	public Vehicle[] getVehicleArray(){
		if(vehiclesDirty_){
			if(vehicles_.size() == 0) vehiclesArray_ = EMPTY_VEHICLE;
			else vehiclesArray_ = vehicles_.toArray(EMPTY_VEHICLE);
			vehiclesDirty_ = false;
		}
		return vehiclesArray_;
	}
	
	/**
	 * Creates a backlink to the worker thread which computes this region.
	 * 
	 * @param thread			the thread
	 * @param numberinThread	the number in the thread
	 */
	public void createBacklink(WorkerThread thread, int numberinThread){
		thread_ = thread;
		numberInThread_ = numberinThread;
	}
	
	/**
	 * Gets the coordinate of the left boundary of this region.
	 * 
	 * @return	the coordinate
	 */
	public int getLeftBoundary(){
		return leftBoundary_;
	}
	
	/**
	 * Gets the coordinate of the right boundary of this region.
	 * 
	 * @return	the coordinate
	 */
	public int getRightBoundary(){
		return rightBoundary_;
	}
	
	/**
	 * Gets the coordinate of the upper boundary of this region.
	 * 
	 * @return	the coordinate
	 */
	public int getUpperBoundary(){
		return upperBoundary_;
	}
	
	/**
	 * Gets the coordinate of the lower boundary of this region.
	 * 
	 * @return	the coordinate
	 */
	public int getLowerBoundary(){
		return lowerBoundary_;
	}
}