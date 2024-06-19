package com.entrevistador.notificaciones.controller;

import com.entrevistador.notificaciones.dto.NotifiacionDto;
import com.entrevistador.notificaciones.service.SseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/eventos")
public class SseController {

    private final SseService sseService;

    public SseController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping(path = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> subscribeToSse(@PathVariable String userId) {
        return sseService.getEventStream(userId)
                .doFinally(signalType -> sseService.removeUserSink(userId)); // Clean up when connection is closed
    }

    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping(path = "/enviar/{username}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<Void> subscribeToSse2(@PathVariable String username,
                                      @RequestBody NotifiacionDto notificacion) {
        return sseService.generarNotificacion(username, notificacion);
    }
}
