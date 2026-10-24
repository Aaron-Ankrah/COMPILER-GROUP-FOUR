package compiler.scanner;

/** Every kind of token the MiniLang scanner can produce. */
public enum TokenType {
    // keywords
    FUNC, INT, BOOL, VOID, IF, ELSE, WHILE, FOR, RETURN, PRINT, TRUE, FALSE,
    // literals and identifiers
    IDENT, INT_LIT, STRING_LIT,
    // operators
    PLUS, MINUS, STAR, SLASH, PERCENT,
    ASSIGN, EQ, NE, LT, LE, GT, GE,
    AND, OR, NOT,
    // punctuation
    LPAREN, RPAREN, LBRACE, RBRACE, COMMA, SEMI, COLON,
    // end of input
    EOF
}
