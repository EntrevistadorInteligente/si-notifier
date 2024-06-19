package com.entrevistador.notificaciones.service.impl;

import com.entrevistador.notificaciones.dto.NotifiacionDto;
import com.entrevistador.notificaciones.enums.TipoNotificacionEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class SseWebFluxServiceTest {
    @InjectMocks
    private SseWebFluxService sseWebFluxService;

    private Map<String, Sinks.Many<ServerSentEvent<String>>> userSinks;

    @BeforeEach
    void setUp() {
        this.userSinks = new ConcurrentHashMap<>();
    }

    @Test
    void testGenerarNotificacion() {
        String userId = "any";
        NotifiacionDto notificacion = NotifiacionDto.builder()
                .tipo(TipoNotificacionEnum.NF)
                .mensaje("test message")
                .build();
        String json = "{\"tipo\":\"NF\",\"mensaje\":\"test message\"}";

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
        this.userSinks.put(userId, sink);

        Mono<Void> publisher = this.sseWebFluxService.generarNotificacion(userId, notificacion);

        StepVerifier
                .create(publisher)
                .verifyComplete();

        sink.tryEmitNext(ServerSentEvent.builder(json).build());

        StepVerifier
                .create(sink.asFlux())
                .expectNextMatches(event -> event.data().equals(json))
                .thenCancel()
                .verify();
    }

    @Test
    void testGetEventStream() {
        String userId = "any";
        String json = "{\"tipo\":\"NF\",\"mensaje\":\"User any connected\"}";

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
        this.userSinks.put(userId, sink);

        Flux<ServerSentEvent<String>> publisher = this.sseWebFluxService.getEventStream(userId);

        StepVerifier
                .create(publisher)
                .thenAwait(Duration.ofSeconds(2))
                .expectNextMatches(event -> event.data().equals(json))
                .thenCancel()
                .verify();
    }

    @Test
    void testRemoveUserSink() {
        String userId = "any";

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
        this.userSinks.put(userId, sink);

        Mono<Void> publisher = this.sseWebFluxService.removeUserSink(userId);

        StepVerifier
                .create(publisher)
                .verifyComplete();

        this.userSinks.remove(userId);
        sink.tryEmitComplete();

        assertNull(this.userSinks.get(userId));

        StepVerifier
                .create(sink.asFlux())
                .expectComplete()
                .verify();
    }
}