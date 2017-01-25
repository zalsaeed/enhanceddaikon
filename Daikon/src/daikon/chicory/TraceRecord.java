package daikon.chicory;

import com.thoughtworks.xstream.XStream;

public class TraceRecord {
	
	boolean isEnter = true;
	Object obj;
	int nonce;
	int index;
	Object[] args;
	Object ret_val;
	int exitLineNumber;
	String objVersion;
	String returnVersion;
	XStream xstream = new XStream();
	
	
	public TraceRecord (boolean enter_flag, /*@Nullable*/ Object obj, int nonce, int index, Object[] args){
		xstream.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
		
		this.isEnter = enter_flag;
		if(obj != null){
			xstream.alias(obj.getClass().getName(), obj.getClass());
			this.objVersion = xstream.toXML(obj);
			this.obj = obj;
		}else {
			this.obj = null;
		}
		this.nonce = nonce;
		this.index = index;
		this.args = new Object[args.length];
		for(int i = 0 ; i < this.args.length ; i ++){ //deep copy
			this.args[i] = args[i];
		}
	}
	
	public TraceRecord (boolean enter_flag, /*@Nullable*/ Object obj, int nonce, int mi_index,
            Object[] args, Object ret_val, int exitLineNum){
		xstream.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
		
		this.isEnter = enter_flag;
		if(obj != null){
			xstream.alias(obj.getClass().getName(), obj.getClass());
			this.objVersion = xstream.toXML(obj);
			this.obj = obj;
		}else {
			this.obj = null;
		}		
		this.nonce = nonce;
		this.index = mi_index;
		
		if(ret_val != null){
			xstream.alias(ret_val.getClass().getName(), ret_val.getClass());
			this.returnVersion = xstream.toXML(ret_val);
			this.ret_val = ret_val;
		}else {
			this.ret_val = null;
		}
		
		this.exitLineNumber = exitLineNum;
		this.args = new Object[args.length];
		for(int i = 0 ; i < this.args.length ; i ++){ //deep copy
			this.args[i] = args[i];
		}
	}
	
	public Object getObj(){
		if(this.obj != null){
			return xstream.fromXML(this.objVersion);
		}else{
			return this.obj; //null
		}
	}
	
	public Object getRetObj(){
		if(this.ret_val != null){
			return xstream.fromXML(this.returnVersion);
		}else{
			return this.ret_val; //null
		}
	}

}
