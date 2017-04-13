package daikon;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.interning.qual.UnknownInterned;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.NonRaw;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

import daikon.util.Pair;
import plume.UtilMDE;


/**
 * 
 * @author zalsaeed
 * 
 * A class to unify the traces records based on the enhanced Chicory version.
 * Also, it provides the ability to join to original objects onto a single made up object now.
 */

public class UnifyTraces {
	
	/** Observed ppts**/
	//List<String> observed_ppts_by_name = new ArrayList<String>();
	static List<String> comments = new ArrayList<String>();
	static String declVersion = "";
	static String varComparability = "";
	
	/* All traces found in the dtrace file, holding them in an ArrayList to preserve order*/
	static List<TraceInfo> all_traces = new ArrayList<TraceInfo>();
	
	/*All identical ppt are going to be identified by this key*/
	static List<String> ppt_keys = new ArrayList<String>();
	
	/* hold all Ppt from the enhanced Chicory many duplicates will be here */
	static HashMap<String,List<PptInfo>> all_ppts = new HashMap<String,List<PptInfo>>();
	
	/* Final Ppts after joining all duplicates */
	static HashMap<String,PptInfo> final_ppts = new HashMap<String,PptInfo>();
	
	
	static int countProcesedPpts = 0;
	static long numberOfLinesProcessed = 0;

