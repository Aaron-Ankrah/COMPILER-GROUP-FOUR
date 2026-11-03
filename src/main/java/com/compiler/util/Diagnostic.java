package compiler.util;

/** A single compile-time error, tagged with the phase that found it. */
public record Diagnostic(String phase, int line, int col, String message) {
    @Override
    public String toString() {
        String where = col > 0 ? "line " + line + ", col " + col : "line " + line;
        return "[" + phase + " error] " + where + ": " + message;
    }
}
