package compiler.codegen;

/** A runtime error raised by the virtual machine (e.g. division by zero). */
@SuppressWarnings("serial")
public class VmException extends RuntimeException {
    public VmException(String message) {
        super(message);
    }
}
