// ***** This file is automatically generated from SequencesUnionFactory.java.jpp

package daikon.derive.binary;

import daikon.*;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
*/

// This controls derivations which use the scalar as an index into the
// sequence, such as getting the element at that index or a subsequence up
// to that index.

public final class SequenceScalarUnionFactory extends BinaryDerivationFactory {

  public BinaryDerivation /*@Nullable*/ [] instantiate(VarInfo seq1, VarInfo seq2) {
    if (! SequenceScalarUnion.dkconfig_enabled) {
      return null;
    }

    if ((seq1.rep_type != ProglangType.INT_ARRAY)
        || (seq2.rep_type != ProglangType.INT_ARRAY)) {
      return null;
    }

    // Intersect only sets with the same declared element type
    if (!seq1.type.base().equals(seq2.type.base()))
      return null;

    // For now, do nothing if the sequences are derived.
    if ((seq1.derived != null) || (seq2.derived != null))
      return null;

    return new BinaryDerivation[] {
      new SequenceScalarUnion(seq1, seq2) };
  }
}
