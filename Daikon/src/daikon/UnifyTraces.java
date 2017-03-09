package daikon;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;


/**
 * 
 * @author zalsaeed
 * 
 * A class to unify the traces records based on the enhanced Chicory version 
 * 
 *
 */

public class UnifyTraces {
	
	/** Observed ppts**/
	//List<String> observed_ppts_by_name = new ArrayList<String>();
	static List<String> comments = new ArrayList<String>();
	static String declVersion = "";
	static String varComparability = "";
	
	static List<TraceInfo> all_traces = new ArrayList<TraceInfo>();
	
	/*All identical ppt are going to be identified by this key*/
	static List<String> ppt_keys = new ArrayList<String>();
	static HashMap<String,List<PptInfo>> all_ppts = new HashMap<String,List<PptInfo>>();
	
	static HashMap<String,PptInfo> final_ppts = new HashMap<String,PptInfo>();
	
	static int countProcesedPpts = 0;

	public static void main(String[] args) {
		
		int countAllPpts = 0;
		
		if (args.length == 0)
			System.out.println("Nothing to read!");
		String file_name = args[0];
		File decl = new File(file_name);
		
		Scanner scanner;
		try {
			scanner = new Scanner(decl);
			
			while (scanner.hasNext()){
				String current =  scanner.nextLine();
				String[] words = current.split("\\s+");
				
				if (words.length > 0 && words[0].matches("//")){
					//this is a comment 
					String currentComment = "";
					for(int i = 1 ; i < words.length ; i++){
						currentComment = currentComment + words[i] + " ";
					}
					comments.add(currentComment);
				
				}else if(words.length > 0 && words[0].matches("decl-version")){
					//this is the decl-version used 
					if(words.length != 2)
						throw new IllegalArgumentException("It wasn't a decl-version!");
					declVersion = words[1];
					
				}else if(words.length > 0 && words[0].matches("var-comparability")){
					//this is the var-comparability (if it was given)
					if(words.length != 2)
						throw new IllegalArgumentException("It wasn't a var-comparability!");
					varComparability = words[1];
					
				}else if (words[0].matches("ppt")){
					
					if(words.length != 2)
						throw new IllegalArgumentException("It wasn't a ppt!");
					PptInfo ppt = constructPpptInfo(scanner, words[1]);
					
					//TODO add the PptInfo s you get to a list
					//writePpt(ppt);
					//all_ppts.add(ppt);
					countAllPpts++;
					
					if(!ppt_keys.contains(ppt.name))
						ppt_keys.add(ppt.name);
					
					//Storing ppts on hashMap based on their name
					List<PptInfo> similar_ppts = all_ppts.get(ppt.name);
				    if (similar_ppts == null) {
				    	similar_ppts = new ArrayList<>();
				    	all_ppts.put(ppt.name, similar_ppts);
				    }
				    similar_ppts.add(ppt);
					//all_ppts_map.put(key, value)
					
				}else if(words.length == 1 && words[0].length() != 0){
					//this is a trace record
					//System.out.println(" this is a trace:" + current);
					TraceInfo ti = constructTraceInfo(scanner, current);
					
					//TODO add the traces to a list
					//traceWriting(ti);
					all_traces.add(ti);
					
					
				}//this is an empty line, do nothing
			}
			scanner.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Num of all obseeved ppts: " + countAllPpts);
		System.out.println("Num of identical ppts keys found: " + ppt_keys.size());
		System.out.println("Num of identical ppts found: " + all_ppts.size());
		System.out.println("Num of traces found: " + all_traces.size());
		
		processPpts();
		
		System.out.println("\nNum of identical ppts found: " + all_ppts.size());
		System.out.println("Num final ppts calculated: " + final_ppts.size());
		System.out.println("Num of processed ppts: " + countProcesedPpts);
		
		//TODO easily wrtie ppts to file. 
		//TODO write traces based on the final_ppts (just put nonsesncial for things doesn't exists
		
	    //File dtrace_file = new File (String.format ("%s_merged.dtrace.gz", file_name.matches("[^\\]*(?=[.][a-zA-Z]+$")));
	    //PrineStream unifier =
		try{
			String out_filename = String.format ("%s_basedOnEnhancedChicory.dtrace.gz", file_name.substring(0 ,file_name.lastIndexOf(".")));
			OutputStream os = new FileOutputStream(out_filename);
            os = new GZIPOutputStream(os);
            PrintWriter writer = new PrintWriter(os);
		    
		    // write the header 
		    for(String cmnt:comments){
		    	writer.println("// " +  cmnt);
		    }
		    writer.println("// merged version\n");
		    
		    writer.println("decl-version " + declVersion);
		    writer.println("var-comparability " + varComparability);
		    
		    writer.println();
		    
		    //write ppts 
		    //for(String ppt_name:final_ppts.keySet())
		    for(String ppt_name:ppt_keys)
		    	writePpt(final_ppts.get(ppt_name), writer);
		    
		    for(TraceInfo ti:all_traces)
		    	traceWriting(ti, writer, final_ppts.get(ti.name));
		    
		    writer.println();
		    writer.println("# EOF (added by Runtime.addShutdownHook)");
		    
		    writer.close();
		} catch (IOException e) {
		   // do something
		}
		
		writeCombinedTraces(file_name);
	
	}	
	
	private static void writeCombinedTraces(String file_name) {
		
		try{
			String out_filename = String.format ("%s_basedOnEnhancedChicoryCombined.dtrace.gz", file_name.substring(0 ,file_name.lastIndexOf(".")));
			OutputStream os = new FileOutputStream(out_filename);
            os = new GZIPOutputStream(os);
            PrintWriter writer = new PrintWriter(os);
		    
		    // write the header 
		    for(String cmnt:comments){
		    	writer.println("// " +  cmnt);
		    }
		    writer.println("// merged version\n");
		    
		    writer.println("decl-version " + declVersion);
		    writer.println("var-comparability " + varComparability);
		    
		    writer.println();
		    
		    //assuming we are only looking at a single package
		    List<String> all_available_unique_classes = new ArrayList<String>();
			for(String s:ppt_keys){
				PptInfo temp = final_ppts.get(s);
				if(temp.point.equals("OBJECT") && !all_available_unique_classes.contains(temp.cls))
					all_available_unique_classes.add(temp.cls);
			}
			System.out.println("totalclasses: " + all_available_unique_classes.size());
		    
		    //get a class
		    //combine it with all other classes
		    	//write decl and traces for each combination
		    
		    //write ppts 
		    //for(String ppt_name:ppt_keys)
		    	//writePpt(final_ppts.get(ppt_name), writer);
		    
		    //for(TraceInfo ti:all_traces)
		    	//traceWriting(ti, writer, final_ppts.get(ti.name));
		    
		    writer.println();
		    writer.println("# EOF (added by Runtime.addShutdownHook)");
		    
		    writer.close();
		} catch (IOException e) {
		   // do something
		}
		
		
	}

	private static void processPpts(){
		
		List<PptInfo> ppts_of_this_key = new ArrayList<PptInfo>();
		
		for (String ppt_key:ppt_keys){
			
			//get ppts from hashMap
			ppts_of_this_key = all_ppts.get(ppt_key);
			
			//remove it from HashMap so that memory is free once we complete this ppt
			all_ppts.remove(ppt_key);
			
			
			PptInfo final_for_this_key = new PptInfo(ppt_key);
			for(PptInfo pi:ppts_of_this_key){
				
				if(final_for_this_key.type == null) //this will only happen in the first iter
					final_for_this_key.setType(pi.type);
				
				
				if(!final_for_this_key.type.equals(pi.type))//n_2 to n ppts must have the same type of current one
					throw new IllegalArgumentException("One of the stored ppts in the hashMap has a differnt type!");
				
				//this should only happen in the first iter
				if(final_for_this_key.cls == null || final_for_this_key.pckg == null || final_for_this_key.point == null){
					final_for_this_key.cls = pi.cls;
					final_for_this_key.pckg = pi.pckg;
					final_for_this_key.point = pi.point;
					if (pi.method != null)
						final_for_this_key.method = pi.method;
				}
				
				if(final_for_this_key.cls != null && !final_for_this_key.cls.equals(pi.cls))
					throw new IllegalArgumentException("The two ppts have different class name!");
				
				if(final_for_this_key.pckg != null && !final_for_this_key.pckg.equals(pi.pckg))
					throw new IllegalArgumentException("The two ppts have different package name!");
				
				if(final_for_this_key.point != null && !final_for_this_key.point.equals(pi.point))
					throw new IllegalArgumentException("The two ppts have different point type!");
				
				if(final_for_this_key.method != null && !final_for_this_key.method.equals(pi.method))
					throw new IllegalArgumentException("The two ppts have different method signature!");
				
				//if the ppts have a parent will execute only once, 
				if(final_for_this_key.parentName == null && pi.parentName !=null){
					final_for_this_key.parentName = pi.parentName;
					final_for_this_key.parentID = pi.parentID; // Assuming the parent ID must be the same 
				}
				
				if(final_for_this_key.parentName != null && !final_for_this_key.parentName.equals(pi.parentName))
					throw new IllegalArgumentException("One of the stored ppts in the hashMap has a differnt parent!");
				
				// iterating over all variables for this pi
				for(String varName:pi.arrangedKeys){
					//System.out.println("variable " + varName);
					String[] prop = final_for_this_key.var_to_prop_reps.get(varName);
					
					// is it not added before?
					if(prop == null){
						String[] temp = pi.var_to_prop_reps.get(varName);
						final_for_this_key.addNewVariable(varName, temp);
						
					}else{
						//make sure they are the same length of prop
						if (prop.length != pi.var_to_prop_reps.get(varName).length)
							throw new IllegalArgumentException("Ppts have same variable but different number of prop!");
					}
				}
				
				countProcesedPpts++;
			}
			// add the merged version of all ppts to the final_ppts hashMap
			final_ppts.put(final_for_this_key.name, final_for_this_key);			
		}
		
	}

	private static void traceWriting(TraceInfo ti, PrintWriter w, PptInfo ppt) {
		
		w.println(ti.name);
		w.println("this_invocation_nonce");
		w.println(ti.nonce);
		
		for(String varName:ppt.arrangedKeys){
			Value val = ti.getValueByName(varName);
			if (val != null){
				w.println(val.valueName);
				w.println(val.givenValue);
				w.println(val.sensicalModifier);
			}else{
				w.println(varName);
				w.println("nonsensical");
				w.println("2");
			}
		}
		
		w.println();
	}


	private static void writePpt(PptInfo one, PrintWriter w) {
		w.println("ppt " + one.name );
		w.println("ppt-type " + one.type);
		
		if(one.parentName != null)
			w.println("parent parent " + one.parentName + " " + one.parentID);
		
		for(String varName:one.arrangedKeys){
			w.println("variable " + varName);
			for(String props:one.var_to_prop_reps.get(varName)){
				w.println(props);
			}
		}
		w.println();
		
	}
	
	private static void readTraceString(String traceName, TraceInfo traceInfo){
		String fullname; //pptName as given
		String fn_name; // the ppt full name without the point type (e.g. ":::ENTER")
		String pkg_cls; // package and class name separated by a "." as given in the ppt
		String point;  // point type (e.g. OBJECT, ENTER, or EXIT10)
		String method; // the method and its arguments full name (e.g. setX(int))
		String packageName; // the package name without any noise
		String clsName; // the class name without any noise
		
		
	    fullname = traceName.intern();
	    int separatorPosition = traceName.indexOf( FileIO.ppt_tag_separator );
	    if (separatorPosition == -1) {
	    	throw new Daikon.TerminationMessage("no ppt_tag_separator in '"+traceName+"'");
	    }
	    
	    fn_name = traceName.substring(0, separatorPosition).intern();
	    point = traceName.substring(separatorPosition + FileIO.ppt_tag_separator.length()).intern();

	    int lparen = fn_name.indexOf('(');
	    if (lparen == -1) {
	    	pkg_cls = fn_name;
	    	method = null;
	    	//This is an obj"
	    	String[] twoWords = pkg_cls.split("\\.+");
	    	packageName = twoWords[0];	      
	    	clsName = twoWords[1];

	    	traceInfo.point = point;
	    	traceInfo.cls = clsName;
	    	traceInfo.pckg = packageName;
	      
	      return;
	    }
	    int dot = fn_name.lastIndexOf('.', lparen);
	    if (dot == -1) {
	      // throw new Daikon.TerminationMessage("No dot in function name " + fn_name);
	      method = fn_name;
	      pkg_cls = null;
	      return;
	    }
	    // now 0 <= dot < lparen
	    pkg_cls = fn_name.substring(0, dot).intern();
	    // a ppt must have the package name and the class name, otherwise the traces are wrong. 
	    String[] twoWords = pkg_cls.split("\\.+");
	    packageName = twoWords[0];
	    clsName = twoWords[1];
	    method = fn_name.substring(dot + 1).intern();
	    
	    
	    traceInfo.point = point;
    	traceInfo.cls = clsName;
    	traceInfo.pckg = packageName;
    	traceInfo.method = method;
	}
	
	private static TraceInfo constructTraceInfo(Scanner scanner, String name){
		TraceInfo ti = new TraceInfo(name);
		
		readTraceString(name, ti);
		
		String invocation_nonce = scanner.nextLine();
		if(!invocation_nonce.matches("this_invocation_nonce"))
			throw new IllegalArgumentException("It wasn't a trace header (can not find the invocation nonce)");
		String nonceValue = scanner.nextLine();
		String[] isOneWord = nonceValue.split("\\s+");
		if (isOneWord.length != 1)
			throw new IllegalArgumentException("It wasn't a trace nonce");
		ti.setNonce(nonceValue);
		
		while(scanner.hasNext()){
			String possibleTraceName = scanner.nextLine();
			
			//if empty line, then break 
			if(possibleTraceName.length() == 0){
				break;
			}
			
			//read next two lines (value and nonsensical value)
			String givenValue = scanner.nextLine();
			String sensModifier = scanner.nextLine();
			
			//add the variable trace to the trace info
			ti.addValue(possibleTraceName, givenValue, sensModifier);
		}
		
		
		return ti;
	}
	
	private static void readPptString(String pptName, PptInfo pptinfo){
		String fullname; //pptName as given
		String fn_name; // the ppt full name without the point type (e.g. ":::ENTER")
		String pkg_cls; // package and class name separated by a "." as given in the ppt
		String point;  // point type (e.g. OBJECT, ENTER, or EXIT10)
		String method; // the method and its arguments full name (e.g. setX(int))
		String packageName; // the package name without any noise
		String clsName; // the class name without any noise
		
		
	    fullname = pptName.intern();
	    int separatorPosition = pptName.indexOf( FileIO.ppt_tag_separator );
	    if (separatorPosition == -1) {
	    	throw new Daikon.TerminationMessage("no ppt_tag_separator in '"+pptName+"'");
	    }
	    
	    fn_name = pptName.substring(0, separatorPosition).intern();
	    point = pptName.substring(separatorPosition + FileIO.ppt_tag_separator.length()).intern();

	    int lparen = fn_name.indexOf('(');
	    if (lparen == -1) {
	    	pkg_cls = fn_name;
	    	method = null;
	    	//This is an obj"
	    	String[] twoWords = pkg_cls.split("\\.+");
	    	packageName = twoWords[0];	      
	    	clsName = twoWords[1];

	    	pptinfo.point = point;
	    	pptinfo.cls = clsName;
	    	pptinfo.pckg = packageName;
	      
	      return;
	    }
	    int dot = fn_name.lastIndexOf('.', lparen);
	    if (dot == -1) {
	      // throw new Daikon.TerminationMessage("No dot in function name " + fn_name);
	      method = fn_name;
	      pkg_cls = null;
	      return;
	    }
	    // now 0 <= dot < lparen
	    pkg_cls = fn_name.substring(0, dot).intern();
	    // a ppt must have the package name and the class name, otherwise the traces are wrong. 
	    String[] twoWords = pkg_cls.split("\\.+");
	    packageName = twoWords[0];
	    clsName = twoWords[1];
	    method = fn_name.substring(dot + 1).intern();
	    
	    
	    pptinfo.point = point;
    	pptinfo.cls = clsName;
    	pptinfo.pckg = packageName;
    	pptinfo.method = method;
	}

	private static PptInfo constructPpptInfo(Scanner scanner, String name){
		PptInfo currentPpt = new PptInfo(name);
		
		//given the full ppt name break it down to package, class, method and point then store the values in the PptInfo
		readPptString(name, currentPpt);
		
		String typeInfo = scanner.nextLine();
		String[] words = typeInfo.split("\\s+");
		if (words[0].matches("ppt-type")){
			if(words.length != 2)
				throw new IllegalArgumentException("It wasn't a ppt-type!");
			
			currentPpt.setType(words[1]);
		}
		
		String currentVarName = null;
		List<String> prop = new ArrayList<String>();
		
		while (scanner.hasNext()){
			String nextLine = scanner.nextLine();
			if (nextLine.length() == 0){ //whitespace then end this ppt parsing
				//TODO flush data here before breaking
				if(currentVarName != null){ //flushing last variable info
					currentPpt.addNewVariable(currentVarName, prop.toArray(new String[0]));
				}
				break;
			}
			
			String[] idenfiers = nextLine.split("\\s+");
			String mayBeParent = idenfiers[0] + " " + idenfiers[1];
			if(idenfiers.length > 0 && mayBeParent.matches("parent parent")){
				addParentInfo(currentPpt,idenfiers);
			}else if(idenfiers.length > 0 && idenfiers[0].matches("variable")){
				if(currentVarName != null){ //flushing last variable info
					currentPpt.addNewVariable(currentVarName, prop.toArray(new String[0]));
					prop.clear();
				}
				if(idenfiers.length != 2)
					throw new IllegalArgumentException("It wasn't a variable!");
				currentVarName = idenfiers[1];
				
			}else{
				String tmepProp = " ";
				for (int j = 1 ; j < idenfiers.length ; j++ ){ //skipping the empty space
					tmepProp = tmepProp + " " + idenfiers[j];
				}
				prop.add(tmepProp);
				//System.out.println("this is the var prop (or traces): " + nextLine + " has " + idenfiers[1].length());
			}
			
		}
		
		return currentPpt;
	}
	
	private static void addParentInfo (PptInfo ppt, String[] words){
		for(int i = 0 ; i < words.length ; i++){
			if(words[i].matches("parent")){
				continue;
			}
			if(words[i].length() <= 1 ){ //it is not possible that the name of the parent is less than 1 in length 
				ppt.setParentID(words[i]);
				continue;
			}
			//otherwise this must be the parent name
			ppt.setParentName(words[i]);
		}
	}

}


