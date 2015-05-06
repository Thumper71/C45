package c45;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

/**
 * Creates and prints a decision tree parsed from a user defined
 * data set and target attribute. This implementation builds the
 * tree until no attributes are left to classify on or the
 * information gain at the current node is 0.
 * An optional test is also performed if the user specifies a
 * testing file.
 * 
 * Input: 	User defined training dataset.
 * 			User defined target attribute.
 * 			User defined testing dataset.
 * 
 * Output:	Simple decision tree visualization.
 * 			Test output.
 * 
 * @author Matthew Tetford
 *
 */
public class DecisionTree {
	private static Dataset training_dataset;
	private static Value target_class;
	private static Dataset testing_dataset;
	private static boolean testing = false;
	private static char percent = '%';
	private static String output_file = "C45_Rules.txt";
	private static int min_continuous_node_size;
	private static int max_tree_depth;
	private static double min_split_gain;
	
	private static boolean VERBOSE_TREE_PRUNE = false;
	
	public static void main(String[] args){
		
		getUserInput();
		
		long begin_time = System.currentTimeMillis();
		
		/*	Testing disabled in this build
		 * 
		 *	if(testing){
		 *		training_dataset.makeDatasetConsistent(testing_dataset);
		 *	}
		 */
		
		C45Node root = new C45Node(training_dataset, null, target_class);
		buildTree(root);
		
		postPruneTree(root);
		
		long end_time = System.currentTimeMillis();
		long duration = (end_time - begin_time);
		
		println("\nPrinting the decision tree for " + target_class.toString() + ":");
		
		//Print the results to the console
		PrintStream output = System.out;
		printTree(root, output);
		output.format("%n");
		if(testing){
			testTree(root, output, testing_dataset);
		}
		
		//Print the results to hardcoded file
		output = setupOutputStream(output_file);
		printTree(root, output);
		output.format("%n");
		if(testing){
			testTree(root, output, testing_dataset);
		}
		
		println("\nBuilding and pruning tree took " + duration + " milliseconds.");
		
		output.flush();
		output.close();
	}
	
	/**
	 * Gets user input to open and create both the training and
	 * testing datasets.
	 * Sets the global variables.
	 */
	public static void getUserInput(){
		Scanner console = new Scanner(System.in);
		
		//Getting the filename of the training data
		print("Enter the name of the training data file you wish to use: ");
		String training_filename = console.nextLine();
		training_dataset = new Dataset(training_filename);
		
		//Getting the filename of the testing data
		
		/*	Testing disabled in this build.
		 * 
		 *	print("If desired, enter the name of the testing file you wish to use: ");
		 *	String testing_filename = console.nextLine();
		 *	if(testing_filename.length() < 1){
		 *		println("No testing file specified, continuing without test.");
		 *	}else{
		 *		testing = false;
		 *		println("Sorry! Tree testing is disabled in this implementation...");
		 *		//testing_dataset = new Dataset(testing_filename);
		 *	}
		 */
		
		//Getting the desired minimum number of continuous records in a node
		boolean min_size_ok = false;
		while(!min_size_ok){
			print("Please enter the minimum number of continuous values to store in a node(enter 0 to ignore): ");
			int temp_min_size = console.nextInt();
			if(temp_min_size >= 0){
				min_size_ok = true;
				min_continuous_node_size = temp_min_size;
			}
		}
		
		//Getting the desired maximum tree depth
		boolean max_depth_ok = false;
		while(!max_depth_ok){
			print("Please enter the maximum tree depth (enter 0 to ignore): ");
			int temp_max_depth = console.nextInt();
			if(temp_max_depth >= 0){
				max_depth_ok = true;
				max_tree_depth = temp_max_depth;
			}
		}
		
		//Getting the minimum gain for node splitting
		boolean min_gain_ok = false;
		while(!min_gain_ok){
			print("Please enter the minimum gain to split on (>= 0): ");
			double temp_min_gain = console.nextDouble();
			if(temp_min_gain >= 0){
				min_gain_ok = true;
				min_split_gain = temp_min_gain;
			}
		}
		
		//Select the target attribute for classification
		println("Please select an attribute to classify on.");
		Value[] attributes = training_dataset.getAttributeArray();
		int number = 1;
		for(Value attribute : attributes){
			println("\t" + attribute.toString() + " : " + number);
			number++;
		}
		print("(Enter a number between 1 and " + (number-1) + "): ");
		int class_index = console.nextInt();
		target_class = training_dataset.getAttribute(class_index-1);
		
		console.close();
	}
	
