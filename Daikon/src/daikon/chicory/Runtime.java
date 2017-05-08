package daikon.chicory;


import daikon.chicory.Runtime;

import java.io.*;
import java.net.*;
import java.net.Socket;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/*>>>
import org.checkerframework.checker.formatter.qual.*;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.signature.qual.*;
import org.checkerframework.dataflow.qual.*;
*/

/**
 * Runtime support for Enhanced Chicory v1.1, the Daikon front end for Java. This class is a collection of
 * methods; it should never be instantiated.
 */
@SuppressWarnings(
    "initialization.fields.uninitialized") // library initialized in code added by run-time instrumentation
public class Runtime {
  /** Unique id for method entry/exit (so they can be matched up) */
  public static int nonce = 0;

  /** debug flag */
  public static boolean debug = false;

  /** setting debug to show setting information on console */
  public static final boolean settings_debug = false;

  /** working debug to show the actual instrumentation process on console */
    public static final boolean working_debug = false;
    
  /**
   * Flag indicating that a dtrace record is currently being written used to prevent a call to
   * instrumented code that occurs as part of generating a dtrace record (eg, toArray when
   * processing lists or pure functions) from generating a nested dtrace record.
   */
  public static boolean in_dtrace = false;

  /** True if ChicoryPremain was unable to load. */
  public static boolean chicoryLoaderInstantiationError = false;

  /**
   * List of classes recently transformed. This list is examined in each enter/exit and the decl
   * information for any new classes are printed out and the class is then removed from the list.
   */
  // The order of this list depends on the order of loading by the JVM.
  // Declared as LinkedList instead of List to permit use of removeFirst().
  public static final /*@GuardedBy("<self>")*/ LinkedList<ClassInfo> new_classes =
      new LinkedList<ClassInfo>();

  /** List of all instrumented classes */
  public static final /*@GuardedBy("<self>")*/ List<ClassInfo> all_classes =
      new ArrayList<ClassInfo>();

  /** List of all observed dtraces records at enter/exit */
  public static List<TraceRecord> all_traces =
      new ArrayList<TraceRecord>();

  /** Flag that indicates when the first class has been processed. */
  static boolean first_class = true;

  /** List of all instrumented methods */
  public static final /*@GuardedBy("Runtime.class")*/ List<MethodInfo> methods =
      new ArrayList<MethodInfo>();

  //
  // Control over what classes (ppts) are instrumented
  //
  /** Ppts to omit (regular expression) */
  public static List<Pattern> ppt_omit_pattern = new ArrayList<Pattern>();

  /** Ppts to include (regular expression) */
  public static List<Pattern> ppt_select_pattern = new ArrayList<Pattern>();

  /** Comparability information (if any) */
  static /*@Nullable*/ DeclReader comp_info = null;

  //
  // Setups that control what information is written
  //
  /** Render linked lists as vectors */
  static boolean linked_lists = true;

  /** Depth to wich to examine structure components */
  static int nesting_depth = 2;

  //
  // Dtrace file vars
  //
  /** Max number of records in dtrace file */
  static long dtraceLimit = Long.MAX_VALUE;

  /** Number of records printed to date */
  static long printedRecords = 0;

  /** Terminate the program when the dtrace limit is reached */
  static boolean dtraceLimitTerminate = false;

  /** Dtrace output stream. Null if no_dtrace is true. */
  // Not annotated *@MonotonicNonNull* because initialization and use
  // happen in generated instrumentation code that cannot be type-checked
  // by a source code checker.
  static /*@GuardedBy("<self>")*/ PrintStream dtrace;

  /** Set to true when the dtrace stream is closed */
  static boolean dtrace_closed = false;

  /** True if no dtrace is being generated. */
  static boolean no_dtrace = false;

  static String method_indent = "";

  /** Decl writer setup for writing to the trace file */
  // Set in ChicoryPremain.premain().
  static DeclWriter decl_writer;

  /** Dtrace writer setup for writing to the trace file */
  // Set in ChicoryPremain.premain().
  static /*@GuardedBy("Runtime.class")*/ DTraceWriter dtrace_writer;

  /**
   * Which static initializers have been run. Each element of the Set is a fully qualified class
   * name.
   */
  private static Set<String> initSet = new HashSet<String>();

    //TODO delete it, used it to deboug the the number of traces written at entery
    static int traces_counter = 0;
    static int ppt_counter = 0;

  /** Class of information about each active call */
  private static class CallInfo {
    /** nonce of call */
    int nonce;
    /** whether or not the call was captured on enter */
    boolean captured;

