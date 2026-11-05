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
