package cl.duoc.guia_service.producer.dto;

import javax.validation.constraints.NotBlank;

public record CrearGuiaRequest(
        @NotBlank(message = "El nombre del archivo es obligatorio")
        String nombreArchivo,

        @NotBlank(message = "El transportista es obligatorio")
        String transportista
) {
}