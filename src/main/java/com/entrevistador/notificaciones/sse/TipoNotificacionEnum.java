package com.entrevistador.notificaciones.sse;

import lombok.Getter;

@Getter
public enum TipoNotificacionEnum {
    NF("NOTIFICACION_FRONT"),
    PG("PREGUNTAS_GENERADAS"),
    FG("FEEDBACK_GENERADO");

    private String descripcion;

    TipoNotificacionEnum(String descripcion) {
        this.descripcion = descripcion;
    }

}
