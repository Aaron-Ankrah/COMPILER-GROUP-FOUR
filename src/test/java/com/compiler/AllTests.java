package compiler.tests;

/** Runs every suite that exists at this phase. */
public final class AllTests {
    public static void main(String[] args) {
        ScannerTests.run();
        ParserTests.run();
        System.exit(T.finish());
    }
}
