// ***** This file is automatically generated from Bound.java.jpp

package daikon.inv.unary.scalar;

import daikon.*;
import daikon.inv.*;

  import daikon.inv.unary.sequence.*;
  import daikon.inv.binary.sequenceScalar.*;

import daikon.inv.unary.*;
import daikon.derive.unary.*;
import plume.*;

import java.util.*;

/*>>>
import org.checkerframework.checker.interning.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;
import org.checkerframework.framework.qual.*;
import typequals.*;
*/

  /**
   * Represents the invariant <tt>x &lt;= c</tt>, where <code>c</code>
   * is a constant and <code>x</code> is a double scalar.
   **/

// One reason not to combine LowerBound and UpperBound into a single range
// invariant is that they have separate justifications:  one may be
// justified when the other is not.
public class UpperBoundFloat
  extends SingleFloat
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030822L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff UpperBoundFloat invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;
  /**
   * Long integer.  Together with the corresponding
   * <code>maximal_interesting</code> parameter, specifies the
   * range of the computed constant that is ``interesting'' --- the range
   * that should be reported.  For instance, setting
   * <code>minimal_interesting</code>
   * to -1 and <code>maximal_interesting</code>
   * to 2 would only permit output of
   * UpperBoundFloat invariants whose cutoff was one of (-1,0,1,2).
   **/
  public static long dkconfig_minimal_interesting = -1;
  /**
   * Long integer.  Together with the corresponding
   * <code>minimal_interesting</code> parameter, specifies the
   * range of the computed constant that is ``interesting'' --- the range
   * that should be reported.  For instance, setting
   * <code>minimal_interesting</code>
   * to -1 and <code>maximal_interesting</code>
   * to 2 would only permit output of
   * UpperBoundFloat invariants whose cutoff was one of (-1,0,1,2).
   **/
  public static long dkconfig_maximal_interesting = 2;

  /*@Unused(when=Prototype.class)*/
  private UpperBoundCoreFloat core;

  @SuppressWarnings("nullness") // circular initialization
  private UpperBoundFloat(PptSlice slice) {
    super (slice);
    assert slice != null;
    core = new UpperBoundCoreFloat(this);
  }

  private /*@Prototype*/ UpperBoundFloat() {
    super ();
    // do we need a core?
  }

  private static /*@Prototype*/ UpperBoundFloat proto = new /*@Prototype*/ UpperBoundFloat ();

  /** Returns the prototype invariant for UpperBoundFloat **/
  public static /*@Prototype*/ UpperBoundFloat get_proto() {
    return (proto);
  }

  /** returns whether or not this invariant is enabled **/
  public boolean enabled() {
    return dkconfig_enabled;
  }

  /** UpperBoundFloat is only valid on integral types **/
  public boolean instantiate_ok (VarInfo[] vis) {

    if (!valid_types (vis))
      return (false);

    return (vis[0].file_rep_type.baseIsFloat());
    }

  /** instantiate an invariant on the specified slice **/
  public UpperBoundFloat instantiate_dyn (/*>>> @Prototype UpperBoundFloat this,*/ PptSlice slice) {
    return new UpperBoundFloat (slice);
  }

  /*@SideEffectFree*/ public UpperBoundFloat clone() {
    UpperBoundFloat result = (UpperBoundFloat) super.clone();
    result.core = core.clone();
    result.core.wrapper = result;
    return result;
  }

  public double max() {
    return core.max();          // i.e., core.max1
  }

  public String repr() {
    return "UpperBoundFloat" + varNames() + ": "
      + core.repr();
  }

  /*@SideEffectFree*/ public String format_using(OutputFormat format) {
    String name = var().name_using(format);
    PptTopLevel pptt = ppt.parent;

    if ((format == OutputFormat.DAIKON)
        || (format == OutputFormat.ESCJAVA)
        || (format.isJavaFamily())
        || (format == OutputFormat.CSHARPCONTRACT))
    {

      if (PrintInvariants.dkconfig_static_const_infer) {
        for (VarInfo vi : pptt.var_infos) {
          // Check is static constant, and variables are comparable
          if (vi.isStaticConstant() && VarComparability.comparable(vi, var())) {
            // If variable is a double, then use fuzzy comparison
            if (vi.rep_type == ProglangType.DOUBLE) {
              Double constantVal = (Double)vi.constantValue();
              if (Global.fuzzy.eq(constantVal, core.max1) || (Double.isNaN(constantVal) && Double.isNaN(core.max1)))
                return name + " <= " + vi.name();
            }
            // Otherwise just use the equals method
            else {
              Object constantVal = vi.constantValue();
              if (constantVal.equals(core.max1)) {
                return name + " <= " + vi.name();
              }
            }
          }
        }
      }

      return name + " <= " + core.max1;
    }

    if (format == OutputFormat.SIMPLIFY) {

      return "(<= " + name + " " + simplify_format_double(core.max1) + ")";
    }

    return format_unimplemented(format);
  }

  public InvariantStatus add_modified(double value, int count) {
    // System.out.println("UpperBoundFloat" + varNames() + ": "
    //              + "add(" + value + ", " + modified + ", " + count + ")");

    return core.add_modified(value, count);

  }

  public InvariantStatus check_modified(double value, int count) {

    return core.check(value);

  }

  public boolean enoughSamples() {
    return core.enoughSamples();
  }

  protected double computeConfidence() {
    return core.computeConfidence();
  }

  /*@Pure*/ public boolean isExact() {
    return core.isExact();
  }

  /*@Pure*/ public boolean isSameFormula(Invariant other) {
    return core.isSameFormula(((UpperBoundFloat) other).core);
  }

  // XXX FIXME This looks like a hack that should be removed.  -MDE 6/13/2002
  // Use hasUninterestingConstant() instead. -SMcC 2/26/2003
  /*@Pure*/ public boolean isInteresting() {
    return (-1 < core.max1 && core.max1 < 2);
  }

  public boolean hasUninterestingConstant() {
    // If the constant bound is not in some small range of interesting
    // values (by default {-1, 0, 1, 2}), call it uninteresting.
    if ((core.max1 < dkconfig_minimal_interesting) ||
        (core.max1 > dkconfig_maximal_interesting)) {
      return true;
    }

    else if (core.max1 != (int)core.max1) {
      // Non-integer bounds are uninteresting even if small.
      return true;
    }

    return false;
  }

  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousStatically (VarInfo[] vis) {
    VarInfo var = vis[0];
    if ((var.derived instanceof SequenceLength)
         && (((SequenceLength) var.derived).shift != 0)) {
      return new DiscardInfo(this, DiscardCode.obvious, "Bounds are preferrable over"
                             + " sequence lengths with no shift");
    }

    if (var.aux.hasValue(VarInfoAux.MAXIMUM_VALUE)) {
      @SuppressWarnings("keyfor")   // needs EnsuresQualifier
      int minVal = var.aux.getInt(VarInfoAux.MAXIMUM_VALUE);
      if (minVal == core.max1) {
        return new DiscardInfo(this, DiscardCode.obvious,
          var.name() + " GTE " + core.max1 + " is already known");
      }
    }

    return super.isObviousStatically (vis);
  }

  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousDynamically(VarInfo[] vis) {
    DiscardInfo super_result = super.isObviousDynamically(vis);
    if (super_result != null) {
      return super_result;
    }

    PptTopLevel pptt = ppt.parent;

    // This check always lets invariants pass through (even if it is not within
    // the default range of (-1 to 2) if it matches a static constant
    // As noted below, this check really doesn't belong here, but should be
    // moved to hasUninterestingConstant() whenever that is implemented
    if (PrintInvariants.dkconfig_static_const_infer) {
      if (core.matchConstant()) {
        return null;
      }
    }

    // if the value is not in some range (like -1,0,1,2) then say that it is obvious
    if ((core.max1 < dkconfig_minimal_interesting) ||
        (core.max1 > dkconfig_maximal_interesting)) {
      // XXX This check doesn't really belong here. However It
      // shouldn't get removed until hasUninterestingConstant() is
      // suitable to be turned on everywhere by default. -SMcC
      // if the value is not in some range (like -1,0,1,2) then say that
      // it is obvious
      String discardString = "";
      if (core.max1 < dkconfig_minimal_interesting) {
        discardString = "MIN1="+core.max1+" is less than dkconfig_minimal_interesting=="
          + dkconfig_minimal_interesting;
      } else {
        discardString = "MIN1="+core.max1+" is greater than dkconfig_maximal_interesting=="+
          dkconfig_maximal_interesting;
      }
      return new DiscardInfo(this, DiscardCode.obvious, discardString);
    }
    OneOfFloat oo = OneOfFloat.find(ppt);
    if ((oo != null) && oo.enoughSamples() && oo.num_elts() > 0) {
      assert oo.var().isCanonical();
      // We could also use core.max1 == oo.max_elt(), since the LowerBound
      // will never have a core.max1 that does not appear in the OneOf.
      if (core.max1 >= oo.max_elt()) {
        String varName = vis[0].name();
        String discardString = varName+">="+core.max1+" is implied by "+varName+">="+oo.max_elt();
        log ("%s", discardString);
        return new DiscardInfo(this, DiscardCode.obvious, discardString);
      }
    }

    // NOT: "VarInfo v = var();" because we want to operate not on this
    // object's own variables, but on the variables that were passed in.
    VarInfo v = vis[0];

    // For each sequence variable, if this is an obvious member/subsequence, and
    // it has the same invariant, then this one is obvious.
    for (int i=0; i<pptt.var_infos.length; i++) {
      VarInfo vi = pptt.var_infos[i];

      if (MemberFloat.isObviousMember(v, vi))
      {
        PptSlice1 other_slice = pptt.findSlice(vi);
        if (other_slice != null) {
          EltUpperBoundFloat eb = EltUpperBoundFloat.find(other_slice);
          if ((eb != null)
              && eb.enoughSamples()
              && eb.max() == max()) {
            String otherName = other_slice.var_infos[0].name();
            String varName = v.name();
            String discardString = varName+" is a subsequence of "+otherName+" for which the invariant holds.";
            log ("%s", discardString);
            return new DiscardInfo(this, DiscardCode.obvious, discardString);
          }
        }
      }
    }

    return null;
  }

  /*@Pure*/ public boolean isExclusiveFormula(Invariant other) {

    if (other instanceof LowerBoundFloat) {
      if (max() < ((LowerBoundFloat) other).min())
        return true;
    }

    if (other instanceof OneOfFloat) {
      return other.isExclusiveFormula(this);
    }
    return false;
  }

  // Look up a previously instantiated invariant.
  public static /*@Nullable*/ UpperBoundFloat find(PptSlice ppt) {
    assert ppt.arity() == 1;
    for (Invariant inv : ppt.invs) {
      if (inv instanceof UpperBoundFloat)
        return (UpperBoundFloat) inv;
    }
    return null;
  }

  /**
   * Bound can merge different formulas from lower points to create a single
   * formula at an upper point.  See merge() below.
   */
  public boolean mergeFormulasOk() {
    return (true);
  }

  /**
   * Merge the invariants in invs to form a new invariant.  Each must be
   * a UpperBoundFloat invariant.  This code finds all of the min/max values
   * in each invariant, applies them to a new parent invariant and
   * returns the merged invariant (if any).
   *
   * @param invs        List of invariants to merge.  The invariants must all
   *                    be of the same type and should come from the
   *                    children of parent_ppt.  They should also all
   *                    be permuted to match the variable order in
   *                    parent_ppt.
   * @param parent_ppt  Slice that will contain the new invariant
   */
  public /*@Nullable*/ Invariant merge (List<Invariant> invs, PptSlice parent_ppt) {

    // Create the initial parent invariant from the first child
    UpperBoundFloat first = (UpperBoundFloat) invs.get(0);
    UpperBoundFloat result= first.clone();
    result.ppt = parent_ppt;

    // Loop through the rest of the child invariants
    for (int i = 1; i < invs.size(); i++ ) {
      UpperBoundFloat lb = (UpperBoundFloat) invs.get (i);
      result.core.add(lb.core);
    }

    result.log ("Merged '%s' from %s child invariants", result.format(),invs.size());
    return (result);
  }
}
