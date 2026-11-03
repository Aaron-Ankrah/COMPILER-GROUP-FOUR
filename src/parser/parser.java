package compiler.parser;

import compiler.scanner.Token;
import compiler.scanner.TokenType;
import compiler.util.Diagnostic;

import java.util.ArrayList;
import java.util.List;

import static compiler.scanner.TokenType.*;

/**
 * PHASE 2 - Parser (syntax analysis).
 * Recursive-descent parser for MiniLang with panic-mode error recovery, so several
 * syntax errors can be reported in one run. See docs/LANGUAGE_SPEC.md for the grammar.
 */
public final class Parser {
    /** Internal signal used to unwind to a recovery point. */
    @SuppressWarnings("serial")
    private static final class ParseError extends RuntimeException {
        ParseError() {
            super(null, null, false, false);
        }
    }

    private final List<Token> tokens;
    private int cur = 0;
    private final List<Diagnostic> errors = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Diagnostic> errors() {
        return errors;
    }

    public boolean hadErrors() {
        return !errors.isEmpty();
    }

    // ------------------------------------------------------------------ program / functions

    /** program -> function* EOF */
    public Ast.Program parseProgram() {
        List<Ast.Function> fns = new ArrayList<>();
        while (!check(EOF)) {
            try {
                fns.add(function());
            } catch (ParseError e) {
                while (!check(FUNC) && !check(EOF)) advance();
            }
        }
        return new Ast.Program(fns);
    }

    /** function -> 'func' IDENT '(' params? ')' ':' type block */
    private Ast.Function function() {
        Token kw = consume(FUNC, "expected 'func' to start a function declaration");
        Token name = consume(IDENT, "expected function name after 'func'");
        consume(LPAREN, "expected '(' after function name");
        List<Ast.Param> params = new ArrayList<>();
        if (!check(RPAREN)) {
            do {
                Token t = typeToken("expected parameter type");
                Token pn = consume(IDENT, "expected parameter name");
                params.add(new Ast.Param(t.lexeme(), pn.lexeme(), pn.line()));
            } while (match(COMMA));
        }
        consume(RPAREN, "expected ')' after parameters");
        consume(COLON, "expected ':' before the return type");
        Token ret = typeToken("expected return type (int, bool or void)");
        Ast.Block body = block();
        return new Ast.Function(name.lexeme(), params, ret.lexeme(), body, kw.line());
    }

    private Token typeToken(String msg) {
        if (check(INT) || check(BOOL) || check(VOID)) return advance();
        throw error(peek(), msg);
    }

    // ------------------------------------------------------------------ statements

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

    private Ast.Stmt ifStmt() {
        Token kw = advance();
        consume(LPAREN, "expected '(' after 'if'");
        Ast.Expr cond = expression();
        consume(RPAREN, "expected ')' after condition");
        Ast.Stmt thenB = statement();
        Ast.Stmt elseB = null;
        if (match(ELSE)) elseB = statement();
        return new Ast.If(cond, thenB, elseB, kw.line());
    }

    private Ast.Stmt whileStmt() {
        Token kw = advance();
        consume(LPAREN, "expected '(' after 'while'");
        Ast.Expr cond = expression();
        consume(RPAREN, "expected ')' after condition");
        return new Ast.While(cond, statement(), kw.line());
    }

    /** forStmt -> 'for' '(' (varDecl | simpleStmt ';' | ';') expr? ';' simpleStmt? ')' statement */
    private Ast.Stmt forStmt() {
        Token kw = advance();
        consume(LPAREN, "expected '(' after 'for'");
        Ast.Stmt init = null;
        if (match(SEMI)) {
            // empty initializer
        } else if (check(INT) || check(BOOL)) {
            init = varDecl(true);
        } else {
            init = simpleStmt();
            consume(SEMI, "expected ';' after for-initializer");
        }
        Ast.Expr cond = check(SEMI) ? null : expression();
        consume(SEMI, "expected ';' after for-condition");
        Ast.Stmt update = check(RPAREN) ? null : simpleStmt();
        consume(RPAREN, "expected ')' after for-clauses");
        return new Ast.For(init, cond, update, statement(), kw.line());
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

    // ------------------------------------------------------------------ expressions (lowest to highest precedence)

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

    private Ast.Expr term() {
        Ast.Expr left = factor();
        while (check(PLUS) || check(MINUS)) {
            Token op = advance();
            left = new Ast.Binary(op.lexeme(), left, factor(), op.line());
        }
        return left;
    }

    private Ast.Expr factor() {
        Ast.Expr left = unary();
        while (check(STAR) || check(SLASH) || check(PERCENT)) {
            Token op = advance();
            left = new Ast.Binary(op.lexeme(), left, unary(), op.line());
        }
        return left;
    }

    private Ast.Expr unary() {
        if (check(NOT) || check(MINUS)) {
            Token op = advance();
            return new Ast.Unary(op.lexeme(), unary(), op.line());
        }
        return primary();
    }

    private Ast.Expr primary() {
        Token t = peek();
        switch (t.type()) {
            case INT_LIT: advance(); return new Ast.IntLit((Integer) t.value(), t.line());
            case TRUE: advance(); return new Ast.BoolLit(true, t.line());
            case FALSE: advance(); return new Ast.BoolLit(false, t.line());
            case STRING_LIT: advance(); return new Ast.StrLit((String) t.value(), t.line());
            case IDENT: {
                advance();
                if (match(LPAREN)) {
                    List<Ast.Expr> args = new ArrayList<>();
                    if (!check(RPAREN)) {
                        do {
                            args.add(expression());
                        } while (match(COMMA));
                    }
                    consume(RPAREN, "expected ')' after call arguments");
                    return new Ast.Call(t.lexeme(), args, t.line());
                }
                return new Ast.VarRef(t.lexeme(), t.line());
            }
            case LPAREN: {
                advance();
                Ast.Expr e = expression();
                consume(RPAREN, "expected ')' after expression");
                return e;
            }
            default:
                throw error(t, "expected an expression");
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Panic-mode recovery: skip tokens until a likely statement boundary. */
    private void synchronize() {
        while (!check(EOF)) {
            if (cur > 0 && tokens.get(cur - 1).type() == SEMI) return;
            switch (peek().type()) {
                case INT: case BOOL: case IF: case WHILE: case FOR:
                case RETURN: case PRINT: case RBRACE: case FUNC: case LBRACE:
                    return;
                default:
            }
            advance();
        }
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token at, String message) {
        String found = at.type() == EOF ? "end of file" : "'" + at.lexeme() + "'";
        errors.add(new Diagnostic("Parser", at.line(), at.col(), message + " (found " + found + ")"));
        return new ParseError();
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private Token peek() {
        return tokens.get(cur);
    }

    private Token peekNext() {
        return tokens.get(Math.min(cur + 1, tokens.size() - 1));
    }

    private Token advance() {
        Token t = tokens.get(cur);
        if (t.type() != EOF) cur++;
        return t;
    }
}
