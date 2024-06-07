package com.entrevistador.notificaciones.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseWebFluxService implements SseService {

    private final Map<String, Sinks.Many<ServerSentEvent<String>>> userSinks = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> generarNotificacion(String userId, NotifiacionDto notifiacion) {
        return convertirDtoAStringJson(notifiacion)
                .flatMap(json -> emitirEvento(userId, ServerSentEvent.<String>builder().data(json).build()));
    }

    public Flux<ServerSentEvent<String>> getEventStream(String userId) {
        Sinks.Many<ServerSentEvent<String>> sink = userSinks.computeIfAbsent(userId, id -> Sinks.many().multicast().onBackpressureBuffer());
        var evento = sink.asFlux().publish().autoConnect();
        notificarConexion(userId);
        return evento;
    }

    private Disposable notificarConexion(String userId) {
        return Mono.delay(Duration.ofSeconds(2))
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

    private Mono<Void> emitirEvento(String userId, ServerSentEvent<String> event) {
        Sinks.Many<ServerSentEvent<String>> sink = userSinks.get(userId);
        if (sink != null) {
            sink.tryEmitNext(event);
        }
        return Mono.empty();
    }

    public Mono<Void> removeUserSink(String userId) {
        Sinks.Many<ServerSentEvent<String>> sink = userSinks.remove(userId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
        return Mono.empty();
    }
}
