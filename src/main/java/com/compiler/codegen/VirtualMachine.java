package compiler.codegen;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Executes the bytecode produced by {@link CodeGenerator}.
 * One shared operand stack; one Frame (locals + program counter) per active call.
 */
public final class VirtualMachine {
    public static final int MAX_CALL_DEPTH = 10_000;
    private static final int STACK_SIZE = 1 << 20;

    private static final class Frame {
        final BcFunction fn;
        final int[] locals;
        int pc = 0;

        Frame(BcFunction fn, int[] locals) {
            this.fn = fn;
            this.locals = locals;
        }
    }

    private final BcProgram program;
    private final PrintStream out;
    private final int[] stack = new int[STACK_SIZE];
    private int sp = 0;
    private long steps = 0;
    private long maxSteps = 500_000_000L;

    public VirtualMachine(BcProgram program, PrintStream out) {
        this.program = program;
        this.out = out;
    }

    /** Number of bytecode instructions executed by the last run (useful for comparing optimizations). */
    public long stepsExecuted() {
        return steps;
    }

    public void setMaxSteps(long maxSteps) {
        this.maxSteps = maxSteps;
    }

    public void run() {
        if (program.mainIndex < 0) throw new VmException("no 'main' function");
        Deque<Frame> callStack = new ArrayDeque<>();
        BcFunction mainFn = program.functions.get(program.mainIndex);
        Frame cur = new Frame(mainFn, new int[mainFn.numLocals]);

        while (true) {
            BcInstr in = cur.fn.code.get(cur.pc++);
            if (++steps > maxSteps) throw new VmException("execution step limit exceeded (possible infinite loop)");
            switch (in.op()) {
                case PUSH: push(in.a()); break;
                case LOAD: push(cur.locals[in.a()]); break;
                case STORE: cur.locals[in.a()] = pop(); break;
                case POP: pop(); break;
                case ADD: { int r = pop(), l = pop(); push(l + r); break; }
                case SUB: { int r = pop(), l = pop(); push(l - r); break; }
                case MUL: { int r = pop(), l = pop(); push(l * r); break; }
                case DIV: {
                    int r = pop(), l = pop();
                    if (r == 0) throw new VmException("division by zero in function '" + cur.fn.name + "'");
                    push(l / r);
                    break;
                }
                case MOD: {
                    int r = pop(), l = pop();
                    if (r == 0) throw new VmException("modulo by zero in function '" + cur.fn.name + "'");
                    push(l % r);
                    break;
                }
                case NEG: push(-pop()); break;
                case NOT: push(pop() == 0 ? 1 : 0); break;
                case LT: { int r = pop(), l = pop(); push(l < r ? 1 : 0); break; }
                case LE: { int r = pop(), l = pop(); push(l <= r ? 1 : 0); break; }
                case GT: { int r = pop(), l = pop(); push(l > r ? 1 : 0); break; }
                case GE: { int r = pop(), l = pop(); push(l >= r ? 1 : 0); break; }
                case EQ: { int r = pop(), l = pop(); push(l == r ? 1 : 0); break; }
                case NE: { int r = pop(), l = pop(); push(l != r ? 1 : 0); break; }
                case JMP: cur.pc = in.a(); break;
                case JZ: if (pop() == 0) cur.pc = in.a(); break;
                case JNZ: if (pop() != 0) cur.pc = in.a(); break;
                case CALL: {
                    BcFunction callee = program.functions.get(in.a());
                    if (callStack.size() >= MAX_CALL_DEPTH) {
                        throw new VmException("stack overflow: call depth exceeded " + MAX_CALL_DEPTH
                                + " (infinite recursion?)");
                    }
                    int[] locals = new int[callee.numLocals];
                    for (int i = in.b() - 1; i >= 0; i--) locals[i] = pop();
                    callStack.push(cur);
                    cur = new Frame(callee, locals);
                    break;
                }
                case RET:   // return value stays on the shared stack for the caller
                case RETV:
                    if (callStack.isEmpty()) {
                        out.flush();
                        return;
                    }
                    cur = callStack.pop();
                    break;
                case PRINT_INT: out.print(pop()); break;
                case PRINT_BOOL: out.print(pop() != 0 ? "true" : "false"); break;
                case PRINT_STR: out.print(program.strings.get(in.a())); break;
                case PRINTLN: out.print('\n'); break;
                default: throw new VmException("illegal instruction " + in);
            }
        }
    }

    private void push(int v) {
        if (sp >= STACK_SIZE) throw new VmException("operand stack overflow");
        stack[sp++] = v;
    }

    private int pop() {
        if (sp <= 0) throw new VmException("operand stack underflow (compiler bug)");
        return stack[--sp];
    }
}
