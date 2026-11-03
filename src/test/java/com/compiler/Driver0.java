package compiler.tests;

import compiler.Driver;
import compiler.scanner.Token;

import java.util.List;

/** Small convenience wrappers so test classes stay short. */
final class Driver0 {
    private Driver0() {}

    static List<Token> scan(String src) {
        return Driver.scan(src);
    }

    /** Wraps statements in a main function. */
    static String inMain(String body) {
        return "func main() : void {\n" + body + "\n}\n";
    }

    static String out(String src) {
        return Driver.run(src, false);
    }

    static String outOpt(String src) {
        return Driver.run(src, true);
    }
}
