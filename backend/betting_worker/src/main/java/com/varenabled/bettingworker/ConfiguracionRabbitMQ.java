package com.varenabled.bettingworker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.varenabled.aplicacion.apuestas.RegistroEstadoApuestas;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionRabbitMQ {

    public static final String COLA_COMANDOS = "betting_commands";

    @Bean
    public Queue colaComandos() {
        return new Queue(COLA_COMANDOS, true);
    }

    @Bean
    public MessageConverter conversorJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RegistroEstadoApuestas registroEstadoApuestas() {
        return new RegistroEstadoApuestas();
    }
}