    /*@Holding("Runtime.class")*/
    public CallInfo(int nonce, boolean captured) {
      this.nonce = nonce;
      this.captured = captured;
    }
  }

  /** Stack of active methods. */
  private static /*@GuardedBy("Runtime.class")*/ Map<Thread, Stack<CallInfo>> thread_to_callstack =
      new LinkedHashMap<Thread, Stack<CallInfo>>();

  /**
   * Sample count at a call site to begin sampling. All previous calls will be recorded. Sampling
   * starts at 10% and decreases by a factor of 10 each time another sample_start samples have been
   * recorded. If sample_start is 0, then all calls will be recorded.
   */
  public static int sample_start = 0;

  /**
   * A list of life instance that Chicory encountered during running the 
   * targeted application. This is will be used to see if we should construct 
   * the ppt for a method/class or not. From now on we will only construct ppt
   * for classes as soon as we have an example life instances of it. This is to 
   * help us reconstruct a collection from the reflected objects. 
   */
  public static List<Object> observed_objects = new ArrayList<Object>();
  //TODO Think about a better way than haveing all objects in a list. E.g. pass the current object as you encounter it 
  // to the enter or exit methods. 

  // Constructor
  private Runtime() {
    throw new Error("Do not create instances of Runtime");
  }

  /**
   * Thrown to indicate that main should not print a stack trace, but only print the message itself
   * to the user. If the string is null, then this is normal termination, not an error.
   */
  public static class TerminationMessage extends RuntimeException {
    static final long serialVersionUID = 20050923L;

    public TerminationMessage(String s) {
      super(s);
    }

    public TerminationMessage() {
      super();
    }
  }

  // Whenever a method call occurs in the target program, output
  // information about that call to the trace file.  However, if the
  // method is a pure method that is being called to create a value for
  // the trace file, don't record it.
  // TODO: invokingPure should be annotated with @GuardedByName("Runtime.class")
  // once that annotation is available.  Currently all the methods that access
  // invokingPure are annotated with @Holding("Runtime.class"), but annotating
  // the boolean would prevent any new methods from accessing it without holding
  // the lock.
  private static boolean invokingPure = false;

  /*@Holding("Runtime.class")*/
  public static boolean dontProcessPpts() {
    return invokingPure;
  }

  /*@Holding("Runtime.class")*/
  public static void startPure() {
    invokingPure = true;
  }

  /*@Holding("Runtime.class")*/
  public static void endPure() {
    invokingPure = false;
  }

  /**
   * Called when a method is entered.
   * The change introduced is to make Chicory always write traces at entrance and exit.
   *
   * @author zalsaeed (enhancment not creation)
   *
   * @param obj receiver of the method that was entered, or null if method is static
   * @param nonce nonce identifying which enter/exit pair this is
   * @param mi_index index in methods of the MethodInfo for this method
   * @param args array of arguments to method
   */
  public static synchronized void enter(
      /*@Nullable*/ Object obj, int nonce, int mi_index, Object[] args) {

    if(Runtime.working_debug)
      System.out.println("\nenter >>>>> [Chicory.Runtime.enter()] mi-> "
          + methods.get(mi_index).class_info.class_name +"."
          + methods.get(mi_index).method_name);

      //Check if the instance is already alive? to associate it with the class name when instrumented. 
      if(obj!= null) {
        if(Runtime.working_debug)
          System.out.println("[Chicory.Runtime.enter()] Given reciver obj -> " + obj.getClass().getName());
        if(!observed_objects.contains(obj)) {
          observed_objects.add(obj);
        }
      } else {
        if(Runtime.working_debug)
          System.out.println("[Chicory.Runtime.enter()] given reciver obj is NULL");
      }

      if(Runtime.working_debug) {
        for(Object o:observed_objects) {
          System.out.println("In list: " + o.getClass().getName());
        }
      }

    if (debug) {
      MethodInfo mi = methods.get(mi_index);
      System.out.printf(
          "%smethod_entry %s.%s%n", method_indent, mi.class_info.class_name, mi.method_name);
      method_indent = method_indent.concat("  ");
    }

    //The method is excluded by the user .. do nothing
    if (dontProcessPpts()) return;

    // Make sure that the in_dtrace flag matches the stack trace
    // check_in_dtrace();

    // Ignore this call if we are already processing a dtrace record
    if (in_dtrace) return;

    // Note that we are processing a dtrace record until we return
    in_dtrace = true;
    try {
      //process_new_classes();
      process_classes(true, obj, mi_index);

      //this is the method that we encountered and would like to get the its traces
      MethodInfo mi = methods.get(mi_index);
      mi.call_cnt++;

      mi.capture_cnt++;
      traces_counter++;

      dtrace_writer.methodEntry(mi, nonce, obj, args);

    } finally {
      //release the writing flag for dtrace
      in_dtrace = false;
    }
    if(Runtime.working_debug)
      System.out.println("exit >>>>> [Chicory.Runtime.enter()] -> "
          + methods.get(mi_index).class_info.class_name +"."
          + methods.get(mi_index).method_name);
  }

