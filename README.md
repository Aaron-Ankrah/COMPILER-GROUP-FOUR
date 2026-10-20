# Compiler Group Four

Compiler Construction term project for Group Four

This compiler targets the class mini-language and is implemented in Java 21 using Maven. The project covers five phases, each building on the previous one: scanning, parsing, semantic analysis with intermediate representation, code generation, and optimization.

## Prerequisites

- **JDK 21** or newer
- **Apache Maven 3.9+**

Verify your setup:

```
java -version
mvn -version
```

## Build

Compile the project:

```
mvn compile
```

## Test

Run all tests:

```
mvn test
```

## Run

After building, run the compiler from the command line:

```
mvn compile
java -cp target/classes com.compiler.Main <source-file>
```

Or package it as a jar and run:

```
mvn package
java -jar target/compiler-group-four-1.0-SNAPSHOT.jar <source-file>
```

## Project Structure

```
src/
  main/java/com/compiler/    Main source code
  test/java/com/compiler/    Tests
pom.xml                      Maven build configuration
```

As phases are implemented, source code will grow under `com.compiler`. Suggested sub-packages:

- `com.compiler.scanner` - Phase 1: lexical analysis
- `com.compiler.parser` - Phase 2: parsing
- `com.compiler.semantic` - Phase 3: semantic analysis and IR generation
- `com.compiler.codegen` - Phase 4: code generation
- `com.compiler.optimizer` - Phase 5: optimization

## Assignment Phases and Deadlines

| Phase | Description | Deadline |
|-------|-------------|----------|
| 1 | Scanner | 25 October 2026 |
| 2 | Parser | 8 November 2026 |
| 3 | Semantic Analysis / IR | 22 November 2026 |
| 4 | Code Generation | 6 December 2026 |
| 5 | Optimizer and Final Demo | 13 December 2026 |

## Final Demonstration (Phase 5)

The Phase 5 submission requires:

- A working optimizer integrated with the full compiler pipeline.
- A sample program written in the class mini-language.
- Evidence that the compiler successfully compiles and runs the sample program: terminal transcript, screenshot, and short recording as specified in the Phase 5 assignment sheet.
- Documentation in this repository.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the group's workflow, branch naming, commit messages, and pull request process.

