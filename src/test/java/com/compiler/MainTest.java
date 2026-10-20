package com.compiler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void mainRunsWithoutError() {
        assertDoesNotThrow(() -> Main.main(new String[]{"sample.txt"}));
    }

    @Test
    void mainRunsWithNoArgs() {
        assertDoesNotThrow(() -> Main.main(new String[]{}));
    }
}