  /**
   * Called when a method is exited.
   * This was introduced to make Chicory always write traces at entrance and exit
   *
   * @author zalsaeed
   *
   * @param obj receiver of the method that was entered, or null if method is static
   * @param nonce nonce identifying which enter/exit pair this is
   * @param mi_index index in methods of the MethodInfo for this method
   * @param args array of arguments to method
   * @param ret_val return value of method, or null if method is void
   * @param exitLineNum the line number at which this method exited
   */
  public static synchronized void exit(
      /*@Nullable*/ Object obj,
      int nonce,
      int mi_index,
      Object[] args,
      Object ret_val,
      int exitLineNum) {

      if(Runtime.working_debug)
        System.out.println("\nenter >>>>> [Chicory.Runtime.exit()] -> "
            + methods.get(mi_index).class_info.class_name +"."
            + methods.get(mi_index).method_name);

      if(obj!= null) {
        if(Runtime.working_debug)
          System.out.println("[Chicory.Runtime.exit()] Given reciver obj -> " + obj.getClass().getName());
        if(!observed_objects.contains(obj)) {
          observed_objects.add(obj);
        }
      } else {
        if(Runtime.working_debug)
          System.out.println("[Chicory.Runtime.exit()] given reciver obj is NULL");
      }

      if(Runtime.working_debug) {
        for(Object o:observed_objects) {
          System.out.println("In list: " + o.getClass().getName());
        }
      }
    	
    if (debug) {
      MethodInfo mi = methods.get(mi_index);
      method_indent = method_indent.substring(2);
      System.out.printf(
          "%smethod_exit  %s.%s%n", method_indent, mi.class_info.class_name, mi.method_name);
    }

    if (dontProcessPpts()) return;

    // Make sure that the in_dtrace flag matches the stack trace
    // check_in_dtrace();

    // Ignore this call if we are already processing a dtrace record
    if (in_dtrace) return;

    // Note that we are processing a dtrace record until we return
    in_dtrace = true;
    try {

      //process_new_classes();
      process_classes(false, obj, mi_index);

      // Write out the infromation for this method
      MethodInfo mi = methods.get(mi_index);
      traces_counter++;

      // long start = System.currentTimeMillis();
      dtrace_writer.methodExit(mi, nonce, obj, args, ret_val, exitLineNum);
      // long duration = System.currentTimeMillis() - start;
      // System.out.println ("Exit " + mi + " " + duration + "ms");
    } finally {
      in_dtrace = false;
    }

    if(Runtime.working_debug)
      System.out.println("exit >>>>> [Chicory.Runtime.exit()] -> "
          + methods.get(mi_index).class_info.class_name +"."
          + methods.get(mi_index).method_name);
  }

  /**
   * Checks the in_dtrace flag by looking back up the stack trace. Throws an exception if there is a
   * discrepancy.
   */
  private static void check_in_dtrace() {

    Throwable st = new Throwable();
    st.fillInStackTrace();
    List<StackTraceElement> enter_exit_list = new ArrayList<StackTraceElement>();
    for (StackTraceElement ste : st.getStackTrace()) {
      if (ste.getClassName().endsWith("chicory.Runtime")
          && (ste.getMethodName().equals("enter") || ste.getMethodName().equals("exit")))
        enter_exit_list.add(ste);
    }
    if (in_dtrace && (enter_exit_list.size() <= 1)) {
      throw new RuntimeException("in dtrace and stack contains " + enter_exit_list);
    } else if (!in_dtrace && (enter_exit_list.size() > 1)) {
      throw new RuntimeException("not in dtrace and stack contains " + enter_exit_list);
    }
  }

