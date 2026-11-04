private Ast.Expr term() {
       Ast.Expr expr = factor();
       while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
           Token op = advance();
           expr = new Ast.Binary(op.lexeme(), expr, factor(), op.line());
       }
       return expr;
   }
 
   private Ast.Expr factor() {
       Ast.Expr expr = unary();
       while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
           Token op = advance();
           expr = new Ast.Binary(op.lexeme(), expr, unary(), op.line());
       }
       return expr;
   }
 
   private Ast.Expr unary() {
       if (check(TokenType.NOT) || check(TokenType.MINUS)) {
           Token op = advance();
           return new Ast.Unary(op.lexeme(), unary(), op.line());
       }
       return primary();
   }
 
   private Ast.Expr primary() {
       Token tok = peek();
       if (match(TokenType.INT_LIT)) return new Ast.IntLit((Integer) tok.value(), tok.line());
       if (match(TokenType.TRUE)) return new Ast.BoolLit(true, tok.line());
       if (match(TokenType.FALSE)) return new Ast.BoolLit(false, tok.line());
       if (match(TokenType.STRING_LIT)) return new Ast.StrLit((String) tok.value(), tok.line());
 
       if (match(TokenType.IDENT)) {
           if (match(TokenType.LPAREN)) {
               List<Ast.Expr> args = new ArrayList<>();
               if (!check(TokenType.RPAREN)) {
                   do { args.add(expression()); } while (match(TokenType.COMMA));
               }
               consume(TokenType.RPAREN, "expected ')'");
               return new Ast.Call(tok.lexeme(), args, tok.line());
           }
           return new Ast.VarRef(tok.lexeme(), tok.line());
       }
 
       if (match(TokenType.LPAREN)) {
           Ast.Expr expr = expression();
           consume(TokenType.RPAREN, "expected ')'");
           return expr;
       }
 
       throw error(tok, "expected an expression");
   }