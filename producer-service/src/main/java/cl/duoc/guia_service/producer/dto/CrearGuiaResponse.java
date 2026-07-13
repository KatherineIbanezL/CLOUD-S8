package cl.duoc.guia_service.producer.dto;

public record CrearGuiaResponse(
        String mensaje,
        String numeroGuia,
        String eventId
) {
}