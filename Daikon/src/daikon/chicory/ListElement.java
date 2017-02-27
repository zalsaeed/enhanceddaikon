package daikon.chicory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class ListElement extends DaikonVariableInfo{

	/*The class */
	private Class<?> clazz;
	
	private boolean is_static;
	
	private boolean is_primitive;
	
	private Class<? extends List<?>> listType;
	
	public ListElement(Class<?> type, String theName, String typeName, 
			String repTypeName, boolean arr, Class<? extends List<?>> parentType) {
		super(theName, typeName, repTypeName, arr);
		clazz = type;
		
		is_static = Modifier.isStatic(clazz.getModifiers());
		is_primitive = false;
		listType = parentType;
		
		
	}

	@Override
	public Object getMyValFromParentVal(Object parentVal) {
		
		Method arrayMethod = null;
        try
        {
            arrayMethod = listType.getMethod("toArray", new Class<?>[0]);
        }
        catch (NoSuchMethodException e)
        {
            throw new Error(listType.getName() + " seems to implement java.util.List, but method toArray() not found");
        }

        Object arrayVal = null;

        if (parentVal != null && !(parentVal instanceof NonsensicalObject) && !(parentVal instanceof NonsensicalList))
        {

            //TODO why can't we just cast to List and call toArray directly?

            try
            {
                arrayVal = arrayMethod.invoke(parentVal, new Object[0]);
            }
            catch (IllegalArgumentException e1)
            {
                throw new Error(e1);
            }
            catch (IllegalAccessException e1)
            {
                throw new Error(e1);
            }
            catch (InvocationTargetException e1)
            {
                throw new Error(e1);
            }
        }
        else
            arrayVal = NonsensicalObject.getInstance();

        
        if (arrayVal instanceof  NonsensicalObject){
        	return arrayVal;
        }else{
        	@SuppressWarnings("nullness") // We just verified (or set) arrayVal in code above.
            Object tmp = DTraceWriter.getListFromArray(arrayVal, clazz.hashCode());
            if(Runtime.working_debug)
            	System.out.println("Temp is: " + tmp.getClass().getName());
            //TODO traverse the array here looking for the obj by hashcode
            
            return tmp;
        }
        
	}

	@Override
	public VarKind get_var_kind() {
		if (is_static)
			return VarKind.VARIABLE;
		else
			return VarKind.FIELD;
		//throw new RuntimeException ("No var-kind for ListElement");
	}

	public boolean is_primitive() {
		return is_primitive;
	}
	
	/**
     * Returns the name of this field.  Since statics are top level, they
     * have no relative name.  Fields return their field name.
     **/
    public /*@Nullable*/ String get_relative_name() {
      if (is_static)
        return null;
      else {
        //String theName = field.getName();
        // Convert the internal reflection name for an outer class
        // 'this' field to the Java language format.
        //if (theName.startsWith("this$")) {
        //    theName = field.getType().getName() + ".this";
        //}
    	String theName = clazz.getSimpleName();  
        return theName;
      }
    }
}
