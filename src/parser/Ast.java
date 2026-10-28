package compiler.parser;

import java.util.List;

/** AST node definitions for MiniLang (shared by the parser, semantic analyzer and IR generator). */
public final class Ast {
    private Ast() {}

    public interface Node { int line(); }
    public interface Expr extends Node {}
    public interface Stmt extends Node {}

    // ---- expressions
    public record IntLit(int value, int line) implements Expr {}
    public record BoolLit(boolean value, int line) implements Expr {}
    public record StrLit(String value, int line) implements Expr {}
    public record VarRef(String name, int line) implements Expr {}
    public record Unary(String op, Expr operand, int line) implements Expr {}
    public record Binary(String op, Expr left, Expr right, int line) implements Expr {}
    public record Call(String name, List<Expr> args, int line) implements Expr {}

    // ---- statements
    public record VarDecl(String typeName, String name, Expr init, int line) implements Stmt {}
    public record Assign(String name, Expr value, int line) implements Stmt {}
    public record If(Expr cond, Stmt thenBranch, Stmt elseBranch, int line) implements Stmt {}
    public record While(Expr cond, Stmt body, int line) implements Stmt {}
    public record For(Stmt init, Expr cond, Stmt update, Stmt body, int line) implements Stmt {}
    public record Return(Expr value, int line) implements Stmt {}
    public record Print(List<Expr> args, int line) implements Stmt {}
    public record ExprStmt(Expr expr, int line) implements Stmt {}
    public record Block(List<Stmt> stmts, int line) implements Stmt {}

    // ---- top level
    public record Param(String typeName, String name, int line) {}
    public record Function(String name, List<Param> params, String returnType, Block body, int line) {}
    public record Program(List<Function> functions) {}
}
