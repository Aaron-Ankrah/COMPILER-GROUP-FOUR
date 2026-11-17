package compiler.semantic;

/** MiniLang types. ERROR is used internally to avoid cascading error messages. */
public enum Type { 
    INT, BOOL, VOID, STRING, ERROR;

    public static Type fromName(String name) {
        switch (name) {
            case "int": return INT;
            case "bool": return BOOL;
            case "void": return VOID;
            default: return ERROR;
        }
    }

    @Override
    public String toString() {
        return this == ERROR ? "<error>" : name().toLowerCase();
    }
}
