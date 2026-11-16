package compiler;

import compiler.parser.Ast;
import compiler.parser.AstPrinter;
import compiler.parser.Parser;
import compiler.scanner.MiniScanner;
import compiler.scanner.Token;
import compiler.util.Diagnostic;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Command-line entry point (stage: Phase 2 - Parser). */
public final class Main {
    private static final String USAGE = "MiniLang compiler (through Phase 2: Parser)\nUsage: java -cp build/classes compiler.Main [options] <file.mini>\n\n  --tokens  Phase 1: print the token stream\n  --ast     Phase 2: print the abstract syntax tree\n  --help    show this message\n\nExit codes: 0 success, 1 compile error, 64 bad usage.\n";

    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        boolean tokens = false, ast = false;
        String file = null;
        for (String a : args) {
            switch (a) {
                case "--tokens": tokens = true; break;
                case "--ast": ast = true; break;
                case "--help": case "-h": System.out.print(USAGE); return 0;
                default:
                    if (a.startsWith("-")) {
                        System.err.println("Unknown option: " + a + "\n");
                        System.err.print(USAGE);
                        return 64;
                    }
                    file = a;
            }
        }
        if (file == null) {
            System.err.print(USAGE);
            return 64;
        }
        if (!(tokens || ast)) ast = true; // default for this phase

        String src;
        try {
            src = Files.readString(Path.of(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Cannot read '" + file + "': " + e.getMessage());
            return 64;
        }

        // ---- Phase 1: scanner
        MiniScanner scanner = new MiniScanner(src);
        List<Token> toks = scanner.scanAll();
        if (tokens) {
            for (Token t : toks) System.out.println(t);
        }
        if (scanner.hadErrors()) return report(scanner.errors());
        if (tokens) return 0;

        // ---- Phase 2: parser
        Parser parser = new Parser(toks);
        Ast.Program program = parser.parseProgram();
        if (parser.hadErrors()) return report(parser.errors());
        if (ast) {
            System.out.print(AstPrinter.print(program));
            return 0;
        }
        return 0;
    }

    private static int report(List<Diagnostic> errors) {
        for (Diagnostic d : errors) System.err.println(d);
        System.err.println(errors.size() + " error(s). Compilation failed.");
        return 1;
    }
}
