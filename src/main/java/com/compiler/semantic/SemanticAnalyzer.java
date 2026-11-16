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