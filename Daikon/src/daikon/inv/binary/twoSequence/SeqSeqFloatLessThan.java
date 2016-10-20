// ***** This file is automatically generated from SeqComparison.java.jpp

package daikon.inv.binary.twoSequence;

import daikon.*;
import daikon.inv.*;
import daikon.suppress.*;
import daikon.derive.binary.*;
import daikon.Quantify.QuantFlags;

import plume.*;
import java.util.logging.Logger;
import java.util.*;

/*>>>
import org.checkerframework.checker.interning.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;
import typequals.*;
*/

/**
 * Represents invariants between two sequences of double values.  If order
 * matters for each variable (which it does by default), then the
 * sequences are compared lexically.
 * Prints as <code>x[] &lt; y[] lexically</code>.
 *

 * If the auxiliary information (e.g., order matters)
 * doesn't match between two variables, then this invariant cannot
 * apply to those variables.
 **/
public class SeqSeqFloatLessThan
  extends TwoSequenceFloat
  implements Comparison
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030822L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff SeqSeqFloatLessThan invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  /**
   * Debugging logger.
   **/
  static final Logger debug = Logger.getLogger ("daikon.inv.binary.twoSequence.SeqSeqFloatLessThan");

  @SuppressWarnings("interning")  // bug with generics
  static Comparator<double[]> comparator = Global.fuzzy.new DoubleArrayComparatorLexical();

  boolean orderMatters;

  protected SeqSeqFloatLessThan(PptSlice ppt, boolean order) {
    super(ppt);
    orderMatters = order;
  }

  protected /*@Prototype*/ SeqSeqFloatLessThan(boolean order) {
    super();
    orderMatters = order;
  }

  protected SeqSeqFloatLessThan(SeqSeqFloatGreaterThan seq_swap) {
    super(seq_swap.ppt);
    orderMatters = seq_swap.orderMatters;
  }

  private static /*@Prototype*/ SeqSeqFloatLessThan proto = new /*@Prototype*/ SeqSeqFloatLessThan (true);

  /** Returns the prototype invariant for SeqSeqFloatLessThan **/
  public static /*@Prototype*/ SeqSeqFloatLessThan get_proto() {
    return (proto);
  }

  /** Returns whether or not this invariant is enabled **/
  public boolean enabled() {
    return dkconfig_enabled;
  }

  /** Non-Equal SeqComparison is only valid on integral types **/
  public boolean instantiate_ok (VarInfo[] vis) {

    if (!valid_types (vis))
      return (false);

    VarInfo var1 = vis[0];
    VarInfo var2 = vis[1];
    ProglangType type1 = var1.type;
    ProglangType type2 = var2.type;

      // This intentonally checks dimensions(), not pseudoDimensions.
      boolean only_eq = (! ((type1.dimensions() == 1)
                            && type1.baseIsFloat()
                            && (type2.dimensions() == 1)
                            && type2.baseIsFloat()));
      if (only_eq)
        return (false);

      // non equality comparisons don't make sense if the arrays aren't ordered
      if (!var1.aux.hasOrder()
        || !var2.aux.hasOrder())
        return (false);

    return (true);
  }

  /** Instantiates the invariant on the specified slice **/
  protected SeqSeqFloatLessThan instantiate_dyn (/*>>> @Prototype SeqSeqFloatLessThan this,*/ PptSlice slice) {
    boolean has_order = slice.var_infos[0].aux.hasOrder()
                      && slice.var_infos[1].aux.hasOrder();
    return new SeqSeqFloatLessThan(slice, has_order);
  }

  protected Invariant resurrect_done_swapped() {

    return new SeqSeqFloatGreaterThan(this);
  }

  public String repr() {
    return "SeqSeqFloatLessThan" + varNames() + ": "
      + ",orderMatters=" + orderMatters
      + ",enoughSamples=" + enoughSamples()
      ;
  }

  /*@SideEffectFree*/ public String format_using(OutputFormat format) {
    // System.out.println("Calling SeqSeqFloatLessThan.format for: " + repr());

    if (format == OutputFormat.SIMPLIFY) {
      return format_simplify();
    }

    if (format == OutputFormat.DAIKON)
    {
      String name1 = var1().name_using(format);
      String name2 = var2().name_using(format);

      String lexically = (var1().aux.hasOrder()
                          ? " (lexically)"
                          : "");
      return name1 + " < " + name2 + lexically;
    }

    if (format == OutputFormat.CSHARPCONTRACT)
    {
      String name1 = var1().csharp_collection_string();
      String name2 = var2().csharp_collection_string();

      if (var1().aux.hasOrder())
      {
        String dbc = "L" + ("lexLT").substring(1);
        return name1 + "." + dbc + "(" + name2 + ")";
      } else {
        return "\"SeqComparison.java.jpp: sequence comparison does not apply to unordered collections unimplemented\" != null)"; // interned
      }

    }

    if (format.isJavaFamily()) {
      String name1 = var1().name_using(format);
      String name2 = var2().name_using(format);

      return "daikon.Quant." + (var1().aux.hasOrder()
                                ? "lexLT"
                                : "setEqual" )
        + "(" + name1 + ", " + name2 + ")";

    }

    return format_unimplemented(format);
  }

  public String format_simplify() {
    if (Invariant.dkconfig_simplify_define_predicates)
      return format_simplify_defined();
    else
      return format_simplify_explicit();
  }

  private String format_simplify_defined() {
    String[] var1_name = var1().simplifyNameAndBounds();
    String[] var2_name = var2().simplifyNameAndBounds();
    if (var1_name == null || var2_name == null) {
      return "format_simplify can't handle one of these sequences: "
        + format();
    }
    return "(|lexical-<| " +
      var1_name[0] + " " + var1_name[1] + " " + var1_name[2] + " " +
      var2_name[0] + " " + var2_name[1] + " " + var2_name[2] + ")";
  }

  private String format_simplify_explicit() {

      String classname = this.getClass().toString().substring(6);
      return "warning: method " + classname
        + ".format_simplify_explicit() needs to be implemented: " + format();

  }

  public InvariantStatus check_modified(double /*@Interned*/ [] v1, double /*@Interned*/ [] v2, int count) {
    /// This does not do the right thing; I really want to avoid comparisons
    /// if one is missing, but not if one is zero-length.
    // // Don't make comparisons with empty arrays.
    // if ((v1.length == 0) || (v2.length == 0)) {
    //   return;
    // }

    int comparison = 0;
    if (orderMatters) {
      // Standard element wise comparison
       comparison = comparator.compare(v1, v2);
    } else {
      // Do a double subset comparison
      comparison = Global.fuzzy.isElemMatch (v1, v2) ? 0 : -1;
    }

    if (! (comparison < 0) ) {
      return InvariantStatus.FALSIFIED;
    }
    return InvariantStatus.NO_CHANGE;
  }

  public InvariantStatus add_modified(double /*@Interned*/ [] v1, double /*@Interned*/ [] v2, int count) {
    if (logDetail())
      log ("add_modified (%s, %s)", ArraysMDE.toString(v1), ArraysMDE.toString(v2));
        return check_modified(v1, v2, count);
  }

  protected double computeConfidence() {

    return 1 - Math.pow(.5, ppt.num_values());
  }

  // For Comparison interface
  public double eq_confidence() {

      return Invariant.CONFIDENCE_NEVER;
  }

  /*@Pure*/ public boolean isSameFormula(Invariant o) {
    return true;
  }

  /*@Pure*/ public boolean isExclusiveFormula(Invariant o) {
    return false;
  }

  /**
   *  Since this invariant can be a postProcessed equality, we have to
   *  handle isObvious especially to avoid circular isObvious
   *  relations.
   **/
  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousStatically_SomeInEquality() {
    if (var1().equalitySet == var2().equalitySet) {
      return isObviousStatically (this.ppt.var_infos);
    } else {
      return super.isObviousStatically_SomeInEquality();
    }
  }

  /**
   *  Since this invariant can be a postProcessed equality, we have to
   *  handle isObvious especially to avoid circular isObvious
   *  relations.
   **/
  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousDynamically_SomeInEquality() {
    if (logOn())
      log ("Considering dynamically_someInEquality");
    if (var1().equalitySet == var2().equalitySet) {
      return isObviousDynamically (this.ppt.var_infos);
    } else {
      return super.isObviousDynamically_SomeInEquality();
    }
  }

  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousStatically(VarInfo[] vis) {

      VarInfo var1 = vis[0];
      VarInfo var2 = vis[1];
      DiscardInfo di;
      di = SubSequenceFloat.isObviousSubSequence(this, var1, var2);
      if (di == null) {
        di = SubSequenceFloat.isObviousSubSequence(this, var2, var1);
      }
      if (di != null) {
        return di;
      }

    return super.isObviousStatically (vis);
  }

  /*@Pure*/
  public /*@Nullable*/ DiscardInfo isObviousDynamically(VarInfo[] vis) {
    DiscardInfo super_result = super.isObviousDynamically(vis);
    if (super_result != null) {
      return super_result;
    }
    assert ppt != null;

      Debug debug = new Debug (getClass(), ppt, vis);

      if (logOn())
        debug.log ("Checking IsObviousDynamically");

      // Check to see if the same Pairwise invariant exists
      DiscardInfo di = new DiscardInfo (this, DiscardCode.obvious, "");
      if (ppt.parent.check_implied (di, vis[0], vis[1],
                                              PairwiseFloatLessThan.get_proto())) {
        di.add_implied_vis (vis);
        return (di);
      }

      // If either variable is a subsequence and the original arrays
      // are related elementwise this isn't interesting
      VarInfo v1 = vis[0];
      VarInfo v2 = vis[1];
      VarInfo arr1 = v1;
      VarInfo arr2 = v2;
      if (v1.derived instanceof SequenceFloatSubsequence)
        arr1 = ((SequenceFloatSubsequence) v1.derived).seqvar();
      if (v2.derived instanceof SequenceFloatSubsequence)
        arr2 = ((SequenceFloatSubsequence) v2.derived).seqvar();
      if (!isEqual() && ((arr1 != v1) || (arr2 != v2))) {
        VarInfo[] avis = new VarInfo [] {arr1, arr2};
        PptSlice slice = this.ppt.parent.findSlice_unordered (avis);
        if (slice != null) {
          PairwiseFloatEqual picEQ = PairwiseFloatEqual.find(slice);
          if (picEQ != null)
            return new DiscardInfo(this, DiscardCode.obvious, "Implied by " + picEQ.format());
          PairwiseFloatLessThan picLT = PairwiseFloatLessThan.find(slice);
          if (picLT != null)
            return new DiscardInfo(this, DiscardCode.obvious, "Implied by " + picLT.format());
          PairwiseFloatGreaterThan picGT = PairwiseFloatGreaterThan.find(slice);
          if (picGT != null)
            return new DiscardInfo(this, DiscardCode.obvious, "Implied by " + picGT.format());
          PairwiseFloatLessEqual picLE = PairwiseFloatLessEqual.find(slice);
          if (picLE != null)
            return new DiscardInfo(this, DiscardCode.obvious, "Implied by " + picLE.format());
          PairwiseFloatGreaterEqual picGE = PairwiseFloatGreaterEqual.find(slice);
          if (picGE != null)
            return new DiscardInfo(this, DiscardCode.obvious, "Implied by " + picGE.format());
        }
      }

      // Similarly, if either variable is a subsequence and the original
      // arrays are related lexically this isn't interesting
      if ((arr1 != v1) || (arr2 != v2)) {
        if (arr1 == arr2) {
          debug.log ("Obvious Dynamic- subsequence from same array");
          return new DiscardInfo(this, DiscardCode.obvious, "Supersequences are related lexically");
        }
        VarInfo[] avis = {arr1, arr2};
        debug.log ("looking for " + avis[0].name()
             + " " + avis[1].name());
        PptSlice slice = this.ppt.parent.findSlice_unordered (avis);
        debug.log ("Found ppt " + slice);
        if (slice != null) {
          for (Invariant inv : slice.invs)
            debug.log ("-- invariant " + inv.format());
          Invariant inv;
          inv = SeqSeqFloatEqual.find(slice);
          if (inv != null) {
            if (logOn())
              debug.log ("Obvious Dynamic from " + inv.format() + "(" + inv.getClass() + ")");
            return new DiscardInfo(this, DiscardCode.obvious, "Implied by " + inv.format());
          }
          inv = SeqSeqFloatLessThan.find(slice);
          if (inv != null) {
            if (logOn())
              debug.log ("Obvious Dynamic from " + inv.format() + "(" + inv.getClass() + ")");
            return new DiscardInfo(this, DiscardCode.obvious, "Implied by " + inv.format());
          }
          inv = SeqSeqFloatGreaterThan.find(slice);
          if (inv != null) {
            if (logOn())
              debug.log ("Obvious Dynamic from " + inv.format() + "(" + inv.getClass() + ")");
            return new DiscardInfo(this, DiscardCode.obvious, "Implied by " + inv.format());
          }
          inv = SeqSeqFloatLessEqual.find(slice);
          if (inv != null) {
            if (logOn())
              debug.log ("Obvious Dynamic from " + inv.format() + "(" + inv.getClass() + ")");
            return new DiscardInfo(this, DiscardCode.obvious, "Implied by " + inv.format());
          }
          inv = SeqSeqFloatGreaterEqual.find(slice);
          if (inv != null) {
            if (logOn())
              debug.log ("Obvious Dynamic from " + inv.format() + "(" + inv.getClass() + ")");
            return new DiscardInfo(this, DiscardCode.obvious, "Implied by " + inv.format());
          }
        }
      }

      // Check to see if these variables are obviously related
      if (v1.isDerived() || v2.isDerived()) {
        if (SubSequenceFloat.isObviousSubSequenceDynamically (this, v1, v2)
          || SubSequenceFloat.isObviousSubSequenceDynamically (this, v2, v1)) {
          if (logOn())
            debug.log ("Obvious SubSequence Dynamically");
          assert ppt != null;
          return new DiscardInfo(this, DiscardCode.obvious, "Both vars are derived and one is a subsequence "
                                 + "of the other");
        }
      }

    return null;
  }

  public void repCheck() {
    super.repCheck();
    /*
      This code is no longer needed now that the can_be_x's are gone
    if (! (this.can_be_eq || this.can_be_lt || this.can_be_gt)
        && ppt.num_samples() != 0) {
      System.err.println (this.repr());
      System.err.println (this.ppt.num_samples());
      throw new Error();
    }
    */
  }

  /*@Pure*/ public boolean isEqual() {

    return false;
  }

  // Look up a previously instantiated invariant.
  public static /*@Nullable*/ SeqSeqFloatLessThan find(PptSlice ppt) {
    assert ppt.arity() == 2;
    for (Invariant inv : ppt.invs) {
      if (inv instanceof SeqSeqFloatLessThan)
        return (SeqSeqFloatLessThan) inv;
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

  /** Definition of this invariant (the suppressee) **/
  private static NISuppressee suppressee
    = new NISuppressee (SeqSeqFloatLessThan.class, 2);

    // Suppressor definitions (used in suppressions below)
    private static NISuppressor
      v1_pw_v2 = new NISuppressor (0, 1, PairwiseFloatLessThan.class);

    private static NISuppressionSet suppressions =
      new NISuppressionSet (new NISuppression[] {
        // pairwise => lexical
        new NISuppression (v1_pw_v2, suppressee),
      });

}
