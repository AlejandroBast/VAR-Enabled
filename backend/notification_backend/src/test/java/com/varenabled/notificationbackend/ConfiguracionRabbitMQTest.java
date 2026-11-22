package com.varenabled.notificationbackend;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfiguracionRabbitMQTest {

    private final ConfiguracionRabbitMQ configuracion = new ConfiguracionRabbitMQ();

    @Test
    void laColaDeAlertasQuedaEnlazadaAlFanoutDeAlertasEnVivo() {
        FanoutExchange exchange = configuracion.exchangeAlertas();
        Queue cola = configuracion.colaAlertas();

        Binding binding = configuracion.bindingAlertas(cola, exchange);

        assertEquals(ConfiguracionRabbitMQ.EXCHANGE_ALERTAS, binding.getExchange());
        assertEquals(ConfiguracionRabbitMQ.COLA_ALERTAS, binding.getDestination());
        assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
    }
}
