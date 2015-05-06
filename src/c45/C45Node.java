package c45;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Nodes used internally by DecisionTree for the C4.5 decision tree algorithm.
 * @author Matthew Tetford
 *
 */
public class C45Node {
	private Dataset dataset;
	private C45Node parent;
	private ArrayList<C45Node> children;
	private HashSet<Value> attributes_remaining;
	private Value split_attribute;
	private Value split_value;
	private boolean continuous_split;
	private int split_number;
	private boolean gte;
	private int depth;
	
	/**
	 * Creates a node with the given data set. Excludes the given target class
	 * from splitting. Used for the root node to exclude the target attribute.
	 * @param _dataset (Dataset): The data set we wish to store.
	 * @param _parent (C45Node): The parent of this node.
	 * @param _class_remove (String): The attribute we wish to exclude from splitting.
	 */
	public C45Node(Dataset _dataset, C45Node _parent, Value _class_remove){
		dataset = _dataset;
		parent = _parent;
		continuous_split = false;
		
		if(parent != null){
			attributes_remaining = parent.getRemainingAttributes();
			attributes_remaining.remove(_class_remove);
			depth = (parent.getDepth()+1);
		}else{
			attributes_remaining = dataset.getAttributeSet();
			attributes_remaining.remove(_class_remove);
			depth = 0;
		}
		
		children = new ArrayList<C45Node>();
		split_attribute = _class_remove;
		split_value = new Value();
	}
	
	/**
	 * Creates a node with the given data set. Excludes the given target class
	 * from splitting. Used for children nodes which have data sets that are
	 * subsets of their parents.
	 * @param _dataset (Dataset): The data set we wish to store.
	 * @param _parent (C45Node): The parent of this node.
	 * @param _class_remove (String): The attribute we wish to exclude from splitting
	 * @param _split_value (String): The attribute value this node's data set was split on.
	 */
	public C45Node(Dataset _dataset, C45Node _parent, Value _class_remove, Value _split_value){
		dataset = _dataset;
		parent = _parent;
		continuous_split = false;
		
		if(parent != null){
			attributes_remaining = parent.getRemainingAttributes();
			attributes_remaining.remove(_class_remove);
			depth = (parent.getDepth()+1);
		}else{
			attributes_remaining = dataset.getAttributeSet();
			attributes_remaining.remove(_class_remove);
			depth = 0;
		}
		
		children = new ArrayList<C45Node>();
		split_attribute = _class_remove;
		split_value = _split_value;
	}
	
	/**
	 * Creates a node with the given data set. Splits on the given numerical attribute
	 * with integers greater than or equal or less than the integer given.
	 * Used for children nodes which have data sets that are subsets of their parents.
	 * @param _subdataset (Dataset)
	 * @param _parent (C45Node)
	 * @param _split_attribute (Value) 
	 * @param _split_number (Integer)
	 * @param _gte (boolean)
	 */
	public C45Node(Dataset _subdataset, C45Node _parent, Value _split_attribute, int _split_number, boolean _gte){
		dataset = _subdataset;
		parent = _parent;
		split_number = _split_number;
		continuous_split = true;
		gte = _gte;
		
		if(parent != null){
			attributes_remaining = parent.getRemainingAttributes();
			depth = (parent.getDepth()+1);
		}else{
			attributes_remaining = dataset.getAttributeSet();
			depth = 0;
		}
		
		children = new ArrayList<C45Node>();
		split_attribute = _split_attribute;
		split_number = _split_number;
	}
	
	/**
	 * Adds a child to the node.
	 * @param child(C45Node): Child to add.
	 */
	public void addChild(C45Node child){
		children.add(child);
	}
	
	/**
	 * Clears the children of the node.
	 */
	public void clearChildren(){
		children = new ArrayList<C45Node>();
	}
	
	/**
	 * Returns the nodes data set.
	 * @return (Dataset): The nodes data set.
	 */
	public Dataset getDataset(){
		return dataset;
	}
	
	/**
	 * Gets the nodes parent.
	 * @return (ID3Node): Parent of this node.
	 */
	public C45Node getParent(){
		return parent;
	}
	
	/**
	 * Check to see if this node is the root.
	 * @return (boolean): True if node is root. False otherwise.
	 */
	public boolean isRoot(){
		boolean isRoot = false;
		if(parent == null){
			isRoot = true;
		}
		return isRoot;
	}
	
	/**
	 * Gets the attritbute this node's data set was split on.
	 * @return (String): The split attribute.
	 */
	public Value getSplitAttribute(){
		return split_attribute;
	}
	
	/**
	 * Gets the remaining attributes we may split on.
	 * @return (HashSet<String>): Remaining attributes.
	 */
	public HashSet<Value> getRemainingAttributes(){
		return attributes_remaining;
	}
	
	/**
	 * Used to determine if the node has any children.
	 * @return (boolean): True if node has children. False otherwise.
	 */
	public boolean hasChild(){
		return !children.isEmpty();
	}
	
	/**
	 * Used to determine if the node is a leaf.
	 * @return (boolean): False if node has children. True otherwise.
	 */
	public boolean isLeaf(){
		return children.isEmpty();
	}
	
	/**
	 * Gets a list of the children of a node.
	 * @return (ArrayList<ID3Node>): The children of this node.
	 */
	public ArrayList<C45Node> getChildren(){
		return children;
	}
	
	/**
	 * Gets the most frequent value from the specified target attribute.
	 * If there is a tie, the first looked at is returned.
	 * @param target_attribute (String): The attribute we wish to evaluate the values of.
	 * @return (String): The most frequent value.
	 */
	public Value getMaxValue(Value target_attribute){
		//System.out.println("\nGetting Max Value");
		Value max_value = new Value();
		
		HashSet<Value> values = dataset.getValueSet(target_attribute);
		double max_count = 0;
		int count = 1;
		
		for(Value value : values){
			if(count > 0){
				double num_of_values = dataset.getValueCount(target_attribute, value);
				if(num_of_values > max_count){
					max_count = num_of_values;
					max_value = value;
					//System.out.println("New Max is " + value.toString() + " with count " + max_count);
				}
			}
			count++;
		}
		
		return max_value;
	}
	
	/**
	 * Gets the usefulness measure from the current node given
	 * the target value.
	 * @param target_Attribute (Value): The attribute we wish to measure.
	 * @param value (Value): The value we wish to measure.
	 * @return (double): The usefulness measure.
	 */
	public double getAccuracy(Value target_Attribute, Value value){
		double usefulness_measure = 0;
		
		double numerator = (double)dataset.getValueCount(target_Attribute, value);
		double denominator = (double)(dataset.height-1);
		
		usefulness_measure = numerator / denominator;
		
		return usefulness_measure;
	}
	
	/**
	 * Simply returns the value this node was split on.
	 * @return (String): The split value.
	 */
	public Value getSplitValue(){
		return split_value;
	}
	
	/**
	 * Gets the depth of the node. The root is at depth 0.
	 * @return (int): The depth of the node.
	 */
	public int getDepth(){
		return depth;
	}
	
	public boolean isContinuousSplit(){
		return continuous_split;
	}
	
	public boolean isGTE(){
		return gte;
	}
	
	public int getSplitNumber(){
		return split_number;
	}
}
