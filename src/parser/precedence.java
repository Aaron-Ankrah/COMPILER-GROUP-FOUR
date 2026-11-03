private Ast.Expr expression() {
        return or();
    }

    private Ast.Expr or() {
        Ast.Expr left = and();
        while (check(OR)) {
            Token op = advance();
            left = new Ast.Binary("||", left, and(), op.line());
        }
        return left;
    }

    private Ast.Expr and() {
        Ast.Expr left = equality();
        while (check(AND)) {
            Token op = advance();
            left = new Ast.Binary("&&", left, equality(), op.line());
        }
        return left;
    }

    private Ast.Expr equality() {
        Ast.Expr left = comparison();
        while (check(EQ) || check(NE)) {
            Token op = advance();
            left = new Ast.Binary(op.lexeme(), left, comparison(), op.line());
        }
        return left;
    }

    private Ast.Expr comparison() {
        Ast.Expr left = term();
        while (check(LT) || check(LE) || check(GT) || check(GE)) {
            Token op = advance();
            left = new Ast.Binary(op.lexeme(), left, term(), op.line());
        }
        return left;
    }