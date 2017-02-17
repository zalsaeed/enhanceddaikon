package daikon;

import java.util.ArrayList;
import java.util.List;

public class Variable {
	
	/* variable name*/
	String name;
	
	List<String> properties = new ArrayList<String>();
	
	
	public Variable(String name){
		this.name = name;
	}
	
	public void addProp(String prop){
		this.properties.add(prop);
	}
	
	public boolean isEqaul (Variable other){
		if(this.name.matches(other.name) && 
				this.properties.size() == other.properties.size()){
			return true;
		}else
			return false;
	}

}
