package compiler.scanner;

public record Token(TokenType type, String lexeme, Object value, int line, int col) {
    @Override
    public String toString() {
        String lex = type == TokenType.EOF ? "<eof>" : lexeme.replace("\n", "\\n");
        return String.format("%4d:%-3d %-11s %s", line, col, type, lex);
    }
}
