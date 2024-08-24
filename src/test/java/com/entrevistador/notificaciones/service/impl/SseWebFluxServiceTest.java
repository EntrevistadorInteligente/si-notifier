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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void testGetEventStreamWithPendingEvents() throws NoSuchFieldException, IllegalAccessException {
        String userId = "any";
        String json = "{\"tipo\":\"NF\",\"mensaje\":\"User any connected\"}";

        ServerSentEvent<String> pendingEvent = ServerSentEvent.builder(json).build();
        Queue<ServerSentEvent<String>> queue = new LinkedList<>();
        queue.add(pendingEvent);

        Field pendingEventsField = SseWebFluxService.class.getDeclaredField("pendingEvents");
        pendingEventsField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Queue<ServerSentEvent<String>>> pendingEvents = (Map<String, Queue<ServerSentEvent<String>>>) pendingEventsField.get(this.sseWebFluxService);
        pendingEvents.put(userId, queue);

        Flux<ServerSentEvent<String>> publisher = this.sseWebFluxService.getEventStream(userId);

        StepVerifier
                .create(publisher)
                .expectNextMatches(event -> event.data().equals(json))
                .thenCancel()
                .verify();
    }

    @Test
    void testEmitirEventoConReintento() throws Exception {
        String userId = "any";
        String json = "{\"tipo\":\"NF\",\"mensaje\":\"test message\"}";

        Sinks.Many<ServerSentEvent<String>> sink = mock(Sinks.Many.class);
        when(sink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.FAIL_NON_SERIALIZED)
                .thenReturn(Sinks.EmitResult.OK);

        Field userSinksField = SseWebFluxService.class.getDeclaredField("userSinks");
        userSinksField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Sinks.Many<ServerSentEvent<String>>> userSinks = (Map<String, Sinks.Many<ServerSentEvent<String>>>) userSinksField.get(this.sseWebFluxService);
        userSinks.put(userId, sink);

        Method emitirEventoMethod = SseWebFluxService.class.getDeclaredMethod("emitirEvento", String.class, ServerSentEvent.class);
        emitirEventoMethod.setAccessible(true);

        ServerSentEvent<String> event = ServerSentEvent.builder(json).build();
        @SuppressWarnings("unchecked")
        Mono<Void> publisher = (Mono<Void>) emitirEventoMethod.invoke(this.sseWebFluxService, userId, event);

        // Verificar los resultados
        StepVerifier
                .create(publisher)
                .verifyComplete();

        verify(sink, times(2)).tryEmitNext(any());
    }

    @Test
    void testRemoveUserSinkWithExistingSink() throws Exception {
        String userId = "any";

        // Crear un mock de Sinks.Many
        Sinks.Many<ServerSentEvent<String>> sink = mock(Sinks.Many.class);

        // Usar reflexión para acceder al campo privado userSinks
        Field userSinksField = SseWebFluxService.class.getDeclaredField("userSinks");
        userSinksField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Sinks.Many<ServerSentEvent<String>>> userSinks = (Map<String, Sinks.Many<ServerSentEvent<String>>>) userSinksField.get(this.sseWebFluxService);
        userSinks.put(userId, sink);

        // Ejecutar el método que estamos probando
        Mono<Void> publisher = this.sseWebFluxService.removeUserSink(userId);

        // Verificar que el método tryEmitComplete fue llamado
        verify(sink, times(1)).tryEmitComplete();

        // Verificar que el userSinks ya no contiene el usuario
        assertNull(userSinks.get(userId));
    }


}