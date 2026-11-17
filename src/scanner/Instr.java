package compiler.ir;
 
import java.util.ArrayList;
import java.util.List;
 
public class Instr {
   public Op op; public Operand result; public Operand a; public Operand b;
   public String label; public int argc; public String str;
 
   private Instr(Op op) { this.op = op; }
 
   public static Instr assign(Operand res, Operand a) { Instr i = new Instr(Op.ASSIGN); i.result = res; i.a = a; return i; }
   public static Instr binary(Op op, Operand res, Operand a, Operand b) { Instr i = new Instr(op); i.result = res; i.a = a; i.b = b; return i; }
   public static Instr unary(Op op, Operand res, Operand a) { Instr i = new Instr(op); i.result = res; i.a = a; return i; }
   public static Instr label(String lbl) { Instr i = new Instr(Op.LABEL); i.label = lbl; return i; }
   public static Instr jump(String target) { Instr i = new Instr(Op.JUMP); i.label = target; return i; }
   public static Instr jumpIfFalse(Operand cond, String target) { Instr i = new Instr(Op.JUMP_IF_FALSE); i.a = cond; i.label = target; return i; }
   public static Instr jumpIfTrue(Operand cond, String target) { Instr i = new Instr(Op.JUMP_IF_TRUE); i.a = cond; i.label = target; return i; }
   public static Instr param(Operand arg) { Instr i = new Instr(Op.PARAM); i.a = arg; return i; }
   public static Instr call(Operand res, String name, int argc) { Instr i = new Instr(Op.CALL); i.result = res; i.label = name; i.argc = argc; return i; }
   public static Instr ret(Operand val) { Instr i = new Instr(Op.RETURN); i.a = val; return i; }
   public static Instr printInt(Operand a) { Instr i = new Instr(Op.PRINT_INT); i.a = a; return i; }
   public static Instr printBool(Operand a) { Instr i = new Instr(Op.PRINT_BOOL); i.a = a; return i; }
   public static Instr printStr(String s) { Instr i = new Instr(Op.PRINT_STR); i.str = s; return i; }
   public static Instr println() { return new Instr(Op.PRINTLN); }
 
   public Instr copy() {
       Instr i = new Instr(op); i.result = result; i.a = a; i.b = b;
       i.label = label; i.argc = argc; i.str = str; return i;
   }
 
   public boolean isBinary() {
       return op == Op.ADD || op == Op.SUB || op == Op.MUL || op == Op.DIV || op == Op.MOD ||
              op == Op.LT || op == Op.LE || op == Op.GT || op == Op.GE || op == Op.EQ || op == Op.NE;
   }
   public boolean isJump() { return op == Op.JUMP || op == Op.JUMP_IF_FALSE || op == Op.JUMP_IF_TRUE; }
   public Operand def() { return result; }
   public List<Operand> uses() {
       List<Operand> list = new ArrayList<>();
       if (a != null && a.isLocal()) list.add(a);
       if (b != null && b.isLocal()) list.add(b);
       return list;
   }
 
   @Override
   public String toString() {
       return switch (op) {
           case LABEL -> label + ":";
           case ASSIGN -> result + " = " + a;
           case NEG, NOT -> result + " = " + op.symbol() + a;
           case ADD, SUB, MUL, DIV, MOD, LT, LE, GT, GE, EQ, NE -> result + " = " + a + " " + op.symbol() + " " + b;
           case JUMP -> "goto " + label;
           case JUMP_IF_FALSE -> "ifFalse " + a + " goto " + label;
           case JUMP_IF_TRUE -> "ifTrue " + a + " goto " + label;
           case PARAM -> "param " + a;
           case CALL -> (result != null ? result + " = " : "") + "call " + label + ", " + argc;
           case RETURN -> "return" + (a != null ? " " + a : "");
           case PRINT_INT -> "print_int " + a;
           case PRINT_BOOL -> "print_bool " + a;
           case PRINT_STR -> "print_str "" + str + """;
           case PRINTLN -> "println";
       };
   }
}