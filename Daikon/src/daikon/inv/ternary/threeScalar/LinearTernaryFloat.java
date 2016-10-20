// ***** This file is automatically generated from LinearTernary.java.jpp

package daikon.inv.ternary.threeScalar;

import daikon.*;
import daikon.inv.*        ;
import daikon.inv.DiscardInfo;
import daikon.inv.DiscardCode;
import daikon.inv.binary.twoScalar.*;
import daikon.inv.unary.scalar.*;
import daikon.suppress.NIS;
import java.util.*;
import plume.*;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;
import org.checkerframework.framework.qual.*;
import typequals.*;
*/

/**
 * Represents a Linear invariant over three double scalars <code>x</code>,
 * <code>y</code>, and <code>z</code>, of the form
 * <code>ax + by + cz + d = 0</code>.
 * The constants <code>a</code>, <code>b</code>, <code>c</code>, and
 * <code>d</code> are mutually relatively prime, and the constant
 * <code>a</code> is always positive.
 **/

public class LinearTernaryFloat
  extends ThreeFloat
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030822L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff LinearTernary invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  public static final boolean debugLinearTernary = false;
  // public static final boolean debugLinearTernary = true;

  /*@Unused(when=Prototype.class)*/
  public LinearTernaryCoreFloat core;

  @SuppressWarnings("nullness") // circular initialization
  protected LinearTernaryFloat(PptSlice ppt) {
    super(ppt);
    core = new LinearTernaryCoreFloat(this);
  }

  protected /*@Prototype*/ LinearTernaryFloat() {
    super();
  }

  private static /*@Prototype*/ LinearTernaryFloat proto = new /*@Prototype*/ LinearTernaryFloat ();

  /** Returns the prototype invariant for LinearTernaryFloat **/
  public static /*@Prototype*/ LinearTernaryFloat get_proto() {
    return (proto);
  }

  /** returns whether or not this invariant is enabled **/
  public boolean enabled() {
    return dkconfig_enabled;
  }

  /** LinearTernary is only valid on non-constant integral types **/
  public boolean instantiate_ok (VarInfo[] vis) {

    if (!valid_types (vis))
      return (false);

    // make sure the variables are integral
    if (!vis[0].file_rep_type.isFloat()
        || !vis[1].file_rep_type.isFloat()
        || !vis[2].file_rep_type.isFloat())
      return (false);

    // Don't create if any of the variables are constant.
    // DynamicConstants will create this from LinearBinary
    // and the constant value if/when all of its variables are non-constant
    PptTopLevel parent = vis[0].ppt;
    if (NIS.dkconfig_enabled && (parent.is_constant (vis[0])
                                 || parent.is_constant(vis[1])
                                 || parent.is_constant (vis[2])))
      return (false);

    /*
    // JHP: This code is removed because these sorts of static checks
    // can't be reliably performed with equality sets (just because
    // the leaders are obvious subsets, does not imply that all members
    // are.  Eventually this should be moved to isObviousStatically()

    VarInfo x = ppt.var_infos[0];
    VarInfo y = ppt.var_infos[1];
    VarInfo z = ppt.var_infos[2];

    if (((x.derived instanceof SequenceLength)
         && (((SequenceLength) x.derived).shift != 0))
        || ((y.derived instanceof SequenceLength)
            && (((SequenceLength) y.derived).shift != 0))
        || ((z.derived instanceof SequenceLength)
            && (((SequenceLength) z.derived).shift != 0))) {
      // Do not instantiate z-1 = ax + by + c.  Instead, choose a different c.
      Global.implied_noninstantiated_invariants += 1;
      return null;
    }

    VarInfo x_summand = null;
    VarInfo y_summand = null;
    VarInfo z_summand = null;
    if (x.derived instanceof SequenceSum) x_summand = ((SequenceSum) x.derived).base;
    if (y.derived instanceof SequenceSum) y_summand = ((SequenceSum) y.derived).base;
    if (z.derived instanceof SequenceSum) z_summand = ((SequenceSum) z.derived).base;

    if ((x_summand != null) && (y_summand != null) && (z_summand != null)) {
      // all 3 of x, y, and z are "sum(...)"
      // avoid sum(a[0..i]) + sum(a[i..]) = sum(a[])

      if (debugLinearTernary) {
        System.out.println(ppt.varNames() + " 3 summands: " + x_summand.name + " " + y_summand.name + " " + z_summand.name);
      }

      VarInfo x_seq = x_summand.isDerivedSubSequenceOf();
      VarInfo y_seq = y_summand.isDerivedSubSequenceOf();
      VarInfo z_seq = z_summand.isDerivedSubSequenceOf();
      if (x_seq == null) x_seq = x_summand;
      if (y_seq == null) y_seq = y_summand;
      if (z_seq == null) z_seq = z_summand;
      VarInfo seq = x_seq;

      if (debugLinearTernary) {
        System.out.println(ppt.varNames() + " 3 sequences: " + x_seq.name + " " + y_seq.name + " " + z_seq.name);
      }

      if (((seq == x_summand) || (seq == y_summand) || (seq == z_summand))
          && (x_seq == y_seq) && (x_seq == z_seq)) {
        assert y_seq == z_seq;
        if (debugLinearTernary) {
          System.out.println(ppt.varNames() + " 3 sequences match");
        }

        SequenceFloatSubsequence part1 = null;
        SequenceFloatSubsequence part2 = null;
        for (int i=0; i<3; i++) {
          VarInfo vi = ppt.var_infos[i];
          VarInfo summand = ((SequenceSum) vi.derived).base;
          if (debugLinearTernary) {
            System.out.println("considering: " + summand.name + " " + vi.name);
          }
          if (summand.derived instanceof SequenceFloatSubsequence) {
            SequenceFloatSubsequence sss = (SequenceFloatSubsequence) summand.derived;
            if (sss.from_start) {
              part1 = sss;
            } else {
              part2 = sss;
            }
          } else {
            if (debugLinearTernary) {
              System.out.println("Not subseq: " + summand.name + " " + vi.name);
            }
          }
        }
        if (debugLinearTernary) {
          System.out.println(ppt.varNames() + " part1=" + part1 + ", part2=" + part2 + ", seq=" + seq.name);
        }
        if ((part1 != null) && (part2 != null)) {
          // now part1, and part2 are set
          if ((part1.sclvar() == part2.sclvar())
              && (part1.index_shift + 1 == part2.index_shift)) {
            if (debugLinearTernary) {
              System.out.println("LinearTernary suppressed: " + ppt.varNames());
            }
            Global.implied_noninstantiated_invariants += 1;
            return null;
          }
        }
      }
    } else if ((x_summand != null) && (y_summand != null)
               || ((x_summand != null) && (z_summand != null))
               || ((y_summand != null) && (z_summand != null))) {
      // two of x, y, and z are "sum(...)"
      // avoid sum(a[0..i-1]) + a[i] = sum(a[0..i])

      // if (debugLinearTernary) {
      //   System.out.println(ppt.varNames() + " 2 summands: " + x_summand + " " + y_summand + " " + z_summand);
      // }

      // The intention is that parta is a[0..i], partb is a[0..i-1], and
      // notpart is a[i].
      VarInfo parta;
      VarInfo partb;
      VarInfo notpart;

      if (x_summand != null) {
        parta = x;
        if (y_summand != null) {
          partb = y;
          notpart = z;
        } else {
          partb = z;
          notpart = y;
        }
      } else {
        notpart = x;
        parta = y;
        partb = z;
      }
      parta = ((SequenceSum) parta.derived).base;
      partb = ((SequenceSum) partb.derived).base;
      VarInfo seq = null;
      VarInfo eltindex = null;
      if (notpart.derived instanceof SequenceFloatSubscript) {
        SequenceFloatSubscript sss = (SequenceFloatSubscript) notpart.derived;
        seq = sss.seqvar();
        eltindex = sss.sclvar();
      }
      if ((seq != null)
          && (seq == parta.isDerivedSubSequenceOf())
          && (seq == partb.isDerivedSubSequenceOf())) {
        // For now, don't deal with case where one variable is the entire
        // sequence.
        if (! ((parta == seq) || (partb == seq))) {
          SequenceFloatSubsequence a_sss = (SequenceFloatSubsequence) parta.derived;
          SequenceFloatSubsequence b_sss = (SequenceFloatSubsequence) partb.derived;
          if ((a_sss.sclvar() == eltindex)
              && (b_sss.sclvar() == eltindex)) {
            if ((a_sss.from_start
                 && b_sss.from_start
                 && (((a_sss.index_shift == -1) && (b_sss.index_shift == 0))
                     || ((a_sss.index_shift == 0) && (b_sss.index_shift == -1))))
                || ((! a_sss.from_start)
                    && (! b_sss.from_start)
                    && (((a_sss.index_shift == 0) && (b_sss.index_shift == 1))
                        || ((a_sss.index_shift == 1) && (b_sss.index_shift == 0))))) {
            Global.implied_noninstantiated_invariants += 1;
            return null;
            }
          }
        }
      }
    }
    */

    return (true);
  }

  /** Instantiate the invariant on the specified slice **/
  public LinearTernaryFloat instantiate_dyn (/*>>> @Prototype LinearTernaryFloat this,*/ PptSlice slice) {
    return new LinearTernaryFloat (slice);
  }

  /*@SideEffectFree*/ public LinearTernaryFloat clone() {
    LinearTernaryFloat result = (LinearTernaryFloat) super.clone();
    result.core = core.clone();
    result.core.wrapper = result;
    return result;
  }

  protected Invariant resurrect_done(int[] permutation) {
    core.permute(permutation);
    return this;
  }

  public String repr() {
    return "LinearTernaryFloat" + varNames() + ": "
      + "falsified=" + falsified
      + "; " + core.repr();
  }

  /*@SideEffectFree*/ public String format_using(OutputFormat format) {
    return core.format_using (format, var1().name_using (format),
                        var2().name_using(format), var3().name_using(format));
  }

  // public String format_reversed() {
  //   return core.format_reversed(var1().name.name(), var2().name.name(), var3().name.name());
  // }

  /*@Pure*/ public boolean isActive() {
    return core.isActive();
  }

  public InvariantStatus setup (LinearBinaryFloat lb, VarInfo con_var,
                                double con_val) {
    return core.setup (lb, con_var, con_val);
  }

  public InvariantStatus setup (OneOfFloat oo, VarInfo v1, double con1,
                                VarInfo v2, double con2) {
    return core.setup (oo, v1, con1, v2, con2);
  }

  public InvariantStatus check_modified(double x, double y, double z, int count) {
    return clone().add_modified(x, y, z, count);
  }

  public InvariantStatus add_modified(double x, double y, double z, int count) {
    return core.add_modified(x, y, z, count);
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
  public /*@Nullable*/ DiscardInfo isObviousDynamically(VarInfo[] vis) {
    DiscardInfo super_result = super.isObviousDynamically(vis);
    if (super_result != null) {
      return super_result;
    }

    if (core.a == 0 || core.b == 0 || core.c == 0) {
      return new DiscardInfo(this, DiscardCode.obvious, "If a coefficient is 0, a LinearBinary should" +
                             " exist over the other two variables");
    }

    return null;
  }

  /*@Pure*/ public boolean isSameFormula(Invariant other) {
    return core.isSameFormula(((LinearTernaryFloat) other).core);
  }

  /*@Pure*/ public boolean isExclusiveFormula(Invariant other) {
    if (other instanceof LinearTernaryFloat) {
      return core.isExclusiveFormula(((LinearTernaryFloat) other).core);
    }
    return false;
  }

  // Look up a previously instantiated invariant.
  public static /*@Nullable*/ LinearTernaryFloat find(PptSlice ppt) {
    assert ppt.arity() == 3;
    for (Invariant inv : ppt.invs) {
      if (inv instanceof LinearTernaryFloat)
        return (LinearTernaryFloat) inv;
    }
    return null;
  }

  // Returns a vector of LinearTernaryFloat objects.
  // This ought to produce an iterator instead.
  public static Vector<LinearTernaryFloat> findAll(VarInfo vi) {
    Vector<LinearTernaryFloat> result = new Vector<LinearTernaryFloat>();
    for (PptSlice view : vi.ppt.views_iterable()) {
      if ((view.arity() == 3) && view.usesVar(vi)) {
        LinearTernaryFloat lt = LinearTernaryFloat.find(view);
        if (lt != null) {
          result.add(lt);
        }
      }
    }
    return result;
  }

  public boolean mergeFormulasOk() {
    return (core.mergeFormulasOk());
  }

  /**
   * Merge the invariants in invs to form a new invariant.  Each must be
   * a LinearTernaryFloat invariant.  The work is done by the LinearTernary core
   *
   * @param invs        List of invariants to merge.  They should all be
   *                    permuted to match the variable order in parent_ppt.
   * @param parent_ppt  Slice that will contain the new invariant
   */
  public /*@Nullable*/ Invariant merge (List<Invariant> invs, PptSlice parent_ppt) {

    // Create a matching list of cores
    List<LinearTernaryCoreFloat> cores = new ArrayList<LinearTernaryCoreFloat>();
    for (Invariant inv : invs) {
      cores.add (((LinearTernaryFloat) inv).core);
    }

    // Merge the cores and build a new invariant containing the merged core
    LinearTernaryFloat result = new LinearTernaryFloat (parent_ppt);
    LinearTernaryCoreFloat newcore = core.merge (cores, result);
    if (newcore == null)
      return (null);
    result.core = newcore;
    return (result);
  }

}
