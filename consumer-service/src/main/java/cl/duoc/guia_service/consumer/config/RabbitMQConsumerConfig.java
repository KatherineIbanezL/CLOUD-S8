package cl.duoc.guia_service.consumer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConsumerConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue}")
    private String queueName;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    @Value("${app.rabbitmq.dlx-exchange}")
    private String dlxExchangeName;

    @Value("${app.rabbitmq.dlq}")
    private String dlqName;

    @Value("${app.rabbitmq.dlq-routing-key}")
    private String dlqRoutingKey;

    @Bean
    Queue guiasQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxExchangeName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    DirectExchange guiasExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    Binding guiasBinding() {
        return BindingBuilder.bind(guiasQueue()).to(guiasExchange()).with(routingKey);
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxExchangeName);
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(dlqRoutingKey);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter("cl.duoc.guia_service.shared.events");
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            final SimpleRabbitListenerContainerFactoryConfigurer configurer,
            final ConnectionFactory connectionFactory,
            final MessageConverter jsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false); // Si falla, va directo a la DLQ sin hacer bucles
        return factory;
    }
}