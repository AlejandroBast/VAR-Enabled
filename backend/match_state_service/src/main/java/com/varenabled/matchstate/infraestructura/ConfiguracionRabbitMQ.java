package com.varenabled.matchstate.infraestructura;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionRabbitMQ {

    public static final String EXCHANGE_MARCADOR = "live_updates";
    public static final String EXCHANGE_ALERTAS = "live_alerts";

    @Bean
    public TopicExchange exchangeMarcador() {
        return new TopicExchange(EXCHANGE_MARCADOR);
    }

    @Bean
    public FanoutExchange exchangeAlertas() {
        return new FanoutExchange(EXCHANGE_ALERTAS);
    }

    @Bean
    public MessageConverter conversorJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
