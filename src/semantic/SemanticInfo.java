package compiler.semantic;

import compiler.parser.Ast;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of semantic analysis: annotations attached to AST nodes (by identity) that the IR generator needs. 
 */
public final class SemanticInfo {
    /** Static type of every expression. */
    public final Map<Ast.Expr, Type> exprTypes = new IdentityHashMap<>();
    /** Unique IR name for every VarDecl / Assign / VarRef / Param (resolves shadowing). */
    public final Map<Object, String> symbolNames = new IdentityHashMap<>();
    /** All function signatures, in declaration order. */
    public final Map<String, FuncSig> functions = new LinkedHashMap<>();
}
