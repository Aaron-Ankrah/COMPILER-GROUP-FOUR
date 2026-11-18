package compiler.ir;

import compiler.parser.Ast;
import compiler.semantic.FuncSig;
import compiler.semantic.SemanticInfo;
import compiler.semantic.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * PHASE 3b - IR generation.
 * Lowers the type-checked AST into three-address code. Control flow becomes labels and
 * jumps; && and || become short-circuit jumps; booleans are represented as 0/1.
 * Must only be run on a program that passed semantic analysis.
 */
public final class IrGenerator {
    private final SemanticInfo info;
    private int tempCount;
    private int labelCount;
    private List<Instr> code;

    public IrGenerator(SemanticInfo info) {
        this.info = info;
    }

    public IrProgram generate(Ast.Program program) {
        IrProgram out = new IrProgram();
        for (Ast.Function f : program.functions()) out.functions.add(function(f));
        return out;
    }

    private IrFunction function(Ast.Function f) {
        code = new ArrayList<>();
        tempCount = 0;
        List<Operand> params = new ArrayList<>();
        for (Ast.Param p : f.params()) params.add(Operand.var(info.symbolNames.get(p)));
        for (Ast.Stmt s : f.body().stmts()) stmt(s);

        FuncSig sig = info.functions.get(f.name());
        boolean returnsValue = sig.returnType() != Type.VOID;
        // Safety net: every function ends with a return (the optimizer removes it if unreachable).
        code.add(returnsValue ? Instr.ret(Operand.konst(0)) : Instr.ret(null));
        return new IrFunction(f.name(), params, returnsValue, code);
    }

    // ------------------------------------------------------------------ statements

    private void stmt(Ast.Stmt s) {
        if (s instanceof Ast.Block b) {
            for (Ast.Stmt inner : b.stmts()) stmt(inner);
        } else if (s instanceof Ast.VarDecl v) {
            Operand target = Operand.var(info.symbolNames.get(v));
            Operand value = v.init() != null ? expr(v.init()) : Operand.konst(0); // default 0/false
            code.add(Instr.assign(target, value));
        } else if (s instanceof Ast.Assign a) {
            Operand value = expr(a.value());
            code.add(Instr.assign(Operand.var(info.symbolNames.get(a)), value));
        } else if (s instanceof Ast.If i) {
            ifStmt(i);
        } else if (s instanceof Ast.While w) {
            String top = newLabel(), end = newLabel();
            code.add(Instr.label(top));
            code.add(Instr.jumpIfFalse(expr(w.cond()), end));
            stmt(w.body());
            code.add(Instr.jump(top));
            code.add(Instr.label(end));
        } else if (s instanceof Ast.For f) {
            String top = newLabel(), end = newLabel();
            if (f.init() != null) stmt(f.init());
            code.add(Instr.label(top));
            if (f.cond() != null) code.add(Instr.jumpIfFalse(expr(f.cond()), end));
            stmt(f.body());
            if (f.update() != null) stmt(f.update());
            code.add(Instr.jump(top));
            code.add(Instr.label(end));
        } else if (s instanceof Ast.Return r) {
            code.add(Instr.ret(r.value() != null ? expr(r.value()) : null));
        } else if (s instanceof Ast.Print p) {
            for (Ast.Expr arg : p.args()) {
                if (arg instanceof Ast.StrLit str) {
                    code.add(Instr.printStr(str.value()));
                } else {
                    Operand o = expr(arg);
                    code.add(info.exprTypes.get(arg) == Type.BOOL ? Instr.printBool(o) : Instr.printInt(o));
                }
            }
            code.add(Instr.println());
        } else if (s instanceof Ast.ExprStmt e) {
            call((Ast.Call) e.expr(), false);
        }
    }

    private void ifStmt(Ast.If i) {
        String elseLbl = newLabel();
        code.add(Instr.jumpIfFalse(expr(i.cond()), elseLbl));
        stmt(i.thenBranch());
        if (i.elseBranch() != null) {
            String end = newLabel();
            code.add(Instr.jump(end));
            code.add(Instr.label(elseLbl));
            stmt(i.elseBranch());
            code.add(Instr.label(end));
        } else {
            code.add(Instr.label(elseLbl));
        }
    }

    // ------------------------------------------------------------------ expressions

    private Operand expr(Ast.Expr e) {
        if (e instanceof Ast.IntLit i) return Operand.konst(i.value());
        if (e instanceof Ast.BoolLit b) return Operand.konst(b.value() ? 1 : 0);
        if (e instanceof Ast.VarRef v) return Operand.var(info.symbolNames.get(v));
        if (e instanceof Ast.Unary u) {
            Operand o = expr(u.operand());
            Operand t = newTemp();
            code.add(Instr.unary(u.op().equals("-") ? Op.NEG : Op.NOT, t, o));
            return t;
        }
        if (e instanceof Ast.Binary b) return binary(b);
        if (e instanceof Ast.Call c) return call(c, true);
        throw new IllegalStateException("unexpected expression: " + e);
    }

    private Operand binary(Ast.Binary b) {
        if (b.op().equals("&&") || b.op().equals("||")) {
            boolean isAnd = b.op().equals("&&");
            Operand t = newTemp();
            String end = newLabel();
            code.add(Instr.assign(t, expr(b.left())));
            code.add(isAnd ? Instr.jumpIfFalse(t, end) : Instr.jumpIfTrue(t, end));
            code.add(Instr.assign(t, expr(b.right())));
            code.add(Instr.label(end));
            return t;
        }
        Operand l = expr(b.left());
        Operand r = expr(b.right());
        Operand t = newTemp();
        code.add(Instr.binary(opFor(b.op()), t, l, r));
        return t;
    }

    private Operand call(Ast.Call c, boolean wantResult) {
        List<Operand> args = new ArrayList<>();
        for (Ast.Expr a : c.args()) args.add(expr(a)); // evaluate all arguments first...
        for (Operand o : args) code.add(Instr.param(o)); // ...then push them back-to-back
        FuncSig sig = info.functions.get(c.name());
        Operand result = (sig.returnType() != Type.VOID && wantResult) ? newTemp() : null;
        code.add(Instr.call(result, c.name(), args.size()));
        return result;
    }

    private static Op opFor(String op) {
        switch (op) {
            case "+": return Op.ADD;
            case "-": return Op.SUB;
            case "*": return Op.MUL;
            case "/": return Op.DIV;
            case "%": return Op.MOD;
            case "<": return Op.LT;
            case "<=": return Op.LE;
            case ">": return Op.GT;
            case ">=": return Op.GE;
            case "==": return Op.EQ;
            case "!=": return Op.NE;
            default: throw new IllegalArgumentException("unknown operator " + op);
        }
    }

    private Operand newTemp() {
        return Operand.temp(++tempCount);
    }

    private String newLabel() {
        return "L" + (++labelCount);
    }
}