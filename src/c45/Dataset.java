package c45;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * Used to store and perform operations on datasets.
 * Used internally by Decision Tree to implement C4.5 algorithm.
 * @author Matthew Tetford
 */
public class Dataset {
	private Value[][] dataset;
	public final int height;
	public final int width;
	
	public Dataset(){
		dataset = new Value[0][0];
		height = 0;
		width = 0;
	}
	
	/**
	 * Constructs a Dataset from a given text file.
	 * Files must be properly formatted csv's.
	 * All rows must be the same length.
	 * Stray newlines do not matter.
	 * All values are converted to lower case.
	 * @param filename (String): The filename of the file we wish to parse.
	 */
	public Dataset(String filename){
		ArrayList<String> rows = new ArrayList<String>();
		int tempHeight = 0;
		int tempWidth = 0;
		
		//Read file, line by line, append to a list
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String row;	
			
			//Try just saving lines on first go
			while((row = br.readLine()) != null){
				row = row.replaceAll("(\\r|\\n|\")", "");
				row = row.replaceAll("(;)", "");
				if(row.length() > 0){
					rows.add(row);
					tempHeight++;
				}
		    }//end while read file
			
			//Close the things
			br.close();
		}
		catch (FileNotFoundException e) {
			//e.printStackTrace();
			System.err.println("File not found");
			System.exit(1);
		} 
	    catch (IOException e) {
			//e.printStackTrace();
			System.err.println("IO Error");
			System.exit(1);
		}

		//Get the row length, all rows should be the same length
		tempWidth = rows.get(0).split(",+").length;
		
		//can now init array since we now know the size of each dimension
		height = tempHeight;
		width = tempWidth;
		dataset = new Value[width][height];
		//println("x = " + row_length + " y = " + transaction_count);
		
		//Now tokenize
		int y = 0;
		for(String row : rows){
			String[] tokens = row.split(",+");
			for(int x = 0; x < tokens.length; x++){
				tokens[x] = tokens[x].toLowerCase();
				if(tokens[x].matches("-?\\d+(\\.\\d+)?")){
					dataset[x][y] = new Value(Integer.parseInt(tokens[x]));
				}else{
					dataset[x][y] = new Value(tokens[x]);
				}
			}
			y++;
		}
		
