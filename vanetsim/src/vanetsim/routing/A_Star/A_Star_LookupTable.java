package vanetsim.routing.A_Star;

import vanetsim.map.Node;
/**
 * A LookupTable for the mapping between Nodes and A_Star_Nodes. Uses the nodeid to
 * construct an extremely very efficient lookup in an array. This has a far better 
 * performance than traditional HashMaps because no hashing is needed and no 
 * collisions can occur.<br>
 * If you encounter any problems you may switch to the standard Java Hashmap by just
 * replacing all occurences.
 */

public class A_Star_LookupTable<K, V>{

	/** The lookup table. */
	private A_Star_Node[] table_;

	/** The number of key-value mappings contained in this map.*/
	private int size_;

	/**
	 * Constructs an empty LookupTable.
	 *
	 * @param initialCapacity	the initial capacity.
	 */
	public A_Star_LookupTable(int initialCapacity) {
		if (initialCapacity < 0) initialCapacity = 1000;

		table_ = new A_Star_Node[initialCapacity];
		size_ = initialCapacity;
	}


	/**
	 * Returns the value to which the specified node is mapped,
	 * or <code>null</code> if this map contains no mapping for the key.
	 */
	public A_Star_Node get(Node key) {
		int pos = key.getNodeID();
		if(pos > size_) return null;
		else return table_[key.getNodeID()];
	}


	/**
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for the key, the old
	 * value is replaced.
	 *
	 * @param key	key with which the specified value is to be associated
	 * @param value	value to be associated with the specified key
	 */
	public void put(Node key, A_Star_Node value) {
		int pos = key.getNodeID();
		if(pos > size_){
			A_Star_Node[] newTable = new A_Star_Node[pos+1];
			System.arraycopy(table_, 0, newTable, 0, size_);
			table_ = newTable;
			size_ = pos+1;
		} 
		table_[pos] = value;
	}
}

