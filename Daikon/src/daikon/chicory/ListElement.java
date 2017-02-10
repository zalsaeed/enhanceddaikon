package daikon.chicory;

import java.lang.reflect.Modifier;

public class ListElement extends DaikonVariableInfo{

	/*The class */
	private Class<?> clazz;
	
	private boolean is_static;
	
	private boolean is_primitive;
	
	public ListElement(Class<?> type, String theName, String typeName, 
			String repTypeName, boolean arr) {
		super(theName, typeName, repTypeName, arr);
		clazz = type;
		
		is_static = Modifier.isStatic(clazz.getModifiers());
		is_primitive = false;
		
		
	}

	@Override
	public Object getMyValFromParentVal(Object parentVal) {
		return null; //dump value for now
		//throw new RuntimeException ("You cant get an object value from a list (ListElement)");
	}

	@Override
	public VarKind get_var_kind() {
		if (is_static)
			return VarKind.VARIABLE;
		else
			return VarKind.LISTELEMENT;
		//throw new RuntimeException ("No var-kind for ListElement");
	}

	public boolean is_primitive() {
		return is_primitive;
	}


}
