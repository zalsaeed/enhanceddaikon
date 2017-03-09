package daikon;

import java.util.HashMap;

public class TraceInfo {

	/**Trace method name**/
	String name;
	
	/* point type (e.g. OBJECT, ENTER, or EXIT10) */
	String point;
	
	/* the package name without any noise */
	String pckg;
	
	/* the class name without any noise */
	String cls;
	
	/* the method and its arguments full name (e.g. setX(int)) */
	String method = null;
	
	/** method nonce as given in the trace **/
	String nonce = null;
	
	/** parent has Id associated with them 
	 * Mostly they are 1, but I can't confirm that this is always the case 
	 * Thus, I'm defining as 9, meaning there was no parent ID given.
	 * Otherwise, it should be whatever is in the traces. This must be refactored 
	 * once I have better knowledge about the possible values of parent ID.  
	 */
	String parentID = null;
	
	HashMap<String,Value> values = new HashMap<String,Value>();
	
	public TraceInfo (String name){
		this.name = name;
	}
	
	public void setNonce(String nonce){
		this.nonce = nonce;
	}
	
	public void addValue(String vName, String gValue, String sens){
		Value newValue = new Value();
		newValue.valueName = vName;
		newValue.givenValue = gValue;
		newValue.sensicalModifier = sens;
		
		values.put(vName, newValue);
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getNonce(){
		return this.nonce;
	}
	
	public Value getValueByName (String valueName){
		return this.values.get(valueName);
	}

}
