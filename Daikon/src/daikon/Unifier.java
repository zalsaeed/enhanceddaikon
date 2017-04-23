package daikon;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.interning.qual.UnknownInterned;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.NonRaw;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

import plume.UtilMDE;

/**
 * A class to unify the traces records based on the enhanced Chicory version.
 * Also, it provides the ability to join to original objects onto a single made up object now.
 * 
 * @author zalsaeed
 */
public class Unifier {
	
	/** All comments found on the dtrace file, to be written back latter */
	static List<String> comments = new ArrayList<String>();

	/** Declaration version as its given in the dtrace file to be written latter */
	static String declVersion = "";
	
	/** Variable Comparability value as it is given in the dtrace file to be written latter */
	static String varComparability = "";
	
	/** End of file declaration value as it is given in the dtrace file to be written latter */
	static String endOfFileStmt = "";
	
	/** All identical ppt are going to be identified by this key*/
	public static List<String> ppt_keys = new ArrayList<String>();
	
	/** Final Ppts after joining all duplicates */
	static HashMap<String,PptInfo> final_ppts = new HashMap<String,PptInfo>();

	/** Enumerator for types of possibles headlines in a .dtrace file  */
	public enum Line {COMMENT, VERSION, COMPAT, PPT, TRACE, BLANK, END}
	
	//Statistical variables 
	
	/** count to hold the number of all observed ppts in file */
	public static int countAllPpts = 0;
	
	/** count all the ppts we observe to make sure we are not doing anything wrong */ 
	public static int countProcesedPpts = 0;
	
	/** Getting a sense of how far we got in reading the file */
	static double numberOfLinesProcessed = 0;
	
	/** The total nuber of lines in the given trace file to show reading process percentage */
	public static double total_number_of_lines= 0;
	
	
	/**
	 * This application will take a d trace file that has program points (ppts) 
	 * with an unmatched number of variables. It then would unify those ppts as 
	 * well as the traces associated with them to make them readable by Daikon.
	 * 
	 * <br>
	 * Expected args:
	 * <li>Option 1: Give only trace file name and location (e.g. [filename].dtrace.gz).</li>
	 * <li>Option 2: Give the file name and a value for the buffer size to adjust it based 
	 * 		on the memory capacity (e.g. [filename].dtrace.gz 3000).</li>
	 * <li>Option 3: Give the file trigger the combine functionality to combine all possible 
	 * 		objects under new unique combined object (e.g. combine [filename].dtrace.gz 3000).</li>
	 * 
	 * @param args: see options. 
	 */
	public static void main(String[] args) {
		String file_name;
		boolean isCompressed = true;
		Boolean combine = false;
		int defaultBufferSize = 8192;
		
		// gathering and validating inputs
		if(args.length == 3){
			if (args[0].equals("combine"))
				combine = true;
			else
				throw new IllegalArgumentException("The first flag (combine) is not passed correctley!");
			
			if(args[1].endsWith(".dtrace.gz")){
				file_name = args[1];
				isCompressed = true;
			}
			else if (args[1].endsWith(".dtrace")){
				file_name = args[1];
				isCompressed = false;
			}
			else
				throw new IllegalArgumentException("The given files is not a trace file (.dtrace or .dtrace.gz)!");
			
			try {
				defaultBufferSize = Integer.parseInt(args[2]);
				
			} catch (NumberFormatException e){
				System.err.println("NumberFormatException: " + e.getMessage());
				System.exit(1);
			}
			
		}else if (args.length == 2){
			if(args[0].endsWith(".dtrace.gz")){
				file_name = args[0];
				isCompressed = true;
			}
			else if (args[0].endsWith(".dtrace")){
				file_name = args[0];
				isCompressed = false;
			}
			else
				throw new IllegalArgumentException("The given files is not a trace file (.dtrace or .dtrace.gz)!");
			
			try {
				defaultBufferSize = Integer.parseInt(args[1]);
				
			} catch (NumberFormatException e){
				System.err.println("NumberFormatException: " + e.getMessage());
				System.exit(1);
			}
		}
		else if (args.length == 1){
			if(args[0].endsWith(".dtrace.gz")){
				file_name = args[0];
				isCompressed = true;
			}
			else if (args[0].endsWith(".dtrace")){
				file_name = args[0];
				isCompressed = false;
			}
			else
				throw new IllegalArgumentException("The given files is not a trace file (.dtrace or .dtrace.gz)!");
			
		}else
			throw new IllegalArgumentException("There must be either one, two, or three arguments\n "
					+ "[filename].dtrace.gz,"
					+ "\n [filename].dtrace.gz [buffer_size],"
					+ "\n or combine [filename].dtrace.gz [buffer_size]");
		
		//Get the totla number of lines in the file 
		try {
			total_number_of_lines = (double)UtilMDE.count_lines(file_name);
			System.out.format("Total number of lines: %,8.0f%n", total_number_of_lines);
		} catch (@UnknownKeyFor @NonRaw @NonNull @Initialized @UnknownInterned  IOException e1) {
			e1.printStackTrace();
		}
		
		readFileForPpts(file_name, isCompressed, defaultBufferSize);
		
		readAndWriteFileForTrace(file_name, isCompressed, defaultBufferSize);
		
		//Combine traces and decals of all available objects to show traces based on them both
		if(combine)
			System.out.println("passed combine");
			//writeCombinedTraces(file_name);

	}
	
