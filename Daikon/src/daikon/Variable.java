package daikon;

import java.util.ArrayList;
import java.util.List;

public class Variable {
	
	/* variable name*/
	String varFullName;
	
	List<String> properties = new ArrayList<String>();
	
	
	public Variable(String name){
		this.varFullName = name;
	}
	
	public void addProp(String prop){
		this.properties.add(prop);
	}
	
	public boolean isEqaul (Variable other){
		if(this.varFullName.matches(other.varFullName) && 
				this.properties.size() == other.properties.size()){
			return true;
		}else
			return false;
	}

}
