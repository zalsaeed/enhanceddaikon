// ***** This file is automatically generated from BoundCore.java.jpp

package daikon.inv.unary;

import daikon.*;
import daikon.inv.*;
import java.text.DecimalFormat;

import java.io.Serializable;

/*>>>
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.*;
*/

// One reason not to combine LowerBound and Upperbound is that they have
// separate justifications:  one may be justified when the other is not.

// What should we do if there are few values in the range?
// This can make justifying that invariant easier, because with few values
// naturally there are more instances of each value.
// This might also make justifying that invariant harder, because to get more
// than (say) twice the expected number of samples (under the assumption of
// uniform distribution) requires many samples.
// Which of these dominates?  Is the behavior what I want?

public class LowerBoundCoreFloat
  implements Serializable, Cloneable
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030822L;

  static final int required_samples = 5; // for enoughSamples
  static final int required_samples_at_bound = 3; // for justification

  // min1 < min2 < min3
  public double min1 = Double.MAX_VALUE;
  public int num_min1 = 0;
  public double min2 = Double.MAX_VALUE;
  public int num_min2 = 0;
  public double min3 = Double.MAX_VALUE;
  public int num_min3 = 0;
  public double max = Double.MIN_VALUE;

  int samples = 0;

  public Invariant wrapper;

  public LowerBoundCoreFloat(Invariant wrapper) {
    this.wrapper = wrapper;
  }

  public double min() {
    return min1;
  }

  /*@SideEffectFree*/
  public LowerBoundCoreFloat clone(/*>>>@GuardSatisfied LowerBoundCoreFloat this*/) {
    try {
      return (LowerBoundCoreFloat) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new Error(); // can't happen
    }
  }

  private static DecimalFormat two_decimals = new java.text.DecimalFormat("#.##");

  public String repr(/*>>>@GuardSatisfied LowerBoundCoreFloat this*/) {
    double modulus = calc_modulus();
    double range = calc_range();
    double avg_samples_per_val = calc_avg_samples_per_val(modulus, range);
    return "min1=" + min1
      + ", num_min1=" + num_min1
      + ", min2=" + min2
      + ", num_min2=" + num_min2
      + ", min3=" + min3
      + ", num_min3=" + num_min3
      + ", max=" + max + ", range=" + range + ", " +
      "avg_samp=" + two_decimals.format(avg_samples_per_val);
  }

  private double calc_avg_samples_per_val(/*>>>@GuardSatisfied LowerBoundCoreFloat this,*/ double modulus, double range) {
    // int num_samples = wrapper.ppt.num_mod_samples();
    int num_samples = wrapper.ppt.num_samples();
    double avg_samples_per_val =
      ((double) num_samples) * modulus / range;
    avg_samples_per_val = Math.min(avg_samples_per_val, 100);

    return avg_samples_per_val;
  }

  private double calc_range(/*>>>@GuardSatisfied LowerBoundCoreFloat this*/) {
    // If I used Math.abs, the order of arguments to minus would not matter.
    return (max - min1) + 1;
  }

  private double calc_modulus(/*>>>@GuardSatisfied LowerBoundCoreFloat this*/) {
    // Need to reinstate this at some point.
    // {
    //   for (Invariant inv : wrapper.ppt.invs) {
    //     if ((inv instanceof Modulus) && inv.enoughSamples()) {
    //       modulus = ((Modulus) inv).modulus;
    //       break;
    //     }
    //   }
    // }
    return 1;
  }

  /**
   * Whether this would change if the given value was seen.  Used to
   * test for need of cloning and flowing before this would be
   * changed.
   */
  public boolean wouldChange (double value) {
    double v = value;
    return (value < min1);
  }

  public InvariantStatus add_modified(double value, int count) {
    samples += count;

    // System.out.println("LowerBoundCoreFloat" + varNames() + ": "
    //                    + "add(" + value + ", " + modified + ", " + count + ")");

    double v = value;

    if (v > max) {
      max = v;
    }

    if (v == min1) {
      num_min1 += count;
    } else if (v < min1) {
      min3 = min2;
      num_min3 = num_min2;
      min2 = min1;
      num_min2 = num_min1;
      min1 = v;
      num_min1 = count;
      return InvariantStatus.WEAKENED;
    } else if (v == min2) {
      num_min2 += count;
    } else if (v < min2) {
      min3 = min2;
      num_min3 = num_min2;
      min2 = v;
      num_min2 = count;
    } else if (v == min3) {
      num_min3 += count;
    } else if (v < min3) {
      min3 = v;
      num_min3 = count;
    }
    return InvariantStatus.NO_CHANGE;
  }

  public InvariantStatus check(double value) {
    if (value < min1) {
      return InvariantStatus.WEAKENED;
    } else {
      return InvariantStatus.NO_CHANGE;
    }
  }

  public boolean enoughSamples(/*>>>@GuardSatisfied LowerBoundCoreFloat this*/) {
    return samples > required_samples;
  }

  // Convenience methods; avoid need for "Invariant." prefix.
  private final double prob_is_ge(double x, double goal) {
    return Invariant.prob_is_ge(x, goal);
  }
  private final double prob_and(double p1, double p2) {
    return Invariant.prob_and(p1, p2);
  }
  private final double prob_or(double p1, double p2) {
    return Invariant.prob_or(p1, p2);
  }

  public double computeConfidence() {
    if (PrintInvariants.dkconfig_static_const_infer && matchConstant()) {
      return Invariant.CONFIDENCE_JUSTIFIED;
    }

    return 1 - computeProbability();
  }

  public boolean matchConstant() {
    PptTopLevel pptt = wrapper.ppt.parent;

    for (VarInfo vi : pptt.var_infos) {
      if (vi.isStaticConstant()) {
        if (vi.rep_type == ProglangType.DOUBLE) {
          // If variable is a double, then use fuzzy comparison
          Double constantVal = (Double)vi.constantValue();
          if (Global.fuzzy.eq(constantVal, min1) || (Double.isNaN(constantVal) && Double.isNaN(min1))) {
            return true;
          }
        } else {
          // Otherwise just use the equals method
          Object constantVal = vi.constantValue();
          if (constantVal.equals(min1)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  // used by computeConfidence
  public double computeProbability() {
    // The bound is justified if both of two conditions is satisfied:
    //  1. there are at least required_samples_at_bound samples at the bound
    //  2. one of the following holds:
    //      2a. the bound has five times the expected number of samples (the
    //          number it would have if the values were uniformly distributed)
    //      2b. the bound and the two next elements all have at least half
    //          the expected number of samples.
    // The expected number of samples is the total number of samples
    // divided by the range of the samples; it is the average number
    // of samples at each point.

    // Value "1" from above.
    double bound_samples_prob = prob_is_ge(num_min1, required_samples_at_bound);

    double modulus = calc_modulus();

    double range = calc_range();
    double avg_samples_per_val = calc_avg_samples_per_val(modulus, range);

    // Value "2a" from above
    double trunc_prob = prob_is_ge(num_min1, 5*avg_samples_per_val);

    // Value "2b" from above
    double unif_prob;
    boolean unif_mod_OK = (((min3 - min2) == modulus)
                           && ((min2 - min1) == modulus));
    if (unif_mod_OK) {
      double half_avg_samp = avg_samples_per_val/2;
      double unif_prob_1 = prob_is_ge(num_min1, half_avg_samp);
      double unif_prob_2 = prob_is_ge(num_min2, half_avg_samp);
      double unif_prob_3 = prob_is_ge(num_min3, half_avg_samp);
      unif_prob = Invariant.prob_and(unif_prob_1, unif_prob_2, unif_prob_3);
      // System.out.println("Unif_probs: " + unif_prob + " <-- " + unif_prob_1 + " " + unif_prob_2 + " " + unif_prob_3);
    } else {
      unif_prob = 1;
    }

    // Value "2" from above
    double bound_prob = prob_or(trunc_prob, unif_prob);

    // Final result
    return prob_and(bound_samples_prob, bound_prob);

    // System.out.println("LowerBoundCoreFloat.computeProbability(): ");
    // System.out.println("  " + repr());
    // System.out.println("  ppt=" + wrapper.ppt.name()
    //                    + ", wrapper.ppt.num_mod_samples()="
    //                    + wrapper.ppt.num_mod_samples()
    //                    // + ", values=" + values
    //                    + ", avg_samples_per_val=" + avg_samples_per_val
    //                    + ", result = " + result
    //                    + ", bound_samples_prob=" + bound_samples_prob
    //                    + ", bound_prob=" + bound_prob
    //                    + ", trunc_prob=" + trunc_prob
    //                    + ", unif_prob=" + unif_prob);
    // PptSlice pptsg = (PptSlice) ppt;
    // System.out.println("  " + ppt.name());

  }

  /*@Pure*/
  public boolean isSameFormula(LowerBoundCoreFloat other) {
    return min1 == other.min1;
  }

  /*@Pure*/
  public boolean isExact() {
    return false;
  }

  // Merge lbc into this.
  public void add (LowerBoundCoreFloat lbc) {

    // Pass each value and its count to this invariant's add_modified.  Since
    // bound is never destroyed, we don't need to check the results.
    if (lbc.num_min1 > 0) {
      add_modified (lbc.min1, lbc.num_min1);
    }
    if (lbc.num_min2 > 0) {
      add_modified (lbc.min2, lbc.num_min2);
    }
    if (lbc.num_min3 > 0) {
      add_modified (lbc.min3, lbc.num_min3);
    }
    // num_min1 will be positive if and only if we've ever seen any
    // real samples. Only then does max represent a real sample.
    if (lbc.num_min1 > 0) {
      add_modified (lbc.max, 1);
    }
    if (Debug.logDetail()) {
      wrapper.log ("Added vals %s of %s, %s of %s, %s of %s, %s from ppt %s",
                   lbc.num_min1, lbc.min1, lbc.num_min2, lbc.min2,
                   lbc.num_min3, lbc.min3,
                   ((lbc.num_min1 > 0) ?  "1 of " + lbc.max : ""),
                   lbc.wrapper.ppt.parent.ppt_name);
    }
  }

}
