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
			String out_filename = String.format ("%s_merged.dtrace.gz", file_name.substring(0 ,file_name.lastIndexOf(".")));
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
	    //dtrace.println("# EOF (added by no_more_output)");
	
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
				
				if(!final_for_this_key.type.matches(pi.type))//n_2 to n ppts must have the same type of current one
					throw new IllegalArgumentException("One of the stored ppts in the hashMap has a differnt type!");
				
				//if the ppts have a parent will execute only once, 
				if(final_for_this_key.parentName == null && pi.parentName !=null){
					final_for_this_key.parentName = pi.parentName;
					final_for_this_key.parentID = pi.parentID; // Assuming the parent ID must be the same 
				}
				
				if(final_for_this_key.parentName != null && !final_for_this_key.parentName.matches(pi.parentName))
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
	
	private static TraceInfo constructTraceInfo(Scanner scanner, String name){
		TraceInfo ti = new TraceInfo(name);
		
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

	private static PptInfo constructPpptInfo(Scanner scanner, String name){
		PptInfo currentPpt = new PptInfo(name);
		
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
	
//	public static void setDtrace(String filename, boolean append, PrintStream dtrace)
//    {
//
//        try
//        {
//            File file = new File(filename);
//            File parent = file.getParentFile();
//            if (parent != null)
//                parent.mkdirs();
//            OutputStream os = new FileOutputStream(filename, append);
//            if (filename.endsWith(".gz"))
//            {
//                if (append)
//                    throw new Error("DTRACEAPPEND environment variable is set, " + "Cannot append to gzipped dtrace file " + filename);
//                os = new GZIPOutputStream(os);
//            }
//
//            //System.out.println("limit = " + dtraceLimit + " terminate " + dtraceLimitTerminate);
//
//            // 8192 is the buffer size in BufferedReader
//            BufferedOutputStream bos = new BufferedOutputStream(os, 8192);
//            dtrace = new PrintStream(bos);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//            throw new Error(e);
//        }
//        
//        // System.out.printf("exited daikon.chicory.Runtime.setDtrace(%s, %b)%n", filename, append);
//    }
}


