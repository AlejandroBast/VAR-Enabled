package com.varenabled.dashboardbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfiguracionRabbitMQTest {

    @Test
    void cadaColaQuedaUnidaASuExchange() {
        try (AnnotationConfigApplicationContext contexto = new AnnotationConfigApplicationContext()) {
            contexto.registerBean(ObjectMapper.class);
            contexto.register(ConfiguracionRabbitMQ.class);
            contexto.refresh();

            Map<String, Binding> bindings = contexto.getBeansOfType(Binding.class);

            Binding marcador = bindings.get("bindingMarcador");
            assertEquals(ConfiguracionRabbitMQ.COLA_MARCADOR, marcador.getDestination());
            assertEquals(ConfiguracionRabbitMQ.EXCHANGE_MARCADOR, marcador.getExchange());

            Binding alertas = bindings.get("bindingAlertas");
            assertEquals(ConfiguracionRabbitMQ.COLA_ALERTAS, alertas.getDestination());
            assertEquals(ConfiguracionRabbitMQ.EXCHANGE_ALERTAS, alertas.getExchange());
        }
    }
}
