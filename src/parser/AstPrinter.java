package compiler.parser;

import java.util.List;
import java.util.stream.Collectors;
/** Pretty-prints an AST as an indented tree (used by {@code --ast}). */
public final class AstPrinter {
    private final StringBuilder sb = new StringBuilder();

    public static String print(Ast.Program p) {
        AstPrinter pr = new AstPrinter();
        pr.line(0, "Program");
        for (Ast.Function f : p.functions()) pr.function(1, f);
        return pr.sb.toString();
    }

    private void line(int depth, String text) {
        sb.append("  ".repeat(depth)).append(text).append('\n');
    }

    private void function(int d, Ast.Function f) {
        String params = f.params().stream().map(p -> p.typeName() + " " + p.name()).collect(Collectors.joining(", "));
        line(d, "Function " + f.name() + "(" + params + ") : " + f.returnType());
        stmt(d + 1, f.body());
    }

    private void stmts(int d, List<Ast.Stmt> list) {
        for (Ast.Stmt s : list) stmt(d, s);
    }

    private void stmt(int d, Ast.Stmt s) {
        if (s instanceof Ast.Block b) {
            line(d, "Block");
            stmts(d + 1, b.stmts());
        } else if (s instanceof Ast.VarDecl v) {
            line(d, "VarDecl " + v.typeName() + " " + v.name());
            if (v.init() != null) expr(d + 1, v.init());
        } else if (s instanceof Ast.Assign a) {
            line(d, "Assign " + a.name());
            expr(d + 1, a.value());
        } else if (s instanceof Ast.If i) {
            line(d, "If");
            line(d + 1, "cond:");
            expr(d + 2, i.cond());
            line(d + 1, "then:");
            stmt(d + 2, i.thenBranch());
            if (i.elseBranch() != null) {
                line(d + 1, "else:");
                stmt(d + 2, i.elseBranch());
            }
        } else if (s instanceof Ast.While w) {
            line(d, "While");
            line(d + 1, "cond:");
            expr(d + 2, w.cond());
            line(d + 1, "body:");
            stmt(d + 2, w.body());
        } else if (s instanceof Ast.For f) {
            line(d, "For");
            if (f.init() != null) { line(d + 1, "init:"); stmt(d + 2, f.init()); }
            if (f.cond() != null) { line(d + 1, "cond:"); expr(d + 2, f.cond()); }
            if (f.update() != null) { line(d + 1, "update:"); stmt(d + 2, f.update()); }
            line(d + 1, "body:");
            stmt(d + 2, f.body());
        } else if (s instanceof Ast.Return r) {
            line(d, "Return");
            if (r.value() != null) expr(d + 1, r.value());
        } else if (s instanceof Ast.Print p) {
            line(d, "Print");
            for (Ast.Expr e : p.args()) expr(d + 1, e);
        } else if (s instanceof Ast.ExprStmt e) {
            line(d, "ExprStmt");
            expr(d + 1, e.expr());
        } else {
            line(d, "?" + s);
        }
    }

    private void expr(int d, Ast.Expr e) {
        if (e instanceof Ast.IntLit i) line(d, "IntLit " + i.value());
        else if (e instanceof Ast.BoolLit b) line(d, "BoolLit " + b.value());
        else if (e instanceof Ast.StrLit s) line(d, "StrLit \"" + s.value().replace("\n", "\\n") + "\"");
        else if (e instanceof Ast.VarRef v) line(d, "Var " + v.name());
        else if (e instanceof Ast.Unary u) {
            line(d, "Unary " + u.op());
            expr(d + 1, u.operand());
        } else if (e instanceof Ast.Binary b) {
            line(d, "Binary " + b.op());
            expr(d + 1, b.left());
            expr(d + 1, b.right());
        } else if (e instanceof Ast.Call c) {
            line(d, "Call " + c.name());
            for (Ast.Expr a : c.args()) expr(d + 1, a);
        } else {
            line(d, "?" + e);
        }
    }
}
