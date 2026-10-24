package compiler.scanner;

import compiler.util.diagnostic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Lexical analysis

public final class MiniScanner {
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();


 static {
        KEYWORDS.put("func", TokenType.FUNC);
        KEYWORDS.put("int", TokenType.INT);
        KEYWORDS.put("bool", TokenType.BOOL);
        KEYWORDS.put("void", TokenType.VOID);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("print", TokenType.PRINT);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
    }


private final String src;
    private int pos = 0;
    private int line = 1;
    private int col = 1;
    private final List<Token> tokens = new ArrayList<>();
    private final List<Diagnostic> errors = new ArrayList<>();

    public MiniScanner(String src) {
        this.src = src;
    }
//Scans the whole input. The returned list always ends with an EOF token.
    public List<Token> scanAll() {
        while (!atEnd()) {
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, col));
        return tokens;
    }

    public List<Diagnostic> errors() {
        return errors;
    }

    public boolean hadErrors() {
        return !errors.isEmpty();
    }

    // --------------------------------------------------- main dispatch

    private void scanToken() {
        int start = pos, sl = line, sc = col;
        char c = advance();
        switch (c) {
            case ' ': case '\t': case '\r': case '\n': return;
            case '(': add(TokenType.LPAREN, start, sl, sc); return;
            case ')': add(TokenType.RPAREN, start, sl, sc); return;
            case '{': add(TokenType.LBRACE, start, sl, sc); return;
            case '}': add(TokenType.RBRACE, start, sl, sc); return;
            case ',': add(TokenType.COMMA, start, sl, sc); return;
            case ';': add(TokenType.SEMI, start, sl, sc); return;
            case ':': add(TokenType.COLON, start, sl, sc); return;
            case '+': add(TokenType.PLUS, start, sl, sc); return;
            case '-': add(TokenType.MINUS, start, sl, sc); return;
            case '*': add(TokenType.STAR, start, sl, sc); return;
            case '%': add(TokenType.PERCENT, start, sl, sc); return;
            case '/':
                if (match('/')) {
                    while (!atEnd() && peek() != '\n') advance();
                } else if (match('*')) {
                    blockComment(sl, sc);
                } else {
                    add(TokenType.SLASH, start, sl, sc);
                }
                return;
            case '=': add(match('=') ? TokenType.EQ : TokenType.ASSIGN, start, sl, sc); return;
            case '!': add(match('=') ? TokenType.NE : TokenType.NOT, start, sl, sc); return;
            case '<': add(match('=') ? TokenType.LE : TokenType.LT, start, sl, sc); return;
            case '>': add(match('=') ? TokenType.GE : TokenType.GT, start, sl, sc); return;
            case '&':
                if (match('&')) add(TokenType.AND, start, sl, sc);
                else error(sl, sc, "unexpected '&' (did you mean '&&'?)");
                return;
            case '|':
                if (match('|')) add(TokenType.OR, start, sl, sc);
                else error(sl, sc, "unexpected '|' (did you mean '||'?)");
                return;
            case '"': string(start, sl, sc); return;
            default:
                if (isDigit(c)) number(start, sl, sc);
                else if (isIdentStart(c)) identifier(start, sl, sc);
                else error(sl, sc, "unexpected character '" + c + "'");
        }
    }

    // ---------------------------------------------------------- token readers

    private void blockComment(int sl, int sc) {
        while (!atEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                return;
            }
            advance();
        }
        error(sl, sc, "unterminated block comment");
    }

    private void string(int start, int sl, int sc) {
        StringBuilder sb = new StringBuilder();
        while (!atEnd() && peek() != '"' && peek() != '\n') {
            char ch = advance();
            if (ch == '\\') {
                if (atEnd() || peek() == '\n') break;
                int ecol = col;
                char e = advance();
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: error(line, ecol - 1, "invalid escape sequence '\\" + e + "'");
                }
            } else {
                sb.append(ch);
            }
        }
        if (atEnd() || peek() == '\n') {
            error(sl, sc, "unterminated string literal");
            return;
        }
        advance(); // closing quote
        tokens.add(new Token(TokenType.STRING_LIT, src.substring(start, pos), sb.toString(), sl, sc));
    }

    private void number(int start, int sl, int sc) {
        while (isDigit(peek())) advance();
        if (isIdentStart(peek())) { // e.g. 12abc
            while (isIdentPart(peek())) advance();
            error(sl, sc, "malformed number '" + src.substring(start, pos) + "'");
            return;
        }
        String text = src.substring(start, pos);
        boolean tooBig = text.length() > 10 || Long.parseLong(text) > Integer.MAX_VALUE;
        if (tooBig) {
            error(sl, sc, "integer literal out of range: " + text);
            return;
        }
        tokens.add(new Token(TokenType.INT_LIT, text, Integer.parseInt(text), sl, sc));
    }

    private void identifier(int start, int sl, int sc) {
        while (isIdentPart(peek())) advance();
        String text = src.substring(start, pos);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENT);
        tokens.add(new Token(type, text, null, sl, sc));
    }

    // ------------------------------------------------------------------ helpers

    private void add(TokenType type, int start, int sl, int sc) {
        tokens.add(new Token(type, src.substring(start, pos), null, sl, sc));
    }

    private void error(int l, int c, String msg) {
        errors.add(new Diagnostic("Scanner", l, c, msg));
    }

    private boolean atEnd() {
        return pos >= src.length();
    }

    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private char peekNext() {
        return pos + 1 < src.length() ? src.charAt(pos + 1) : '\0';
    }

    private char advance() {
        char c = src.charAt(pos++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    private boolean match(char expected) {
        if (atEnd() || src.charAt(pos) != expected) return false;
        advance();
        return true;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return isIdentStart(c) || isDigit(c);
    }
}

