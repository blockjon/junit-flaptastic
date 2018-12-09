package com.github.blockjon.flaptastic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
// import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith({ Disableable.class })
class MainTest {

    @Test
    void testMyTest1() {
        System.out.println("This test method should be run");
        assertEquals(2, 2);
    }

    @Test
    void justAnExample2() {
        System.out.println("A faulty test");
        assertEquals(1, 1);
    }
}
