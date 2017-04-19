package daikon;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.events.Comment;

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.interning.qual.UnknownInterned;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.NonRaw;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

import plume.UtilMDE;

public class Unifier {
	
	/**
	 * 
	 * @author zalsaeed
	 * 
	 * A class to unify the traces records based on the enhanced Chicory version.
	 * Also, it provides the ability to join to original objects onto a single made up object now.
	 */
	
	/* All comments found on the dtrace file, to be written back latter */
	static List<String> comments = new ArrayList<String>();

	/* Declaration version as its given in the dtrace file to be written latter */
	static String declVersion = "";
	
	/* Variable Comparability value as it is given in the dtrace file to be written latter */
	static String varComparability = "";
	
	/* End of file declaration value as it is given in the dtrace file to be written latter */
	static String endOfFileStmt = "";
	
	//TODO delete once no need to use it (
	/* All traces found in the dtrace file, holding them in an ArrayList to preserve order*/
	static List<TraceInfo> all_traces = new ArrayList<TraceInfo>();
	
	/*All identical ppt are going to be identified by this key*/
	public static List<String> ppt_keys = new ArrayList<String>();
	
	//TODO delete once no need to use it (as soon as we process ppts live)
	/* hold all Ppt from the enhanced Chicory many duplicates will be here */
	static HashMap<String,List<PptInfo>> all_ppts = new HashMap<String,List<PptInfo>>();
	
	/* Final Ppts after joining all duplicates */
	static HashMap<String,PptInfo> final_ppts = new HashMap<String,PptInfo>();

	public enum Line {COMMENT, VERSION, COMPAT, PPT, TRACE, BLANK, END}
	//Statistical variables 
	
	/* count to hold the number of all observed ppts in file */
	public static int countAllPpts = 0;
	
	/* count all the ppts we observe to make sure we are not doing anything wrong */ 
	public static int countProcesedPpts = 0;
	
	/* Getting a sense of how far we got in reading the file */
	static long numberOfLinesProcessed = 0;
	
	/* The total nuber of lines in the given trace file to show reading process percentage */
	public static long total_number_of_lines= 0;
	
