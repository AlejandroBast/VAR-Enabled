package com.varenabled.matchstate.infraestructura;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.TopicExchange;
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
}
