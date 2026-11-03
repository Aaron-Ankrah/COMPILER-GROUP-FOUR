package compiler;

import compiler.codegen.BcProgram;
import compiler.codegen.CodeGenerator;
import compiler.codegen.VirtualMachine;
import compiler.ir.IrGenerator;
import compiler.ir.IrProgram;
import compiler.optimizer.Optimizer;
import compiler.parser.Ast;
import compiler.parser.Parser;
import compiler.scanner.MiniScanner;
import compiler.scanner.Token;
import compiler.semantic.SemanticAnalyzer;
import compiler.semantic.SemanticInfo;
import compiler.util.CompilationFailed;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Wires the five phases together. Every method throws {@link CompilationFailed}
 * if the phase it runs (or an earlier one) reports errors.
 * Used by {@link Main} and by the automated tests.
 */
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

    public static SemanticInfo analyze(String src, Ast.Program ast) {
        SemanticAnalyzer sa = new SemanticAnalyzer();
        SemanticInfo info = sa.analyze(ast);
        if (sa.hadErrors()) throw new CompilationFailed(sa.errors());
        return info;
    }

    /** Source -> IR (optionally optimized). */
    public static IrProgram toIr(String src, boolean optimize) {
        Ast.Program ast = parse(src);
        SemanticInfo info = analyze(src, ast);
        IrProgram ir = new IrGenerator(info).generate(ast);
        return optimize ? new Optimizer().optimize(ir) : ir;
    }

    /** Source -> bytecode. */
    public static BcProgram compile(String src, boolean optimize) {
        return new CodeGenerator().generate(toIr(src, optimize));
    }

    /** Compile and run, returning everything the program printed. */
    public static String run(String src, boolean optimize) {
        BcProgram bc = compile(src, optimize);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos, true, StandardCharsets.UTF_8);
        new VirtualMachine(bc, ps).run();
        ps.flush();
        return bos.toString(StandardCharsets.UTF_8);
    }
}
