//
// Generated by JTB 1.1.2
//

package jtb.cparser.syntaxtree;

// Grammar production:
// f0 -> ( "(" TypeName() ")" CastExpression() | UnaryExpression() )
public class CastExpression implements Node {
  static final long serialVersionUID = 20050923L;

   public NodeChoice f0;

   public CastExpression(NodeChoice n0) {
      f0 = n0;
   }

   public void accept(jtb.cparser.visitor.Visitor v) {
      v.visit(this);
   }
}
