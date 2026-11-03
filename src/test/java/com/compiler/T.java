package compiler.tests;

import java.util.ArrayList;
import java.util.List;

/** Tiny dependency-free test helper (so the project builds with plain javac, no JUnit download needed). */
public final class T {
    private static int passed = 0;
    private static final List<String> failures = new ArrayList<>();

    private T() {}

    public static void test(String name, Runnable body) {
        try {
            body.run();
            passed++;
            System.out.println("  PASS  " + name);
        } catch (Throwable t) {
            failures.add(name);
            System.out.println("  FAIL  " + name + "\n        -> " + t);
        }
    }

    public static void suite(String title) {
        System.out.println("\n== " + title);
    }

    public static void eq(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void isTrue(boolean cond, String message) {
        if (!cond) throw new AssertionError(message);
    }

    /** Runs the block and asserts it fails with a CompilationFailed whose messages contain {@code fragment}. */
    public static void failsWith(String fragment, Runnable body) {
        try {
            body.run();
        } catch (compiler.util.CompilationFailed e) {
            StringBuilder all = new StringBuilder();
            e.diagnostics().forEach(d -> all.append(d).append('\n'));
            isTrue(all.toString().contains(fragment), "expected an error containing \"" + fragment + "\" but got:\n" + all);
            return;
        }
        throw new AssertionError("expected compilation to fail with \"" + fragment + "\" but it succeeded");
    }

    /** Prints the summary and returns the process exit code. */
    public static int finish() {
        System.out.println("\n" + passed + " passed, " + failures.size() + " failed");
        failures.forEach(f -> System.out.println("  FAILED: " + f));
        return failures.isEmpty() ? 0 : 1;
    }
}
