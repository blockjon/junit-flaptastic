package com.github.blockjon.flaptastic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({FlaptasticDisableableExtension.class})
class MainTest {

    @Test
    void testMyTest1() {
        assertEquals(1, 2);
    }

    @Test
    void justAnExample2() {
        assertEquals(1, 1);
    }
}
