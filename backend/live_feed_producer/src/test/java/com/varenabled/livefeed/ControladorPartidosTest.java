package com.varenabled.livefeed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.varenabled.contratos.EventoPartidoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControladorPartidosTest {

    private GestorPartidos gestor;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @BeforeEach
    void preparar() {
        gestor = Mockito.mock(GestorPartidos.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorPartidos(gestor))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void crearUnPartidoDevuelve201() throws Exception {
        mockMvc.perform(post("/api/partidos")
                        .contentType("application/json")
                        .content("{\"match_id\":\"101\"}"))
                .andExpect(status().isCreated());

        verify(gestor).crearPartido("101");
    }

    @Test
    void crearUnPartidoSinMatchIdDevuelve400() throws Exception {
        mockMvc.perform(post("/api/partidos")
                        .contentType("application/json")
                        .content("{\"match_id\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarPartidosDevuelveLosCreados() throws Exception {
        when(gestor.listarPartidos()).thenReturn(List.of("101", "202"));

        mockMvc.perform(get("/api/partidos"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"101\",\"202\"]"));
    }

    @Test
    void crearUnEventoParaUnPartidoExistenteDevuelve201() throws Exception {
        EventoPartidoDto evento = new EventoPartidoDto("101", "FOOTBALL", "GOAL", "home", "evt_g_abc123", null, null, Instant.now());
        when(gestor.registrarEvento("101", "GOAL", "home", null, null)).thenReturn(evento);

        mockMvc.perform(post("/api/partidos/101/eventos")
                        .contentType("application/json")
                        .content("{\"event_type\":\"GOAL\",\"team\":\"home\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(evento)));
    }

    @Test
    void crearUnEventoParaUnPartidoInexistenteDevuelve404() throws Exception {
        when(gestor.registrarEvento(eq("999"), any(), any(), any(), any()))
                .thenThrow(new PartidoNoEncontradoException("999"));

        mockMvc.perform(post("/api/partidos/999/eventos")
                        .contentType("application/json")
                        .content("{\"event_type\":\"GOAL\",\"team\":\"home\"}"))
                .andExpect(status().isNotFound());
    }
}
