package c45;

/**
 * Used to store attribute values and headers.
 * Used internally by Decision Tree to implement C4.5 algorithm.
 * @author Matthew Tetford
 */

public class Value {
	boolean attribute;
	boolean attribute_is_numeric;
	public boolean empty;
	
	String alpha_value;
	boolean numeric;
	int num_value;
	
	public Value(){
		empty = true;
	}
	
	public Value(int x){
		empty = false;
		numeric = true;
		num_value = x;
	}
	
	public Value(String s){
		empty = false;
		numeric = false;
		alpha_value = s.toLowerCase();
	}
	
	public String toString(){
		String s = "";
		
		if(numeric){
			s = (num_value + "");
		}else{
			s = alpha_value;
		}
		
		return s;
	}
	
	public int getNumValue(){
		return num_value;
	}
	
	public boolean isNumeric(){
		return numeric;
	}
	
	public boolean isEmpty(){
		return empty;
	}
	
	public boolean equals(Value test){
		boolean equals = false;
		if(!test.empty){
			if(test.numeric){
				if(test.num_value == num_value){
					equals = true;
				}
			}else if(test.alpha_value.equals(alpha_value)){
				equals = true;
			}
		}
		return equals;
	}
}
