package com.reactivespring.controller;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class SinksTest {

    @Test
    void sink() {
        Sinks.Many<Integer> replaySink = Sinks.many().replay().all();

        replaySink.emitNext(1, Sinks.EmitFailureHandler.FAIL_FAST);
        replaySink.emitNext(2, Sinks.EmitFailureHandler.FAIL_FAST);

        Flux<Integer> intFlux = replaySink.asFlux();
        intFlux.subscribe(i-> System.out.println("Subscriber 1 : " + i));

        Flux<Integer> intFlux1 = replaySink.asFlux();
        intFlux1.subscribe(i-> System.out.println("Subscriber 2 : " + i));

        replaySink.tryEmitNext(3);

        Flux<Integer> intFlux2 = replaySink.asFlux();
        intFlux2.subscribe(i-> System.out.println("Subscriber 3 : " + i));
    }

}
