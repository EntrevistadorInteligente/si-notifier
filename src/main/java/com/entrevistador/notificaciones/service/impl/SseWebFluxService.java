package com.entrevistador.notificaciones.service.impl;

import com.entrevistador.notificaciones.service.SseService;
import com.entrevistador.notificaciones.dto.NotifiacionDto;
import com.entrevistador.notificaciones.enums.TipoNotificacionEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseWebFluxService implements SseService {

    private final Map<String, Sinks.Many<ServerSentEvent<String>>> userSinks = new ConcurrentHashMap<>();
    private final Map<String, Queue<ServerSentEvent<String>>> pendingEvents = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> generarNotificacion(String userId, NotifiacionDto notifiacion) {
        log.info(notifiacion.toString());
        return convertirDtoAStringJson(notifiacion)
                .flatMap(json -> {
                    ServerSentEvent<String> event = ServerSentEvent.<String>builder().data(json).build();
                    return emitirEvento(userId, event)
                            .doOnError(error -> {
                                log.warn("Error sending event, storing in queue. User: {}, Event: {}", userId, event);
                                pendingEvents.computeIfAbsent(userId, k -> new LinkedList<>()).add(event);
                            })
                            .then();
                });
    }

    public Flux<ServerSentEvent<String>> getEventStream(String userId) {
        Sinks.Many<ServerSentEvent<String>> sink = userSinks.computeIfAbsent(userId, id -> Sinks.many().multicast().onBackpressureBuffer());
        var evento = sink.asFlux().publish().refCount(1);

        notificarConexion(userId);

        Queue<ServerSentEvent<String>> userPendingEvents = pendingEvents.get(userId);
        if (userPendingEvents != null) {
            while (!userPendingEvents.isEmpty()) {
                ServerSentEvent<String> event = userPendingEvents.poll();
                if (event != null) {
                    emitirEvento(userId, event).subscribe();
                }
            }
        }

        return evento;
    }

    private Disposable notificarConexion(String userId) {
        return Mono.delay(Duration.ofSeconds(1))
                .flatMap(t -> convertirDtoAStringJson(NotifiacionDto.builder()
                        .tipo(TipoNotificacionEnum.NF)
                        .mensaje("User " + userId + " connected")
                        .build()))
                .flatMap(json -> emitirEvento(userId, ServerSentEvent.<String>builder().data(json).build()))
                .subscribe();
    }

    private Mono<String> convertirDtoAStringJson(NotifiacionDto notifiacion) {
        return Mono.fromCallable(() -> new ObjectMapper().writeValueAsString(NotifiacionDto.builder()
                .tipo(notifiacion.getTipo())
                .mensaje(notifiacion.getMensaje())
                .build()));
    }

    private Mono<Void> emitirEventoConReintento(String userId, ServerSentEvent<String> event, int retryCount) {
        return Mono.defer(() -> {
            Sinks.Many<ServerSentEvent<String>> sink = userSinks.get(userId);
            if (sink != null) {
                Sinks.EmitResult result = sink.tryEmitNext(event);
                if (result.isFailure()) {
                    if (retryCount < 5) {
                        log.warn("Error emitting event, retrying... Attempt: {}", retryCount);
                        return emitirEventoConReintento(userId, event, retryCount + 1).delayElement(Duration.ofSeconds(1));
                    } else {
                        log.error("Failed to emit event after {} attempts", retryCount);
                    }
                }
            } else {
                log.warn("No active sink found for user: {}", userId);
            }
            return Mono.empty();
        });
    }

    private Mono<Void> emitirEvento(String userId, ServerSentEvent<String> event) {
        return emitirEventoConReintento(userId, event, 0);
    }

    public Mono<Void> removeUserSink(String userId) {
        log.info("cerrando conexion " + userId);
        Sinks.Many<ServerSentEvent<String>> sink = userSinks.remove(userId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
        return Mono.empty();
    }
}
