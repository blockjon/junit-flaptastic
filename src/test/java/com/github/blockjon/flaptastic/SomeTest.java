package com.github.blockjon.flaptastic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({ Disableable.class })
class SomeTest {

    @Test
    void testSomething1() {
        System.out.println("This test method should be run");
        assertEquals(2, 2);
    }

    @Test
    void testSomething2() {
        System.out.println("A faulty test");
        assertEquals(1, 1);
    }
}