	/**
	 * Sets up our output for a given filename.
	 * @param filename (String): The filename we wish to use.
	 * @return (PrintStream): The print stream for the given file.
	 */
	public static PrintStream setupOutputStream(String filename){
		PrintStream output = null;
		try {
			output = new PrintStream(new FileOutputStream(filename, false));
		} catch (FileNotFoundException e) {
			System.err.println("Write file not found.");
			//e.printStackTrace();
			System.exit(1);
		}
		return output;
	}
	
	/**
	 * Builds the decision tree.
	 * @param current (ID3Node): The node we start to build the tree from (typically the root).
	 */
	public static void buildTree(C45Node current){
		HashSet<Value> remaining_attributes = current.getRemainingAttributes();
		double max_gain = 0;
		Value split_attribute = new Value();
		int split_value = 0;
		
		for(Value attribute : remaining_attributes){
			if(attribute.attribute_is_numeric){
				HashSet<Integer> values = current.getDataset().getValueNumericSet(attribute);
				
				for(int value : values){
					double gain = continuousGain(current.getDataset(), target_class, attribute, value);
					if(gain > max_gain){
						max_gain = gain;
						split_attribute = attribute;
						split_value = value;
					}
				}
			}else{
				double gain = gain(current.getDataset(), target_class, attribute);
				if(gain > max_gain){
					max_gain = gain;
					split_attribute = attribute;
				}
			}
		}
		
		if(max_gain > 0 && max_gain >= min_split_gain){
			//Build and assign children
			if(split_attribute.attribute_is_numeric){
				Dataset upper = new Dataset(current.getDataset(), split_attribute, split_value, true);
				C45Node upper_child = new C45Node(upper, current, split_attribute, split_value, true);
				current.addChild(upper_child);
				buildTree(upper_child);
				Dataset lower = new Dataset(current.getDataset(), split_attribute, split_value, false);
				C45Node lower_child = new C45Node(lower, current, split_attribute, split_value, false);
				current.addChild(lower_child);
				buildTree(lower_child);
			}else{
				HashSet<Value> child_values = current.getDataset().getValueSet(split_attribute);
				for(Value value : child_values){
					Dataset subset = new Dataset(current.getDataset(), split_attribute, value);
					C45Node child = new C45Node(subset, current, split_attribute, value);
					current.addChild(child);
					buildTree(child);
				}
			}
		}
	}
	
	/**
	 * Prunes the resulting tree after generation according using values recorded
	 * during user input.
	 * @param current (C45Node)
	 */
	public static void postPruneTree(C45Node current){
		
		if(!current.isRoot()){
			int size = (current.getDataset().height-1);
			if(size < min_continuous_node_size){
				if(VERBOSE_TREE_PRUNE){
					println("Children deleted. Target value count was " +
							size + ", threshold is " + min_continuous_node_size);
				}
				
				current.clearChildren();
				
			}else if(current.getDepth() > max_tree_depth && max_tree_depth != 0){
				if(VERBOSE_TREE_PRUNE){
					println("Children deleted. Tree became too deep.");
				}
				
				current.clearChildren();
				
			}else{
				ArrayList<C45Node> children = current.getChildren();
				for(C45Node child : children){
					postPruneTree(child);
				}
			}
		}else{
			ArrayList<C45Node> children = current.getChildren();
			for(C45Node child : children){
				postPruneTree(child);
			}
		}
	}
	
