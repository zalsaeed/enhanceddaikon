package daikon.chicory;

import daikon.Chicory;
import daikon.util.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

/*>>>
import org.checkerframework.checker.interning.qual.*;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.signature.qual.*;
import org.checkerframework.dataflow.qual.*;
*/

/**
 * Each DaikonVariableInfo object is a node in the tree structure of the variables in the target
 * application. The tree structure is built in the DeclWriter and traversed in the DTraceWriter.
 * There is such a tree structure associated with every program point. This architecture makes it
 * possible to avoid the issue of "traversal pattern duplication" in which both the DeclWriter and
 * DTraceWriter must traverse the target application's variables identically. In general, the
 * variable a will be the parent of the variables a.b and a.c in the tree, where b and c are fields
 * in a's class.
 *
 * <p>Each node can have any non-negative number of child nodes. DaikonVariableInfo is an abstract
 * class. Its subtypes are designed to represent specific types of variables, such as arguments,
 * arrays, etc.
 */
public abstract class DaikonVariableInfo
    implements Iterable<DaikonVariableInfo>, Comparable<DaikonVariableInfo> {

  /** Enable experimental techniques on static constants. */
  public static boolean dkconfig_constant_infer = false;

  /** The variable name. Sensible for all subtypes except RootInfo. */
  private final /*@Interned*/ String name;

  /** The child nodes */
  public List<DaikonVariableInfo> children;

  /** True iff this variable is an array */
  protected final boolean isArray;

  /** Print debug information about the variables */
  // "false" argument means it's disabled by default.
  static SimpleLog debug_vars = new SimpleLog(false);

  private static SimpleLog debug_array = new SimpleLog(false);

  /** Default string for comparability info. */
  private static final String compareInfoDefaultString = "22";

  // It's not enough to use one of the following 3 strings.
  // You also need to set the flags that are returned by get_var_flags()!

  /** Indicates that a given variable is non-null. */
  protected static final String isNonNullString = " # isNonNull=true";

  /** Indicates that a given variable is a parameter to a method. */
  protected static final String isParamString = " # isParam=true";

  /** Indicates that a given variable is non-null and a parameter. */
  protected static final String isNonNullParamString = " # isNonNull=true isParam=true";

  // Certain hardcoded class names
  protected static final String stringClassName = "java.lang.String";
  protected static final String classClassName = "java.lang.Class";

  // Suffix for "typeOf" (CLASSNAME) variables that represent a class,
  // eg, "foo.getClass().getName()".
  public static final String class_suffix = ".getClass().getName()";

  /** Determines whether or not synthetic variables should be ignored */
  private static boolean skip_synthetic = true;

  /**
   * The printed type that will appear in the .decls declaration. May include aux information at the
   * end, such as isParamString.
   *
   * @see #getTypeName()
   * @see #getTypeNameOnly()
   */
  protected String typeName;
  /** The printed representation type that will appear in the .decls declaration. */
  protected String repTypeName;
  /** The printed comparability information that will appear in the .decls declaration. */
  protected String compareInfoString = compareInfoDefaultString;

  /** Value of static constants. Access via {@link #get_const_val} method. */
  /*@Nullable*/ String const_val = null;

  /** Arguments used to create a function. Access via {@link #get_func_args()} method. */
  /*@Nullable*/ String function_args = null;

  /** True iff the DeclWriter should print this variable */
  protected boolean declShouldPrint = true;

  /** True iff the DTraceWriter should print this variable */
  protected boolean dtraceShouldPrint = true;

  /** True iff the DTraceWriter should print the children of this variable */
  protected boolean dtraceShouldPrintChildren = true;

  /**
   * If false, use standard dfej behavior (any field in an instrumented class is visible). If true,
   * use standard Java behavior (if the field is in a class in a different package, it is only
   * visible if public, etc.).
   */
  public static boolean std_visibility = false;

  /**
   * Set of fully qualified static variable names for this ppt. Used to ensure that each static is
   * only included once (regardless of how many other variables may include its declaring class).
   */
  protected static Set<String> ppt_statics = new LinkedHashSet<String>();

  /**
   * Constructs a non-array type DaikonVariableInfo object
   *
   * @param theName the name of the variable
   */
  public DaikonVariableInfo(String theName, String typeName, String repTypeName) {
    this(theName, typeName, repTypeName, false);
  }

  /**
   * Constructs a DaikonVariableInfo object
   *
   * @param theName the variable's name
   * @param arr true iff the variable is an array
   */
  public DaikonVariableInfo(String theName, String typeName, String repTypeName, boolean arr) {
    // Intern the names because there will be many of the
    // same variable names at different program points within
    // the same class.
    name = theName.intern();
    this.typeName = typeName.intern();
    this.repTypeName = repTypeName.intern();

    debug_vars.log(
        "Construct DaikonVariableInfo: %s : %s : %s", this.getClass().getName(), name, typeName);

    children = new ArrayList<DaikonVariableInfo>();
    isArray = arr;

    if ((theName != null) && (theName.contains("[..]") || theName.contains("[]")) && !isArray) {
      debug_array.log_tb("%s is not an array", theName);
    }
  }

  /** Returns the name of this variable. */
  public /*@Nullable*/ String getName(/*>>>@GuardSatisfied DaikonVariableInfo this*/) {
    if (name == null) return null;

    if (Chicory.new_decl_format) {
      return name.replaceFirst("\\[]", "[..]");
    } else {
      return name;
    }
  }

  /**
   * Add a child to this node. Should only be called while the tree is being constructed.
   *
   * @param info the child object, must be non-null. The child's fields name, typeName, repTypeName,
   *     and compareInfoString should also be non-null.
   */
  protected void addChild(DaikonVariableInfo info) {
    assert info != null : "info cannot be null in DaikonVariableInfo.addChild()";
    assert info.name != null : "Child's name should not be null";
    assert info.typeName != null : "Child's type name should not be null";
    assert info.repTypeName != null : "Child's representation type name should not be null";
    assert info.compareInfoString != null : "Child's comparability information should not be null";

    if(Runtime.working_debug)
        System.out.printf("\t\t\t\t\t[DaikonVariableInfo.addChild()] Adding: %s to %s%n", info , this);
    debug_vars.log("Adding %s to %s", info, this);
    children.add(info);
    if(Runtime.working_debug)
        System.out.printf("\t\t\t\t\t[DaikonVariableInfo.addChild()] new children.size() =  %s in %s%n", children.size(), this);
        
  }

  /** Returns a string representation of this node. */
  /*@SideEffectFree*/
  public String toString(/*>>>@GuardSatisfied DaikonVariableInfo this*/) {
    return getClass().getName() + ":" + getName();
  }

  /** Returns a string representative of this node and its children */
  public String treeString() {
    return getStringBuffer(new StringBuffer("--")).toString();
  }

  /**
   * Return a StringBuffer that contains the name of this node and all ancestors of this node.
   * Longer indentations correspond to further distance in the tree.
   *
   * @param offset the offset to begin each line with
   * @return StringBuffer that contains all children of this node
   */
  private StringBuffer getStringBuffer(StringBuffer offset) {
    StringBuffer theBuf = new StringBuffer();

    theBuf.append(offset + name + DaikonWriter.lineSep);

    StringBuffer childOffset = new StringBuffer(offset);
    childOffset.append("--");
    for (DaikonVariableInfo info : children) {
      theBuf.append(info.getStringBuffer(childOffset));
    }

    return theBuf;
  }

  /**
   * Return an iterator over all the node's children. Don't modify the list of children through the
   * iterator, as an unmodifiable list is used to generate the iterator.
   */
  public Iterator<DaikonVariableInfo> iterator() {
    return Collections.unmodifiableList(children).iterator();
  }

  /** Returns the complete tree of variables as a list */
  public List<DaikonVariableInfo> tree_as_list() {
    List<DaikonVariableInfo> list = new ArrayList<DaikonVariableInfo>();
    list.add(this);
    for (DaikonVariableInfo dv : children) {
      list.addAll(dv.tree_as_list());
    }
    return list;
  }

  /**
   * Given an object value corresponding to the parent of this DaikonVariableInfo variable, return
   * the value (of the corresponding value in the target application) of this DaikonVariableInfo
   * variable.
   *
   * <p>For instance, if the variable a has a field b, then calling getMyValFromParentVal(val_of_a)
   * will return the value of a.b
   *
   * @param parentVal the parent object. Can be null for static fields. (Are there any other
   *     circumstances where it can be null?)
   */
  public abstract /*@Nullable*/ Object getMyValFromParentVal(Object parentVal);

  /**
   * Returns a String representation of this object suitable for a {@code .dtrace} file
   *
   * @param val the object whose value to print
   */
  @SuppressWarnings("unchecked")
  public String getDTraceValueString(Object val) {
    if (isArray) {
      return getValueStringOfListWithMod((List<Object>) val); // unchecked cast
    } else {
      return getValueStringOfObjectWithMod(val, true);
    }
  }

  /** Gets the value of an object and concatenates the associated "modified" integer. */
  protected String getValueStringOfObjectWithMod(Object theValue, boolean hashArray) {
    String retString = getValueStringOfObject(theValue, hashArray) + DaikonWriter.lineSep;

    if (theValue instanceof NonsensicalObject) {
      retString += "2";
    } else {
      retString += "1";
    }

    return retString;
  }

  /**
   * Gets the value, but with no endline. If hashArray is true, it prints the "hash code" of the
   * array and not its separate values.
   */
  private String getValueStringOfObject(Object theValue, boolean hashArray) {
    if (theValue == null) {
      return "null";
    }

    Class<?> type = theValue.getClass();

    assert !(type.isPrimitive()) : "Objects cannot be primitive";

    if (theValue instanceof Runtime.PrimitiveWrapper) {
      return getPrimitiveValueString(theValue);
    } else if (!hashArray && (type.isArray())) {
      //show the full array
      return getValueStringOfArray(theValue);
    } else if (theValue instanceof NonsensicalObject) {
      return "nonsensical";
    } else {
      //basically, show the hashcode of theValue
      return getObjectHashCode(theValue);
    }
  }

  /** Get value string for a primitive (wrapped) object. */
  private String getPrimitiveValueString(Object obj) {
    assert (obj instanceof Runtime.PrimitiveWrapper)
        : "Objects passed to showPrimitive must implement PrimitiveWrapper"
            + DaikonWriter.lineSep
            + "This object is type: "
            + obj.getClass().getName();

    //use wrapper classes toString methods to print value
    return (obj.toString());
  }

  /** Gets a string representation of the values in an array. */
  private String getValueStringOfArray(Object array) {
    List<Object> theList = DTraceWriter.getListFromArray(array);
    return getValueStringOfList(theList);
  }

  /** Gets the Object's unique ID as a string. In other words, a "hash code". */
  private String getObjectHashCode(Object theObject) {
    if (theObject == null) {
      return "null";
    } else if (theObject instanceof NonsensicalObject) {
      return "nonsensical";
    } else {
      return Integer.toString(System.identityHashCode(theObject));
    }
  }

  /**
   * Gets the list of values (as a string) from getValueStringOfList and concatenates the "modified"
   * value.
   */
  private String getValueStringOfListWithMod(List<Object> theValues) {
    String retString = getValueStringOfList(theValues) + DaikonWriter.lineSep;

    if (theValues instanceof NonsensicalList) {
      retString += ("2");
    } else {
      retString += ("1");
    }

    return retString;
  }

  /**
   * Returns a string representation of the values of a list of values as if it were an array.
   *
   * @param theValues the values to print out
   */
  protected String getValueStringOfList(List<Object> theValues) {
    if (theValues == null) {
      return "null";
    }

    if (theValues instanceof NonsensicalList) {
      return "nonsensical";
    }

    StringBuffer buf = new StringBuffer();

    buf.append("[");
    for (Iterator<Object> iter = theValues.iterator(); iter.hasNext(); ) {
      Object elementVal = iter.next();

      //hash arrays...
      //don't want to print arrays within arrays
      buf.append(getValueStringOfObject(elementVal, true));

      //put space between elements in array
      if (iter.hasNext()) buf.append(" ");
    }
    buf.append("]");

    return buf.toString();
  }

  /** Add the parameters of the given method to this node. */
  protected void addParameters(
      ClassInfo cinfo, Member method, List<String> argnames, String offset, int depth) {

    if(Runtime.working_debug)
        System.out.println("\t\t\tenter >>>>> [Chicory.DaikonVariableInfo.addParampter()] "
    				+ "cinfo: " + cinfo.class_name + " method: " + method.getName()
    				+ " offset: " + offset + " depth: " + depth);    	
    Class<?>[] arguments =
        (method instanceof Constructor<?>)
            ? ((Constructor<?>) method).getParameterTypes()
            : ((Method) method).getParameterTypes();

    debug_vars.log("enter addParameters%n");
    Iterator<String> argnamesiter = argnames.iterator();
    int param_offset = 0;
    for (int i = 0; (i < arguments.length) && argnamesiter.hasNext(); i++) {
      Class<?> type = arguments[i];
      String name = argnamesiter.next();
      if (type.getName().equals("daikon.dcomp.DCompMarker")) continue;
      if (type.getName().equals("java.lang.DCompMarker")) continue;
      if(Runtime.working_debug)
          System.out.printf("\t\t\t\t[Chicory.DaikonVariableInfo.addParampter()] processing parameter '%s'%n", name);
      debug_vars.indent("processing parameter '%s'%n", name);
      DaikonVariableInfo theChild =
          addParamDeclVar(cinfo, type, name, offset, depth, i, param_offset);
      param_offset++;
      if ((type == Double.TYPE) || (type == Long.TYPE)) param_offset++;
      assert cinfo.clazz != null : "@AssumeAssertion(nullness): need to check justification";
      theChild.addChildNodes(cinfo, type, name, offset, depth);
      debug_vars.exdent();
    }
    debug_vars.log("exit addParameters%n");
    if(Runtime.working_debug)
        System.out.println("\t\t\texit <<<<< [Chicory.DaikonVariableInfo.addParampter()] "
            + "cinfo: " + cinfo.class_name);
  }

  /**
   * Adds class variables (i.e., the fields) for the given type and attach new nodes as children of
   * this node.
   *
   * @param type the class whose fields should all be added to this node
   */
  /*@RequiresNonNull("#1.clazz")*/
  protected void addClassVars(
      ClassInfo cinfo, boolean dontPrintInstanceVars, Class<?> type, String offset, int depth) {

    if(Runtime.working_debug)
        System.out.printf("\t\t\tenter >>>>> [Chicory.DaikonVariableInfo.addClassVars] "
            + "%s : %s : %s : [%s] isStatic: %s :"
            + " shouldAddRuntimeClass: %s%n",
            this, cinfo, type, offset, dontPrintInstanceVars,
            shouldAddRuntimeClass(type));
    	
    //DaikonVariableInfo corresponding to the "this" object
    DaikonVariableInfo thisInfo;

    //System.out.printf("\t\taddClassVars: %s : %s : %s: [%s]%n", this, cinfo, type, offset);
    debug_vars.log("addClassVars: %s : %s : %s: [%s]%n", this, cinfo, type, offset);

    //must be at the first level of recursion (not lower) to print "this" field
    //if not isStatic and the first one on tree ...
    if (!dontPrintInstanceVars && offset.equals("")) {
      // "this" variable
      if(Runtime.working_debug)
          System.out.println();
      thisInfo = new ThisObjInfo(type);
      addChild(thisInfo);

      //.class variable
      if (shouldAddRuntimeClass(type)) {
        DaikonVariableInfo thisClass =
            new DaikonClassInfo(
                "this" + class_suffix, classClassName, stringClassName, "this", false);
        thisInfo.addChild(thisClass);
      }
    } else if (offset.equals("")) {
      // Create a non-printing root for static variables.
      thisInfo = new StaticObjInfo(type);
      addChild(thisInfo);
    } else {
      thisInfo = this;
    }

    // Get the fields
    // System.out.printf ("getting fields for %s%n", type);

    // We need to get fields of superclass(es) as well.
    List<Field> fields = new ArrayList<Field>();
    Class<?> c = type;
    while (c != null && c != Object.class) {
      fields.addAll(Arrays.asList(c.getDeclaredFields()));
      c = c.getSuperclass();
    }

    // if (fields.length > 50)
    //    System.out.printf ("%d fields in %s%n", fields.length, type);

    debug_vars.log(
        "%s: [%s] %d dontPrintInstanceVars = %b, " + "inArray = %b%n",
        type, offset, fields.size(), dontPrintInstanceVars, isArray);

    if(Runtime.working_debug)
        System.out.println("\t\t\tFields of: " + type.getName());

    for (Field classField : fields) {
      boolean is_static = Modifier.isStatic(classField.getModifiers());

      debug_vars.log("considering field %s -> %s%n", offset, classField);

      // Skip some fields

      // In the future, perhaps skip some synthetic fields.  So far, the
      // only ones we have seen are 'this$0' (a field in an inner class
      // that contains a pointer to the instance of the outer class),
      // 'this$1' for an inner inner class, and so on.  These variables
      // expose the outer class fields of an inner class to Daikon.

      if (!is_static && dontPrintInstanceVars) {
        //if the filed is not static but the class that contains it is 
        // then do nothing for this field ...
        debug_vars.log("--field !static and instance var %b%n", dontPrintInstanceVars);
        continue;
      }

      // Skip variables that match the ignore pattern
      if (Chicory.omit_var != null) {
        String fullname = offset + "." + classField.getName();
        if (is_static) {
          fullname = classField.getDeclaringClass().getName() + "." + classField.getName();
        }
        Matcher m = Chicory.omit_var.matcher(fullname);
        if (m.find()) {
          // System.out.printf("VAR %s matches omit pattern %s%n", fullname, Chicory.omit_var);
          continue;
        }
      }

      // Don't print arrays of the same static field
      if (is_static && isArray) {
        //if field is static but the containing object is array, then skip
        debug_vars.log("--field static and inArray%n");
        continue;
      }

      // Skip any statics that have been already included
      if (is_static) {
        String full_name = classField.getDeclaringClass().getName() + "." + classField.getName();
        if (ppt_statics.contains(full_name) && (depth <= 0)) {
          debug_vars.log("already included static %s (no children)", full_name);

          continue;
        }
      }

      if (!isFieldVisible(cinfo.clazz, classField)) {
    	//skip if field is not visible from within this current object
    	//look at isFieldVisible method to undestand what is execluded.
        debug_vars.log("--field not visible%n");
        continue;
      }

      // ... end of code to skip some fields.
      //System.out.printf("\t\t\tclassfield type: %s %n", classField.getType());
      if(Runtime.working_debug)
          System.out.printf("\t\t\t\tprocessing: %s %n", classField.getName());

      Class<?> fieldType = classField.getType();

      StringBuffer buf = new StringBuffer();
      DaikonVariableInfo newChild = thisInfo.addDeclVar(classField, offset, buf, type);

      debug_vars.indent("--Created DaikonVariable %s%n", newChild);

      String newOffset = buf.toString();
            if(Runtime.working_debug)
            	System.out.printf("\t\t\tbuf: %s %n", newOffset);

      newChild.addChildNodes(cinfo, fieldType, classField.getName(), newOffset, depth);
      debug_vars.exdent();
    }

    // If appropriate, print out decls information for pure methods
    // and add to the tree
    // Check dontPrintInstanceVars is basically checking if the program point method
    // (not the pure method) is static.  If it is, don't continue because we can't
    // call instance methods (all pure methods we consider are instance methods)
    // from static methods
    if (ChicoryPremain.shouldDoPurity() && !dontPrintInstanceVars) {
      //TODO see if this is get called ...
      ClassInfo typeInfo = null;

      try {
        typeInfo = Runtime.getClassInfoFromClass(type);
      } catch (RuntimeException e) {
        // Could not find the class... no further purity analysis
        typeInfo = null;
      }

      if (typeInfo != null) {
        //Pure methods with no parameters
        for (MethodInfo meth : typeInfo.method_infos) {
          if (meth.isPure() && meth.arg_names.length == 0) {
            StringBuffer buf = new StringBuffer();
            DaikonVariableInfo newChild =
                thisInfo.addPureMethodDecl(
                    cinfo, meth, new DaikonVariableInfo[] {}, offset, depth, buf);
            String newOffset = buf.toString();
            debug_vars.indent("Pure method");
            assert meth.member != null
                : "@AssumeAssertion(nullness): member of method_infos have .member field"; // fix with dependent type
            newChild.addChildNodes(
                cinfo,
                ((Method) meth.member).getReturnType(),
                meth.member.getName(),
                newOffset,
                depth);
            debug_vars.exdent();
          }
        }

        // List containing all class variables, excluding pure methods with parameters
        List<DaikonVariableInfo> siblings = new ArrayList<DaikonVariableInfo>(thisInfo.children);

        // Pure methods with one parameter
        for (MethodInfo meth : typeInfo.method_infos) {
          if (meth.isPure() && meth.arg_names.length == 1) {
            for (DaikonVariableInfo sib : siblings) {
              String sibType = sib.getTypeNameOnly();
              Class<?> sibClass = null;

              // Get class type of the class variable
              try {
                sibClass = UtilMDE.classForName(UtilMDE.binaryNameToClassGetName(sibType));
              } catch (ClassNotFoundException e) {
                throw new Error(e);
              }

              // Add node if the class variable can be used as the pure method's parameter
              if (UtilMDE.isSubtype(sibClass, meth.arg_types[0])) {
                DaikonVariableInfo[] arg = {sib};
                StringBuffer buf = new StringBuffer();
                DaikonVariableInfo newChild =
                    thisInfo.addPureMethodDecl(cinfo, meth, arg, offset, depth, buf);
                String newOffset = buf.toString();
                debug_vars.indent("Pure method");
                assert meth.member != null
                    : "@AssumeAssertion(nullness): member of method_infos have .member field"; // fix with dependent type
                newChild.addChildNodes(
                    cinfo,
                    ((Method) meth.member).getReturnType(),
                    meth.member.getName(),
                    newOffset,
                    depth);
                debug_vars.exdent();
              }
            }
          }
        }
      }
    }
    debug_vars.log("exit addClassVars%n");
    if(Runtime.working_debug)
        System.out.println("\t\t\texit <<<<< [Chicory.DaikonVariableInfo.assClassVars]");
  }

    /**
     * @author zalsaeed
     * 
     * This class gets a Java Collection an returns its fields 
     * to add them later as child nodes in for the current method
     * 
     * @param field
     * @param offset
     * @param buff
     */
    private void getCollectionFileds(Field field, String offset, StringBuffer buff) {
    	   	
       
 	   //System.out.println("\t\t\tThis is my List: " + field.getName());
 	   
 	   //Signature --> addChildNodes(final ClassInfo cinfo, Class<?> type, String theName,String offset, int depthRemaining)
 	   //Class --> newChild.addChildNodes(cinfo, fieldType, classField.getName(),newOffset, depth);

 	    ClassInfo cinfo = Runtime.getClassInfoFromClass(field.getDeclaringClass());
        String value = null;
        boolean isPrimitive = true;

        if (cinfo != null) {
            value = cinfo.staticMap.get(field.getDeclaringClass().getName());
            //System.out.println("\t\t\tThe name value: " + value);
            
            String className = cinfo.class_name;
            //System.out.println("\t\t\tOwner class name: " + className);
            
            // If the class has already been statically initialized, get its hash
            //System.out.println("\t\t\tAll instrumented classes: " + Runtime.initSet);
            if (Runtime.isInitialized(className)) { //not returning true!!!!
            	//System.out.println("\t\t\tThe name value: " + value);
                try {
                    value = Integer.toString(System.identityHashCode(field.get(null)));
                } catch(Exception e) {}
            }
            try {
                value = Integer.toString(System.identityHashCode(field.get(null)));
            } catch(Exception e) {}
            //System.out.println("\t\t\tagain value: " + value);
            
        }
    	
    }
    
    /**
     * Added this method to store the info of a list elements in a child. 
     *
     * @author zalsaeed
     *
     * @return The newly created DaikonVariableInfo object, whose
     * parent is this.
     */
    protected DaikonVariableInfo addParamDeclVar(Class<?> type,
           String name, String offset, Class<? extends List<?>>parentType)
    {
        debug_vars.log ("enter addDeclVar(ListElement)%n");
        // add this variable to the tree as a child of curNode
        //TODO create the appropiate newChidl
        
        DaikonVariableInfo newChild = new ListElement(type, 
        		offset + name, 
        		name, 
        		getRepName(type, false),
                type.isArray(),
                parentType);
        
        addChild(newChild);

        //I'm not checking derived variable for now for simplicity 
//        boolean ignore = newChild.check_for_dup_names();
//        if (!ignore)
//            newChild.checkForDerivedVariables(type, name, offset);

        debug_vars.log ("exit addDeclVar(ListElement)%n");
        return newChild;
    }

  /**
   * Adds the decl info for a single parameter as a child of this node. Also adds "derived"
   * variables such as the runtime .class variable.
   *
   * @return the newly created DaikonVariableInfo object, whose parent is this
   */
  protected DaikonVariableInfo addParamDeclVar(
      ClassInfo cinfo,
      Class<?> type,
      String name,
      String offset,
      int depth,
      int argNum,
      int param_offset) {
    debug_vars.log("enter addParamDeclVar%n");
    // add this variable to the tree as a child of curNode
    DaikonVariableInfo newChild = new ParameterInfo(offset + name, argNum, type, param_offset);

    addChild(newChild);

    boolean ignore = newChild.check_for_dup_names();
    if (!ignore) newChild.checkForDerivedVariables(type, name, offset);

    debug_vars.log("exit addParamDeclVar%n");
    return newChild;
  }

  /** Adds the decl info for a pure method. */
  //TODO factor out shared code with printDeclVar
  protected DaikonVariableInfo addPureMethodDecl(
      ClassInfo curClass,
      MethodInfo minfo,
      DaikonVariableInfo[] args,
      String offset,
      int depth,
      StringBuffer buf) {
    String arr_str = "";
    if (isArray) {
      arr_str = "[]";
    }

    @SuppressWarnings("nullness") // method precondition
    /*@NonNull*/ Method meth = (Method) minfo.member;

    boolean changedAccess = false;

    //we want to access all fields...
    if (!meth.isAccessible()) {
      changedAccess = true;
      meth.setAccessible(true);
    }

    Class<?> type = meth.getReturnType();
    assert type != null;

    String theName = meth.getName() + "(";
    if (args.length > 0) {
      theName += args[0].getName();
    }
    if (args.length > 1) {
      for (int i = 1; i < args.length - 1; i++) {
        theName += ", " + args[i].getName();
      }
    }
    theName += ")";

    if (offset.length() > 0) {
      // offset already starts with "this"
    } else {
      offset = "this.";
    }

    String type_name = stdClassName(type);
    // TODO: Passing incorrect receiver name????
    DaikonVariableInfo newPure =
        new PureMethodInfo(
            offset + theName,
            minfo,
            type_name + arr_str,
            getRepName(type, isArray) + arr_str,
            offset.substring(0, offset.length() - 1),
            isArray,
            args);

    addChild(newPure);

    newPure.checkForDerivedVariables(type, theName, offset);

    buf.append(offset);

    if (changedAccess) {
      meth.setAccessible(false);
    }

    return newPure;
  }

  /**
   * Adds the decl info for a single class variable (a field) as a child of this node. Also adds
   * "derived" variables such as the runtime .class variable.
   * 
   * edited by @author zalsaeed to act as needed for dynamic data structures.
   *
   * @return the newly created DaikonVariableInfo object, whose parent is this
   */
  protected DaikonVariableInfo addDeclVar(Field field, String offset, StringBuffer buf, Class<?> parentType) {
    //System.out.printf("enter addDeclVar(field):%n");
    //System.out.printf("  field: %s, offset: %s%n", field, offset);
    debug_vars.log("enter addDeclVar(field):%n");
    debug_vars.log("  field: %s, offset: %s%n", field, offset);
    if(Runtime.working_debug)
        System.out.printf("\t\t\tenter >>>>> [Chicory.DaikonVariableInfo.addDeclVar(field)]: field: %s offset: %s%n", field, offset);
    String arr_str = "";
    if (isArray) arr_str = "[]";

    // Temporarily make the field accessible.
    boolean changedAccess = false;
    if (!field.isAccessible()) {
      changedAccess = true;
      field.setAccessible(true);
    }

    int modifiers = field.getModifiers();
    if (Modifier.isStatic(modifiers)) {
      offset = field.getDeclaringClass().getName() + ".";
    } else if (offset.length() == 0) { // instance fld, 1st recursion step
      offset = "this.";
    }

    Class<?> type = field.getType();
    String type_name = stdClassName(type) + arr_str + appendAuxInfo(field);

    String theName = field.getName();

    // Convert the internal reflection name for an outer class
    // 'this' field to the Java language format.
    if (theName.startsWith("this$")) {
      offset = "";
      theName = type.getName() + ".this";
      if (!type_name.contains("#")) {
        type_name += " # isNonNull=true";
      } else {
        type_name += ", isNonNull=true";
      }
    }

    DaikonVariableInfo newField =
        new FieldInfo(
            offset + theName, field, type_name, getRepName(type, false) + arr_str, isArray);
    boolean ignore = newField.check_for_dup_names();

    if (DaikonWriter.isStaticConstField(field) && !isArray) {
      ClassInfo cinfo = Runtime.getClassInfoFromClass(field.getDeclaringClass());
      String value = null;
      boolean isPrimitive = true;

      if (cinfo != null) {
        value = cinfo.staticMap.get(theName);

        if (DaikonVariableInfo.dkconfig_constant_infer) {
          if (value == null) {
            isPrimitive = false;
            String className = field.getDeclaringClass().getName();
            // If the class has already been statically initialized, get its hash
            if (Runtime.isInitialized(className)) {
              try {
                value = Integer.toString(System.identityHashCode(field.get(null)));
              } catch (Exception e) {
              }
            }
          }
        }
      }

      // System.out.printf ("static final value = %s%n", value);

      // in this case, we don't want to print this variable to
      // the dtrace file
      if (value != null) {
        newField.repTypeName += " = " + value;
        newField.const_val = value;
        newField.dtraceShouldPrint = false;
        if (DaikonVariableInfo.dkconfig_constant_infer && isPrimitive) {
          newField.dtraceShouldPrintChildren = false;
        }
      }
      //else
      //{
      // don't print anything
      // because this field wasn't declared with an actual "hardcoded" constant
      //}

    }

    addChild(newField);

    if (!ignore) {
      /**
       * @author zalsaeed
       * changed this to call my own method
       */
      newField.checkForDerivedVariables(type, theName, offset, parentType);
      //System.out.printf("\t\t\tAddiing derived variables: type: %s name: %s offset: %s", type, theName, offset);
    }

    buf.append(offset);

    if (changedAccess) {
      field.setAccessible(false);
    }

    debug_vars.log("exit addDeclVar(field)%n");
    if(Runtime.working_debug)
        System.out.printf("\t\t\texit <<<<< [Chicory.DaikonVariableInfo.addDeclVar(field)]%n");
    return newField;
  }

  /**
   * Returns the class name of the specified class as a binary name (i.e., as the class would have
   * been declared in Java source code, except with '$' instead of '.' separating outer and inner
   * classes).
   */
  public static /*@BinaryName*/ String stdClassName(Class<?> type) {
    return Runtime.classGetNameToBinaryName(type.getName());
  }

  /**
   * Given a type, gets the representation type to be used in Daikon. For example, the
   * representation type of a class object is "hashcode."
   *
   * @param type the type of the variable
   * @param asArray whether the variable is being output as an array (true) or as a pointer (false)
   * @return the representation type as a string
   */
  public static String getRepName(Class<?> type, boolean asArray) {
    if (type == null) {
      return "hashcode";
    } else if (type.isPrimitive()) {
      if (type.equals(Double.TYPE)) return "double";
      else if (type.equals(Float.TYPE)) return "double";
      else if (type.equals(Boolean.TYPE)) return "boolean";
      else return "int";
    } else if (type.getName().equals("java.lang.String")) {
      // if we are printing the actual array, the rep type is "java.lang.String"
      if (true) return "hashcode";
      if (asArray) return "java.lang.String";
      // otherwise, it is just a hashcode
      else return "hashcode";
    } else {
      return "hashcode";
    }
  }

  /**
   * Determines if type needs a corresponding .class runtime class variable.
   *
   * <p>The .class variable is printed for interfaces, abstract classes, and Object. For these
   * types, the run-time class is always (or, for Object, usually) different than the declared type.
   * An alternate, and possibly more useful, heuristic would be to print the .class variable for any
   * type that has subtypes.
   *
   * @param type the variable's type
   */
  protected static boolean shouldAddRuntimeClass(Class<?> type) {
    // For some reason, abstracts seems to be set on arrays
    // and primitives.  This is a temporary fix to get things
    // close.
    if (type.isPrimitive()) return false;
    if (type.isArray()) {
      Class<?> eltType = type.getComponentType();
      assert eltType != null; // because type is an array
      return !(eltType.isPrimitive());
    }

    if (type.getName().equals("java.lang.Object")) {
      // Objects
      // System.out.println ("type is object " + type);
      return true;
    } else if (Modifier.isAbstract(type.getModifiers())) {
      // System.out.printf ("Type [%s] is abstract %Xh %Xh %s%n", type,
      //                   type.getModifiers(), Modifier.ABSTRACT,
      //                   Modifier.toString (type.getModifiers()));
      return true;
    } else if (type.isInterface()) {
      // System.out.println ("type is interface " + type);
      return true;
    } else if (type.isArray()) {
      // Arrays of non-primitive types
      // System.out.println ("type is array " + type);
      Class<?> eltType = type.getComponentType();
      assert eltType != null; // because type is an array
      return !(eltType.isPrimitive());
    } else {
      return false;
    }
  }

  /**
   * Returns whether or not the specified field is visible from the Class current. All fields within
   * instrumented classes are considered visible from everywhere (to match dfej behavior).
   */
  public static boolean isFieldVisible(Class<?> current, Field field) {
    Class<?> fclass = field.getDeclaringClass();
    int modifiers = field.getModifiers();

    // If the field is within the current class, it is always visible
    if (current.equals(fclass)) return true;

    if (!std_visibility) {
      // If the field is in any instrumented class it is always visible
      synchronized (Runtime.all_classes) {
        for (ClassInfo ci : Runtime.all_classes) {
          // System.out.printf ("comparing %s vs %s%n", ci.class_name,
          // fclass.getName());
          if (ci.class_name.equals(fclass.getName())) {
            return true;
          }
        }
      }
    }

    // Otherwise we consider the variable not to be visible, even
    // though it is.  This mimics dfej behavior
    if (!std_visibility) return false;

    // Everything in the same class is visible
    if (current == fclass) return true;

    // If the field is in the same package, it's visible if it is
    // not private or protected.
    if (current.getPackage() != null && current.getPackage().equals(fclass.getPackage())) {
      return !(Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers));
    }

    // The field must be in an unrelated class, it must be marked
    // public to be visible
    return (Modifier.isPublic(modifiers));
  }

  // Appends as auxiliary information:
  // the package name of the declaring class
  private String appendAuxInfo(Field field) {
    // int modifiers = field.getModifiers();

    Package p = field.getDeclaringClass().getPackage();
    String pkgName = (p == null ? null : p.getName());

    //System.out.printf("Package name for type  %s is %s%n", type, pkgName);

    StringBuilder ret = new StringBuilder();

    // String staticString = (Modifier.isStatic(modifiers)) ? "true" : "false";
    ret.append(" # ");
    if (pkgName != null) {
      ret.append("declaringClassPackageName=" + pkgName + ", ");
    }

    return ret.toString();
  }

    /**
     * @author zalsaeed
     * This is my version of theis method, I changed its sigature to pass 
     * the parent cinfo to the child. This is becuase we need it 
     * to determen the object from which we are going to construct 
     * the class again. 
    *
    * Checks for "derived" Chicory variables: .class, .tostring, and java.util.List implementors
    * and adds appropriate children to this node.
    */
   protected void checkForDerivedVariables(Class<?> type, String theName,
           String offset, Class<?> parentType)
   {
       checkForListDecl(type, theName, offset, parentType); // implements java.util.List?

       //Not fully implemented yet, don't call
       //checkForImplicitList(cinfo, type, name, offset, depth);

       checkForRuntimeClass(type, theName, offset); //.class var
       checkForString(type, theName, offset); //.tostring var
   }
   
  /**
   * Checks for "derived" Chicory variables: .class, .tostring, and java.util.List implementors and
   * adds appropriate children to this node.
   */
  protected void checkForDerivedVariables(Class<?> type, String theName, String offset) {
    checkForListDecl(type, theName, offset); // implements java.util.List?

    //Not fully implemented yet, don't call
    //checkForImplicitList(cinfo, type, name, offset, depth);

    checkForRuntimeClass(type, theName, offset); //.class var
    checkForString(type, theName, offset); //.tostring var
  }

   /**
    * @author zalsaeed
    * 
    * Determines if type implements list
    * and prints associated decls, if necessary
    * @param parentType: to know who is having the list.
    */
   protected void checkForListDecl(Class<?> type, String theName, String offset, 
		   Class<?> parentType)
   {
       if (isArray || type.isPrimitive() || type.isArray())
           return;

       // System.out.printf ("checking %s %sto for list implementation = %b%n",
       //                    type, theName, implementsList (type));

       //I might need to extend the method implementList() to also consider collections 
       if (implementsList(type)) {
           @SuppressWarnings("unchecked")
           DaikonVariableInfo child = new ListInfo(offset + theName + "[]",
                                              (Class<? extends List<?>>)type);

           addChild(child);

           boolean ignore = child.check_for_dup_names();

           // CLASSNAME var
           if (!ignore) {
               DaikonVariableInfo childClass
                   = new DaikonClassInfo(offset + theName + "[]" + class_suffix, 
                		   classClassName + "[]", 
                		   stringClassName + "[]", 
                		   offset + theName + "[]", 
                		   true);
               
               child.addChild(childClass);
           }
           
           
           Object currentObj = null;
           
           if(Runtime.working_debug)
        	   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] parentType: " 
        			   + parentType);
           /*
            * getting the instance from my observed_objects instead of passing it down
            * through all the method calls. My choice here is not safe, since objects could 
            * be died in the actual program, but they are only available because my list 
            * is holding them. However, this choice works for me now. Until this is proven 
            * valuable there is no need to go over all the trouble of changing all the 
            * methods involved in this process. 
            * 
            * parentType is the parent of this list, or where this list is defined.
            */
           if (parentType != null){
        	   if(Runtime.working_debug){
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] ParentClass: " + parentType.getName());
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] Parent HashCode: " + parentType.hashCode());
        	   }
           
	           for (Object obj:Runtime.observed_objects){
	        	   if(obj.getClass().hashCode() == parentType.hashCode()){
	        		   currentObj = obj;
	        	   }
	        	   if(Runtime.working_debug){
	        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] Avialable Objects: " + obj.getClass().getName());
	        		   System.out.println("\t\t\t\t" + obj.getClass().hashCode());
	        	   }
	           }
           }
           
           List<Object> listElements = new ArrayList<Object>();
           //I'm only looking for user defined lists and if their object is instantiated
           //theName here is the name of the list as the user defined it.
           if (currentObj != null){
        	   if(Runtime.working_debug)
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] Will construct tree of list elements ...");
        	   listElements = getListElements(currentObj, theName);
           }else{
        	   if(Runtime.working_debug)
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] nothing to do ...");
           }
           
           /*
            * At this stage I either don't have any element or I know that the elements I have 
            * are non-primitive (could be user defined object, java defined object, or array)
            * 
            * I'm adding those elements as children of this ListInfo object 
            * (e.g. listName.[..].element1) with type ListElement. I'm also adding the fields 
            * of the elements (given that I can access them) as children of the newly added
            * ListEelemnt child (e.g. listName.[..].element1.field1) with type FieldInfo.
            * 
            * Here are some choices I made.
            * 
            * Choice 1: 
    	    * I'm using the class name here (obj.getClass().getSimpleName()) to identify 
    	    * an element in the list, but this could be problematic as there could be two 
    	    * different elements of the same class. Thus, it would be confusing when 
    	    * reading invariants generated by Daikon. Another identifier that could be used
    	    * is the element ID in the as it is in the list (maybe along with the class 
    	    * name). However, I will not go with this choice until I find an actual problem 
    	    * caused by this choice.
    	    * 
    	    * Choice 2:
    	    * I'm only observing the elements's fields. I have no intention to go deeper into 
    	    * the field's fields (e.g. element.field.field_of_the_field). This is to be consistent
    	    * With the normally given depth (most of the times 2, but could be 1). Meaning, if we 
    	    * are inspecting a list of the "this" node, then we are still complying with the given 
    	    * depth. However, if we are inspecting a child of the node "this" this we are making 
    	    * sure that we don't go too deep into the tree of fields.  
    	    * 
    	    * Choice 3: 
    	    * offset, here is fixed manually according to where we are in the execution. Manually
    	    * means that I didn't follow how Chicory's original developers handled offset. This is 
    	    * because first I'm dealing with a list and no variable names (related to choice 1), second 
    	    * it is safe to handle the offset manually in this level since we are not going deeper
    	    * (see choice 2).
            */
           for (Object obj:listElements){
        	   /*
        	    * Tracking elements of same type issue:
        	    * 
        	    * Tracking the ID of the elements this way doesn't guarantee that we are tracking the same element 
        	    * each time we observe this list/collection. It is possible that the order of elements would change and
        	    * thus all IDs would change and cause confusion. Also, it is possible that one element gets deleted and
        	    * elements after it get shifted one position to the left, hence change their IDs too. The only way to
        	    * track these elements and make sure which element is deleted or shifted is by using the hashcode of
        	    * the object for tracking. And keeping it in our records for later possible comparison. However, even
        	    * this solution has some flaws. First, it is expensive to keep track of collections in memory. Second,
        	    * elements of same object type could have same hashcode, thus the hashcode is not unique. 
        	    */
        	   
        	   if(Runtime.working_debug){
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] Got them: " + obj.getClass().getSimpleName());
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] this is classClassName: " + classClassName);
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] this is stringClassName: " + stringClassName);
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] this is class_suffix: " + class_suffix);
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] this is theName: " + theName);
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] this is offset: " + offset);
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] this is this: " + this);
        		   System.out.println("\t\t\t\t[Chicory.DaikonVariableInfo.checkForListDecl()] this is child.getName(): " + child.getName());
        	   }
        	   
        	   //manually handling the offset.
        	   String currentOffset = offset + theName + "[" + obj.hashCode() +"].";
        	   
        	   //add this object as a ListElement to the ListInfo
        	   @SuppressWarnings("unchecked")
        	   DaikonVariableInfo newChild = child.addParamDeclVar(obj.getClass(), 
        			   obj.getClass().getSimpleName(), currentOffset, (Class<? extends List<?>>)type);
        	   
        	   //Adding the object's (or elements) fields as FieldInfos to the ListElement
        	   for(Field objField:obj.getClass().getDeclaredFields()){
        		   
        		   DaikonVariableInfo newField = new FieldInfo(currentOffset + obj.getClass().getSimpleName() + "." + objField.getName(), 
        				   objField,
        				   objField.getType().getName(), 
        				   getRepName(objField.getType(), false), 
        				   false);
        		   //TODO need a case where the field is not primitive to see how it would be handled.
        		   newChild.addChild(newField);
        		   
        	   }
           }
       }
   }
   
   /**
    * @author zalsaeed
    * A method to return a list of elements in the given object.
    * 
    * @param obj
    * @param listName
    */
   protected List<Object> getListElements(Object obj, String listName){
		//TODO add nested primitive values to scope.
	   
	   List<Object> listOfElements = new ArrayList<Object>();
		
	   if(Runtime.working_debug)
		   System.out.println("\t\t\t\t\t[Chicory.DaikonVariableInfo.getListElements()] Got Called!");
	   Field field = null;

	   //if the list (dynamic object) object is already null, then return immediately with an empty list of elements.
	   //if(obj == null){
	   //  return listOfElements;
       //}

	   try {
		   field = obj.getClass().getDeclaredField(listName);
		   
	   } catch (NoSuchFieldException | SecurityException e) {
		   //TODO throw a proper message
		   e.printStackTrace();   
	   }
	   
	   //we want to access all fields,,, then return it back to its setup
	   boolean accessChanged = false;
       if (!field.isAccessible())
       {
           accessChanged = true;
           field.setAccessible(true);
       }
	   
	   Object listInstance = null;
	   try {
		   listInstance = field.get(obj);   
	   } catch (IllegalArgumentException | IllegalAccessException e) {
		   //TODO throw a proper message
		   e.printStackTrace();   
	   }
	   
	   //return immediately if the list is still not initialized.
	   if(listInstance == null) {
	     if(Runtime.working_debug)
           System.out.println("\t\t\t\t\t[Chicory.DaikonVariableInfo.getListElements()] List is not initialized, abort! " + listName);
         return listOfElements;
       }
	   
	   if(listInstance instanceof Collection){
		   if(Runtime.working_debug)
			   System.out.println("\t\t\t\t\t[Chicory.DaikonVariableInfo.getListElements()] I got the list type ..." + field.getName());
	   }
	   
	   Type genericFieldType = field.getGenericType();
	   if(Runtime.working_debug)
		   System.out.println("\t\t\t\t\t[Chicory.DaikonVariableInfo.getListElements()] List<Type>: " + genericFieldType);
	   
	   if(genericFieldType instanceof ParameterizedType){
		   ParameterizedType aType = (ParameterizedType) genericFieldType;
		   Type[] fieldArgTypes = aType.getActualTypeArguments(); //getActualTypeArguments
		   if(Runtime.working_debug)
			   System.out.println("\t\t\t\t\t[Chicory.DaikonVariableInfo.getListElements()] Field args types: " + fieldArgTypes);
		   for(Type fieldArgType : fieldArgTypes){
			   if(fieldArgType instanceof ParameterizedType) { //(e.g. List<Hashtable<String, String>>) store it as it is
				   for (Iterator<?> iter = ((Iterable) listInstance).iterator(); iter.hasNext(); ) {
					   //TODO check if the objects in list are primitive values
					   Object element = iter.next();
					   element.getClass();
					   listOfElements.add(element);
				   }
			   } else { // cast it to class to check oif it is primitive 
				   Class<?> fieldArgClass = (Class<?>) fieldArgType;
				   if (!fieldArgClass.isPrimitive()){
					   if(Runtime.working_debug)
						   System.out.println("\t\t\t\t\t[Chicory.DaikonVariableInfo.getListElements()] Content of collection is not primitive." + fieldArgClass.getTypeName());
					   //Go over each object (e.g. in a list) and get its fields.
					   for (Iterator<?> iter = ((Iterable) listInstance).iterator(); iter.hasNext(); ) {
						   //TODO check if the objects in list are primitive values
						   Object element = iter.next();
						   element.getClass();
						   listOfElements.add(element);   
					   }
				   }
			   }
		   }
	   }
	   
	   if(accessChanged)
		   field.setAccessible(false);
	   
	   return listOfElements;
   }

  /** Determines if type implements list and prints associated decls, if necessary. */
  protected void checkForListDecl(Class<?> type, String theName, String offset) {
    if (isArray || type.isPrimitive() || type.isArray()) {
      return;
    }

    // System.out.printf ("checking %s %sto for list implementation = %b%n",
    //                    type, theName, implementsList (type));

    if (implementsList(type)) {
      @SuppressWarnings("unchecked")
      DaikonVariableInfo child =
          new ListInfo(offset + theName + "[]", (Class<? extends List<?>>) type);

      addChild(child);

      boolean ignore = child.check_for_dup_names();

      // CLASSNAME var
      if (!ignore) {
        DaikonVariableInfo childClass =
            new DaikonClassInfo(
                offset + theName + "[]" + class_suffix,
                classClassName + "[]",
                stringClassName + "[]",
                offset + theName + "[]",
                true);

        child.addChild(childClass);
      }
    }
  }

  /**
   * Checks the given type to see if it requires a .class addition to the decls file. If so, it adds
   * the correct child to this node.
   */
  protected void checkForRuntimeClass(Class<?> type, String theName, String offset) {
    if (!shouldAddRuntimeClass(type)) return;

    String postString = ""; //either array braces or an empty string

    if (theName.contains("[]") || offset.contains("[]")) postString = "[]";

    // add daikoninfo type
    DaikonVariableInfo classInfo =
        new DaikonClassInfo(
            offset + theName + class_suffix,
            classClassName + postString,
            stringClassName + postString,
            offset + theName,
            (offset + theName).contains("[]"));

    addChild(classInfo);
  }

  /**
   * Checks the given type to see if it is a string. If so, it adds the correct child to this node.
   */
  private void checkForString(Class<?> type, String theName, String offset) {
    if (!type.equals(String.class)) return;

    String postString = ""; //either array braces or an empty string
    if (isArray) postString = "[]";

    // add DaikonVariableInfo type
    DaikonVariableInfo stringInfo =
        new StringInfo(
            offset + theName + ".toString",
            stringClassName + postString,
            stringClassName + postString,
            offset + theName,
            isArray);

    addChild(stringInfo);
  }

  /**
   * Returns true iff type implements the List interface.
   *
   * @return true iff type implements the List interface
   */
  public static boolean implementsList(Class<?> type) {
    if (type.equals(java.util.List.class)) {
      return true;
    }

    // System.out.println(type);
    Class<?>[] interfaces = type.getInterfaces();
    for (Class<?> inter : interfaces) {
      // System.out.println("  implements: " + inter.getName());
      if (inter.equals(java.util.List.class)) return true;
    }
    return false;
  }

  /**
   * Explores the tree one level deeper (see {@link DaikonVariableInfo}). This method adds child
   * nodes to this node.
   *
   * <p>For example: "recurse" on a hashcode array object to print the actual array of values or
   * recurse on hashcode variable to print its fields. Also accounts for derived variables (.class,
   * .tostring) and "recurses" on arrays (that is, adds a variable to print out the arrays's
   * elements as opposed to just the hashcode of the array).
   *
   * @param theName the name of the variable currently being examined, such as "ballCount"
   * @param offset the representation of the variables we have previously examined. For examples,
   *     offset could be "this." in which case offset + name would be "this.ballCount.".
   */
  /*@RequiresNonNull("#1.clazz")*/
  protected void addChildNodes(
      ClassInfo cinfo, Class<?> type, String theName, String offset, int depthRemaining) {

    if(Runtime.working_debug)
        System.out.printf("\t\t\tenter >>>>> [Chicory.DaikonVariableInfo.addChildNodes()] name: %s, offset: %s type:%s%n",
            theName, offset, getTypeName());

    debug_vars.log("enter addChildNodes:%n");
    debug_vars.log("  name: %s, offset: %s%n", theName, offset);

    if (type.isPrimitive()) return;

    // Convert the internal reflection name for an outer class
    // 'this' field to the Java language format.
    if (theName.startsWith("this$")) {
      theName = type.getName() + ".this";
      offset = "";
    }

    if (type.isArray()) {
      // don't go into more than one dimension of a multi-dimensional array
      if (isArray) return;

      Class<?> eltType = type.getComponentType();
      assert eltType != null; // because type is an array
      if (eltType.isPrimitive()) {

        DaikonVariableInfo newChild = new ArrayInfo(offset + theName + "[]", eltType);

        newChild.check_for_dup_names();

        addChild(newChild);
      }
      // multi-dimensional arrays (not currently used)
      else if (eltType.isArray()) {
        DaikonVariableInfo newChild = new ArrayInfo(offset + theName + "[]", eltType);

        newChild.check_for_dup_names();

        addChild(newChild);

        debug_vars.indent("Array variable");
        newChild.addChildNodes(cinfo, eltType, "", offset + theName + "[]", depthRemaining);
        debug_vars.exdent();
      }
      // array is 1-dimensional and element type is a regular class
      else {
        DaikonVariableInfo newChild = new ArrayInfo(offset + theName + "[]", eltType);

        boolean ignore = newChild.check_for_dup_names();

        addChild(newChild);

        // Print out the class of each element in the array.  For
        // some reason dfej doesn't include this on returned arrays
        // or parameters.
        // The offset will only be equal to ""
        // if we are examining a local variable (parameter).
        if (!ignore) {
          if (!theName.equals("return") && !offset.equals("")) {
            newChild.checkForRuntimeClass(type, theName + "[]", offset);
          }

          newChild.checkForString(eltType, theName + "[]", offset);
        }
        newChild.addClassVars(cinfo, false, eltType, offset + theName + "[].", depthRemaining - 1);
      }
    }
    // regular old class type
    else {
      if(Runtime.working_debug)
      System.out.printf("\t\t\t**Depth Remaining = %d%n", depthRemaining);
      debug_vars.log("**Depth Remaining = %d%n", depthRemaining);

      if (depthRemaining <= 0) {
        // don't recurse any more!
        return;
      }
      if (!systemClass(type)) {
        addClassVars(cinfo, false, type, offset + theName + ".", depthRemaining - 1);
      }
    }
    debug_vars.log("exit addChildNodes%n");
    if(Runtime.working_debug)
        System.out.printf("\t\t\texit <<<<< [Chicory.DaikonVariableInfo.addChildNodes()]%n");
  }

  /**
   * Returns whether or not the fields of the specified class should be included, based on whether
   * the Class type is a system class or not. Right now, any system classes are excluded, but a
   * better way of determining this is probably necessary.
   */
  public static boolean systemClass(Class<?> type) {
    String class_name = type.getName();
    // System.out.printf ("type name is %s%n", class_name);
    return (class_name.startsWith("java.") || class_name.startsWith("javax."));
  }

  /**
   * Returns the declared type name of this variable. May include auxiliary information (represented
   * as a suffix starting with "#").
   *
   * @see #getTypeNameOnly()
   */
  public String getTypeName() {
    assert typeName != null : "Type name cannot be null";

    return typeName;
  }

  /**
   * Return the type name without aux information.
   *
   * @see #getTypeName()
   */
  @SuppressWarnings("signature") // substring
  public /*@BinaryName*/ String getTypeNameOnly() {
    return typeName.replaceFirst(" # .*", "");
  }

  /** Returns the representation type name of this variable. */
  public String getRepTypeName() {
    assert typeName != null : "Representation type name cannot be null";

    return repTypeName;
  }

  /** Return the rep type name without the constant value */
  public String getRepTypeNameOnly() {
    return repTypeName.replaceFirst(" = .*", "");
  }

  /**
   * Returns the constant value of the variable. If the variable is not static and final, or if the
   * constant value is not available in the class file, returns null.
   */
  public /*@Nullable*/ String get_const_val() {
    return const_val;
  }

  /**
   * Returns the function args of the variable. If the variable is not a function, or does not have
   * any arguments, returns null.
   */
  public /*@Nullable*/ String get_function_args() {
    return function_args;
  }

  /** Returns the comparability information for this variable. */
  public String getCompareString() {
    assert typeName != null : "Coparability info cannot be null";

    return compareInfoString;
  }

  /** Return true iff the DeclWriter should print this node. */
  public boolean declShouldPrint() {
    return declShouldPrint;
  }

  /** Return true iff the DTraceWriter should print this node. */
  public boolean dTraceShouldPrint() {
    return dtraceShouldPrint;
  }

  public boolean dTraceShouldPrintChildren() {
    return dtraceShouldPrintChildren;
  }

  /** Compares based on the name of the variable. */
  /*@Pure*/
  public int compareTo(/*>>>@GuardSatisfied DaikonVariableInfo this,*/ DaikonVariableInfo dv) {
    return name.compareTo(dv.name);
  }

  /** Returns whether or not this variable is an array */
  public boolean isArray() {
    return isArray;
  }

  /** Returns the direct child that is an array, null if one does not exist */
  public /*@Nullable*/ DaikonVariableInfo array_child() {
    for (DaikonVariableInfo dv : children) {
      if (dv.isArray()) return dv;
    }
    return null;
  }

  /** Returns whether or not this variable has a rep type of hashcode */
  public boolean isHashcode() {
    return getRepTypeName().equals("hashcode");
  }

  public boolean isHashcodeArray() {
    return getRepTypeName().equals("hashcode[]");
  }

  /** Returns whether or not the declared type of this variable is int */
  public boolean isInt() {
    String[] sarr = getTypeName().split("  *");
    return sarr[0].equals("int");
  }

  /** Returns the kind of the variable (array, field, function, etc) */
  public abstract VarKind get_var_kind();

  /**
   * Returns the name of this variable relative to its enclosing variable. For example the relative
   * name for 'this.a' is 'a'.
   */
  public /*@Nullable*/ String get_relative_name() {
    return null;
  }

  /** Empty set of variable flags */
  private static EnumSet<VarFlags> empty_var_flags = EnumSet.noneOf(VarFlags.class);

  /**
   * Returns the variable flags for this variable. Subclasses should call super() and or in any
   * flags that they add.
   */
  public EnumSet<VarFlags> get_var_flags() {
    return empty_var_flags;
  }

  /** Returns true iff the variable is static. Overridden by subclasses that can be static. */
  /*@Pure*/
  public boolean isStatic() {
    return false;
  }

  /**
   * If the variable name has been seen before (which can happen with statics and children of
   * statics, set the flags so that the variable is not considered for decl or dtrace and return
   * true. Otherwise, do nothing and return false.
   */
  private boolean check_for_dup_names() {

    if (ppt_statics.contains(name)) {
      debug_vars.log("ignoring already included variable %s [%s]", name, getClass());
      if (false && !isStatic()) {
        System.out.printf("ignoring already included variable %s [%s]", name, getClass());
      }
      declShouldPrint = false;
      dtraceShouldPrint = false;
      return true;
    } else { // new variable
      ppt_statics.add(name);
      return false;
    }
  }
}
