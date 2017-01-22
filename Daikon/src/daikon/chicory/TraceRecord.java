package daikon.chicory;

public class TraceRecord {
	
	boolean isEnter = true;
	Object obj;
	int nonce;
	int index;
	Object[] args;
	Object ret_val;
	int exitLineNumber;
	
	public TraceRecord (boolean enter_flag, /*@Nullable*/ Object obj, int nonce, int index, Object[] args){
		this.isEnter = enter_flag;
		this.obj = obj;
		this.nonce = nonce;
		this.index = index;
		this.args = args;
	}
	
	public TraceRecord (boolean enter_flag, /*@Nullable*/ Object obj, int nonce, int mi_index,
            Object[] args, Object ret_val, int exitLineNum){
		
		this.isEnter = enter_flag;
		this.obj = obj;
		this.nonce = nonce;
		this.index = mi_index;
		this.args = args;
		this.ret_val = ret_val;
		this.exitLineNumber = exitLineNum;
		
	}

}
