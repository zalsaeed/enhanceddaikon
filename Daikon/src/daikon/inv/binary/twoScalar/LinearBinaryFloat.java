// ***** This file is automatically generated from LinearBinary.java.jpp

package daikon.inv.binary.twoScalar;

import daikon.*;
import daikon.inv.*;
import daikon.derive.unary.SequenceLength;
import plume.*;
import java.util.*;
import java.util.logging.Level;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;
import typequals.*;
*/

/**
 * Represents a Linear invariant between two double
 * scalars <code>x</code> and <code>y</code>, of the form
 * <code>ax + by + c = 0</code>.
 * The constants <code>a</code>, <code>b</code> and <code>c</code> are
 * mutually relatively prime,
 * and the constant <code>a</code> is always positive.
 **/
public class LinearBinaryFloat
  extends TwoFloat
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030822L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff LinearBinary invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  public LinearBinaryCoreFloat core;

  @SuppressWarnings("nullness") // circular initialization
  protected LinearBinaryFloat(PptSlice ppt) {
    super(ppt);
    core = new LinearBinaryCoreFloat(this);
  }

  @SuppressWarnings("nullness") // circular initialization
  protected /*@Prototype*/ LinearBinaryFloat() {
    super();
    // Do we need core to be set for a prototype invariant?
    core = new LinearBinaryCoreFloat(this);
  }

  private static /*@Prototype*/ LinearBinaryFloat proto = new /*@Prototype*/ LinearBinaryFloat ();

  /** Returns a prototype LinearBinaryFloat invariant **/
  public static /*@Prototype*/ LinearBinaryFloat get_proto() {
    return (proto);
  }

  /** Returns whether or not this invariant is enabled **/
  public boolean enabled() {
    return dkconfig_enabled;
  }

  /** LinearBinary is only valid on integral types **/
  public boolean instantiate_ok (VarInfo[] vis) {

    if (!valid_types (vis))
      return (false);

    return (true);
  }

  /** Instantiate an invariant on the specified slice **/
  protected LinearBinaryFloat instantiate_dyn (/*>>> @Prototype LinearBinaryFloat this,*/ PptSlice slice) {
    return new LinearBinaryFloat(slice);
  }

  /*@SideEffectFree*/ public LinearBinaryFloat clone() {
    LinearBinaryFloat result = (LinearBinaryFloat) super.clone();
    result.core = core.clone();
    result.core.wrapper = result;
    return result;
  }

  protected Invariant resurrect_done_swapped() {
    core.swap();
    return this;
  }

  public String repr() {
    return "LinearBinaryFloat" + varNames() + ": "
      + "falsified=" + falsified
      + "; " + core.repr();
  }

  /*@SideEffectFree*/ public String format_using(OutputFormat format) {
    return core.format_using(format, var1().name_using(format),
                             var2().name_using(format));
  }

  /*@Pure*/ public boolean isActive() {
    return core.isActive();
  }

  public boolean mergeFormulasOk() {
    return (core.mergeFormulasOk());
  }

  /**
   * Merge the invariants in invs to form a new invariant.  Each must be
   * a LinearBinaryFloat invariant.  The work is done by the LinearBinary core
   *
   * @param invs        List of invariants to merge.  They should all be
   *                    permuted to match the variable order in parent_ppt.
   * @param parent_ppt  Slice that will contain the new invariant
   */
  public /*@Nullable*/ Invariant merge (List<Invariant> invs, PptSlice parent_ppt) {

    // Create a matching list of cores
    List<LinearBinaryCoreFloat> cores = new ArrayList<LinearBinaryCoreFloat>();
    for (Invariant inv : invs) {
      cores.add (((LinearBinaryFloat) inv).core);
    }

    // Merge the cores and build a new invariant containing the merged core
    LinearBinaryFloat result = new LinearBinaryFloat (parent_ppt);
    LinearBinaryCoreFloat newcore = core.merge (cores, result);
    if (newcore == null)
      return (null);
    result.core = newcore;
    return (result);
  }

  public InvariantStatus check_modified(double x, double y, int count) {
    return clone().add_modified(x, y, count);
  }

  public InvariantStatus add_modified(double x, double y, int count) {
    return core.add_modified(x, y, count);
  }

  public boolean enoughSamples() {
    return core.enoughSamples();
  }

  protected double computeConfidence() {
    return core.computeConfidence();
  }

  /*@Pure*/ public boolean isExact() {
    return true;
  }

  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousStatically(VarInfo[] vis) {
    // Obvious derived
    VarInfo var1 = vis[0];
    VarInfo var2 = vis[1];
    // avoid comparing "size(a)" to "size(a)-1"; yields "size(a)-1 = size(a) - 1"
    if (var1.isDerived() && (var1.derived instanceof SequenceLength)
        && var2.isDerived() && (var2.derived instanceof SequenceLength)) {
      @SuppressWarnings("nullness") // checker bug: flow
      /*@NonNull*/ SequenceLength sl1 = (SequenceLength) var1.derived;
      @SuppressWarnings("nullness") // checker bug: flow
      /*@NonNull*/ SequenceLength sl2 = (SequenceLength) var2.derived;
      if (sl1.base == sl2.base) {
        String discardString = var1.name()+" and "+var2.name()+" derived from "+
          "same sequence: "+sl1.base.name();
        return new DiscardInfo(this, DiscardCode.obvious, discardString);
      }
    }
    // avoid comparing "size(a)-1" to anything; should compare "size(a)" instead
    if (var1.isDerived() && (var1.derived instanceof SequenceLength)
        && ((SequenceLength) var1.derived).shift != 0) {
      String discardString = "Variables of the form 'size(a)-1' are not compared since 'size(a)' "+
        "will be compared";
      return new DiscardInfo(this, DiscardCode.obvious, discardString);
    }
    if (var2.isDerived() && (var2.derived instanceof SequenceLength)
        && ((SequenceLength) var2.derived).shift != 0) {
      String discardString = "Variables of the form 'size(a)-1' are not compared since 'size(a)' "+
        "will be compared";
      return new DiscardInfo(this, DiscardCode.obvious, discardString);
    }

    return super.isObviousStatically(vis);
  }

  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousDynamically(VarInfo[] vis) {
    DiscardInfo super_result = super.isObviousDynamically(vis);
    if (super_result != null) {
      return super_result;
    }

    if (core.a == 0) {
      return new DiscardInfo(this, DiscardCode.obvious, var2().name() + " is constant");
    }
    if (core.b == 0) {
      return new DiscardInfo(this, DiscardCode.obvious, var1().name() + " is constant");
    }
//    if (core.a == 1 && core.b == 0) {
//      return new DiscardInfo(this, DiscardCode.obvious, "Variables are equal");
//    }
    if (core.a == -core.b && core.c == 0) {
     return new DiscardInfo(this, DiscardCode.obvious, "Variables are equal");
    }
    return null;
  }

  /*@Pure*/ public boolean isSameFormula(Invariant other) {
    return core.isSameFormula(((LinearBinaryFloat) other).core);
  }

  /*@Pure*/ public boolean isExclusiveFormula(Invariant other) {
    if (other instanceof LinearBinaryFloat) {
      return core.isExclusiveFormula(((LinearBinaryFloat) other).core);
    }
    return false;
  }

  // Look up a previously instantiated invariant.
  public static /*@Nullable*/ LinearBinaryFloat find(PptSlice ppt) {
    assert ppt.arity() == 2;
    for (Invariant inv : ppt.invs) {
      if (inv instanceof LinearBinaryFloat)
        return (LinearBinaryFloat) inv;
    }
    return null;
  }

  // Returns a vector of LinearBinary objects.
  // This ought to produce an iterator instead.
  public static Vector<LinearBinaryFloat> findAll(VarInfo vi) {
    Vector<LinearBinaryFloat> result = new Vector<LinearBinaryFloat>();
    for (PptSlice view : vi.ppt.views_iterable()) {
      if ((view.arity() == 2) && view.usesVar(vi)) {
        LinearBinaryFloat lb = LinearBinaryFloat.find(view);
        if (lb != null) {
          result.add(lb);
        }
      }
    }
    return result;
  }

}
