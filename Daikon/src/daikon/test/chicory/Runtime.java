package daikon.test.chicory;

import junit.framework.*;
import daikon.*;
import daikon.chicory.DTraceWriter;
import daikon.tools.DtraceDiff;

import java.io.PrintStream;
import java.lang.reflect.*;
import java.net.URL;

public class Runtime extends TestCase {

	
	public static void testEnter() {
		
		PrintStream outFile = new PrintStream(System.out);
		
		DTraceWriter my_writer = new DTraceWriter(outFile);
		
		
		fail("Not yet implemented");
	}

}
