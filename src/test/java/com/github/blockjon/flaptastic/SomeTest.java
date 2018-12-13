package com.github.blockjon.flaptastic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.ExtendWith;
import com.github.blockjon.flaptastic.FlaptasticDisableableExtension;


class SomeTest {

    @ExtendWith({ FlaptasticDisableableExtension.class })
    @Test
    void testSomethingGreat1() {
        assertEquals(1, 2);
    }

    // @ExtendWith({ FlaptasticDisableableExtension.class })
    @Test
    void testSomething2() {
        assertEquals(1, 1);
    }
}
