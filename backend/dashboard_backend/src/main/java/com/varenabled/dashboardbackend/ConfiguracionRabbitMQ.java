package com.varenabled.dashboardbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionRabbitMQ {

    public static final String EXCHANGE_MARCADOR = "live_updates";
    public static final String COLA_MARCADOR = "dashboard_backend.marcador";
    public static final String PATRON_ROUTING_KEY = "score.*";

    @Bean
    public TopicExchange exchangeMarcador() {
        return new TopicExchange(EXCHANGE_MARCADOR);
    }

    @Bean
    public Queue colaMarcador() {
        return new Queue(COLA_MARCADOR, true);
    }

    @Bean
    public Binding bindingMarcador(Queue colaMarcador, TopicExchange exchangeMarcador) {
        return BindingBuilder.bind(colaMarcador).to(exchangeMarcador).with(PATRON_ROUTING_KEY);
    }

    @Bean
    public MessageConverter conversorJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
