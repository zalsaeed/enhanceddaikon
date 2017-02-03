package daikon.chicory;

import java.util.ArrayList;
import java.util.List;

public class DecalState {
	
	
	public List<ClassInfo> all_classes = new ArrayList<ClassInfo>();
    //public List<MethodInfo> methods = new ArrayList<MethodInfo>();
    
    public DecalState (){
    	
    }
    
    public void setAllClassesList (List<ClassInfo> all_classes){
    	this.all_classes = all_classes;
    }
    
//    public void setAllMethodsList(List<MethodInfo> methods){
//    	this.methods = methods;
//    }

}
