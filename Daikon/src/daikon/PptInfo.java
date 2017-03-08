package daikon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PptInfo {
	
	String name;
	
	/**Ppt given type **/
	String type = null;
	
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
		//Store the keys and value ...
		var_to_prop_reps.put(varName, properties);
	}

}
