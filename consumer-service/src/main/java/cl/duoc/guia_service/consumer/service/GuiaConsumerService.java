package cl.duoc.guia_service.consumer.service;

import cl.duoc.guia_service.shared.events.GuiaCreadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GuiaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(GuiaConsumerService.class);
    private static final String TRASPORTISTA_ERROR = "ERROR";

    private final DocumentoService documentoService;

    @Value("${app.consumer.simulate-error-for-transportista:false}")
    private boolean simulateErrorForTransportista;

    public GuiaConsumerService(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void recibirMensajeGuia(GuiaCreadaEvent event) {
        log.info("Mensaje recibido desde RabbitMQ. eventId={}, numeroGuia={}, transportista={}",
                event.eventId(), event.numeroGuia(), event.transportista());

        if (simulateErrorForTransportista && event.transportista() != null && event.transportista().equalsIgnoreCase(TRASPORTISTA_ERROR)) {
            log.error("¡Simulación de falla activada! Desviando evento {} a la Dead Letter Queue (DLQ)...", event.eventId());
            throw new IllegalStateException("Error simulado para probar reintentos y Dead Letter Queue");
        }

        log.info("Iniciando procesamiento asíncrono y actualización de estado para guía {}", event.numeroGuia());
        log.info("Archivo en S3={}, Estado inicial del evento={}", event.nombreArchivo(), event.estado());

        try {
            documentoService.procesarEstadoGuia(event.nombreArchivo());
            
            log.info("Guía {} procesada correctamente y registrada en Oracle Cloud.", event.numeroGuia());
            
        } catch (Exception e) {
            log.error("Error crítico al persistir la guía {} en la base de datos: {}", event.numeroGuia(), e.getMessage());
            throw e; // Lanza la excepción para que RabbitMQ la envíe a la DLQ
        }
    }
}