	/**
	 * A method to read the a given .dtrace file. This method is dedicated to only read .dtrace file information 
	 * (e.g. comments and version ... etc) and ppts. Trace are ignored when given this method. 
	 * 
	 * @author zalsaeed
	 * 
	 * @param filename The file that contains the traces
	 * @param isCompressed identify if the traces are compressed (e.g. using gzip) or not to establish the appropriate 
	 * 		reading method. 
	 * @param bufferSize The size of the BufferedReader that is going to be reading file. This is given to allow for 
	 * 		manual adjustment in case it is needed. 
	 */
	private static void readFileForPpts(String filename, boolean isCompressed, int bufferSize){
		
		// initializing the tracking variable
		numberOfLinesProcessed=0;
		
		InputStream gzipStream;
		Reader decoder;
		FileReader file;
		BufferedReader buffered;
	    
		try {
			if (isCompressed){
				gzipStream = new GZIPInputStream(new FileInputStream(filename));
				decoder = new InputStreamReader(gzipStream);
				buffered = new BufferedReader(decoder ,bufferSize); // 8192 is the default change it as see fit
				System.out.println("Reading traces: " + filename + " (compressed)  for processing ppts ...");
			}else{
				file = new FileReader(filename);
		    	buffered = new BufferedReader(file ,bufferSize); // 8192 is the default change it as see fit
		    	System.out.println("Reading traces: " + filename + " (uncompressed)  for processing ppts ...");
			}
			
			
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
				if((currentTime - lastRecordedTime) > 2000){ //if a minute passed and not finished.
					lastRecordedTime = currentTime;
					double percentage = (numberOfLinesProcessed / total_number_of_lines)*100;
					System.out.print("\rStill processing the file ... %" + String.format("%.2f", percentage));
				}
			}
			buffered.close();
			System.out.println(); //jumping a line once done after showing the progress
			long finishTime = System.currentTimeMillis();
			long sec = ((finishTime - startTime) / 1000) % 60;
			long min = ((finishTime - startTime) / 1000) / 60;
			System.out.println("Finished reading " + (numberOfLinesProcessed / total_number_of_lines)*100 
					+ "of the file in: " + min + ":" + sec + " min:sec (" + (finishTime - startTime) + " millis)");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * A method to read the .dtrace file (again) and write traces at the same time. This is to save as much as 
	 * possible of the memory space.
	 * 
	 * @author zalsaeed
	 * 
	 * @param filename
	 * @param isCompressed
	 * @param bufferSize
	 */
	private static void readAndWriteFileForTrace(String filename, boolean isCompressed, int bufferSize){
		
		// initializing the tracking variable
		numberOfLinesProcessed=0;
		// For writing purposes
		String out_filename;
		if(isCompressed)
			out_filename = String.format ("%sMerged.dtrace.gz", 
					filename.substring(0 ,filename.lastIndexOf(".dtrace.gz")));
		else
			out_filename = String.format ("%sMerged.dtrace.gz", 
					filename.substring(0 ,filename.lastIndexOf(".dtrace")));
		
		OutputStream os;
        PrintWriter writer; 
		
        // For reading purposes
		InputStream gzipStream;
		Reader decoder;
		FileReader file;
		BufferedReader buffered;
		
		try {
			
			os = new FileOutputStream(out_filename);
			os = new GZIPOutputStream(os);
			writer = new PrintWriter(os);
			
			writeTraceFileHeader(writer);
			
			// Write all know ppts
			for(String ppt_name:ppt_keys)
		    	writePpt(final_ppts.get(ppt_name), writer);
			
			if (isCompressed){
				gzipStream = new GZIPInputStream(new FileInputStream(filename));
				decoder = new InputStreamReader(gzipStream);
				buffered = new BufferedReader(decoder ,bufferSize); // 8192 is the default change it as see fit
				System.out.println("Reading traces from: " + filename + " (compressed)  and writing new traces on " 
				+ out_filename);
			}else{
				file = new FileReader(filename);
		    	buffered = new BufferedReader(file ,bufferSize); // 8192 is the default change it as see fit
		    	System.out.println("Reading traces from: " + filename + " (uncompressed)  and writing new traces on " 
						+ out_filename);
			}
			
			
			long startTime = System.currentTimeMillis();
			long lastRecordedTime = startTime;
			
			String line;
			
			while ((line = buffered.readLine()) != null) {
				numberOfLinesProcessed++;
				
				//processLine(buffered, line);
				
				Line lineType = identifyLine(line);
				
				switch (lineType){
				case COMMENT:
				case VERSION:
				case COMPAT:
				case END:
				case BLANK:
				    break;
				case PPT:
					// fast looping through the ppt lines that have being seen before
					while(!(buffered.readLine()).matches("\\s*$")){ // while not a blank line
						numberOfLinesProcessed++;
					}
					break;
					
				case TRACE:
					
					String nextLine;
					List<String> listOfTraceData = new ArrayList<String>();
					
					//read all lines of this trace
					while(!(nextLine = buffered.readLine()).matches("\\s*$")){
						listOfTraceData.add(nextLine);
						numberOfLinesProcessed++;
					}
					TraceInfo ti = constructTraceInfo(listOfTraceData, line);
					
					//Write the trace as soon as it is seen.
				    traceWriting(ti, writer, final_ppts.get(ti.name));
				    
					break;
				default:
					throw new IllegalArgumentException("Line was not identified");					
				}
				
				//progress feedback 
				long currentTime = System.currentTimeMillis();
				if((currentTime - lastRecordedTime) > 2000){ //if a minute passed and not finished.
					lastRecordedTime = currentTime;
					double percentage = (numberOfLinesProcessed / total_number_of_lines)*100;
					System.out.print("\rReading/Writing traces from/into file ... %" 
							+ String.format("%.2f", percentage));
				}
			}
			System.out.println(); //go to new line once done from processing
			
			buffered.close();
			writer.println();
		    writer.println(endOfFileStmt);
		    writer.close();
			
			long finishTime = System.currentTimeMillis();
			long sec = ((finishTime - startTime) / 1000) % 60;
			long min = ((finishTime - startTime) / 1000) / 60;
			System.out.println("Finished reading/writing " + (numberOfLinesProcessed / total_number_of_lines)*100 
					+ "of the traces in file in: " + min + ":" + sec + " min:sec (" + (finishTime - startTime) 
					+ " millis)");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Given a writing stream, the know .dtrace file header will be written to the given file.
	 * 
	 * @author zalsaeed
	 * 
	 * @param writer
	 */
	public static void writeTraceFileHeader(PrintWriter writer){
		
		// write the header 
	    for(String cmnt:comments){
	    	writer.println(cmnt);
	    }
	    writer.println("// merged version\n");
	    
	    // decl-version
	    writer.println(declVersion);
	    
	    // var-comparability
	    writer.println(varComparability);
	    
	    writer.println();
	}
	
	/**
	 * Given a {@link PptInfo} and a writer it, the ppt will be fully written as it is arranged.
	 * 
	 * @author zalsaeed
	 * 
	 * @param one
	 * @param writer
	 */
	public static void writePpt(PptInfo one, PrintWriter writer) {
		writer.println("ppt " + one.name );
		writer.println("ppt-type " + one.type);
		
		if(one.parentName != null)
			writer.println("parent parent " + one.parentName + " " + one.parentID);
		
		for(String varName:one.arrangedKeys){
			writer.println("variable " + varName);
			for(String props:one.var_to_prop_reps.get(varName)){
				writer.println(props);
			}
		}
		writer.println();
		
	}
	
	/**
	 * Given a {@link TraceInfo}, its representing {@link PptInfo}, and a writing stream, The trace will be written 
	 * according to its ppt.
	 * 
	 * @author zalsaeed
	 * 
	 * @param ti
	 * @param writer
	 * @param ppt
	 */
	public static void traceWriting(TraceInfo ti, PrintWriter writer, PptInfo ppt) {
		
		writer.println(ti.name);
		writer.println("this_invocation_nonce");
		writer.println(ti.nonce);
		
		for(String varName:ppt.arrangedKeys){
			Value val = ti.getValueByName(varName);
			if (val != null){
				writer.println(val.valueName);
				writer.println(val.givenValue);
				writer.println(val.sensicalModifier);
			}else{
				writer.println(varName);
				writer.println("nonsensical");
				writer.println("2");
			}
		}
		
		writer.println();
	}
	
	/**
	 * A method to break line down, identify it, and return its type based on the {@link Line} enumerator. 
	 * 
	 * @author zalsaeed
	 * 
	 * @param line The line which needs to be identified.
	 * @return Line type based on the enumerator {@link Line}.
	 */
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
	
	/**
	 * 
	 * Given a ppt name (e.g. ppt [package_name].[class_name].[method_sigature]:::ENTER) and a list of all the lines
	 * that represent the ppts information only, it constructs a new {@link PptInfo} with all the given information.  
	 * 
	 * @author zalsaeed
	 * 
	 * @param pptData All the information of the ppt to be constructed given in the form of strings. Each string is a 
	 * 		line in the dtrace file that belongs to the ppt. The lines are given based on the order in which they were 
	 * 		read. 
	 * @param name The name of the ppt. For example, "[package_name].[class_name].[method_sigature]:::ENTER"
	 * @return A well constructed {@link PptInfo} that can be easily handled in the future (e.g. for printing or 
	 * 		comparison with other ppts). 
	 */
	public static PptInfo constructPpptInfo(List<String> pptData, String name) {
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
	
	/**
	 * Given a trace for a specific ppt name (e.g. [package_name].[class_name].[method_sigature]:::ENTER) and a list 
	 * of strings that has all the traces associated with the trace name, this method will construct a 
	 * {@link TraceInfo} that will be easy to handle.  
	 * 
	 * @author zalsaeed
	 * 
	 * @param traceData List of all the information found under the given trace from the original .dtrace file. 
	 * @param name the name of the given ppr's trace (e.g. [package_name].[class_name].[method_sigature]:::ENTER). 
	 * @return {@link TeaceInfo} file with all the given traces stored appropriately for later reading and handling. 
	 */
	public static TraceInfo constructTraceInfo(List<String> traceData, String name) {
		TraceInfo ti = new TraceInfo(name);
		
		//TODO fix this to be using only one method for both traces and ppts
		readTraceString(name, ti);
		
		String invocation_nonce = traceData.get(0);
		
		if(!invocation_nonce.matches("this_invocation_nonce"))
			throw new IllegalArgumentException("It wasn't a trace header (can not find the invocation nonce)");
		
		String nonceValue = traceData.get(1);
		
		String[] isOneWord = nonceValue.split("\\s+");
		if (isOneWord.length != 1 || !isNumeric(nonceValue))
			throw new IllegalArgumentException("The given list wasn't for a trace point");
		ti.setNonce(nonceValue);
		
		// Removing the trace header information to validate the values and more
		traceData.remove(0);
		traceData.remove(0); //<-- it was one before the removal precedes it
		
		if(traceData.size() % 3 != 0)
			throw new IllegalArgumentException("The number of lines given doesn't add up for a trace record");

		for(Iterator<String> iter = traceData.iterator(); iter.hasNext();){
			String possibleTraceName = iter.next();
			String givenValue = iter.next();
			String sensModifier = iter.next();
			
			//add the variable trace to the trace info
			ti.addValue(possibleTraceName, givenValue, sensModifier);
		}
		
		return ti;
	}
	
	/**
	 * Takes a {@link PptInfo} and its name (e.g. [package_name].[class_name].[method_sigature]:::ENTER) in a string 
	 * form to break it down to smaller pieces and store them. Meaning, as the name given of the {@link PptInfo} would 
	 * contain package name, class name, method signature, and ppt type, this method will break the name and store each
	 * value appropriately in the given {@link PptInfo}.
	 * 
	 * @author zalsaeed
	 * 
	 * @param pptName The given ppt name (e.g. [package_name].[class_name].[method_sigature]:::ENTER) as it is
	 * found in the dtrace file.
	 * @param pptinfo The {@link PptInfo} to be edited.
	 */
	public static void readPptNameString(String pptName, PptInfo pptinfo){
		String fn_name; // the ppt full name without the point type (e.g. ":::ENTER")
		String pkg_cls; // package and class name separated by a "." as given in the ppt
		String point;  // point type (e.g. OBJECT, ENTER, or EXIT10)
		String method; // the method and its arguments full name (e.g. setX(int))
		String packageName; // the package name without any noise
		String clsName; // the class name without any noise
		
		
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
	
	/**
	 * A method that is very similar to the {@link #readPptNameString(String, PptInfo)} method but for trace. They 
	 * should be unified under a single method, but this would require a lot of redesigning. The redesign should 
	 * consider making the {@link PptInfo} and {@link TraceInfo} extend a single parent.
	 * 
	 * @author zalsaeed
	 * 
	 * @param traceName
	 * @param traceInfo
	 */
	public static void readTraceString(String traceName, TraceInfo traceInfo){
		String fn_name; // the ppt full name without the point type (e.g. ":::ENTER")
		String pkg_cls; // package and class name separated by a "." as given in the ppt
		String point;  // point type (e.g. OBJECT, ENTER, or EXIT10)
		String method; // the method and its arguments full name (e.g. setX(int))
		String packageName; // the package name without any noise
		String clsName; // the class name without any noise
		
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
	
	/**
	 * Helper method to handle storing a possible method parent information as it gets constructed. 
	 * 
	 * @author zalsaeed
	 * 
	 * @param ppt A {@link PptInfo} to be edited and store its parent information. 
	 * @param words An array of strings (no spaces) of the parent information as they were found in dtrace file. 
	 */
	public static void addParentInfo (PptInfo ppt, String[] words){
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
	
	/**
	 * Helper method for joining two program points ({@link PptInfo}). It takes two {@link PptInfo} that are "known" 
	 * to be the for the same program point and merge their variables. It insure that if a variable exists before, the 
	 * variable's properties should also be identical. Also, it doesn't remove any variables. It only adds variables 
	 * that were not observed before (if any).  
	 * 
	 * @author zalsaeed
	 * 
	 * @param originalPpt The original {@link PptInfo} that represent the a given program point in the dtrace file.
	 * @param newPpt The newly found {@link PptInfo} that represent a previously observed program point in the dtrace 
	 * file.
	 * @return A merged {@link PptInfo} version of the program point that represent the other two given program points
	 *  inclusively.
	 */
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
	
		if(!originalPpt.cls.equals(newPpt.cls) || !originalPpt.pckg.equals(newPpt.pckg) || 
				!originalPpt.point.equals(newPpt.point))
			throw new IllegalArgumentException("The given Ppts cls, pckg, or point do NOT match!");
	
		if (originalPpt.method != null && !originalPpt.method.equals(newPpt.method))
			throw new IllegalArgumentException("The given Ppts are for a method but their signature does NOT match!");
	
		//if the ppts have a parent will execute only once, 
		if(originalPpt.parentName == null && newPpt.parentName !=null)
			throw new IllegalArgumentException("newPpt has parent [" + newPpt.parentName + "] but originalPpt doesn't "
					+ "have one!");
		
		if(originalPpt.parentName != null && newPpt.parentName ==null)
			throw new IllegalArgumentException("originalPpt has parent [" + originalPpt.parentName + "] but newPpt "
					+ "doesn't have one!");
		
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
						throw new IllegalArgumentException("Variable from newPpt has a prop that is missing! This must "
								+ "never happen according to how Chicory works.");
					else if(currentProp.length < newPpt.var_to_prop_reps.get(varName).length){
						String[] temp = mergePtopertiesOfSameVairable(originalPpt.name, varName, currentProp, 
								newPpt.var_to_prop_reps.get(varName));
						originalPpt.var_to_prop_reps.remove(varName);
						originalPpt.var_to_prop_reps.put(varName, temp);
					}
				}	
			}			
		}
		return originalPpt;
	}

	/**
	 * A helper method to merge properties of the same variable that is found in two different program points. 
	 * 
	 * @author zalsaeed
	 * 
	 * @param pptName The name of the program point in question 
	 * 	(e.g. [package_name].[class_name].[method_sigature]:::ENTER).
	 * @param varName The variable who's properties are not identical as found from two different program points. 
	 * @param knownVarProps The original properties that were found.
	 * @param newVarProps The new properties that are just observed. 
	 * @return An array of the newly merged properties given the two different properties arrays.
	 */
	public static String[] mergePtopertiesOfSameVairable(String pptName, String varName, String[] knownVarProps, 
			String[] newVarProps){
		
		// we don't need actually to merge the two properties arrays as much that we need to make sure all 
		// properties in the old array are in the new one.
		for(String propFromOldVar:knownVarProps){
			boolean found = false;
			for(String propFromNewVar:newVarProps){
				if(propFromOldVar.equals(propFromNewVar))
					found = true;
			}
			if (!found)
				throw new IllegalArgumentException("The later var has a prop that is missing! This must never happen "
						+ "according to how Chicory works.");
			
		}
		
		System.out.println("One or more new properties add to '" + varName + "' of ppt '" + pptName + "'");
		
		return newVarProps;
	}
	
	/**
	 * A method to check if a string represent a numeral string to cast it into a numeral variable safely.
	 * <br>
	 * <b> Disclosure</b>: This method is taken from StackOverflow See 
	 * <a href="http://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java">
	 * How to check if a String is numeric in Java</a>. 
	 * @param str The string to be checked.
	 * @return <tt>True</tt> if the string represent a numeral, <tt>False</tt> if the string doesn't represent a 
	 * numeral. 
	 */
	public static boolean isNumeric(String str)
	{
	  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}
}