		//Need to determine if some columns are numeric
		for(int x = 0; x < width; x++){
			boolean numeric = true;
			for(y = 1; y < height; y++){
				if(!dataset[x][y].numeric){
					numeric = false;
					break;
				}
			}
			dataset[x][0].attribute = true;
			dataset[x][0].attribute_is_numeric = numeric;
		}
	}//end from file constructor
	
	/**
	 * Creates a subset Dataset from the given Dataset.
	 * @param superset (Dataset): The original dataset.
	 * @param attribute (String): The attribute we will split on.
	 * @param value (String): The value we will split on.
	 */
	public Dataset(Dataset superset, Value attribute, Value value){
		
		Value[][] superset_array = superset.toArray();
		width = superset_array.length;
		int tempHeight = 0;
		ArrayList<Integer> row_indexes = new ArrayList<Integer>();
		row_indexes.add(0);
		
		int header_index = superset.getHeaderIndex(attribute);
		for(int y = 0; y < superset_array[0].length; y++){
			if(superset_array[header_index][y].equals(value)){
				row_indexes.add(y);
				tempHeight++;
			}
		}
		
		height = tempHeight+1;
		dataset = new Value[width][height];
		
		int y = 0;
		for(Integer row : row_indexes){
			for(int x = 0; x < width; x++){
				dataset[x][y] = superset_array[x][row];
			}
			y++;
		}
	}
	
	/**
	 * Creates a subset Dataset from the given Dataset using a
	 * continuous split value.
	 * @param superset (Dataset): The original dataset.
	 * @param attribute (Value): The attribute we will split on.
	 * @param value (int): The value we will split on.
	 * @param greater_than_equal (boolean): Whether we return the upper or lower dataset.
	 */
	public Dataset(Dataset superset, Value attribute, int value, boolean greater_than_equal){
		
		Value[][] superset_array = superset.toArray();
		width = superset_array.length;
		int tempHeight = 0;
		ArrayList<Integer> row_indexes = new ArrayList<Integer>();
		row_indexes.add(0);
		
		int header_index = superset.getHeaderIndex(attribute);
		for(int y = 1; y < superset_array[0].length; y++){
			if(greater_than_equal){
				if(superset_array[header_index][y].num_value >= value){
					row_indexes.add(y);
					tempHeight++;
				}
			}else{
				if(superset_array[header_index][y].num_value < value){
					row_indexes.add(y);
					tempHeight++;
				}
			}
		}
		
		height = tempHeight+1;
		dataset = new Value[width][height];
		
		int y = 0;
		for(Integer row : row_indexes){
			for(int x = 0; x < width; x++){
				dataset[x][y] = superset_array[x][row];
			}
			y++;
		}
	}
	
	/**
	 * Makes the attributes/headers of the datasets consistent.
	 * Assumes similiar data is contained within the same columns.
	 * The dataset given in parameters is changed.
	 * @param edit (Dataset): The dataset we wish to make consistent with this one.
	 */
	public void makeDatasetConsistent(Dataset edit){
		for(int x = 0; x < width; x++){
			Value value = dataset[x][0];
			edit.setValue(x, 0, value);
		}
	}
	
	/**
	 * Gets the header or attribute at the given index.
	 * @param x (int): The index of the attribute we want to retrieve.
	 * @return (String): The attribute.
	 */
	public Value getAttribute(int x){
		return dataset[x][0];
	}
	
	/**
	 * Gets the headers or attributes of the Dataset.
	 * @return (HashSet<String>): The set of headers / attributes.
	 */
	public HashSet<Value> getAttributeSet(){
		HashSet<Value> attributes = new HashSet<Value>();
		HashSet<String> test = new HashSet<String>();
		
		for(int x = 0; x < width; x++){
			if(test.add(dataset[x][0].toString())){
				attributes.add(dataset[x][0]);
			}
		}
		return attributes;
	}
	
	/**
	 * Gets an array of headers or attributes of the Dataset.
	 * @return (Value[]): An array of the headers / attributes.
	 */
	public Value[] getAttributeArray(){
		Value[] attributes = new Value[width];
		
		for(int x = 0; x < width; x++){
			attributes[x] = dataset[x][0];
		}
		
		return attributes;
	}
	
	/**
	 * Gets the total values in the attribute of the dataset.
	 * @param attribute (String): Attribute we wish to count values for.
	 * @return (int): The number of distinct values.
	 */
	public int getAttributeValueCount(Value attribute){
		HashSet<String> values = new HashSet<String>();
		int attribute_index = getHeaderIndex(attribute);
		
		for(int y = 1; y < height; y++){
			values.add(dataset[attribute_index][y].toString());
		}
		
		return values.size();
	}
	
	/**
	 * Simply gets the value at a given index of the Dataset.
	 * @param x (int): X coord of value.
	 * @param y (int): Y coord of value.
	 * @return (String): The value at the index given.
	 */
	public Value getValue(int x, int y){
		return dataset[x][y];
	}
	
	/**
	 * Simply sets the string at the given index.
	 * @param x (int): X coord of value we want to replace.
	 * @param y (int): Y coord of value we want to replace.
	 * @param string (String): The string we wish to place at the given coord.
	 */
	public void setValue(int x, int y, Value value){
		dataset[x][y] = value;
	}
	
	/**
	 * Gets the total number of the given value in the given attribute.
	 * @param attribute (String): Index of the desired attribute.
	 * @param value (String): The value we wish to get the total for.
	 * @return (int): The total of the count.
	 */
	public int getValueCount(Value attribute, Value value){
		int attribute_index = getHeaderIndex(attribute);
		int count = 0;
		for(int y = 1; y < height; y++){
			if(dataset[attribute_index][y].equals(value)){
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Returns the maximum value of the given numerical attribute.
	 * @param attribute (Value)
	 * @return (int)
	 */
	public int getMaxValue(Value attribute){
		int index = getHeaderIndex(attribute);
		int max = dataset[index][1].num_value;
		for(int y = 2; y < height; y++){
			if(dataset[index][y].num_value > max){
				max = dataset[index][y].num_value;
			}
		}
		
		return max;
	}
	
	/**
	 * Returns the minimum value of the given numerical attribute.
	 * @param attribute (Value)
	 * @return (int)
	 */
	public int getMinValue(Value attribute){
		int index = getHeaderIndex(attribute);
		int min = dataset[index][1].num_value;
		for(int y = 2; y < height; y++){
			//System.out.println("Min test " + min + " > " + dataset[index][y].num_value + "\n");
			if(dataset[index][y].num_value < min){
				min = dataset[index][y].num_value;
			}
		}
		
		return min;
	}
	
	/**
	 * Returns the mean of the given numerical attribute.
	 * @param attribute (Value)
	 * @return (double)
	 */
	public double getAverage(Value attribute){
		double avg = 0;
		
		if(attribute.attribute_is_numeric){
			int index = getHeaderIndex(attribute);
			double sum = 0;
			for(int y = 1; y < height; y++){
				sum += (double)dataset[index][y].num_value;
			}
			avg = (sum / (double)(height-1));
		}else{
			System.err.println("Error calculating average");
		}
		
		return avg;
	}
	
	/**
	 * Returns the median of the given numerical attribute.
	 * @param attribute (Value)
	 * @return (int)
	 */
	public int getMedian(Value attribute){
		int median = 0;
		
		if(attribute.attribute_is_numeric){
			ArrayList<Integer> values = new ArrayList<Integer>();
			int index = getHeaderIndex(attribute);
			for(int y = 1; y < height; y++){
				values.add(dataset[index][y].num_value);
			}
			Collections.sort(values);
			
			if(values.size() % 2 == 1){
				//We have an odd number of values... easy case
				median = values. get((values.size()/2) - 1);
			}else if(values.size() > 0){
				//We have an even number of values... not easy case
				//System.out.println("Odd number of values\n");
				int num1 = values.get((values.size()/2) - 1);
				int num2 = values.get(values.size()/2);
				
				median = (num1 + num2) / 2;
			}else{
				//We have one value, so just return it
				median = values.get(0);
			}
			
			
		}else{
			System.err.println("Error: Calculating median for non numeric value");
		}
		
		return median;
	}
	
	/**
	 * Returns the numerical set for the given numerical attribute.
	 * @param attribute (Value)
	 * @return (Hashset<Integer>)
	 */
	public HashSet<Integer> getValueNumericSet(Value attribute){
		HashSet<Integer> values = new HashSet<Integer>();
		int index = getHeaderIndex(attribute);
		
		for(int y = 1; y < height; y++){
			values.add(dataset[index][y].num_value);
		}
		
		return values;
	}
	
	/**
	 * Returns the number of values greater than or equal to the given
	 * number for the given attribute.
	 * @param attribute (Value)
	 * @param number (int)
	 * @return (int)
	 */
	public int getValueCountGTE(Value attribute, int number){
		int count = 0;
		
		if(attribute.attribute_is_numeric){
			int attribute_index = getHeaderIndex(attribute);
			for(int y = 1; y < height; y++){
				if(dataset[attribute_index][y].num_value >= number){
					count++;
				}
			}
		}else{
			System.err.println("Attempted to split non numeric attribute. Exiting");
			System.exit(1);
		}
		
		return count;
	}
	
	/**
	 * Returns the number of values less than the given
	 * number for the given attribute.
	 * @param attribute (Value)
	 * @param number (int)
	 * @return (int)
	 */
	public int getValueCountLT(Value attribute, int number){
		int count = 0;
		
		if(attribute.attribute_is_numeric){
			int attribute_index = getHeaderIndex(attribute);
			for(int y = 1; y < height; y++){
				if(dataset[attribute_index][y].num_value < number){
					count++;
				}
			}
		}else{
			System.err.println("Attempted to split non numeric attribute. Exiting");
			System.exit(1);
		}
		
		return count;
	}
	
	/**
	 * Gets a set of the distinct values in a given attribute.
	 * @param attribute (Value): The attribute we wish to get the values from.
	 * @return (HashSet<Value>): The set of values within the attribute.
	 */
	public HashSet<Value> getValueSet(Value attribute){
		HashSet<Value> values = new HashSet<Value>();
		HashSet<String> test = new HashSet<String>();
		int attribute_index = getHeaderIndex(attribute);
		
		for(int y = 1; y < height; y++){
			if(!attribute.toString().equals(dataset[attribute_index][y].toString())){
				if(test.add(dataset[attribute_index][y].toString())){
					values.add(dataset[attribute_index][y]);
				}
			}
		}
		return values;
	}
	
	/**
	 * Returns the range of a given value.
	 * @param value (Value)
	 * @return (String)
	 */
	public String getRange(Value value){
		String range = "";
		
		int min = getMinValue(value);
		int max = getMaxValue(value);
		
		if(min == max){
			range = (max + "");
		}else{
			range = (min + " - " + max);
		}
		
		return range;
	}
	
	/**
	 * Given an index for the dataset, return a record as an ArrayList of Values.
	 * @param index (int)
	 * @return (ArrayList<Value>)
	 */
	public ArrayList<Value> getRowArrayList(int index){
		ArrayList<Value> row = new ArrayList<Value>();
		
		for(int x = 0; x < width; x++){
			row.add(dataset[x][index]);
		}
		
		return row;
	}
	
	/**
	 * Gets the height of the Dataset (The number of rows in the Dataset, including headers).
	 * @return (int): The height of the Dataset.
	 */
	public int getHeight(){
		return height;
	}
	
	/**
	 * Gets the width of the Dataset (The number of attributes / headers).
	 * @return (int): The width of the Dataset.
	 */
	public int getWidth(){
		return width;
	}
	
	/**
	 * Gets the index of the given header.
	 * Returns -1 if the header isn't found.
	 * @param header (String): The header we wish to get the index of.
	 * @return (int): The index of the header.
	 */
	public int getHeaderIndex(Value header){
		int index = -1;
		for(int x = 0; x < width; x++){
			if(dataset[x][0].equals(header)){
				index = x;
			}
		}
		return index;
	}
	
	/**
	 * Returns a formatted string of the Dataset.
	 * @return (String): A formatted string of the Dataset.
	 */
	public String toString(){
		String s = "";
		
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				s += dataset[x][y].toString();
				s += "\t\t";
			}
			s += "\n";
		}
		
		return s;
	}
	
	/**
	 * Gets the array of the Dataset.
	 * @return (String[][]): The Dataset's data array.
	 */
	public Value[][] toArray(){
		return dataset;
	}
}
