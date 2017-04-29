// ***** This file is automatically generated from EltNonZero.java.jpp

package daikon.inv.unary.sequence;

import daikon.*;
import daikon.inv.*;
import daikon.inv.unary.scalar.*;
import daikon.inv.binary.twoSequence.*;
import daikon.inv.ValueSet;
import daikon.suppress.*;

import plume.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.*;

/*>>>
import org.checkerframework.checker.interning.qual.*;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;
import typequals.*;
*/

/**
 * Represents the invariant "x != 0" where x represents all of the elements
 * of a sequence of double.  Prints as <code>x[] elements != 0</code>.
 */

public final class EltNonZeroFloat
  extends SingleFloatSequence
{
  /**
   * Debug tracer.
   */
  public static final Logger debug =
    Logger.getLogger("daikon.inv.unary.sequence.EltNonZeroFloat");

  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030822L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff EltNonZero invariants should be considered.
   */
  public static boolean dkconfig_enabled = Invariant.invariantEnabledDefault;

  public EltNonZeroFloat(PptSlice ppt) {
    super(ppt);
  }

  public /*@Prototype*/ EltNonZeroFloat() {
    super();
  }

  private static /*@Prototype*/ EltNonZeroFloat proto = new /*@Prototype*/ EltNonZeroFloat ();

  /** Returns the prototype invariant for EltNonZeroFloat */
  public static /*@Prototype*/ EltNonZeroFloat get_proto() {
    return proto;
  }

  /** returns whether or not this invariant is enabled */
  public boolean enabled() {
    return dkconfig_enabled;
  }

  /** instantiate an invariant on the specified slice */
  protected EltNonZeroFloat instantiate_dyn (/*>>> @Prototype EltNonZeroFloat this,*/ PptSlice slice) {
    return new EltNonZeroFloat (slice);
  }

  /**
   * Returns whether or not the variable is a pointer.
   */
  /*@Pure*/
  private boolean is_pointer(/*>>>@GuardSatisfied EltNonZeroFloat this*/) {
    return (!ppt.var_infos[0].type.baseIsFloat());
  }

  public String repr(/*>>>@GuardSatisfied EltNonZeroFloat this*/) {
    return "EltNonZeroFloat" + varNames() + ": "
      + !falsified;
  }

  /*@SideEffectFree*/
  public String format_using(/*>>>@GuardSatisfied EltNonZeroFloat this,*/ OutputFormat format) {
    if (format.isJavaFamily()) return format_java_family(format);

    if (format == OutputFormat.DAIKON) return format_daikon();
    if (format == OutputFormat.ESCJAVA) return format_esc();
    if (format == OutputFormat.CSHARPCONTRACT) return format_csharp_contract();
    if (format == OutputFormat.SIMPLIFY) return format_simplify();

    return format_unimplemented(format);
  }

  public String format_daikon(/*>>>@GuardSatisfied EltNonZeroFloat this*/) {
    return var().name() + " elements != "
           + (is_pointer() ? "null" : "0");
  }

  // We are a special case where a ghost field can actually talk about
  // array contents.
  /*@Pure*/
  public boolean isValidEscExpression() {
    return true;
  }

  public String format_esc(/*>>>@GuardSatisfied EltNonZeroFloat this*/) {
    // If this is an entire array or Collection (not a slice), then
    //  * for arrays: use \nonnullelements(A)
    //  * for Collections: use collection.containsNull == false
    //    (the latter also requires that ghost field to get set)

    // if (var().isDerivedSubSequenceOf() == null) {
    if (var().is_direct_non_slice_array()) {
      VarInfo term = var().get_enclosing_var();
      String esc_name = null;
      if (term == null) {
        // Only happenes in internal format tests
        esc_name = var().name().replace ("[]", "");
      } else {
        // System.out.printf ("term = %s, var = %s%n", term.name, var().name);
        esc_name = term.esc_name();
      }
      if (var().type.isArray()) {
        return "\\nonnullelements(" + esc_name + ")";
      } else {
        return esc_name + ".containsNull == false";
      }
    }

    // If this is just part of an array or Collection (var is a
    // slice), then calling viname.esc_name() will always throw an
    // exception, since var() is certainly a sequence.  So use the
    // standard quantification.

    String[] form = VarInfo.esc_quantify (var());
    return form[0] + "(" + form[1] + " != "
           + (is_pointer() ? "null" : "0") + ")" + form[2];
  }

  public String format_java_family(/*>>>@GuardSatisfied EltNonZeroFloat this,*/ OutputFormat format) {
    String retval =
      "daikon.Quant.eltsNotEqual(" + var().name_using(format)
      + (is_pointer() ? ", null" : ", 0") + ")";

    return retval;
  }

  public String format_csharp_contract(/*>>>@GuardSatisfied EltNonZeroFloat this*/) {
    String[] split = var().csharp_array_split();
    return "Contract.ForAll(" + split[0] + ", x => x" + split[1] + " != " + (is_pointer() ? "null" : "0") + ")";
  }

  public String format_simplify(/*>>>@GuardSatisfied EltNonZeroFloat this*/) {
    String[] form = VarInfo.simplify_quantify (var());
    return form[0] + "(NEQ " + form[1] + " "
      + (is_pointer() ? "null" : "0") + ")" + form[2];
  }

  public InvariantStatus check_modified(double /*@Interned*/ [] a, int count) {
    for (int ai=0; ai<a.length; ai++) {
      double v = a[ai];

      if ((Global.fuzzy.eq (v, 0))) {
        return InvariantStatus.FALSIFIED;
      }
    }
    return InvariantStatus.NO_CHANGE;
  }

  public InvariantStatus add_modified(double /*@Interned*/ [] a, int count) {

    //if (logOn()) {
    //  ValueSet.ValueSetFloatArray vs = (ValueSet.ValueSetFloatArray) ppt.var_infos[0].get_value_set();
    //  log ("max=" + vs.max() + " array=" + ArraysMDE.toString (a));
    //}

    return (check_modified (a, count));
  }

  protected double computeConfidence() {
    // Maybe just use 0 as the min or max instead, and see what happens:
    // see whether the "nonzero" invariant holds anyway.  (Perhaps only
    // makes sense to do if the {Lower,Upper}Bound invariant doesn't imply
    // the non-zeroness.)  In that case, do still check for no values yet
    // received.
    ValueSet.ValueSetFloatArray vs = (ValueSet.ValueSetFloatArray) ppt.var_infos[0].get_value_set();
    // log ("is_pointer()=" + is_pointer() + " vs.min=" + vs.min()
    //       + " vs.max=" + vs.max());
    if (!is_pointer() && ((vs.min() > 0) || (vs.max() < 0))) {
      return Invariant.CONFIDENCE_UNJUSTIFIED;
    } else {
      double probability_one_elt_nonzero = 1 - confidence_one_elt_nonzero();
      double result
        = 1 - Math.pow(probability_one_elt_nonzero, ppt.num_samples());
      // if ((result < 0) || (result > 1))
      //  System.err.println ("bad result: vs.max=" + vs.max() + " vs.min="
      //                      + vs.min() + " conf="
      //                      + confidence_one_elt_nonzero() + " range="
      //                      + (vs.max() - vs.min() + 1)/ 1);
      return result;
    }
  }

  private double confidence_one_elt_nonzero() {
    double range;
    if (is_pointer()) {
      range = 3;
    } else {
      int modulus = 1;

      // I need to come back and make this work.
      // {
      //   for (Invariant inv : ppt.invs) {
      //     if ((inv instanceof Modulus) && inv.enoughSamples()) {
      //       modulus = ((Modulus) inv).modulus;
      //       break;
      //     }
      //   }
      // }

      // Perhaps I ought to check that it's possible (given the modulus
      // constraints) for the value to be zero; otherwise, the modulus
      // constraint implies non-zero.
      ValueSet.ValueSetFloatArray vs = (ValueSet.ValueSetFloatArray) ppt.var_infos[0].get_value_set();
      range = ((vs.max()) - vs.min() + 1) / modulus;
    }
    return 1.0/range;
  }

  /*@Pure*/
  public boolean isSameFormula(Invariant other) {
    assert other instanceof EltNonZeroFloat;
    return true;
  }

  /*@Pure*/
  public boolean isExclusiveFormula(Invariant other) {
    if (other instanceof EltOneOfFloat) {
      EltOneOfFloat eoo = (EltOneOfFloat) other;
      if ((eoo.num_elts() == 1) && (((Double)eoo.elt()).doubleValue() == 0)) {
        return true;
      }
    }
    return false;
  }

  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousStatically(VarInfo[] vis) {
    // This test doesn't seem right: the invariant is obvious if the
    // elements don't have null, not if the collection doesn't have null.
    if (!vis[0].aux.hasNull()) {
      // If it's not a number and null doesn't have special meaning...
      return new DiscardInfo(this, DiscardCode.obvious, "'null' has no special meaning for " + vis[0].name());
    }
    return super.isObviousStatically (vis);
  }

  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousDynamically(VarInfo[] vis) {
    DiscardInfo super_result = super.isObviousDynamically(vis);
    if (super_result != null) {
      return super_result;
    }

    VarInfo v1 = vis[0];

    // (a[] == []) ==> a[] != 0
    if (ppt.parent.is_empty (v1)) {
      return new DiscardInfo (this, DiscardCode.obvious, v1 + "is empty");
    }

    // (a[] > 0) v (a[] < 0) ==> a[] != 0
    EltLowerBoundFloat lb = (EltLowerBoundFloat) ppt.parent.find_inv_by_class
                                                    (vis, EltLowerBoundFloat.class);
    if ((lb != null) && (lb.min() > 0)) {
      return new DiscardInfo (this, DiscardCode.obvious, v1 + " > " + lb.min());
    }
    EltUpperBoundFloat ub = (EltUpperBoundFloat) ppt.parent.find_inv_by_class
                                                    (vis, EltUpperBoundFloat.class);
    if ((ub != null) && (ub.max() < 0)) {
      return new DiscardInfo (this, DiscardCode.obvious, v1 + " < " + ub.max());
    }

    // For every other EltNonZero at this program point, see if there is a
    // subsequence relationship between that array and this one.

    if (debug.isLoggable(Level.FINE)) {
      debug.fine ("Testing isObviousDynamically for " + vis[0].name());
    }

    PptTopLevel parent = ppt.parent;
    for (Iterator<Invariant> itor = parent.invariants_iterator(); itor.hasNext(); ) {
      Invariant inv = itor.next();
      if ((inv instanceof EltNonZeroFloat) && (inv != this) && inv.enoughSamples()) {
        VarInfo v2 = inv.ppt.var_infos[0];
        if (debug.isLoggable(Level.FINE)) {
          debug.fine ("  Have to test: " + inv.repr());
        }

        if (Debug.logOn()) {
          Daikon.current_inv = this;
        }
        if (parent.is_subsequence (v1, v2)) {
          return new DiscardInfo (this, DiscardCode.obvious, v1.name() +
                                  " is a subsequence of " + v2.name());
        }
      }
    }

    return null;
  }

  // Look up a previously instantiated invariant.
  public static /*@Nullable*/ EltNonZeroFloat find(PptSlice ppt) {
    assert ppt.arity() == 1;
    for (Invariant inv : ppt.invs) {
      if (inv instanceof EltNonZeroFloat) {
        return (EltNonZeroFloat) inv;
      }
    }
    return null;
  }

  /** NI suppressions, initialized in get_ni_suppressions() */
  private static /*@Nullable*/ NISuppressionSet suppressions = null;

  /** returns the ni-suppressions for EltNonZeroFloat */
  /*@Pure*/
  public /*@NonNull*/ NISuppressionSet get_ni_suppressions() {
    if (suppressions == null) {
      NISuppressee suppressee = new NISuppressee (EltNonZeroFloat.class, 1);

      // suppressor definitions (used in suppressions below)
      NISuppressor v_eq_one = new NISuppressor (0, EltRangeFloat.EqualOne.class);
      NISuppressor v_eq_minus_one = new NISuppressor (0, EltRangeFloat.EqualMinusOne.class);

      suppressions = new NISuppressionSet (new NISuppression[] {
          // v == 1 ==> v != 0
          new NISuppression (v_eq_one, suppressee),
          // v == -1 ==> v != 0
          new NISuppression (v_eq_minus_one, suppressee),
        });
    }

    return suppressions;
  }

}
