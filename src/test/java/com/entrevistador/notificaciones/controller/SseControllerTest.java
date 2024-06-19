package com.entrevistador.notificaciones.controller;

import com.entrevistador.notificaciones.dto.NotifiacionDto;
import com.entrevistador.notificaciones.enums.TipoNotificacionEnum;
import com.entrevistador.notificaciones.service.SseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {SseController.class})
class SseControllerTest {
    private final StringBuilder URL = new StringBuilder("/v1/eventos");

    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private SseService sseService;

    @Test
    void shouldSubscribeByUserId_WhenGet() {
        Flux<ServerSentEvent<String>> eventStream = Flux.just(
                ServerSentEvent.builder("any1").build(),
                ServerSentEvent.builder("any2").build(),
                ServerSentEvent.builder("any3").build()
        );

        when(this.sseService.getEventStream(anyString())).thenReturn(eventStream);

        Flux<ServerSentEvent<String>> responseBody = this.webTestClient
                .get()
                .uri(URL.append("/subscribe/{userId}").toString(), 1)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .getResponseBody();

        StepVerifier
                .create(responseBody)
                .assertNext(event -> assertThat(event.data()).isEqualTo("any1"))
                .assertNext(event -> assertThat(event.data()).isEqualTo("any2"))
                .assertNext(event -> assertThat(event.data()).isEqualTo("any3"))
                .thenCancel()
                .verify();

        verify(this.sseService, times(1)).getEventStream(anyString());
        verify(this.sseService, times(1)).removeUserSink(anyString());
    }

    @Test
    void shouldSubscribeByUsername_WhenGet() {
        NotifiacionDto notifiacionDto = NotifiacionDto.builder().mensaje("any").tipo(TipoNotificacionEnum.NF).build();

        when(this.sseService.generarNotificacion(anyString(), any())).thenReturn(Mono.empty());

        this.webTestClient
                .post()
                .uri(URL.append("/enviar/{username}").toString(), "any")
                .body(Mono.just(notifiacionDto), NotifiacionDto.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Void.class);

        verify(this.sseService, times(1)).generarNotificacion(anyString(), any());
    }
}