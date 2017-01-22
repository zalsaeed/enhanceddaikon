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
		this.args = new Object[args.length];
		for(int i = 0 ; i < this.args.length ; i ++){ //deep copy
			this.args[i] = args[i];
		}
	}
	
	public TraceRecord (boolean enter_flag, /*@Nullable*/ Object obj, int nonce, int mi_index,
            Object[] args, Object ret_val, int exitLineNum){
		
		this.isEnter = enter_flag;
		this.obj = obj;
		this.nonce = nonce;
		this.index = mi_index;
		this.ret_val = ret_val;
		this.exitLineNumber = exitLineNum;
		this.args = new Object[args.length];
		for(int i = 0 ; i < this.args.length ; i ++){ //deep copy
			this.args[i] = args[i];
		}
	}

}
