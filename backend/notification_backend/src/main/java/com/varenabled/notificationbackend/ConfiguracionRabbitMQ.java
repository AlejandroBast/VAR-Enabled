package com.varenabled.notificationbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionRabbitMQ {

    public static final String EXCHANGE_ALERTAS = "live_alerts";
    public static final String COLA_ALERTAS = "notification_backend.alertas";

    @Bean
    public FanoutExchange exchangeAlertas() {
        return new FanoutExchange(EXCHANGE_ALERTAS);
    }

    @Bean
    public Queue colaAlertas() {
        return new Queue(COLA_ALERTAS, true);
    }

    @Bean
    public Binding bindingAlertas(Queue colaAlertas, FanoutExchange exchangeAlertas) {
        return BindingBuilder.bind(colaAlertas).to(exchangeAlertas);
    }

    @Bean
    public MessageConverter conversorJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
