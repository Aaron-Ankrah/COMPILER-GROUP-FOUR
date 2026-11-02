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