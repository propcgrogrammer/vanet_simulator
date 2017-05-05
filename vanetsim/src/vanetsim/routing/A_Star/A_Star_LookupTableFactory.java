package vanetsim.routing.A_Star;

import java.util.ArrayDeque;
//import java16.util.ArrayDeque;

import vanetsim.map.Node;

/**
 * A factory for creating LookupTables (mapping between <code>Node</code>s and <code>A_Star_Node</code>s)for the A* algorithm.
 * It's extremely efficient to recycle old LookupTables, because the A* algorithm creates lots of new <code>A_Star_Node</code>s
 * which is relatively costly because of the huge amount of objects and necessary garbage collection.
 * This factory takes correctly care of multithreading so you may concurrently calculate routes. However, this may take some memory
 * as every concurrent LookupTable is completely independent and will be stored for the whole program life!<br>
 */
public final class A_Star_LookupTableFactory{
	
	/** A queue with the LookupTables. */
	private static final ArrayDeque<A_Star_LookupTable<Node, A_Star_Node>> table_ = new ArrayDeque<A_Star_LookupTable<Node, A_Star_Node>>();
	
	/** A counter per LookupTable value to detect if an <code>A_Star_Node</code> in the LookupTable is from a previous run or not. */
	private static final ArrayDeque<Integer> counter_ = new ArrayDeque<Integer>();
	
	/**
	 * Gets a LookupTable. Don't forget to put the LookupTable back if you don't need it anymore!
	 * 
	 * @param counter	an array used to return a new counter value in <code>counter[0]</code>
	 * 
	 * @return the LookupTable
	 */
	public static synchronized A_Star_LookupTable<Node, A_Star_Node> getTable(int[] counter){
		if(table_.isEmpty()){
			counter[0] = Integer.MIN_VALUE;
			return new A_Star_LookupTable<Node, A_Star_Node>(Node.getMaxNodeID()+1);			
		} else {
			counter[0] = counter_.poll() + 1;
			if(counter[0] == Integer.MAX_VALUE){	//this only happens after an enormous amount of routings. Recycling further would be dangerous => use a new one with resetted counter!
				table_.poll();
				counter[0] = Integer.MIN_VALUE;
				return new A_Star_LookupTable<Node, A_Star_Node>(Node.getMaxNodeID()+1);
			} else return table_.poll();
		}
	}
	
	/**
	 * Put back a LookupTable so that it can be reused.
	 * 
	 * @param counter	the counter value
	 * @param table		the LookupTable
	 */
	public static synchronized void putTable(int counter, A_Star_LookupTable<Node, A_Star_Node> table){
		table_.add(table);
		counter_.add(counter);
	}
	
	/**
	 * Clear the cached versions. This should be done on map reload to free otherwise unnecessarily wasted memory.
	 */
	public static synchronized void clear(){
		table_.clear();
		counter_.clear();
	}
}