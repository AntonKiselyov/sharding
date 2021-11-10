package ru.akiselev.paymentservice.persistence.generator;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@Scope("prototype")
public class HashKeyGenerator {

    private final AtomicLong id = new AtomicLong(0L);

    public long next() {
        final long value = id.incrementAndGet();
        System.out.println(value);
        return value;
    }
}