	/**
	 * Prints a decision tree to the given print stream.
	 * @param current (ID3Node): The node we wish to start at (typically the root).
	 * @param output (PrintStream): Where we wish to put the output.
	 */
	public static void printTree(C45Node current, PrintStream output){
		String indent = "";
		for(int i = 1; i < current.getDepth(); i++){
			indent += " ";
		}
		
		if(current.hasChild()){
			ArrayList<C45Node> children = current.getChildren();
			if(!current.isRoot()){
				
				Value split = current.getSplitAttribute();
				Value value = current.getSplitValue();
				
				if(current.isContinuousSplit()){
					String operator = "";
					if(current.isGTE()){
						operator = ">=";
					}else{
						operator = "<";
					}
					
					int number = current.getSplitNumber();
					output.format(indent + "If %s is %s %d,%n", split.toString(), operator, number);
					
				}else{
					output.format(indent + "If %s is %s,%n", split.toString(), value.toString());
				}
			}
			for(C45Node child : children){
				printTree(child, output);
			}
		}else{
			Value split = current.getSplitAttribute();
			Value value = current.getSplitValue();
			
			
			if(current.isContinuousSplit()){
				String operator = "";
				if(current.isGTE()){
					operator = ">=";
				}else{
					operator = "<";
				}
				
				int number = current.getSplitNumber();
				output.format(indent + "If %s is %s %d,%n", split.toString(), operator, number);
			}else{
				output.format(indent + "If %s is %s,%n", split.toString(), value.toString());
			}
			
			if(target_class.attribute_is_numeric){
				if(current.getDataset().getValueSet(target_class).size() > 1){
					String range = current.getDataset().getRange(target_class);
					double average = current.getDataset().getAverage(target_class);
					int median = current.getDataset().getMedian(target_class);
					output.format(" " + indent + "Then %s is %s, with average %.2f, "
							+ "median %d, and %d values.%n",
							target_class.toString(), range, average, median, (current.getDataset().height-1));
				}else{
					output.format(" " + indent + "Then %s is %s.%n",
							target_class.toString(), current.getMaxValue(target_class).toString());
				}
				
				
				
			}else{
				Value max_value = current.getMaxValue(target_class);
				double accuracy = current.getAccuracy(target_class, max_value) * 100;
				output.format(" " + indent + "Then %s is %s, with usefulness measure %.2f%c.%n",
						target_class.toString(), max_value.toString(), accuracy, percent);
			}
		}
	}
	
	/**
	 * Compares the testing dataset given to the constructed tree and prints how
	 * accurate the tree's predictions are.
	 * @param root (ID3Node): The root of our constructed tree.
	 * @param output (PrintStream): Where to put the results. (Typically either System.out or a file)
	 * @param testing_dataset (Dataset): The dataset we attempt to predict.
	 */
	private static void testTree(C45Node root, PrintStream output, Dataset testing_dataset){
		double total = testing_dataset.height-1;
		double correct = 0;
		
		for(int i = 1; i < testing_dataset.height; i++){
			ArrayList<Value> record = testing_dataset.getRowArrayList(i);
			if(testTree(record, root)){
				correct++;
			}
		}
		
		double accuracy = (correct/total) * 100;
		output.format("The decision tree predicted the correct value"
				+ " with %.2f%c usefulness measure.%n", accuracy, percent);
	}
	
	/**
	 * Helper method for the main test tree method. Tests one record in
	 * the testing dataset and returns true or false depending on if
	 * the trees prediction was correct.
	 * @param record (ArrayList<String>): The record to be tested.
	 * @param root (ID3Node): The root of the tree we wish to test.
	 * @return (boolean): Is false if our prediction was wrong, true otherwise.
	 */
	private static boolean testTree(ArrayList<Value> record, C45Node node){
		boolean correct = false;
		
		if(node.hasChild()){
			ArrayList<C45Node> children = node.getChildren();
			for(C45Node child : children){
				for(Value value : record){
					if(child.getSplitValue().equals(value)){
						record.remove(value);
						return testTree(record, child);
					}
				}
			}
		}else{
			for(Value value : record){
				if(value.equals(node.getMaxValue(target_class))){
					correct = true;
				}
			}
		}
		
		return correct;
	}
	
