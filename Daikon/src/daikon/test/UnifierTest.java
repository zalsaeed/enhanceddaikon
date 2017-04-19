package daikon.test;

import static org.junit.Assert.*;

import org.junit.Test;

import daikon.PptInfo;
import daikon.Unifier;

public class UnifierTest {
	
	@Test
	public void testMainArgumenHandling() {
		
		//Too much arguments
		try {
			Unifier.main(new String[] {"combine","TesterDdsObserved.dtrace.gz", "111", " "});
		} catch (IllegalArgumentException e){
			
		}
		
		//Wrong first argument 
		try {
			Unifier.main(new String[] {"combin","TesterDdsObserved.dtrace.gz"});
		} catch (IllegalArgumentException e){
			
		}
		
		//Wrong second argument
		try {
			Unifier.main(new String[] {"combine","TesterDdsObserved.txt"});
		} catch (IllegalArgumentException e){
			
		}
		
		try {
			Unifier.main(new String[] {"TesterDdsObserved.txt", "111"});
		} catch (IllegalArgumentException e){
			
		}
		
//		try {
//			Unifier.main(new String[] {"TesterDdsObserved.dtrace", "ABC"});
//		} catch (NumberFormatException e){
//			
//		}
//		
//		try {
//			Unifier.main(new String[] {"TesterDdsObserved.dtrace", "11 11"});
//		} catch (NumberFormatException e){
//			
//		}
		
	}
	
	@Test
	public void testMainFinalReports() {
		
		String[] args = {"TesterDdsObserved.dtrace.gz"};
	    Unifier.main(args);
	    
	    //read all lines in the file
		assertEquals(6043, Unifier.total_number_of_lines);
	    
		//Observed all (non-identical) ppts
		assertEquals(140, Unifier.countAllPpts);
		
		//Make sure we got the right number of identical ppts (the final ppts we want)
		assertEquals(15, Unifier.ppt_keys.size());
		
		//TODO you must try this file also and make sure it is proceesed correctly
		//Server.SocketPackageDdsObserved.dtrace.gz
		
		// Making sure we processed all the observed ppts
		//assertEquals(140, Unifier.countProcesedPpts);
		
		
		
	}
	
	@Test
	public void processLineTest(){
		
		//Unifier.processLine(reader, line);
		
	}
	
	//TODO test the identifyLine(String line) method
	//TODO add test to this method
	//TODO add test for readPptNameString(String pptName, PptInfo pptinfo){
	

}
