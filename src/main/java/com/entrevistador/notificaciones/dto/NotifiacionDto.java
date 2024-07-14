package com.entrevistador.notificaciones.dto;

import com.entrevistador.notificaciones.enums.TipoNotificacionEnum;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class NotifiacionDto {
    private TipoNotificacionEnum tipo;
    private String mensaje;
}
