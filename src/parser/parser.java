package compiler.parser;

import compiler.scanner.Token;
import compiler.scanner.TokenType;
import compiler.util.Diagnostic;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private final List<Diagnostic> errors = new ArrayList<>();
    private int cur = 0;

    @SuppressWarnings("serial")
    private static class ParseError extends RuntimeException { ParseError() { super(null, null, false, false); } }

    public Parser(List<Token> tokens) { this.tokens = tokens != null ? tokens : List.of(); }
    public List<Diagnostic> errors() { return errors; }
    public boolean hadErrors() { return !errors.isEmpty(); }

    private boolean check(TokenType type) { return peek().type() == type; }
    private Token peek() { return tokens.get(cur); }
    private Token advance() { if (!check(TokenType.EOF)) cur++; return tokens.get(cur - 1); }
    private boolean match(TokenType type) { if (check(type)) { advance(); return true; } return false; }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        errors.add(new Diagnostic("Parser", token.line(), token.col(), message + " (found '" + token.lexeme() + "')"));
        return new ParseError();
    }

    public Ast.Program parseProgram() {
        List<Ast.Function> functions = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            try { functions.add(function()); }
            catch (ParseError e) { synchronize(); }
        }
        return new Ast.Program(functions);
    }

    private Ast.Function function() {
        Token funcToken = consume(TokenType.FUNC, "expected 'func'");
        Token nameToken = consume(TokenType.IDENT, "expected function name");
        consume(TokenType.LPAREN, "expected '('");
        List<Ast.Param> params = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Token typeToken = advance();
                Token pName = consume(TokenType.IDENT, "expected parameter name");
                params.add(new Ast.Param(typeToken.lexeme(), pName.lexeme(), typeToken.line()));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "expected ')'");
        consume(TokenType.COLON, "expected ':'");
        Token retType = advance();
        Ast.Block body = block();
        return new Ast.Function(nameToken.lexeme(), params, retType.lexeme(), body, funcToken.line());
    }
