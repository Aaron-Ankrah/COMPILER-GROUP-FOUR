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

    // >>> MEMBER 4 PASTES alwaysReturns() HERE <<<

    // ------------------------------------------------------------------ statements

    // >>> MEMBER 3 PASTES stmt(), scoped(), condition(), checkReturn() HERE <<<

    // ------------------------------------------------------------------ expressions

    // >>> MEMBER 4 PASTES expr(), exprInner(), binary(), call() HERE <<<

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