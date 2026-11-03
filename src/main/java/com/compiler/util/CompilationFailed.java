package compiler.util;

import java.util.List;

/** Thrown when a phase finishes with one or more errors. */
@SuppressWarnings("serial")
public class CompilationFailed extends RuntimeException {
    private final transient List<Diagnostic> diagnostics;

    public CompilationFailed(List<Diagnostic> diagnostics) {
        super(diagnostics.size() + " error(s)");
        this.diagnostics = List.copyOf(diagnostics);
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
