package compiler.semantic;

import java.util.List;

/** Signature of a declared function. */
public record FuncSig(String name, List<Type> paramTypes, Type returnType) {}
