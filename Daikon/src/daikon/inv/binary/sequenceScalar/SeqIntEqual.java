// ***** This file is automatically generated from SeqIntComparison.java.jpp

package daikon.inv.binary.sequenceScalar;

import daikon.*;
import daikon.derive.unary.*;
import daikon.inv.*;
import daikon.inv.unary.sequence.*;
import daikon.inv.binary.twoSequence.*;
import daikon.suppress.*;

import plume.*;

import java.util.*;
import java.util.logging.Logger;

/*>>>
import org.checkerframework.checker.interning.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;
import typequals.*;
*/

/**
 * Represents an invariant between a long scalar and a
 * a sequence of long values.
 * Prints as <code>x[] elements == y</code> where <code>x</code> is a
 * long sequence and <code>y</code> is a long scalar.
 **/
public final class SeqIntEqual
  extends SequenceScalar
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030822L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff SeqIntEqual invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  public static final Logger debug
    = Logger.getLogger("daikon.inv.binary.sequenceScalar.SeqIntEqual");

  static boolean debugSeqIntComparison = false;

  protected SeqIntEqual(PptSlice ppt) {
    super(ppt);
  }

  protected /*@Prototype*/ SeqIntEqual() {
    super();
  }

  private static /*@Prototype*/ SeqIntEqual proto = new /*@Prototype*/ SeqIntEqual ();

  /** Returns the prototype invariant for SeqIntEqual **/
  public static /*@Prototype*/ SeqIntEqual get_proto () {
    return (proto);
  }

  /** Returns whether or not this invariant is enabled **/
  public boolean enabled() {
    return dkconfig_enabled;
  }

  /** Non-equal SeqIntComparison is only valid on integral types **/
  public boolean instantiate_ok (VarInfo[] vis) {

    if (!valid_types (vis))
      return (false);

    VarInfo seqvar;
    VarInfo sclvar;
    if (vis[0].rep_type == ProglangType.INT_ARRAY) {
      seqvar = vis[0];
      sclvar = vis[1];
    } else {
      seqvar = vis[1];
      sclvar = vis[0];
    }

    assert sclvar.rep_type == ProglangType.INT;
    assert seqvar.rep_type == ProglangType.INT_ARRAY;

    return (true);
  }

  /** instantiates the invariant on the specified slice **/
  protected SeqIntEqual instantiate_dyn (/*>>> @Prototype SeqIntEqual this,*/ PptSlice slice) {
    return new SeqIntEqual (slice);
  }

  /**
   * Checks to see if the comparison is obvious statically.  Makes the
   * following checks:
   * <pre>
   *    max(A[]) op A[]
   *    min(A[]) op A[]
   * </pre>
   *
   * JHP: Note that these are not strict implications, these are merely
   * uninteresting comparisons (except when op is GreaterEqual for max
   * and LessEqual for min)
   */
  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousStatically(VarInfo[] vis) {

    SequenceMin seqmin = null;
    SequenceMax seqmax = null;
    VarInfo sclseq = null;
    VarInfo sclvar = sclvar(vis);
    if (sclvar.derived instanceof SequenceMin) {
      seqmin = (SequenceMin) sclvar.derived;
      sclseq = seqmin.base;
    } else if (sclvar.derived instanceof SequenceMax) {
      seqmax = (SequenceMax) sclvar.derived;
      sclseq = seqmax.base;
    }
    if (seqvar(vis) == sclseq) {
      return new DiscardInfo (this, DiscardCode.obvious,
                              sclvar(vis).name() + " is min/max ");
    }
    return (null);
  }

  /*@SideEffectFree*/ public SeqIntEqual clone() {
    SeqIntEqual result = (SeqIntEqual) super.clone();
    return result;
  }

  public String repr() {
    return "SeqIntEqual" + varNames() + ": "
      + ",falsified=" + falsified;
  }

  /*@SideEffectFree*/ public String format_using(OutputFormat format) {

    if (format.isJavaFamily()) return format_java_family(format);

    if (format == OutputFormat.DAIKON) return format_daikon();
    if (format == OutputFormat.ESCJAVA) return format_esc();
    if (format == OutputFormat.SIMPLIFY) return format_simplify();
    if (format == OutputFormat.CSHARPCONTRACT) return format_csharp_contract();

    return format_unimplemented(format);
  }

  public String format_daikon() {
    return seqvar().name() + " elements == " + sclvar().name();
  }

  public String format_esc() {
    String[] form = VarInfo.esc_quantify (seqvar(), sclvar());
    return form[0] + "(" + form[1] + " == " + form[2] + ")"
      + form[3];
  }

  public String format_simplify() {
    String[] form = VarInfo.simplify_quantify (seqvar(), sclvar());
    return form[0] + "(EQ " + form[1] + " "
      + form[2] + ")" + form[3];
  }

  public String format_java_family(OutputFormat format) {
    return "daikon.Quant.eltsEqual("
      + seqvar().name_using(format) + ", " + sclvar().name_using(format) + ")";
  }

  public String format_csharp_contract() {
    String[] split = seqvar().csharp_array_split();
    return "Contract.ForAll(" + split[0] + ", x => x" + split[1] + " == " + sclvar().csharp_name() + ")";
  }

  public InvariantStatus check_modified(long /*@Interned*/ [] a, long x, int count) {
    /*if (logDetail() || debug.isLoggable(Level.FINE))
      log(debug,"(== " + ArraysMDE.toString(a)
      + " " + x);*/
    for (int i=0; i<a.length; i++) {

      if (!(a[i] == x))
        return InvariantStatus.FALSIFIED;
    }
    return InvariantStatus.NO_CHANGE;
  }

  public InvariantStatus add_modified(long /*@Interned*/ [] a, long x, int count) {
    return check_modified(a, x, count);
  }

  protected double computeConfidence() {

    // If there are no samples over our variables, its unjustified
    if (ppt.num_samples() == 0)
      return CONFIDENCE_UNJUSTIFIED;

    // If the array never has any elements, its unjustified
    ValueSet.ValueSetScalarArray vs = (ValueSet.ValueSetScalarArray) seqvar().get_value_set();
    if (vs.elem_cnt() == 0) {
      return CONFIDENCE_UNJUSTIFIED;
    }

      // It's an equality invariant.  I ought to use the actual ranges somehow.
      // Actually, I can't even use this .5 test because it can make
      // equality non-transitive.
      // return Math.pow(.5, ppt.num_samples());
      return Invariant.CONFIDENCE_JUSTIFIED;
  }

  /*@Pure*/ public boolean isExact() {

      return true;
  }

  /*@Pure*/ public boolean isSameFormula(Invariant other) {
    return true;
  }

  /*@Pure*/ public boolean isExclusiveFormula(Invariant other) {
    return false;
  }

  // Look up a previously instantiated invariant.
  public static /*@Nullable*/ SeqIntEqual find(PptSlice ppt) {
    assert ppt.arity() == 2;
    for (Invariant inv : ppt.invs) {
      if (inv instanceof SeqIntEqual)
        return (SeqIntEqual) inv;
    }
    return null;
  }

  /**
   * Checks to see if this is obvious over the specified variables.
   * Implements the following checks:
   * <pre>
   *  (x op B[]) ^ (B[] subsequence A[]) &rArr; (x op A[])
   *  (A[] == [])                        &rArr; (x op A[])
   * </pre>
   */
  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousDynamically(VarInfo[] vis) {

    DiscardInfo super_result = super.isObviousDynamically(vis);
    if (super_result != null) {
      return super_result;
    }

    VarInfo seqvar = seqvar(vis);
    VarInfo sclvar = sclvar(vis);
    //Debug.log (getClass(), ppt, vis, "Considering  over" + seqvar.name()
    //           + " and " + sclvar.name());

    // Look for the same property over a supersequence of this one.  This
    // doesn't need to explicitly ignore oher members of the equality set
    // because those members won't have any invariants over them.
    PptTopLevel pptt = ppt.parent;
    for (Iterator<Invariant> inv_itor = pptt.invariants_iterator(); inv_itor.hasNext(); ) {
      Invariant inv = inv_itor.next();
      if (inv == this) {
        continue;
      }
      if (inv instanceof SeqIntEqual) {
        SeqIntEqual other = (SeqIntEqual) inv;
        // System.out.printf ("considering %s seqvar=%s, other=%s%n", other.format(),
        // seqvar().name(), other.seqvar().name());
        if (pptt.is_subsequence (seqvar(), other.seqvar())
            && (sclvar(vis) == other.sclvar())) {
          // System.out.println ("is subsequence");
          return new DiscardInfo(this, DiscardCode.obvious, seqvar().name()
                                 + " is a subsequence of "
                                 + other.seqvar().name() + " and "
                                 + other.format() + " holds");
        }
      }
    }

    // JHP: handled in confidence test now
    // (A[] == []) ==> A[] op x
    if (false) {
      if (pptt.is_empty (seqvar))
        return new DiscardInfo (this, DiscardCode.obvious, "The sequence "
                                + seqvar.name() + " is always empty");
    }

    if (isExact()) {
      return null;
    }

    // JHP: these presume that this invariant is true and should thus be
    // moved to uninteresting or removed.
    if (sclvar.isDerived() && (sclvar.derived instanceof SequenceLength)) {
      // Sequence length tests
      SequenceLength scl_seqlen = (SequenceLength) sclvar.derived;

    }

    // JHP: this presumes that this invariant is true and should thus be
    // moved to uninteresting or removed.
    {
      PptSlice1 seqslice = pptt.findSlice(seqvar);
      if (seqslice != null) {
        EltOneOf eoo = EltOneOf.find(seqslice);
        if ((eoo != null) && eoo.enoughSamples() && (eoo.num_elts() == 1)) {
          return new DiscardInfo(this, DiscardCode.obvious, "Obvious implied by " + eoo.format());
        }
      }
    }

    return null;
  }

  /**
   * Returns a list of non-instantiating suppressions for this invariant.
   */
  /*@Pure*/
  public /*@Nullable*/ NISuppressionSet get_ni_suppressions() {
    return (suppressions);
  }

  /** definition of this invariant (the suppressee) **/
  private static NISuppressee suppressee
    = new NISuppressee (SeqIntEqual.class, 2);

  // suppressor definitions (used in suppressions below)
  private static NISuppressor v1_eq_v2
    = new NISuppressor (0, 1, SeqIntEqual.class);
  private static NISuppressor v1_gt_v2
    = new NISuppressor (0, 1, SeqIntGreaterThan.class);
  private static NISuppressor v1_lt_v2
    = new NISuppressor (0, 1, SeqIntLessThan.class);

  // NI Suppressions for each type of comparison

    private static /*@Nullable*/ NISuppressionSet suppressions = null;

}
