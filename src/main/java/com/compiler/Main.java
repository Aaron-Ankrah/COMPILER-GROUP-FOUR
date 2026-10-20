package com.compiler;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: compiler <source-file>");
            System.out.println("Compiler Group Four - no phases implemented yet.");
            return;
        }

        String sourceFile = args[0];
        System.out.println("Compiler Group Four");
        System.out.println("Source file: " + sourceFile);
        System.out.println("No compiler phases implemented yet.");
    }
}