  /**
   * Called by classes when they have finished initialization (i.e., their static initializer has
   * completed).
   *
   * <p>This functionality must be enabled by the flag Chicory.checkStaticInit. When enabled, this
   * method should only be called by the hooks created in the Instrument class.
   *
   * @param className fully qualified class name
   */
  public static void initNotify(String className) {
    //To inspect who is calling this method ...
//    	StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
//    	for (StackTraceElement s:stackTraceElements){
//    		System.out.println("Who called me? " + s.getClassName() + " : " + s.getLineNumber() + s.getMethodName());
//    	}
    	
    if (initSet.contains(className)) {
      throw new Error("initNotify(" + className + ") when initSet already contains " + className);
    }
    if(Runtime.working_debug)
      System.out.println("[Chicory.Runtime.initNotify()] initialized ---> " + className);
    initSet.add(className);
  }

  /**
   * Return true iff the class with fully qualified name className has been initialized.
   *
   * @param className fully qualified class name
   */
  public static boolean isInitialized(String className) {
    return initSet.contains(className);
  }
    
    /**
     * @author zalsaeed
     * 
     * This is a method that should process the given class 
     * decal information. At every method entry/exit call we 
     * are going to process its class (Reciver.Class) based on 
     * weather an instance of the class  is given (not null)
     * or not. Then we will write traces based on those fresh 
     * information. 
     * 
     * @param isEntry - true if the method is invoked by enter() false if invoked by exit()
     * @param obj - Receiver of the method that was entered/exited.  Null if method is static or init
     * 
     */
    public static void process_classes(Boolean isEntry, /*@Nullable*/ Object obj, int mi_index){
    	
    	if(Runtime.working_debug)
    		System.out.println("\nenter >>>>> [Chicory.Runtime.process_classes()]");
    	
    	MethodInfo mi = methods.get(mi_index);
    	ClassInfo ci = mi.class_info;
    	
    	if(Runtime.working_debug)
    		System.out.println ("\tstart processing class " + ci.class_name);		
    	    		
    	if (debug)
    		System.out.println ("processing class " + ci.class_name);
    	
    	//print the header of the .dtrace file
    	if (first_class) {
    		decl_writer.printHeaderInfo (ci.class_name);
    		first_class = false;	
    	}
    	
    	ci.initViaReflection();
    	
    	//TODO use this for constructing only this method that we are writing traces for
    	if(isEntry)
    		mi.traversalEnter = RootInfo.enter_process(mi, Runtime.nesting_depth);
    	else
    		mi.traversalExit = RootInfo.exit_process(mi, Runtime.nesting_depth);
    	
    	//Constructing decl for all method of the given class, in case the single method doesn't work
//    	for (MethodInfo m_info: ci.method_infos)
//    	{
//    		m_info.traversalEnter = RootInfo.enter_process(m_info, Runtime.nesting_depth);
//    		m_info.traversalExit = RootInfo.exit_process(m_info, Runtime.nesting_depth);	
//    	}
    	
    	//the comp_info is always null for me.
    	decl_writer.print_decl_class (ci, comp_info, mi, isEntry);
    	
    	if(Runtime.working_debug)
    		System.out.println("exit <<<<< [Chicory.Runtime.process_classes()]");
    }

  /**
   * Writes out decl information for any new classes (those in the new_classes field) and removes
   * them from that list.
   */
  /*@Holding("Runtime.class")*/
  public static void process_new_classes() {

    // Processing of the new_classes list must be
    // very careful, as the call to get_reflection or printDeclClass
    // may load other classes (which then get added to the list).
    if(Runtime.working_debug)
      System.out.println("\nenter >>>>> [Chicory.Runtime.process_new_class()]");
    	
    while (true) {

      // Get the first class in the list (if any)
      ClassInfo class_info = null;
      synchronized (new_classes) {
        if (new_classes.size() > 0) {
          class_info = new_classes.removeFirst();
        }
      }

      //no more classes break the while loop
      if (class_info == null) break;

      if(Runtime.working_debug)
        System.out.println ("\tstart processing class " + class_info.class_name);

      if (debug) System.out.println("processing class " + class_info.class_name);

      //print the header of the .dtrace file 
      if (first_class) {
        decl_writer.printHeaderInfo(class_info.class_name);
        first_class = false;
      }
      class_info.initViaReflection();
      // class_info.dump (System.out);

      // Create tree structure for all method entries/exits in the class
      for (MethodInfo mi : class_info.method_infos) {
        mi.traversalEnter = RootInfo.enter_process(mi, Runtime.nesting_depth);
        mi.traversalExit = RootInfo.exit_process(mi, Runtime.nesting_depth);
      }

      //the comp_info is always null for me. 
      //TODO create your own printDeclClass method since this it self is not used anymore (print_decl_class is the one in use now )
      //TODO I think it is better to pass comp_infoto the new method even though it is null for me, just to avoid fixing all the other methods 
      decl_writer.printDeclClass(class_info, comp_info);
      if(Runtime.working_debug)
        System.out.println ("\tend processing class " + class_info.class_name);
    }
    if(Runtime.working_debug)
      System.out.println("exit <<<<< [Chicory.Runtime.process_new_class()]");
  }

