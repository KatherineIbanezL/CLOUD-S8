package cl.duoc.guia_service.shared.events;


public record GuiaCreadaEvent(
        String eventId,
        String nombreArchivo,
        String transportista,
        String numeroGuia,
        String estado
) {
}