package daikon.test;

import static org.junit.Assert.*;

import org.junit.Test;

import daikon.Unifier;

public class UnifierTest {
	
	@Test
	public void testMainArgumenHandling() {
		
		//Too much arguments
		try {
			Unifier.main(new String[] {"", " ","TesterDdsObserved.dtrace.gz"});
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
	}
	
	@Test
	public void testMainFinalReports() {
		
		String[] args = {"combine" ,"TesterDdsObserved.dtrace.gz"};
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

}