  /** Increment the number of records that have been printed. */
  public static void incrementRecords() {
    printedRecords++;

    // This should only print a percentage if dtraceLimit is not its
    // default value.
    // if (printedRecords%1000 == 0)
    //     System.out.printf("printed=%d, percent printed=%f%n", printedRecords, (float)(100.0*(float)printedRecords/(float)dtraceLimit));

    if (printedRecords >= dtraceLimit) {
      noMoreOutput();
    }
  }

  /**
   * Indicates that no more output should be printed to the dtrace file. The file is closed and iff
   * dtraceLimitTerminate is true the program is terminated.
   */
  public static void noMoreOutput() {
    // The incrementRecords method (which calls this) is called inside a
    // synchronized block, but re-synchronize just to be sure, or in case
    // this is called from elsewhere.

    // Runtime.dtrace should be effectively final in that it refers
    // to the same value throughout the execution of the synchronized
    // block below (including the lock acquisition).
    // Unfortunately, the Lock Checker cannot verify this,
    // so a final local variable is used to satisfy the Lock Checker's
    // requirement that all variables used as locks be final or
    // effectively final.  If a bug exists whereby Runtime.dtrace
    // is not effectively final, this would unfortunately mask that error.
    final /*@GuardedBy("<self>")*/ PrintStream dtrace = Runtime.dtrace;

    synchronized (dtrace) {
      // The shutdown hook is synchronized on this, so close it up
      // ourselves, lest the call to System.exit cause deadlock.
      dtrace.println();
      dtrace.println("# EOF (added by no_more_output)");
      dtrace.close();

      // Don't set dtrace to null, because if we continue running, there will
      // be many attempts to synchronize on it.  (Is that a performance
      // bottleneck, if we continue running?)
      // dtrace = null;
      dtrace_closed = true;

      if (dtraceLimitTerminate) {
        System.out.println("Printed " + printedRecords + " records to dtrace file.  Exiting.");
        throw new TerminationMessage(
            "Printed " + printedRecords + " records to dtrace file.  Exiting.");
        // System.exit(1);
      } else {
        // By default, no special output if the system continues to run.
        no_dtrace = true;
      }
    }
  }

