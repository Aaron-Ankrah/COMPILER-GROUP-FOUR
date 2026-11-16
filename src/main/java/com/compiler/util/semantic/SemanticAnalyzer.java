package compiler.semantic;

import compiler.parser.Ast;
import compiler.util.Diagnostic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PHASE 3a - Semantic analysis.
 * Builds scoped symbol tables, type-checks every expression and statement, checks calls
 * and returns, and records the results in a {@link SemanticInfo}.
 */
public final class SemanticAnalyzer {
    private record Symbol(String name, Type type, String uniqueName) {}

    private final SemanticInfo info = new SemanticInfo();
    private final List<Diagnostic> errors = new ArrayList<>();
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>(); // head = innermost
    private Map<String, Integer> declCounts = new HashMap<>();
    private FuncSig currentFn;

    public List<Diagnostic> errors() {
        return errors;
    }

    public boolean hadErrors() {
        return !errors.isEmpty();
    }

    public SemanticInfo analyze(Ast.Program program) {
        Set<Ast.Function> accepted = Collections.newSetFromMap(new IdentityHashMap<>());

        // Pass 1: collect signatures so functions can call each other in any order.
        for (Ast.Function f : program.functions()) {
            if (info.functions.containsKey(f.name())) {
                err(f.line(), "function '" + f.name() + "' is already defined");
                continue;
            }
            List<Type> pts = new ArrayList<>();
            for (Ast.Param p : f.params()) {
                Type t = Type.fromName(p.typeName());
                if (t == Type.VOID) {
                    err(p.line(), "parameter '" + p.name() + "' cannot have type void");
                    t = Type.ERROR;
                }
                pts.add(t);
            }
            info.functions.put(f.name(), new FuncSig(f.name(), pts, Type.fromName(f.returnType())));
            accepted.add(f);
        }

        FuncSig main = info.functions.get("main");
        if (main == null) {
            err(1, "program must define 'func main() : void'");
        } else if (!main.paramTypes().isEmpty() || main.returnType() != Type.VOID) {
            err(1, "'main' must take no parameters and return void");
        }

        // Pass 2: check function bodies.
        for (Ast.Function f : program.functions()) {
            if (accepted.contains(f)) checkFunction(f);
        }
        return info;
    }

    // ------------------------------------------------------------------ functions

    private void checkFunction(Ast.Function f) {
        currentFn = info.functions.get(f.name());
        declCounts = new HashMap<>();
        scopes.clear();
        scopes.push(new HashMap<>());
        for (int i = 0; i < f.params().size(); i++) {
            Ast.Param p = f.params().get(i);
            declare(p.name(), currentFn.paramTypes().get(i), p.line(), p);
        }
        // Body statements share the parameter scope, so a local cannot redeclare a parameter.
        for (Ast.Stmt s : f.body().stmts()) stmt(s);
        scopes.pop();

        if (currentFn.returnType() != Type.VOID && !alwaysReturns(f.body())) {
            err(f.line(), "function '" + f.name() + "' may reach its end without returning a value");
        }
    }

    private boolean alwaysReturns(Ast.Stmt s) {
        if (s instanceof Ast.Return) return true;
        if (s instanceof Ast.Block b) {
            for (Ast.Stmt inner : b.stmts()) if (alwaysReturns(inner)) return true;
            return false;
        }
        if (s instanceof Ast.If i) {
            return i.elseBranch() != null && alwaysReturns(i.thenBranch()) && alwaysReturns(i.elseBranch());
        }
        return false; // loops are conservatively assumed to possibly not execute
    }

    // ------------------------------------------------------------------ statements

    private void stmt(Ast.Stmt s) {
        if (s instanceof Ast.Block b) {
            scopes.push(new HashMap<>());
            for (Ast.Stmt inner : b.stmts()) stmt(inner);
            scopes.pop();
        } else if (s instanceof Ast.VarDecl v) {
            Type t = Type.fromName(v.typeName());
            if (v.init() != null) {
                Type it = expr(v.init());
                requireAssignable(t, it, v.line(), "initialize '" + v.name() + "'");
            }
            declare(v.name(), t, v.line(), v); // declared after init so `int x = x;` is an error
        } else if (s instanceof Ast.Assign a) {
            Type vt = expr(a.value());
            Symbol sym = lookup(a.name());
            if (sym == null) {
                err(a.line(), "assignment to undeclared variable '" + a.name() + "'");
            } else {
                info.symbolNames.put(a, sym.uniqueName());
                requireAssignable(sym.type(), vt, a.line(), "assign to '" + a.name() + "'");
            }
        } else if (s instanceof Ast.If i) {
            condition(i.cond(), "if");
            scoped(i.thenBranch());
            if (i.elseBranch() != null) scoped(i.elseBranch());
        } else if (s instanceof Ast.While w) {
            condition(w.cond(), "while");
            scoped(w.body());
        } else if (s instanceof Ast.For f) {
            scopes.push(new HashMap<>());
            if (f.init() != null) stmt(f.init());
            if (f.cond() != null) condition(f.cond(), "for");
            if (f.update() != null) stmt(f.update());
            scoped(f.body());
            scopes.pop();
        } else if (s instanceof Ast.Return r) {
            checkReturn(r);
        } else if (s instanceof Ast.Print p) {
            for (Ast.Expr a : p.args()) {
                if (a instanceof Ast.StrLit) {
                    info.exprTypes.put(a, Type.STRING);
                } else if (expr(a) == Type.VOID) {
                    err(a.line(), "cannot print a void expression");
                }
            }
        } else if (s instanceof Ast.ExprStmt e) {
            expr(e.expr());
        }
    }

