package daikon;

import java.util.ArrayList;
import java.util.List;

public class VariableProp {
	
	String propIdentifier;
	
	List<String> propVals;
	
	public VariableProp(String propIdentifier){
		this.propIdentifier = propIdentifier;
		propVals = new ArrayList<String>();
	}
	
	public void addPropVal(String[] vals){
		assert vals.length > 0 : "There must be at least one prop value to be added to " + propIdentifier + " values.";
		
		for(int i = 0 ; i < vals.length ; i++){
			propVals.add(vals[i]);
		}
	}
	
	public void addPropVal(String val){
		propVals.add(val);
	}
	
	public String getPropIdentifier(){
		return propIdentifier;
	}
	
	public String getPropValsAsString(){
		StringBuffer buff = new StringBuffer();
		assert propVals.size() > 0 : "No property values to return in " + propIdentifier;
		
		buff.append(propVals.get(0));
		
		for(int i = 1 ; i < propVals.size(); i++){
			buff.append(" " + propVals.get(i));
		}
		return buff.toString();
	}

}
