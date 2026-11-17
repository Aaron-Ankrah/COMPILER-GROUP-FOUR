package compiler.tests;

import compiler.Driver;
import compiler.parser.Ast;
import compiler.parser.AstPrinter;
import compiler.parser.Parser;
import compiler.scanner.MiniScanner;

import static compiler.tests.Driver0.inMain;
import static compiler.tests.T.*;

/** Phase 2 tests. */
public final class ParserTests {
    private static Ast.Program parse(String src) {
        return Driver.parse(src);
    }

    private static String tree(String body) {
        return AstPrinter.print(parse(inMain(body)));
    }

    public static void run() {
        suite("Phase 2: Parser");

        test("empty program parses", () -> eq(0, parse("").functions().size()));

        test("function with parameters and return type", () -> {
            Ast.Function f = parse("func add(int a, bool b) : int { return a; }").functions().get(0);
            eq("add", f.name());
            eq(2, f.params().size());
            eq("bool", f.params().get(1).typeName());
            eq("int", f.returnType());
        });

        test("operator precedence: * binds tighter than +", () ->
                eq("Binary +", tree("int x = 1 + 2 * 3;").lines()
                        .filter(l -> l.contains("Binary")).findFirst().get().trim()));

        test("precedence tree shape for 1 + 2 * 3", () -> {
            String t = tree("int x = 1 + 2 * 3;");
            isTrue(t.contains("Binary +\n          IntLit 1\n          Binary *"), "unexpected tree:\n" + t);
        });

        test("subtraction is left-associative", () -> {
            String t = tree("int x = 10 - 3 - 2;");
            isTrue(t.contains("Binary -\n          Binary -"), "expected (10-3)-2:\n" + t);
        });

        test("parentheses override precedence", () -> {
            String t = tree("int x = (1 + 2) * 3;");
            isTrue(t.contains("Binary *\n          Binary +"), "unexpected tree:\n" + t);
        });

        test("&& binds tighter than ||", () -> {
            String t = tree("bool b = true || false && false;");
            isTrue(t.contains("Binary ||\n          BoolLit true\n          Binary &&"), "unexpected tree:\n" + t);
        });

        test("unary minus and not", () -> {
            String t = tree("int a = -5; bool b = !true;");
            isTrue(t.contains("Unary -") && t.contains("Unary !"), t);
        });

        test("if / else / while / for / return / print all parse", () -> {
            String t = tree("int i = 0; if (i < 1) { i = 1; } else { i = 2; } while (i < 5) { i = i + 1; } "
                    + "for (int j = 0; j < 3; j = j + 1) { print(j); } return;");
            for (String n : new String[]{"If", "While", "For", "Return", "Print"}) {
                isTrue(t.contains(n), "missing " + n + " in:\n" + t);
            }
        });

        test("dangling else binds to the nearest if", () -> {
            Ast.Function f = parse(inMain("if (true) if (false) print(1); else print(2);")).functions().get(0);
            Ast.If outer = (Ast.If) f.body().stmts().get(0);
            isTrue(outer.elseBranch() == null, "else must not attach to the outer if");
            isTrue(((Ast.If) outer.thenBranch()).elseBranch() != null, "else must attach to the inner if");
        });

        test("for with empty clauses", () -> parse(inMain("for (;;) { return; }")));

        test("call with arguments as expression and statement", () ->
                parse("func f(int a, int b) : int { return a; } func main() : void { f(1, 2); int x = f(f(1,2), 3); }"));

        test("print with zero, one and many arguments", () ->
                parse(inMain("print(); print(1); print(\"a\", 1, true);")));

        // ---- error handling
        test("missing semicolon", () -> failsWith("expected ';'", () -> parse(inMain("int x = 5"))));
        test("missing closing paren", () -> failsWith("expected ')'", () -> parse(inMain("print(1;"))));
        test("missing closing brace", () -> failsWith("expected '}'", () -> parse("func main() : void { int x = 1;")));
        test("missing expression", () -> failsWith("expected an expression", () -> parse(inMain("int x = ;"))));
        test("missing return type", () -> failsWith("expected ':'", () -> parse("func main() { }")));
        test("statement outside a function", () -> failsWith("expected 'func'", () -> parse("int x = 1;")));
        test("void variable is rejected", () -> failsWith("'void' can only be used", () -> parse(inMain("void x;"))));
        test("bare expression statement is rejected", () -> failsWith("must be an assignment or a function call", () -> parse(inMain("1 + 2;"))));

        test("parser recovers and reports several errors", () -> {
            MiniScanner s = new MiniScanner(inMain("int a = 1\nint b = 2\nint c = 3\n"));
            Parser p = new Parser(s.scanAll());
            p.parseProgram();
            isTrue(p.errors().size() >= 2, "expected at least 2 errors, got " + p.errors().size());
        });

        test("parser never loops forever on garbage", () -> {
            MiniScanner s = new MiniScanner(") ) ( } { func func ; ; else else");
            Parser p = new Parser(s.scanAll());
            p.parseProgram(); // must terminate
            isTrue(p.hadErrors(), "garbage must produce errors");
        });
    }

    public static void main(String[] args) {
        run();
        System.exit(finish());
    }
}
