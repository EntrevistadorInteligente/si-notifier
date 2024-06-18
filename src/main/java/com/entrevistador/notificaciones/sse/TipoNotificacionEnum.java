package com.entrevistador.notificaciones.sse;

import lombok.Getter;

@Getter
public enum TipoNotificacionEnum {
    NF("NOTIFICACION_FRONT"),
    PG("PREGUNTAS_GENERADAS"),
    FG("FEEDBACK_GENERADO"),
    HG("HOJA_DE_VIDA_GENERADA");

    private String descripcion;

    TipoNotificacionEnum(String descripcion) {
        this.descripcion = descripcion;
    }

}