	public static void main(String[] args) {
		String file_name;
		Boolean combine = false;
		
		// gathering and validating inputs
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
		
		//Get the totla number of lines in the file 
		try {
			total_number_of_lines = UtilMDE.count_lines(file_name);
			System.out.format("Totla number of lines: %,8d%n", total_number_of_lines);
		} catch (@UnknownKeyFor @NonRaw @NonNull @Initialized @UnknownInterned  IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		//Read the file data
		if(file_name.endsWith(".dtrace.gz")){
			readPptsFromCompressedFile(file_name);
		}else if(file_name.endsWith(".dtrace")){
			
		}
			

	}
	
	private static void readPptsFromCompressedFile(String filename){
		
		System.out.println("Reading .gz traces ...");
		
		InputStream gzipStream;
		Reader decoder;
		BufferedReader buffered;
		
		try {
			gzipStream = new GZIPInputStream(new FileInputStream(filename));
			decoder = new InputStreamReader(gzipStream);
			buffered = new BufferedReader(decoder);
			
			long startTime = System.currentTimeMillis();
			long lastRecordedTime = startTime;
			
			String line;
			
			while ((line = buffered.readLine()) != null) {
				numberOfLinesProcessed++;
				
				//processLine(buffered, line);
				
				Line lineType = identifyLine(line);
				
				switch (lineType){
				case COMMENT:
					comments.add(line);
					break;
				case VERSION:
					declVersion = line;
					break;
				case COMPAT:
					varComparability = line;
					break;
				case PPT:
					String[] words = line.split("\\s+");
					
					if(words.length != 2)
						throw new IllegalArgumentException("It wasn't a ppt!");
					
					String nextLine;
					List<String> listOfPptData = new ArrayList<String>();
					
					//read all lines of this ppt
					while(!(nextLine = buffered.readLine()).matches("\\s*$")){
						listOfPptData.add(nextLine);
						numberOfLinesProcessed++;
					}
					PptInfo ppt = constructPpptInfo(listOfPptData, words[1]);
					
					countAllPpts++;
					
					if(!ppt_keys.contains(ppt.key)){
						//First time observing such ppt
						ppt_keys.add(ppt.key);
						final_ppts.put(ppt.key, ppt);
					}else {
						//We have seen this ppt before, thus merge it then save it
						PptInfo originalPpt = final_ppts.get(ppt.key);
						PptInfo mergedPpt = mergeTwoPpts(originalPpt, ppt);
						final_ppts.remove(ppt.key);
						final_ppts.put(mergedPpt.key, mergedPpt);
						countProcesedPpts++;
					}
					
					
					//Storing ppts on hashMap based on their name
					List<PptInfo> similar_ppts = all_ppts.get(ppt.key);
				    if (similar_ppts == null) {
				    	similar_ppts = new ArrayList<>();
				    	all_ppts.put(ppt.key, similar_ppts);
				    }
				    similar_ppts.add(ppt);
				    break;
				case TRACE:
					break;
				case BLANK:
					break;
				case END:
					endOfFileStmt = line;
					break;
				default:
					throw new IllegalArgumentException("Line was not identified");					
				}
				
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
	
	public static void readPptsFromUncompressedFile(String filename){
		
		FileReader file;
	    BufferedReader buffered;
	    
	    try {
	    	file = new FileReader(filename);
	    	buffered = new BufferedReader(file ,8192); // 8192 is the default change it as see fit
	    	
	    } catch (IOException e){
	    	
	    }
		
		
	}
	
	public static Line identifyLine(String line){

		String[] words = line.split("\\s+");
		
		if (words.length > 0 && words[0].matches("//")){
			return Line.COMMENT;
		
		}else if(words.length > 0 && words[0].matches("decl-version")){
			return Line.VERSION; 
			
		}else if(words.length > 0 && words[0].matches("var-comparability")){
			return Line.COMPAT;
			
		}else if(words.length > 0 && words[0].matches("#")){
			return Line.END;
		
		}else if (words[0].matches("ppt")){
			return Line.PPT;
			
		}else if(words.length >= 1 && words[0].length() != 0){
			return Line.TRACE;
			
		}else {
			//Otherwise this is an empty line or a trace so do nothing
			return Line.BLANK;
		}
		
	}
	
	private static PptInfo constructPpptInfo(List<String> pptData, String name) throws IOException{
		PptInfo currentPpt = new PptInfo(name);
		
		//given the full ppt name break it down to package, class, method and point then store the values in the PptInfo
		readPptNameString(name, currentPpt);
		
		String typeInfo = pptData.get(0);
		numberOfLinesProcessed++;
		String[] words = typeInfo.split("\\s+");
		if (words[0].matches("ppt-type")){
			if(words.length != 2)
				throw new IllegalArgumentException("It wasn't a ppt-type!");
			
			currentPpt.setType(words[1]);
			pptData.remove(0);
		}
		
		String currentVarName = null;
		List<String> prop = new ArrayList<String>();
		
		for(String nextLine:pptData){
			
			//There must be no string with an empty line
			if (nextLine.length() == 0)
				throw new IllegalArgumentException("A list with at least one empty string line was given!");
			
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
				String tempProp = " ";
				for (int j = 1 ; j < idenfiers.length ; j++ ){ //skipping the empty space
					tempProp = tempProp + " " + idenfiers[j]; //empty space were trimmed before
				}
				prop.add(tempProp);
			}
			
		}
		
		//Flushing the last processed variable and its properties before returning
		if(currentVarName != null){ //flushing last variable info
			currentPpt.addNewVariable(currentVarName, prop.toArray(new String[0]));
			prop.clear();
		}
		
		return currentPpt;
	}
	
	//TODO add test to this method
	public static void readPptNameString(String pptName, PptInfo pptinfo){
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
	

	public static PptInfo mergeTwoPpts(PptInfo originalPpt, PptInfo newPpt){
		
		if(originalPpt.key == null) 
			throw new IllegalArgumentException("The original Ppt is not established correctely (missing key)!");
		
		if(!originalPpt.key.equals(newPpt.key))
			throw new IllegalArgumentException("The given Ppts do NOT have the same key!");
	
		if(originalPpt.name == null) 
			throw new IllegalArgumentException("The original Ppt is not established correctely (missing name)!");
		
		if(!originalPpt.name.equals(newPpt.name))
			throw new IllegalArgumentException("The given Ppts do NOT have the same name!");
		
		//Make sure the original Ppt has a type identified
		if(originalPpt.type == null) 
			throw new IllegalArgumentException("The original Ppt is not established correctely (missing type)!");
		
		//n_2 to n ppts must have the same type of current one
		if(!originalPpt.type.equals(newPpt.type))
			throw new IllegalArgumentException("The given Ppts are not of the same type!");
	
		if(originalPpt.cls == null)
			throw new IllegalArgumentException("The original Ppt is not established correctly (missing cls info)!");
	
		if(originalPpt.pckg == null)
			throw new IllegalArgumentException("The original Ppt is not established correctly (missing pckg info)!");
	
		if(originalPpt.point == null)
			throw new IllegalArgumentException("The original Ppt is not established correctly (missing point info)!");
	
		if(!originalPpt.cls.equals(newPpt.cls) || !originalPpt.pckg.equals(newPpt.pckg) || !originalPpt.point.equals(newPpt.point))
			throw new IllegalArgumentException("The given Ppts cls, pckg, or point do NOT match!");
	
		if (originalPpt.method != null && !originalPpt.method.equals(newPpt.method))
			throw new IllegalArgumentException("The given Ppts are for a method but their signature does NOT match!");
	
		//if the ppts have a parent will execute only once, 
		if(originalPpt.parentName == null && newPpt.parentName !=null)
			throw new IllegalArgumentException("newPpt has parent [" + newPpt.parentName + "] but originalPpt doesn't have one!");
		
		if(originalPpt.parentName != null && newPpt.parentName ==null)
			throw new IllegalArgumentException("originalPpt has parent [" + originalPpt.parentName + "] but newPpt doesn't have one!");
		
		if(originalPpt.parentName != null && newPpt.parentName !=null)
			if(!originalPpt.parentName.equals(newPpt.parentName) || !originalPpt.parentID.equals(newPpt.parentID))
				throw new IllegalArgumentException("parentName or parentID of originalPpt nad newPpt do NOT match");
		
		// iterating over all variables of the newPpt
		for(String varName:newPpt.arrangedKeys){
			
			//get properties of the same variable from originalPpt
			String[] currentProp = originalPpt.var_to_prop_reps.get(varName);
			
			// check if this is a new variable (from the originalPpt perspective)
			if(currentProp == null){
				String[] temp = newPpt.var_to_prop_reps.get(varName);
				originalPpt.addNewVariable(varName, temp);
			}else{
				//make sure they are the same length of prop
				/*
				 * If the variable has new one or more properties compared to the current know 
				 * properties in the final representation of the ppt, add the new properties
				 * to the know properties. Otherwise, they are equal, so do nothing.
				 */
				if (currentProp.length != newPpt.var_to_prop_reps.get(varName).length){
					
					if(currentProp.length > newPpt.var_to_prop_reps.get(varName).length)
						throw new IllegalArgumentException("Variable from newPpt has a prop that is missing! This must never happen according to how Chicory works.");
					else if(currentProp.length < newPpt.var_to_prop_reps.get(varName).length){
						String[] temp = mergePtopertiesOfSameVairable(originalPpt.name, varName, currentProp, newPpt.var_to_prop_reps.get(varName));
						originalPpt.var_to_prop_reps.remove(varName);
						originalPpt.var_to_prop_reps.put(varName, temp);
					}
				}	
			}			
		}
		return originalPpt;
	}

	public static String[] mergePtopertiesOfSameVairable(String pptName, String varName, String[] knownVarProps, String[] newVarProps){
		
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
	
}
