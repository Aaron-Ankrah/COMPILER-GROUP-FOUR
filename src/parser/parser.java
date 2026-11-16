    /** block -> '{' statement* '}' */
    private Ast.Block block() {
        Token open = consume(LBRACE, "expected '{'");
        List<Ast.Stmt> stmts = new ArrayList<>();
        while (!check(RBRACE) && !check(EOF)) {
            int startPos = cur;
            try {
                stmts.add(statement());
            } catch (ParseError e) {
                if (cur == startPos) advance(); // guarantee progress
                synchronize();
            }
        }
        consume(RBRACE, "expected '}' to close the block");
        return new Ast.Block(stmts, open.line());
    }

    private Ast.Stmt statement() {
        switch (peek().type()) {
            case INT: case BOOL: return varDecl(true);
            case IF: return ifStmt();
            case WHILE: return whileStmt();
            case FOR: return forStmt();
            case RETURN: return returnStmt();
            case PRINT: return printStmt();
            case LBRACE: return block();
            case VOID: throw error(peek(), "'void' can only be used as a function return type");
            default: {
                Ast.Stmt s = simpleStmt();
                consume(SEMI, "expected ';' after statement");
                return s;
            }
        }
    }

    /** varDecl -> type IDENT ('=' expr)? ';' */
    private Ast.Stmt varDecl(boolean needSemi) {
        Token type = advance();
        Token name = consume(IDENT, "expected variable name");
        Ast.Expr init = null;
        if (match(ASSIGN)) init = expression();
        if (needSemi) consume(SEMI, "expected ';' after variable declaration");
        return new Ast.VarDecl(type.lexeme(), name.lexeme(), init, name.line());
    }

    /** simpleStmt -> IDENT '=' expr | callExpr   (no trailing ';') */
    private Ast.Stmt simpleStmt() {
        if (check(IDENT) && peekNext().type() == ASSIGN) {
            Token name = advance();
            advance(); // '='
            Ast.Expr value = expression();
            return new Ast.Assign(name.lexeme(), value, name.line());
        }
        Token start = peek();
        Ast.Expr e = expression();
        if (!(e instanceof Ast.Call)) {
            // report but keep parsing: it is a semantic-looking mistake, not a structural one
            errors.add(new Diagnostic("Parser", start.line(), start.col(),
                    "expression statement must be an assignment or a function call"));
        }
        return new Ast.ExprStmt(e, start.line());
    }
    private Ast.Stmt returnStmt() {
        Token kw = advance();
        Ast.Expr value = check(SEMI) ? null : expression();
        consume(SEMI, "expected ';' after return");
        return new Ast.Return(value, kw.line());
    }

    /** printStmt -> 'print' '(' (expr (',' expr)*)? ')' ';' */
    private Ast.Stmt printStmt() {
        Token kw = advance();
        consume(LPAREN, "expected '(' after 'print'");
        List<Ast.Expr> args = new ArrayList<>();
        if (!check(RPAREN)) {
            do {
                args.add(expression());
            } while (match(COMMA));
        }
        consume(RPAREN, "expected ')' after print arguments");
        consume(SEMI, "expected ';' after print statement");
        return new Ast.Print(args, kw.line());
    }
