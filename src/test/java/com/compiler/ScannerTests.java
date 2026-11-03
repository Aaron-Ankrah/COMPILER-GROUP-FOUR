package compiler.tests;

import compiler.scanner.MiniScanner;
import compiler.scanner.Token;
import compiler.scanner.TokenType;

import java.util.ArrayList;
import java.util.List;

import static compiler.tests.T.*;

/** Phase 1 tests. Run alone:  java -cp build/classes:build/test-classes compiler.tests.ScannerTests */
public final class ScannerTests {
    private static List<TokenType> types(String src) {
        List<TokenType> out = new ArrayList<>();
        for (Token t : Driver0.scan(src)) out.add(t.type());
        return out;
    }

    public static void run() {
        suite("Phase 1: Scanner");

        test("empty input gives only EOF", () -> eq(List.of(TokenType.EOF), types("")));

        test("keywords vs identifiers", () -> eq(
                List.of(TokenType.FUNC, TokenType.IDENT, TokenType.IF, TokenType.IDENT, TokenType.EOF),
                types("func foo if iffy")));

        test("all single and double char operators", () -> eq(
                List.of(TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH, TokenType.PERCENT,
                        TokenType.ASSIGN, TokenType.EQ, TokenType.NE, TokenType.LT, TokenType.LE,
                        TokenType.GT, TokenType.GE, TokenType.AND, TokenType.OR, TokenType.NOT, TokenType.EOF),
                types("+ - * / % = == != < <= > >= && || !")));

        test("punctuation", () -> eq(
                List.of(TokenType.LPAREN, TokenType.RPAREN, TokenType.LBRACE, TokenType.RBRACE,
                        TokenType.COMMA, TokenType.SEMI, TokenType.COLON, TokenType.EOF),
                types("( ) { } , ; :")));

        test("integer literal carries its value", () -> {
            Token t = Driver0.scan("12345").get(0);
            eq(TokenType.INT_LIT, t.type());
            eq(12345, t.value());
        });

        test("largest int literal accepted", () -> eq(Integer.MAX_VALUE, Driver0.scan("2147483647").get(0).value()));

        test("string literal with escapes is unescaped", () -> {
            Token t = Driver0.scan("\"a\\tb\\n\\\"q\\\"\"").get(0);
            eq(TokenType.STRING_LIT, t.type());
            eq("a\tb\n\"q\"", t.value());
        });

        test("line and block comments are skipped", () -> eq(
                List.of(TokenType.INT_LIT, TokenType.INT_LIT, TokenType.EOF),
                types("1 // comment\n /* multi\nline */ 2")));

        test("token positions (line, col)", () -> {
            List<Token> ts = Driver0.scan("int x\n  = 5;");
            eq(1, ts.get(0).line()); eq(1, ts.get(0).col());
            eq(1, ts.get(1).line()); eq(5, ts.get(1).col());
            eq(2, ts.get(2).line()); eq(3, ts.get(2).col());
        });

        test("true/false are their own tokens", () -> eq(
                List.of(TokenType.TRUE, TokenType.FALSE, TokenType.EOF), types("true false")));

        test("identifier with digits and underscores", () -> {
            Token t = Driver0.scan("_my_var2").get(0);
            eq(TokenType.IDENT, t.type());
            eq("_my_var2", t.lexeme());
        });

        // ---- error cases
        test("illegal character", () -> failsWith("unexpected character '#'", () -> Driver0.scan("int x = 1 # 2;")));
        test("lone & suggests &&", () -> failsWith("did you mean '&&'", () -> Driver0.scan("a & b")));
        test("lone | suggests ||", () -> failsWith("did you mean '||'", () -> Driver0.scan("a | b")));
        test("unterminated string", () -> failsWith("unterminated string", () -> Driver0.scan("\"abc")));
        test("unterminated block comment", () -> failsWith("unterminated block comment", () -> Driver0.scan("/* oops")));
        test("malformed number", () -> failsWith("malformed number '12abc'", () -> Driver0.scan("12abc")));
        test("integer out of range", () -> failsWith("out of range", () -> Driver0.scan("2147483648")));
        test("bad escape sequence", () -> failsWith("invalid escape", () -> Driver0.scan("\"a\\qb\"")));

        test("multiple lexical errors are all reported", () -> {
            MiniScanner s = new MiniScanner("# $ @");
            s.scanAll();
            eq(3, s.errors().size());
        });
    }

    public static void main(String[] args) {
        run();
        System.exit(finish());
    }
}
