package cl.duoc.guia_service.producer.service;

import cl.duoc.guia_service.shared.events.GuiaCreadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GuiaProducerService {

    private static final Logger log = LoggerFactory.getLogger(GuiaProducerService.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public GuiaProducerService(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange}") String exchange,
            @Value("${app.rabbitmq.routing-key}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void enviarGuia(GuiaCreadaEvent event) {
        log.info("Publicando evento en RabbitMQ. eventId={}, numeroGuia={}", event.eventId(), event.numeroGuia());
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}