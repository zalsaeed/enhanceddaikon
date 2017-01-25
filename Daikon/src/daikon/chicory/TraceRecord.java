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
	String version;
	XStream xstream = new XStream();
	
	public TraceRecord (boolean enter_flag, /*@Nullable*/ Object obj, int nonce, int index, Object[] args){
		xstream.setMode(XStream.ID_REFERENCES);
		
		this.isEnter = enter_flag;
		if(obj != null){
			xstream.alias(obj.getClass().getName(), obj.getClass());
			this.version = xstream.toXML(obj);
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
		xstream.setMode(XStream.ID_REFERENCES);
		
		this.isEnter = enter_flag;
		if(obj != null){
			xstream.alias(obj.getClass().getName(), obj.getClass());
			this.version = xstream.toXML(obj);
			this.obj = obj;
		}else {
			this.obj = null;
		}		this.nonce = nonce;
		this.index = mi_index;
		this.ret_val = ret_val;
		this.exitLineNumber = exitLineNum;
		this.args = new Object[args.length];
		for(int i = 0 ; i < this.args.length ; i ++){ //deep copy
			this.args[i] = args[i];
		}
	}
	
	public Object getObj(){
		if(this.obj != null){
			return xstream.fromXML(version);
		}else{
			return this.obj; //null
		}
	}

}