	public static void main(String[] args) throws FileNotFoundException {
		
		int countAllPpts = 0;
		String file_name;
		Boolean combine = false;
		if (args.length == 2){
			if (args[0].equals("combine"))
					combine = true;
			else
				throw new IllegalArgumentException("The first flag (combine) is not passed correctley!");
			
			if(args[1].endsWith(".dtrace.gz") || args[1].endsWith(".dtrace"))
				file_name = args[1];
			else
				throw new IllegalArgumentException("The given files is not a trace file (.dtrace or .dtrace.gz)!");
		}
		else if (args.length == 1){
			if(args[0].endsWith(".dtrace.gz") || args[0].endsWith(".dtrace"))
				file_name = args[0];
			else
				throw new IllegalArgumentException("The given files is not a trace file (.dtrace or .dtrace.gz)!");
			
		}else
			throw new IllegalArgumentException("There must be either one or two arguments\n [filename].dtrace.gz or combine [filename].dtrace.gz!");
		
		System.out.println("Merging traces from file: " + file_name);
		
		InputStream gzipStream;
		Reader decoder;
		BufferedReader buffered;
		
		long total_number_of_lines= 0;
		try {
			total_number_of_lines = UtilMDE.count_lines(file_name);
			System.out.format("Totla number of lines: %,8d%n", total_number_of_lines);
		} catch (@UnknownKeyFor @NonRaw @NonNull @Initialized @UnknownInterned  IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		if(file_name.endsWith(".gz")){
			
			try {
				gzipStream = new GZIPInputStream(new FileInputStream(file_name));
				decoder = new InputStreamReader(gzipStream);
				buffered = new BufferedReader(decoder);
				
				long startTime = System.currentTimeMillis();
				long lastRecordedTime = startTime;
				System.out.println("Reading .gz traces ...");
				
				
				String line;
				while ((line = buffered.readLine()) != null) {
					numberOfLinesProcessed++;
					
					String[] words = line.split("\\s+");
					
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
						
					}else if(words.length > 0 && words[0].matches("#")){
						//doing nothing
						System.out.println("Reached end of file.");
					
					}else if (words[0].matches("ppt")){
						
						if(words.length != 2)
							throw new IllegalArgumentException("It wasn't a ppt!");
						PptInfo ppt = constructPpptInfo(buffered, words[1]);
						
						//writePpt(ppt);
						//all_ppts.add(ppt);
						countAllPpts++;
						
						if(!ppt_keys.contains(ppt.key))
							ppt_keys.add(ppt.name);
						
						//Storing ppts on hashMap based on their name
						List<PptInfo> similar_ppts = all_ppts.get(ppt.name);
					    if (similar_ppts == null) {
					    	similar_ppts = new ArrayList<>();
					    	all_ppts.put(ppt.name, similar_ppts);
					    }
					    similar_ppts.add(ppt);
						//all_ppts_map.put(key, value)
						
					}else if(words.length >= 1 && words[0].length() != 0){
						//this is a trace record
						//System.out.println(" this is a trace:" + current);
						TraceInfo ti = constructTraceInfo(buffered, line);
						
						//TODO add the traces to a list
						//traceWriting(ti);
						all_traces.add(ti);
						
						
					}//this is an empty line, do nothing
					
					//progress feedback 
					long currentTime = System.currentTimeMillis();
					if((currentTime - lastRecordedTime) > 3000){ //if a minute passed and not finished.
						lastRecordedTime = currentTime;
						System.out.println("Still processing the file ... finished %" + (numberOfLinesProcessed / total_number_of_lines)*100 );
					}
				}
				buffered.close();
				System.out.println("Read %" + (numberOfLinesProcessed / total_number_of_lines)*100 + " of the file");
				long finishTime = System.currentTimeMillis();
				long sec = ((finishTime - startTime) / 1000) % 60;
				long min = ((finishTime - startTime) / 1000) / 60;
				System.out.println("Finished reading file in: " + min + ":" + sec + " min (" +(finishTime - startTime) +" millis)");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(file_name.endsWith(".dtrace")){
			File decl = new File(file_name);
			
			Scanner scanner;
			try {
				System.out.println("Reading .dtrace traces ...");
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
						
					}else if(words.length >= 1 && words[0].length() != 0){
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
		}else {
			throw new IllegalArgumentException("The given file is not a .dtrace or .dtrace.gz file!");
		}
		
		System.out.println("Num of all obseeved ppts: " + countAllPpts);
		System.out.println("Num of identical ppts keys found: " + ppt_keys.size());
		System.out.println("Num of identical ppts found: " + all_ppts.size());
		System.out.println("Num of traces found: " + all_traces.size());
		
		processPpts();
		
		System.out.println("\nNum of identical ppts found: " + all_ppts.size());
		System.out.println("Num final ppts calculated: " + final_ppts.size());
		System.out.println("Num of processed ppts: " + countProcesedPpts);
		
		
		// Writing trace as they are merged and based on the order in which they were processed.
		try{
			String out_filename = String.format ("%sMerged.dtrace.gz", file_name.substring(0 ,file_name.lastIndexOf(".")));
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
		
		
		//Combine traces and decals of all available objects to show traces based on them both
		if(combine)
			writeCombinedTraces(file_name);
	
	}	
	
	private static void writeCombinedTraces(String file_name) {
		
		try{
			String out_filename = String.format ("%sMergedOneLevelUp.dtrace.gz", file_name.substring(0 ,file_name.lastIndexOf(".")));
			OutputStream os = new FileOutputStream(out_filename);
            os = new GZIPOutputStream(os);
            PrintWriter writer = new PrintWriter(os);
		    
		    // write the header 
		    for(String cmnt:comments){
		    	writer.println("// " +  cmnt);
		    }
		    writer.println("// combined objects\n");
		    
		    writer.println("decl-version " + declVersion);
		    writer.println("var-comparability " + varComparability);
		    
		    writer.println();
		    
		    // allocating all possible classes
		    // assuming we are only looking at a single package
		    List<String> all_available_unique_classes = new ArrayList<String>();
			for(String pptFullGivenString:ppt_keys){
				String pck_class = getPckgAndClassNameFromPpt(pptFullGivenString);
				PptInfo tmp = new PptInfo(pptFullGivenString);
				readPptNameString(pptFullGivenString, tmp);
				if(tmp.point.equals("OBJECT") && !all_available_unique_classes.contains(tmp.cls))
					all_available_unique_classes.add(pck_class);
			}
			System.out.println("totalclasses: " + all_available_unique_classes.size());
			
			// All possible combinations of the given classes 
			List<Pair<String, String>> all_possible_comb = combinaations(all_available_unique_classes);
			
			for(Pair<String,String> pairs:all_possible_comb){
				PptInfo firstObj = null;
				PptInfo secondObj = null;
				
				for(String pptFullGivenString:ppt_keys){
					//establish a new "this" variable and its trace.
					
					String pck_class = getPckgAndClassNameFromPpt(pptFullGivenString);
					PptInfo tmp = new PptInfo(pptFullGivenString);
					readPptNameString(pptFullGivenString, tmp);
					if(pairs.a.equals(pck_class) && tmp.point.equals("OBJECT"))
						firstObj = final_ppts.get(pptFullGivenString);
					
					if(pairs.b.equals(pck_class) && tmp.point.equals("OBJECT"))
						secondObj = final_ppts.get(pptFullGivenString);
					
					//then write all and edit all ppts for those two classes on the fly
				}
				
				PptInfo newClassObjPpt = joinTwoObjPpts(firstObj, secondObj);
				
				//write ppts
				writePpt(newClassObjPpt, writer);

			    for(String ppt_name:ppt_keys){
			    	if((pairs.a.equals(getPckgAndClassNameFromPpt(ppt_name)) || 
			    			pairs.b.equals(getPckgAndClassNameFromPpt(ppt_name))) &&
			    			!getPointTypeFromPpt(ppt_name).equals("OBJECT"))
			    		joinAndWritePpts(final_ppts.get(ppt_name), writer, newClassObjPpt.cls, newClassObjPpt.pckg, newClassObjPpt.name, "1");
			    }
			    	
			    
			    //write traces and change them on the fly
			    int newObjHash = new Random().nextInt(900000000) + 100000000;
			    
			    for(TraceInfo ti:all_traces)
			    	if(pairs.a.equals(getPckgAndClassNameFromPpt(ti.name)) || pairs.b.equals(getPckgAndClassNameFromPpt(ti.name)))
			    		joinAndWriteTraces(ti, final_ppts.get(ti.name), writer, newClassObjPpt.cls, newClassObjPpt.pckg, newObjHash);
				
			}
			
		    writer.println();
		    writer.println("# EOF (added by Runtime.addShutdownHook)");
		    
		    writer.close();
		} catch (IOException e) {
		   // do something
		}
		
	}
	
	private static void joinAndWriteTraces(TraceInfo ti, PptInfo ppt, PrintWriter w, String newClassName, String newPkgName, int newObjHash){
		w.println(replaceClassNameInPptFullName(newClassName, ti.name));
		w.println("this_invocation_nonce");
		w.println(ti.nonce);  // it could be problematic to have similar nonce for different objects
								//TODO change it by using the loop id for the new object as suffix.
		
		//Only if the first variable in the given ppt is "this" then we introduces a new "this" for our new combined object
		if(ppt.arrangedKeys.size() > 0  && ppt.arrangedKeys.get(0).equals("this")){
			w.println("this");
			w.println(newObjHash);
			w.println("1");
		}
		
		for(String varName:ppt.arrangedKeys){
			Value val = ti.getValueByName(varName);
			if (val != null){
				w.println(injectClassNameInVariableName(ppt.cls, val.valueName));
				w.println(val.givenValue);
				w.println(val.sensicalModifier);
			}else{
				w.println(injectClassNameInVariableName(ppt.cls, varName));
				w.println("nonsensical");
				w.println("2");
			}
		}
		
		w.println();
	}
	
	private static void joinAndWritePpts(PptInfo ppt, PrintWriter w, String newClassName, String newPkgName, String parent, String parentID){
		w.println("ppt " + replaceClassNameInPptFullName(newClassName, ppt.name)); 
		w.println("ppt-type " + ppt.type);
		
		if(ppt.parentName != null)
			w.println("parent parent " + parent + " " + parentID);
		
		//Only if the first variable in the given ppt is "this" then we introduces a new "this" for our new combined object
		if(ppt.arrangedKeys.size() > 0  && ppt.arrangedKeys.get(0).equals("this")){
			String[] prop = {"  var-kind variable", "  dec-type "+ newPkgName + "." + newClassName, "  rep-type hashcode", "  flags is_param", "  comparability 22"};
			w.println("variable this");
			for (String s:prop)
				w.println(s);
		}
			
		for(String varName:ppt.arrangedKeys){
			
			w.println("variable " + injectClassNameInVariableName(ppt.cls, varName));
			for(VariableProp prop:ppt.var_to_propVals.get(varName)){
				if (prop.getPropIdentifier().equals("enclosing-var") || prop.getPropIdentifier().equals("function-args")){
					w.print("  ");
					w.print(prop.getPropIdentifier());
					w.print(" " + injectClassNameInVariableName(ppt.cls, prop.getPropValsAsString()) + "\n");
				}else if(prop.getPropIdentifier().equals("parent")){
						w.print("  ");
						w.print(prop.propIdentifier);
						w.print(" " + parent + " " + parentID + "\n");
					}
				else{
					w.print("  ");
					w.print(prop.getPropIdentifier());
					w.print(" " + prop.getPropValsAsString() + "\n");
				}
			}
		}
		w.println();
	}
	
	/**
	 * A class to join two PptInfos 
	 * 
	 * @param firstPpt
	 * @param secondPpt
	 * @return
	 */
	private static PptInfo joinTwoObjPpts (PptInfo firstPpt, PptInfo secondPpt){

		if (firstPpt == null || secondPpt == null)
			throw new IllegalArgumentException("One of the Ppts given are nulls!");
		
		String first_pkg_cls = getPckgAndClassNameFromPpt(firstPpt.name);
		String second_pkg_cls = getPckgAndClassNameFromPpt(secondPpt.name);
		
		String[] firstTwoWords = first_pkg_cls.split("\\.+");
		String fstPkgName = firstTwoWords[0];
	    String fstClsName = firstTwoWords[1];
	    
	    String[] sndTwoWords = second_pkg_cls.split("\\.+");
	    String sndPkgName = sndTwoWords[0];
	    String sndClsName = sndTwoWords[1];
	    
	    if(!fstPkgName.equals(sndPkgName))
	    	throw new IllegalArgumentException("Two Ppts from different packages!");
	    
	    String newPkgName = fstPkgName;
	    String newClsName = fstClsName + sndClsName;
	    
	    // Because this is an Object Ppt I'm selecting one of the two given full names
	    String newFullPptName = replaceClassNameInPptFullName(newClsName, firstPpt.name);
		
	    PptInfo newPpt = new PptInfo(newFullPptName);
	    
	    if(!firstPpt.type.equals(secondPpt.type))
	    	throw new IllegalArgumentException("Two Ppts of different types trying to join!");
	    
	    //Setting the type
	    newPpt.type = firstPpt.type;
	    
	    if(firstPpt.parentName != null && secondPpt.parentName != null && !firstPpt.parentName.equals(secondPpt.parentName))
	    	throw new IllegalArgumentException("The two Ppts have different parents, not possible to join!");
	    
	    if(firstPpt.parentName != null && secondPpt.parentName != null){
	    	newPpt.parentName = firstPpt.parentName;
	    	newPpt.parentID = firstPpt.parentID;
	    }
	    
	    // add an arbitrary this variable for the new Object
	    String[] prop = {"  var-kind variable", "  dec-type "+ newPkgName + "." + newClsName, "  rep-type hashcode", "  flags is_param", "  comparability 22"}; 
	    newPpt.addNewVariable("this", prop);
		
	    //TODO when constructing Ppts we should parse the properties to make sense of them
    	// the way it is working now, we are dealing with properties as black-box. For the sake of failing fast
    	// we are continuing with the way it is set up now.
	    for(String varName:firstPpt.arrangedKeys){
	    	List<VariableProp> props = new ArrayList<VariableProp>();
	    	for(VariableProp vp:firstPpt.var_to_propVals.get(varName)){
	    		VariableProp tmp = new VariableProp(vp.getPropIdentifier());
	    		if(vp.getPropIdentifier().equals("enclosing-var") || vp.getPropIdentifier().equals("function-args")){
	    			if(vp.propVals.size() == 1){
	    				tmp.addPropVal(injectClassNameInVariableName(firstPpt.cls, vp.getPropValsAsString()));
	    				props.add(tmp);
	    			}
	    			else
	    				throw new IllegalArgumentException("Irregular 'enclosing-var' tried to be replaced when joining " + firstPpt.cls + " and " + secondPpt.cls + " OBJECTs");
	    		}else{
	    			for(String val:vp.propVals)
	    				tmp.addPropVal(val);
	    			props.add(tmp);
	    		}
	    	}
	    	newPpt.addNewVariable(injectClassNameInVariableName(firstPpt.cls, varName), props);
	    }
	    
	    for(String varName:secondPpt.arrangedKeys){
	    	List<VariableProp> props = new ArrayList<VariableProp>();
	    	for(VariableProp vp:secondPpt.var_to_propVals.get(varName)){
	    		VariableProp tmp = new VariableProp(vp.getPropIdentifier());
	    		if(vp.getPropIdentifier().equals("enclosing-var") || vp.getPropIdentifier().equals("function-args")){
	    			if(vp.propVals.size() == 1){
	    				tmp.addPropVal(injectClassNameInVariableName(secondPpt.cls, vp.getPropValsAsString()));
	    				props.add(tmp);
	    			}
	    			else
	    				throw new IllegalArgumentException("Irregular 'enclosing-var' tried to be replaced when joining " + firstPpt.cls + " and " + secondPpt.cls + " OBJECTs");
	    		}else{
	    			for(String val:vp.propVals)
	    				tmp.addPropVal(val);
	    			props.add(tmp);
	    		}
	    	}
	    	newPpt.addNewVariable(injectClassNameInVariableName(secondPpt.cls, varName), props);
	    }
	    
	    newPpt.pckg = newPkgName;
	    newPpt.cls = newClsName;
	    newPpt.point = "OBJECT";
		return newPpt;
	}
	
	private static String injectClassNameInVariableName (String className, String variableName){
		StringBuffer buff = new StringBuffer();
		for(int i = 0 ; i < variableName.length() ; i++){
			buff.append(variableName.charAt(i));
			if(buff.toString().equals("this"))
				buff.append("."+className);
		}
		return buff.toString();
	}
	
	private static String replaceClassNameInPptFullName(String newClassName, String fullPptName){
		String fn_name; // the ppt full name without the point type (e.g. ":::ENTER")
		String pkg_cls; // package and class name separated by a "." as given in the ppt
		String point;  // point type (e.g. OBJECT, ENTER, or EXIT10)
		String method; // the method and its arguments full name (e.g. setX(int))
		String packageName; // the package name without any noise
		
	    int separatorPosition = fullPptName.indexOf( FileIO.ppt_tag_separator );
	    if (separatorPosition == -1) {
	    	throw new Daikon.TerminationMessage("no ppt_tag_separator in '"+fullPptName+"'");
	    }
	    
	    fn_name = fullPptName.substring(0, separatorPosition).intern();
	    point = fullPptName.substring(separatorPosition + FileIO.ppt_tag_separator.length()).intern();

	    int lparen = fn_name.indexOf('(');
	    if (lparen == -1) {
	    	pkg_cls = fn_name;
	    	method = null;
	    	//This is an obj"
	    	String[] twoWords = pkg_cls.split("\\.+");
	    	packageName = twoWords[0];	      
	      
	    	//this is an OBJECT or CLASS thus no method
	      return packageName + "." + newClassName + ":::" + point;
	    }
	    int dot = fn_name.lastIndexOf('.', lparen);
	    if (dot == -1) {
	      throw new Daikon.TerminationMessage("No dot in function name " + fn_name);

	    }
	    // now 0 <= dot < lparen
	    pkg_cls = fn_name.substring(0, dot).intern();
	    // a ppt must have the package name and the class name, otherwise the traces are wrong. 
	    String[] twoWords = pkg_cls.split("\\.+");
	    packageName = twoWords[0];
	    method = fn_name.substring(dot + 1).intern();
	    
    	return packageName + "." + newClassName + "." + method + ":::" + point;
	}
	
	private static List<Pair<String,String>> combinaations(List<String> elements){
		List<Pair<String,String>> current_possible_comb = new ArrayList<Pair<String,String>>();
		//base case
		if(elements.size() == 2){
			Pair<String,String> e = new Pair<String,String>("","");
			e.a = elements.get(0); 
			e.b = elements.get(elements.size() - 1);
			current_possible_comb.add(e);
		}else
		if(elements.size() > 2){
			 String e1 = elements.get(0);
			 elements.remove(e1);
			 for(String others:elements){
				 if(!others.equals(e1))
					 current_possible_comb.add(new Pair<String,String>(e1, others));
			 }
			 current_possible_comb.addAll(combinaations(elements));
		}else 
		if(elements.size() < 2)
			throw new IllegalArgumentException("Accepts ony lists of size 2 or higher!");
		return current_possible_comb;
	}
	
	private static String getPointTypeFromPpt(String pptName){
		String point;  // point type (e.g. OBJECT, ENTER, or EXIT10)
		
	    int separatorPosition = pptName.indexOf( FileIO.ppt_tag_separator );
	    if (separatorPosition == -1) {
	    	throw new Daikon.TerminationMessage("no ppt_tag_separator in '"+pptName+"'");
	    }
	    
	    point = pptName.substring(separatorPosition + FileIO.ppt_tag_separator.length()).intern();

	    return point;	    
	}
	
	private static String getPckgAndClassNameFromPpt(String pptName){
		String fn_name; // the ppt full name without the point type (e.g. ":::ENTER")
		String pkg_cls; // package and class name separated by a "." as given in the ppt
		
		
	    int separatorPosition = pptName.indexOf( FileIO.ppt_tag_separator );
	    if (separatorPosition == -1) {
	    	throw new Daikon.TerminationMessage("no ppt_tag_separator in '"+pptName+"'");
	    }
	    
	    fn_name = pptName.substring(0, separatorPosition).intern();

	    int lparen = fn_name.indexOf('(');
	    if (lparen == -1) {
	    	pkg_cls = fn_name;
	    	//This is an obj"
	    	return pkg_cls;
	      
	    }
	    int dot = fn_name.lastIndexOf('.', lparen);
	    if (dot == -1)
	    	throw new Daikon.TerminationMessage("no method found in '"+pptName+"'");
	    // now 0 <= dot < lparen
	    pkg_cls = fn_name.substring(0, dot).intern();
	    // a ppt must have the package name and the class name, otherwise the traces are wrong. 
	    return pkg_cls;
	    
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
						/*
						 * If the variable has new one or more properties compared to the current know 
						 * properties in the final representation of the ppt, add the new properties
						 * to the know properties. Otherwise, they are equal, so do nothing.
						 */
						if (prop.length != pi.var_to_prop_reps.get(varName).length){
							
							if(prop.length > pi.var_to_prop_reps.get(varName).length)
								throw new IllegalArgumentException("The later var has a prop that is missing! This must never happen according to how Chicory works.");
							else if(prop.length < pi.var_to_prop_reps.get(varName).length){
								String[] temp = mergePtopertiesOfSameVairable(final_for_this_key.name, varName, prop, pi.var_to_prop_reps.get(varName));
								final_for_this_key.var_to_prop_reps.remove(varName);
								final_for_this_key.var_to_prop_reps.put(varName, temp);
							}
						}	
					}
				}
				
				countProcesedPpts++;
			}
			// add the merged version of all ppts to the final_ppts hashMap
			final_ppts.put(final_for_this_key.name, final_for_this_key);			
		}
		
	}
	
