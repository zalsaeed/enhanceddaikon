// ***** This file is automatically generated from ThreeScalar.java.jpp
package daikon.inv.ternary.threeScalar;

import daikon.*;
import daikon.inv.*;
import daikon.inv.ternary.TernaryInvariant;

import plume.*;

/*>>>
import org.checkerframework.checker.interning.qual.*;
import typequals.*;
*/

/**
 * Abstract base class used for comparing three long scalars.
 **/
public abstract class ThreeScalar
  extends TernaryInvariant
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  protected ThreeScalar(PptSlice ppt) {
    super(ppt);
  }

  protected /*@Prototype*/ ThreeScalar() {
    super();
  }

  /** Returns whether or not the specified types are valid **/
  public final boolean valid_types (VarInfo[] vis) {

    return ((vis.length == 3)
            && vis[0].file_rep_type.isScalar()
            && vis[1].file_rep_type.isScalar()
            && vis[2].file_rep_type.isScalar());
  }

  public VarInfo var1() {
    return ppt.var_infos[0];
  }

  public VarInfo var2() {
    return ppt.var_infos[1];
  }

  public VarInfo var3() {
    return ppt.var_infos[2];
  }

  public InvariantStatus check(/*@Interned*/ Object val1, /*@Interned*/ Object val2, /*@Interned*/ Object val3, int mod_index, int count) {
    // Tests for whether a value is missing should be performed before
    // making this call, so as to reduce overall work.
    assert ! falsified;
    if ((mod_index < 0) || (mod_index > 8))
      assert (mod_index >= 0) && (mod_index < 8)
        : "var 1 " + ppt.var_infos[0].name() + " value = "
         + val1 + "mod_index = " +  mod_index;
    long v1 = ((Long) val1).longValue();
    long v2 = ((Long) val2).longValue();
    if (!(val3 instanceof Long))
      System.out.println ("val3 should be PRIMITIVE, but is " + val3.getClass());
    long v3 = ((Long) val3).longValue();
    if (mod_index == 0) {
      return check_unmodified(v1, v2, v3, count);
    } else {
      return check_modified(v1, v2, v3, count);
    }
  }

  public InvariantStatus add(/*@Interned*/ Object val1, /*@Interned*/ Object val2, /*@Interned*/ Object val3, int mod_index, int count) {
    // Tests for whether a value is missing should be performed before
    // making this call, so as to reduce overall work.
    assert ! falsified;
    if ((mod_index < 0) || (mod_index > 8))
      assert (mod_index >= 0) && (mod_index < 8)
        : "var 1 " + ppt.var_infos[0].name() + " value = "
         + val1 + "mod_index = " +  mod_index + " line "
         + FileIO.get_linenum();
    long v1 = ((Long) val1).longValue();
    long v2 = ((Long) val2).longValue();
    if (!(val3 instanceof Long)) {
      System.out.printf ("val3 should be PRIMITIVE, but is %s=%s, v2 is %s=%s%n",
              val3.getClass().getName(), Debug.toString(val3),
              val2.getClass().getName(), Debug.toString(val2));
      System.out.println ("our class = " + this.getClass().getName());
      System.out.println ("our slice = " + this.ppt);
      PptSlice slice = this.ppt;
      System.out.printf ("var3 reptype = %s%n", slice.var_infos[2].rep_type);
      assert (slice.var_infos[0].rep_type == ProglangType.INT)
                  && (slice.var_infos[1].rep_type == ProglangType.INT)
                  && (slice.var_infos[2].rep_type == ProglangType.INT);
    }
    long v3 = ((Long) val3).longValue();
    if (mod_index == 0) {
      return add_unmodified(v1, v2, v3, count);
    } else {
      return add_modified(v1, v2, v3, count);
    }
  }

  public abstract InvariantStatus check_modified(long v1, long v2, long v3, int count);

  public InvariantStatus check_unmodified(long v1, long v2, long v3, int count) {
    return InvariantStatus.NO_CHANGE;
  }

  /**
   * This method need not check for falsified;
   * that is done by the caller.
   **/
  public abstract InvariantStatus add_modified(long v1, long v2, long v3, int count);

  /**
   * By default, do nothing if the value hasn't been seen yet.
   * Subclasses can override this.
   **/
  public InvariantStatus add_unmodified(long v1, long v2, long v3, int count) {
    return InvariantStatus.NO_CHANGE;
  }

}
