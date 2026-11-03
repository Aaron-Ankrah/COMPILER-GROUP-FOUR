package compiler.tests;

/** Runs every suite. From the project root:  scripts/test.sh */
public final class AllTests {
    public static void main(String[] args) {
        ScannerTests.run();
        ParserTests.run();
        SemanticTests.run();
        IrTests.run();
        CodegenVmTests.run();
        OptimizerTests.run();
        ExampleTests.run();
        System.exit(T.finish());
    }
}
