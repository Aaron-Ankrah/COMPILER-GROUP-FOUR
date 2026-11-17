package com.compiler.semantic

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
 
/** A function in IR form: parameters plus a linear list of three-address instructions. */
public final class IrFunction {
   public final String name;
   public final List<Operand> params;
   public final boolean returnsValue;
   public List<Instr> code;

   public IrFunction(String name, List<Operand> params, boolean returnsValue, List<Instr> code) {
       this.name = name;
       this.params = params;
       this.returnsValue = returnsValue;
       this.code = code;
   }

   public IrFunction copy() {
       List<Instr> c = new ArrayList<>();
       for (Instr i : code) c.add(i.copy());
       return new IrFunction(name, new ArrayList<>(params), returnsValue, c);
   }

   @Override
   public String toString() {
       StringBuilder sb = new StringBuilder();
       String ps = params.stream().map(Operand::toString).collect(Collectors.joining(", "));
       sb.append("function ").append(name).append("(").append(ps).append(")")
         .append(returnsValue ? " -> value" : " -> void").append('\n');
        for (Instr i : code) {
           sb.append(i.op == Op.LABEL ? "" : "    ").append(i).append('\n');
       }
       return sb.toString();
   }
}
  {
    
}