	private static String[] mergePtopertiesOfSameVairable(String pptName, String varName, String[] knownVarProps, String[] newVarProps){
		
		// we don't need actually to merge the two properties arrays as much that we need to make sure all 
		// properties in the old array are in the new one.
		for(String propFromOldVar:knownVarProps){
			boolean found = false;
			for(String propFromNewVar:newVarProps){
				if(propFromOldVar.equals(propFromNewVar))
					found = true;
			}
			if (!found)
				throw new IllegalArgumentException("The later var has a prop that is missing! This must never happen according to how Chicory works.");
			
		}
		
		System.out.println("One or more new properties add to '" + varName + "' of ppt '" + pptName + "'");
		
		return newVarProps;
	}

	private static void traceWriting(TraceInfo ti, PrintWriter w, PptInfo ppt) {
		
		w.println(ti.name);
		w.println("this_invocation_nonce");
		w.println(ti.nonce);
		
		System.out.println(ti.name + "<---------- Writing trace for: ");
		System.out.println();
		for(String p:ppt_keys){
			System.out.println(p);
		}
		
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
	
	private static TraceInfo constructTraceInfo(BufferedReader scanner, String name) throws IOException{
		TraceInfo ti = new TraceInfo(name);
		
		readTraceString(name, ti);
		
		String invocation_nonce = scanner.readLine();
		numberOfLinesProcessed++;
		if(!invocation_nonce.matches("this_invocation_nonce"))
			throw new IllegalArgumentException("It wasn't a trace header (can not find the invocation nonce)");
		String nonceValue = scanner.readLine();
		numberOfLinesProcessed++;
		String[] isOneWord = nonceValue.split("\\s+");
		if (isOneWord.length != 1)
			throw new IllegalArgumentException("It wasn't a trace nonce");
		ti.setNonce(nonceValue);
		
		String possibleTraceName;
		while((possibleTraceName = scanner.readLine()) != null){
			numberOfLinesProcessed++;
			
			//if empty line, then break 
			if(possibleTraceName.length() == 0){
				break;
			}
			
			//read next two lines (value and nonsensical value)
			String givenValue = scanner.readLine();
			numberOfLinesProcessed++;
			String sensModifier = scanner.readLine();
			numberOfLinesProcessed++;
			
			//add the variable trace to the trace info
			ti.addValue(possibleTraceName, givenValue, sensModifier);
		}
		
		
		return ti;
	}
	
	private static void readPptNameString(String pptName, PptInfo pptinfo){
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
		readPptNameString(name, currentPpt);
		
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
	
	private static PptInfo constructPpptInfo(BufferedReader scanner, String name) throws IOException{
		PptInfo currentPpt = new PptInfo(name);
		
		//given the full ppt name break it down to package, class, method and point then store the values in the PptInfo
		readPptNameString(name, currentPpt);
		
		String typeInfo = scanner.readLine();
		numberOfLinesProcessed++;
		String[] words = typeInfo.split("\\s+");
		if (words[0].matches("ppt-type")){
			if(words.length != 2)
				throw new IllegalArgumentException("It wasn't a ppt-type!");
			
			currentPpt.setType(words[1]);
		}
		
		String currentVarName = null;
		List<String> prop = new ArrayList<String>();
		
		String nextLine;
		while ((nextLine = scanner.readLine()) != null){
			numberOfLinesProcessed++;
			
			//String nextLine = scanner.nextLine();
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