	/**
	 * Returns the entropy of the given dataset and target class.
	 * @param dataset (Value[][]): The set of data we wish to calculate entropy for.
	 * @param target_class (Value): The attribute we want to classify by.
	 * @return: (double) The calculated entropy.
	 */
	public static double entropy(Dataset dataset, Value target_class){
		double entropy = 0d;

		double total_rows = (double)(dataset.height - 1);
		HashSet<Value> values = dataset.getValueSet(target_class);
		for(Value value : values){
			double value_count = (double)dataset.getValueCount(target_class, value);
			double ratio = (value_count / total_rows);
			entropy -= ratio * Math.log(ratio)/Math.log(2);
		}
		
		return entropy;
	}
	
	/**
	 * Calculates the entropy of two given probabilites.
	 * @param probA: (double) Probability one.
	 * @param probB: (double) Probability two.
	 * @return: (double) The calculated entropy.
	 */
	public static double binaryEntropy(double probA, double probB){
		double entropy = 0d;
		
		entropy -= probA * Math.log(probA)/Math.log(2);
		entropy -= probB * Math.log(probB)/Math.log(2);
		
		return entropy;
	}
	
	/**
	 * Calculate information gain for a continuous numerical attribute and
	 * a number to split on.
	 * @param dataset (Dataset)
	 * @param target_class (Value)
	 * @param split_attribute (Value)
	 * @param number (int)
	 * @return (double)
	 */
	public static double continuousGain(Dataset dataset, Value target_class, Value split_attribute, int number){
		double gain = 0d;
			
		Dataset lower_dataset = new Dataset(dataset, split_attribute, number, false);
		Dataset upper_dataset = new Dataset(dataset, split_attribute, number, true);
		
		double probability_lower = (double)(lower_dataset.height-1) / (double)(dataset.height-1);
		double probability_upper = (double)(upper_dataset.height-1) / (double)(dataset.height-1);
				
		double temp_gain = binaryEntropy(probability_upper, probability_lower);
				
		double lower_split_entropy = entropy(lower_dataset, target_class);
		double upper_split_entropy = entropy(upper_dataset, target_class);
		
		if((lower_split_entropy + upper_split_entropy) <= 0){
			gain = 0d;
			return gain;
		}
				
		temp_gain -= (probability_lower * lower_split_entropy);
		temp_gain -= (probability_upper * upper_split_entropy);
			
		if(temp_gain > gain){
			gain = temp_gain;
		}
		
		return gain;
	}
	
	/**
	 * Returns the information gain from the selection of a given attribute.
	 * @param dataset (Dataset): The set of data we wish to calculate information gain for.
	 * @param target_class (String): The attribute we use as the classifier.
	 * @param split_attribute (String): The attribute we are calculating the information for by splitting.
	 * @return: (double) The calculated information gain.
	 */
	public static double gain(Dataset dataset, Value target_class, Value split_attribute){
		//println("\n\nCalculating Gain for Target " + target_class.toString() + " on split " + split_attribute.toString());
		double gain = 0;
		
		//Get datasets for each category of split_attribute
		HashSet<Value> split_values = dataset.getValueSet(split_attribute);
			
		//Calculate gain = entropy(dataset, class_attribute) - sum of entropy(subdatasets, categories)
		gain = entropy(dataset, target_class);
		//println("Base Gain is: " + gain);
		for(Value value : split_values){
			double ratio = (double)dataset.getValueCount(split_attribute, value) / (double)(dataset.height-1);
			double split_entropy = entropy(new Dataset(dataset, split_attribute, value), target_class);
			double temp = ratio * split_entropy;
			gain -= temp;
		}
		
		//println("Total Gain on split is " + gain + "\n\n");
		return gain;
	}
	
	/**
	 * Just a helper for printing in line.
	 * @param o
	 */
	public static void print(Object o){
		System.out.print(o);
	}
	
	/**
	 * Just a helper for printing a line.
	 * @param o
	 */
	public static void println(Object o){
		System.out.println(o);
	}
	
	/**
	 * Just a helper for printing a line.
	 * @param o
	 */
	public static void println(){
		System.out.println();
	}
}