    private void scoped(Ast.Stmt s) {
        if (s instanceof Ast.Block) {
            stmt(s);
        } else {
            scopes.push(new HashMap<>());
            stmt(s);
            scopes.pop();
        }
    }

    private void condition(Ast.Expr e, String what) {
        Type t = expr(e);
        if (t != Type.BOOL && t != Type.ERROR) {
            err(e.line(), "condition of '" + what + "' must be bool but found " + t);
        }
    }

    private void checkReturn(Ast.Return r) {
        Type expected = currentFn.returnType();
        if (r.value() == null) {
            if (expected != Type.VOID) err(r.line(), "return without a value in a function returning " + expected);
            return;
        }
        Type t = expr(r.value());
        if (expected == Type.VOID) {
            err(r.line(), "cannot return a value from a void function");
        } else if (t != Type.ERROR && t != expected) {
            err(r.line(), "return type mismatch: expected " + expected + " but found " + t);
        }
    }

    // ------------------------------------------------------------------ expressions

    private Type expr(Ast.Expr e) {
        Type t = exprInner(e);
        info.exprTypes.put(e, t);
        return t;
    }

    private Type exprInner(Ast.Expr e) {
        if (e instanceof Ast.IntLit) return Type.INT;
        if (e instanceof Ast.BoolLit) return Type.BOOL;
        if (e instanceof Ast.StrLit) {
            err(e.line(), "string literals are only allowed as arguments of print(...)");
            return Type.ERROR;
        }
        if (e instanceof Ast.VarRef v) {
            Symbol sym = lookup(v.name());
            if (sym == null) {
                err(v.line(), "use of undeclared variable '" + v.name() + "'");
                return Type.ERROR;
            }
            info.symbolNames.put(v, sym.uniqueName());
            return sym.type();
        }
        if (e instanceof Ast.Unary u) {
            Type o = expr(u.operand());
            if (u.op().equals("-")) {
                need(o, Type.INT, u.line(), "-");
                return Type.INT;
            }
            need(o, Type.BOOL, u.line(), "!");
            return Type.BOOL;
        }
        if (e instanceof Ast.Binary b) return binary(b);
        if (e instanceof Ast.Call c) return call(c);
        return Type.ERROR;
    }

    private Type binary(Ast.Binary b) {
        Type l = expr(b.left());
        Type r = expr(b.right());
        switch (b.op()) {
            case "+": case "-": case "*": case "/": case "%":
                need(l, Type.INT, b.line(), b.op());
                need(r, Type.INT, b.line(), b.op());
                if ((b.op().equals("/") || b.op().equals("%"))
                        && b.right() instanceof Ast.IntLit lit && lit.value() == 0) {
                    err(b.line(), "division by zero");
                }
                return Type.INT;
            case "<": case "<=": case ">": case ">=":
                need(l, Type.INT, b.line(), b.op());
                need(r, Type.INT, b.line(), b.op());
                return Type.BOOL;
            case "&&": case "||":
                need(l, Type.BOOL, b.line(), b.op());
                need(r, Type.BOOL, b.line(), b.op());
                return Type.BOOL;
            default: // == and !=
                if (l != Type.ERROR && r != Type.ERROR) {
                    if (l != r) err(b.line(), "cannot compare " + l + " with " + r);
                    else if (l == Type.VOID || l == Type.STRING) err(b.line(), "cannot compare " + l + " values");
                }
                return Type.BOOL;
        }
    }

    private Type call(Ast.Call c) {
        List<Type> argTypes = new ArrayList<>();
        for (Ast.Expr a : c.args()) argTypes.add(expr(a));
        FuncSig sig = info.functions.get(c.name());
        if (sig == null) {
            err(c.line(), "call to undefined function '" + c.name() + "'");
            return Type.ERROR;
        }
        if (sig.paramTypes().size() != argTypes.size()) {
            err(c.line(), "function '" + c.name() + "' expects " + sig.paramTypes().size()
                    + " argument(s) but got " + argTypes.size());
        } else {
            for (int i = 0; i < argTypes.size(); i++) {
                Type want = sig.paramTypes().get(i), got = argTypes.get(i);
                if (want != Type.ERROR && got != Type.ERROR && want != got) {
                    err(c.line(), "argument " + (i + 1) + " of '" + c.name() + "' should be " + want + " but is " + got);
                }
            }
        }
        return sig.returnType();
    }

    // ------------------------------------------------------------------ symbol table helpers

    private Symbol declare(String name, Type type, int line, Object node) {
        Map<String, Symbol> top = scopes.peek();
        if (top.containsKey(name)) err(line, "variable '" + name + "' is already declared in this scope");
        int n = declCounts.merge(name, 1, Integer::sum);
        String unique = n == 1 ? name : name + "_" + (n - 1);
        Symbol s = new Symbol(name, type, unique);
        top.put(name, s);
        info.symbolNames.put(node, unique);
        return s;
    }

    private Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) { // iterates innermost -> outermost
            Symbol s = scope.get(name);
            if (s != null) return s;
        }
        return null;
    }

    private void need(Type actual, Type expected, int line, String op) {
        if (actual != Type.ERROR && actual != expected) {
            err(line, "operator '" + op + "' requires " + expected + " operand(s) but found " + actual);
        }
    }

    private void requireAssignable(Type target, Type value, int line, String what) {
        if (target != Type.ERROR && value != Type.ERROR && target != value) {
            err(line, "cannot " + what + " (" + target + ") with a value of type " + value);
        }
    }

    private void err(int line, String msg) {
        errors.add(new Diagnostic("Semantic", line, 0, msg));
    }
}
