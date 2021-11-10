package ru.akiselev.paymentservice.persistence;

import org.junit.jupiter.api.Test;
import ru.akiselev.paymentservice.persistence.generator.HashKeyGenerator;

class HashKeyGeneratorTest {

    @Test
    void next() {
        System.out.println(new HashKeyGenerator().next());
    }

    @Test
    void next2() {
        System.out.println(new HashKeyGenerator().next());
    }
}