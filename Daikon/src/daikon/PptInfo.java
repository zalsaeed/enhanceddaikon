package daikon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PptInfo {
	
	/* Full ppt string e.g. ppt SimpleExample.Modifier.Modifier():::ENTER*/
	String name;
	
	/* point type (e.g. OBJECT, ENTER, or EXIT10) */
	String point;
	
	/* the package name without any noise */
	String pckg;
	
	/* the class name without any noise */
	String cls;
	
	/* the method and its arguments full name (e.g. setX(int)) */
	String method = null;
	
	/*Ppt given type */
	String type = null; //enter, exit, subexit, object
	
	/** if a parent exists, then its name should be this **/
	String parentName = null;
	
	/** parent has Id associated with them 
	 * Mostly they are 1, but I can't confirm that this is always the case 
	 * Thus, I'm defining as 9, meaning there was no parent ID given.
	 * Otherwise, it should be whatever is in the traces. This must be refactored 
	 * once I have better knowledge about the possible values of parent ID.  
	 */
	String parentID = null;
	
	List<String> arrangedKeys = new ArrayList<String>();
	final HashMap<String,String[]> var_to_prop_reps = new HashMap<String,String[]>();
	HashMap<String,List<VariableProp>> var_to_propVals = new HashMap<String,List<VariableProp>>();
	
	public PptInfo (String name){
		this.name = name;
	}
	
	public void setType(String name){
		this.type = name;
	}
	
	public void setParentName(String name){
		this.parentName = name;
	}
	
	public void setParentID(String id){
		this.parentID = id;
	}
	
	public void addNewVariable (String varName, String[] properties){
		//TODO check for whitespace in the name and the properties.
		//Store the key
		arrangedKeys.add(varName);
		//Store the keys and value ... this is the old way it must be removed and refacor all the code depends on it to use the new way
		var_to_prop_reps.put(varName, properties);
		
		List<VariableProp> props = new ArrayList<VariableProp>();
		if(properties.length > 0){
			for(int i = 0 ; i < properties.length ; i++){
				String[] prop = properties[i].split("\\s+");
				
				assert prop[0].equals(" ") : "Observed an irregular property when adding it to PptInfo";
				
				VariableProp temp = new VariableProp(prop[1]);
				
				for(int j = 2 ; j < prop.length ; j ++)
					temp.addPropVal(prop[j]);
				props.add(temp);
			}
		}
		
		var_to_propVals.put(varName, props);
		
	}

}