    /**
     * Description by @author zalsaeed
     * This is called only when we run daikon and Chicory at the 
     * same time "online".
     * @param port
     */
  /*@EnsuresNonNull("dtrace")*/
  public static void setDtraceOnlineMode(int port) {
    dtraceLimit = Long.getLong("DTRACELIMIT", Integer.MAX_VALUE).longValue();
    dtraceLimitTerminate = Boolean.getBoolean("DTRACELIMITTERMINATE");

    Socket daikonSocket = null;
    try {
      daikonSocket = new Socket();
      @SuppressWarnings("nullness") // unannotated: java.net.Socket is not yet annotated
      /*@NonNull*/ SocketAddress dummy = null;
      daikonSocket.bind(dummy);
      //System.out.println("Attempting to connect to Daikon on port --- " + port);
      daikonSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(), port), 5000);
    } catch (UnknownHostException e) {
      System.out.println(
          "UnknownHostException connecting to Daikon : " + e.getMessage() + ". Exiting");
      System.exit(1);
    } catch (IOException e) {
      System.out.println(
          "IOException, could not connect to Daikon : " + e.getMessage() + ". Exiting");
      System.exit(1);
    }

    try {
      dtrace = new PrintStream(daikonSocket.getOutputStream());
    } catch (IOException e) {
      System.out.println("IOException connecting to Daikon : " + e.getMessage() + ". Exiting");
      System.exit(1);
    }

    if (supportsAddShutdownHook()) {
      addShutdownHook();
    } else {
      System.err.println("Warning: .dtrace file may be incomplete if program is aborted");
    }
  }

  // Copied from daikon.Runtime
  /** Specify the dtrace file to which to write */
  /*@EnsuresNonNull("dtrace")*/
  public static void setDtrace(String filename, boolean append) {

    if(Runtime.settings_debug)
      System.out.printf("entered daikon.chicory.Runtime.setDtrace(%s, %b)...%n", filename, append);

    if (no_dtrace) {
      throw new Error("setDtrace called when no_dtrace was specified");
    }
    try {
      File file = new File(filename);
      File parent = file.getParentFile();
      if (parent != null) parent.mkdirs();
      OutputStream os = new FileOutputStream(filename, append);
      if (filename.endsWith(".gz")) {
        if (append) {
          throw new Error(
              "DTRACEAPPEND environment variable is set, "
                  + "Cannot append to gzipped dtrace file "
                  + filename);
        }
        os = new GZIPOutputStream(os);
      }
      dtraceLimit = Long.getLong("DTRACELIMIT", Integer.MAX_VALUE).longValue();
      dtraceLimitTerminate = Boolean.getBoolean("DTRACELIMITTERMINATE");

      //System.out.println("limit = " + dtraceLimit + " terminate " + dtraceLimitTerminate);

      // 8192 is the buffer size in BufferedReader
      BufferedOutputStream bos = new BufferedOutputStream(os, 8192);
      dtrace = new PrintStream(bos);
    } catch (Exception e) {
      e.printStackTrace();
      throw new Error(e);
    }
    if (supportsAddShutdownHook()) {
      addShutdownHook();
    } else {
      System.err.println("Warning: .dtrace file may be incomplete if program is aborted");
    }
    // System.out.printf("exited daikon.chicory.Runtime.setDtrace(%s, %b)%n", filename, append);
  }

  /**
   * If the current data trace file is not yet set, then set it. The value of the DTRACEFILE
   * environment variable is used; if that environment variable is not set, then the argument to
   * this method is used instead.
   */
  public static void setDtraceMaybe(String default_filename) {
    // Copied from daikon.Runtime
    // System.out.println ("Setting dtrace maybe: " + default_filename);
    if ((dtrace == null) && (!no_dtrace)) {
      String filename = System.getProperty("DTRACEFILE", default_filename);
      boolean append = System.getProperty("DTRACEAPPEND") != null;
      setDtrace(filename, append);
    }
  }

  private static boolean supportsAddShutdownHook() {
    // Copied from daikon.Runtime

    try {
      Class<java.lang.Runtime> rt = java.lang.Runtime.class;
      rt.getMethod("addShutdownHook", new Class<?>[] {java.lang.Thread.class});
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /** Add a shutdown hook to close the PrintStream when the program exits. */
  private static void addShutdownHook() {
    // Copied from daikon.Runtime, then modified

    java.lang.Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @SuppressWarnings(
                  "lock") // TODO: Fix Checker Framework issue 523 and remove this @SuppressWarnings.
              public void run() {
                if (!dtrace_closed) {
                  // When the program being instrumented exits, the buffers
                  // of the "dtrace" (PrintStream) object are not flushed,
                  // so we miss the tail of the file.

                  synchronized (Runtime.dtrace) {
                    dtrace.println();
                    // These are for debugging, I assume. -MDE
                    for (Pattern p : ppt_omit_pattern) {
                      dtrace.println("# ppt-omit-pattern: " + p);
                    }
                    for (Pattern p : ppt_select_pattern) {
                      dtrace.println("# ppt-select-pattern: " + p);
                    }
                    // This lets us know we didn't lose any data.
                    dtrace.println("# EOF (added by Runtime.addShutdownHook)");
                    dtrace.close();
                  }
                    System.out.println("\n\nenhanced Chciory Report:");
                    System.out.println("Total ppts written: " + ppt_counter);
                    System.out.println("Total traces written: " + traces_counter);
                }

                if (chicoryLoaderInstantiationError) {
                  // Warning messages have already been printed.
                } else if (all_classes.size() == 0) {
                  System.out.println("Chicory warning: No methods were instrumented.");
                  if ((!ppt_select_pattern.isEmpty()) || (!ppt_omit_pattern.isEmpty())) {
                    System.out.println(
                        "Check the --ppt-select-pattern and --ppt-omit-pattern options");
                  }
                } else if (printedRecords == 0) {
                  System.out.println("Chicory warning: no records were printed");
                }
              }
            });
  }

  private static Process chicory_proc;

  private static StreamRedirectThread err_thread;

  private static StreamRedirectThread out_thread;

  static void setDaikonInfo(StreamRedirectThread err, StreamRedirectThread out, Process proc) {
    chicory_proc = proc;
    err_thread = err;
    out_thread = out;
  }

  /** Wait for Daikon to terminate. */
  public static void endDaikon() {
    try {
      int status = chicory_proc.waitFor();
      System.out.println("daikon ended with status " + status);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }

    try {
      err_thread.join();
      out_thread.join();
    } catch (InterruptedException e) {
    }

    System.out.println("Finished endDaikon");
  }

  /**
   * Gets the ClassInfo structure corresponding to type. Returns null if the class was not
   * instrumented.
   *
   * @param type declaring class
   * @return ClassInfo structure corresponding to type
   */
  public static /*@Nullable*/ ClassInfo getClassInfoFromClass(Class<?> type) {
    try {
      synchronized (Runtime.all_classes) {
        for (ClassInfo cinfo : Runtime.all_classes) {
          if (cinfo.clazz == null) cinfo.initViaReflection();

          assert cinfo.clazz != null
              : "@AssumeAssertion(nullness): checker bug: flow problem (postcondition)";

          if (cinfo.clazz.equals(type)) {
            return cinfo;
          }
        }
      }
    } catch (ConcurrentModificationException e) {
      // occurs if cinfo.get_reflection() causes a new class to be loaded
      // which causes all_classes to change
      return getClassInfoFromClass(type);
    }

    // throw new RuntimeException("Unable to find class " + type.getName() + " in Runtime's class list");
    return null;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Wrappers for the various primitive types.
  /// Used to distinguish wrappers created by user code
  /// from wrappers created by Chicory.

  public static interface PrimitiveWrapper {
    // returns corresponding java.lang wrapper
    public Object getJavaWrapper();

    public Class<?> primitiveClass();
  }

  /** wrapper used for boolean arguments */
  public static class BooleanWrap implements PrimitiveWrapper {
    boolean val;

    public BooleanWrap(boolean val) {
      this.val = val;
    }
    /*@SideEffectFree*/
    public String toString(/*>>>@GuardSatisfied BooleanWrap this*/) {
      return Boolean.toString(val);
    }

    public Boolean getJavaWrapper() {
      return new Boolean(val);
    }

    public Class<?> primitiveClass() {
      return boolean.class;
    }
  }

  /** wrapper used for int arguments */
  public static class ByteWrap implements PrimitiveWrapper {
    byte val;

    public ByteWrap(byte val) {
      this.val = val;
    }
    /*@SideEffectFree*/
    public String toString(/*>>>@GuardSatisfied ByteWrap this*/) {
      return Byte.toString(val);
    }

    public Byte getJavaWrapper() {
      return new Byte(val);
    }

    public Class<?> primitiveClass() {
      return byte.class;
    }
  }

  /** wrapper used for int arguments */
  public static class CharWrap implements PrimitiveWrapper {
    char val;

    public CharWrap(char val) {
      this.val = val;
    }
    // Print characters as integers.
    /*@SideEffectFree*/
    public String toString(/*>>>@GuardSatisfied CharWrap this*/) {
      return Integer.toString(val);
    }

    public Character getJavaWrapper() {
      return new Character(val);
    }

    public Class<?> primitiveClass() {
      return char.class;
    }
  }

  /** wrapper used for int arguments */
  public static class FloatWrap implements PrimitiveWrapper {
    float val;

    public FloatWrap(float val) {
      this.val = val;
    }
    /*@SideEffectFree*/
    public String toString(/*>>>@GuardSatisfied FloatWrap this*/) {
      return Float.toString(val);
    }

    public Float getJavaWrapper() {
      return new Float(val);
    }

    public Class<?> primitiveClass() {
      return float.class;
    }
  }

  /** wrapper used for int arguments */
  public static class IntWrap implements PrimitiveWrapper {
    int val;

    public IntWrap(int val) {
      this.val = val;
    }
    /*@SideEffectFree*/
    public String toString(/*>>>@GuardSatisfied IntWrap this*/) {
      return Integer.toString(val);
    }

    public Integer getJavaWrapper() {
      return new Integer(val);
    }

    public Class<?> primitiveClass() {
      return int.class;
    }
  }

  /** wrapper used for int arguments */
  public static class LongWrap implements PrimitiveWrapper {
    long val;

    public LongWrap(long val) {
      this.val = val;
    }
    /*@SideEffectFree*/
    public String toString(/*>>>@GuardSatisfied LongWrap this*/) {
      return Long.toString(val);
    }

    public Long getJavaWrapper() {
      return new Long(val);
    }

    public Class<?> primitiveClass() {
      return long.class;
    }
  }

  /** wrapper used for int arguments */
  public static class ShortWrap implements PrimitiveWrapper {
    short val;

    public ShortWrap(short val) {
      this.val = val;
    }
    /*@SideEffectFree*/
    public String toString(/*>>>@GuardSatisfied ShortWrap this*/) {
      return Short.toString(val);
    }

    public Short getJavaWrapper() {
      return new Short(val);
    }

    public Class<?> primitiveClass() {
      return short.class;
    }
  }

  /** wrapper used for double arguments */
  public static class DoubleWrap implements PrimitiveWrapper {
    double val;

    public DoubleWrap(double val) {
      this.val = val;
    }
    /*@SideEffectFree*/
    public String toString(/*>>>@GuardSatisfied DoubleWrap this*/) {
      return Double.toString(val);
    }

    public Double getJavaWrapper() {
      return new Double(val);
    }

    public Class<?> primitiveClass() {
      return double.class;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Copied code
  ///

  // Lifted directly from plume/UtilMDE.java, where it is called
  // escapeNonJava(), but repeated here to make this class self-contained.
  /** Quote \, ", \n, and \r characters in the target; return a new string. */
  public static String quote(String orig) {
    StringBuffer sb = new StringBuffer();
    // The previous escape (or escaped) character was seen right before
    // this position.  Alternately:  from this character forward, the string
    // should be copied out verbatim (until the next escaped character).
    int post_esc = 0;
    int orig_len = orig.length();
    for (int i = 0; i < orig_len; i++) {
      char c = orig.charAt(i);
      switch (c) {
        case '\"':
        case '\\':
          if (post_esc < i) {
            sb.append(orig.substring(post_esc, i));
          }
          sb.append('\\');
          post_esc = i;
          break;
        case '\n': // not lineSep
          if (post_esc < i) {
            sb.append(orig.substring(post_esc, i));
          }
          sb.append("\\n"); // not lineSep
          post_esc = i + 1;
          break;
        case '\r':
          if (post_esc < i) {
            sb.append(orig.substring(post_esc, i));
          }
          sb.append("\\r");
          post_esc = i + 1;
          break;
        default:
          // Do nothing; i gets incremented.
      }
    }
    if (sb.length() == 0) return orig;
    sb.append(orig.substring(post_esc));
    return sb.toString();
  }

  private static HashMap<String, String> primitiveClassesFromJvm = new HashMap<String, String>(8);

  static {
    primitiveClassesFromJvm.put("Z", "boolean");
    primitiveClassesFromJvm.put("B", "byte");
    primitiveClassesFromJvm.put("C", "char");
    primitiveClassesFromJvm.put("D", "double");
    primitiveClassesFromJvm.put("F", "float");
    primitiveClassesFromJvm.put("I", "int");
    primitiveClassesFromJvm.put("J", "long");
    primitiveClassesFromJvm.put("S", "short");
  }

  /**
   * Convert a classname from JVML format to Java format. For example, convert "[Ljava/lang/Object;"
   * to "java.lang.Object[]".
   *
   * <p>If the argument is not a field descriptor, returns it as is. This enables this method to be
   * used on the output of {@link Class#getName()}.
   */
  @Deprecated
  public static String classnameFromJvm(/*@FieldDescriptor*/ String classname) {
    return fieldDescriptorToBinaryName(classname);
  }

  /**
   * Convert a classname from JVML format to Java format. For example, convert "[Ljava/lang/Object;"
   * to "java.lang.Object[]".
   *
   * <p>If the argument is not a field descriptor, returns it as is. This enables this method to be
   * used on the output of {@link Class#getName()}.
   */
  @SuppressWarnings("signature") // conversion routine
  public static String fieldDescriptorToBinaryName(/*@FieldDescriptor*/ String classname) {

    //System.out.println(classname);

    int dims = 0;
    while (classname.startsWith("[")) {
      dims++;
      classname = classname.substring(1);
    }

    String result;
    //array of reference type
    if (classname.startsWith("L") && classname.endsWith(";")) {
      result = classname.substring(1, classname.length() - 1);
      result = result.replace('/', '.');
    } else {
      if (dims > 0) //array of primitives
      result = primitiveClassesFromJvm.get(classname);
      else //just a primitive
      result = classname;

      if (result == null) {
        // As a failsafe, use the input; perhaps it is in Java, not JVML,
        // format.
        result = classname;
        // throw new Error("Malformed base class: " + classname);
      }
    }
    for (int i = 0; i < dims; i++) {
      result += "[]";
    }
    return result;
  }

  @SuppressWarnings("signature") // conversion method
  public static final /*@BinaryName*/ String classGetNameToBinaryName(
      /*@ClassGetName*/ String cgn) {
    if (cgn.startsWith("[")) {
      return fieldDescriptorToBinaryName(cgn);
    } else {
      return cgn;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// end of copied code
  ///

}
