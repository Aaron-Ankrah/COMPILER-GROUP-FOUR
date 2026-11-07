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

    // >>> MEMBER 3 PASTES block(), statement(), varDecl(), simpleStmt() HERE <<<
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


    // >>> MEMBER 4 PASTES ifStmt(), whileStmt(), forStmt() HERE <<<

    // >>> MEMBER 3 PASTES returnStmt(), printStmt() HERE <<<

    // ------------------------------------------------------------------ expressions (lowest to highest precedence)

    // >>> MEMBER 5 PASTES expression(), or(), and(), equality(), comparison() HERE <<<

    // >>> MEMBER 6 PASTES term(), factor(), unary(), primary() HERE <<<

    // ------------------------------------------------------------------ helpers

    // >>> MEMBER 7 PASTES synchronize() HERE <<<

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
