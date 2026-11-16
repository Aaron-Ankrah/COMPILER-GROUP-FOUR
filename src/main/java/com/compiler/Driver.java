package compiler;

import compiler.parser.Ast;
import compiler.parser.Parser;
import compiler.scanner.MiniScanner;
import compiler.scanner.Token;
import compiler.util.CompilationFailed;
import java.util.List;

/** Wires the phases built so far together (grows every phase). */
public final class Driver {
    private Driver() {}

    public static List<Token> scan(String src) {
        MiniScanner s = new MiniScanner(src);
        List<Token> tokens = s.scanAll();
        if (s.hadErrors()) throw new CompilationFailed(s.errors());
        return tokens;
    }

    public static Ast.Program parse(String src) {
        Parser p = new Parser(scan(src));
        Ast.Program ast = p.parseProgram();
        if (p.hadErrors()) throw new CompilationFailed(p.errors());
        return ast;
    }
}
