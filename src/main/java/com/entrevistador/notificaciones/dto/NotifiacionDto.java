package com.entrevistador.notificaciones.dto;

import com.entrevistador.notificaciones.enums.TipoNotificacionEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotifiacionDto {
    private TipoNotificacionEnum tipo;
    private String mensaje;
}
