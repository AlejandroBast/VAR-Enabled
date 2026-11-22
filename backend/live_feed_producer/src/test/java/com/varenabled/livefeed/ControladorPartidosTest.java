package com.varenabled.livefeed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.futbol.MinutoInvalidoException;
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
    void listarPartidosDevuelveSuEstadoYMinuto() throws Exception {
        when(gestor.listarPartidos()).thenReturn(List.of(
                new ResumenPartido("101", "EN_JUEGO", 23, 0),
                new ResumenPartido("202", "POR_JUGAR", null, null)));

        mockMvc.perform(get("/api/partidos"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"match_id\":\"101\",\"estado\":\"EN_JUEGO\",\"minuto\":23,\"minuto_extra\":0},"
                        + "{\"match_id\":\"202\",\"estado\":\"POR_JUGAR\"}]"));
    }

    @Test
    void iniciarUnPartidoDevuelveElEventoDeInicio() throws Exception {
        EventoPartidoDto inicio = new EventoPartidoDto("101", "FOOTBALL", "MATCH_STARTED", null, "evt_i_1", null, 1, 0, null, Instant.now());
        when(gestor.iniciarPartido("101")).thenReturn(inicio);

        mockMvc.perform(post("/api/partidos/101/iniciar"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(inicio)));
    }

    @Test
    void iniciarUnPartidoYaIniciadoDevuelve409() throws Exception {
        when(gestor.iniciarPartido("101")).thenThrow(new IllegalStateException("El partido 101 ya empezó"));

        mockMvc.perform(post("/api/partidos/101/iniciar"))
                .andExpect(status().isConflict());
    }

    @Test
    void crearUnEventoSinMinutoDevuelve201ConElMinutoDelReloj() throws Exception {
        EventoPartidoDto evento = new EventoPartidoDto("101", "FOOTBALL", "GOAL", "home", "evt_g_abc123", null, 23, 0, null, Instant.now());
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

    @Test
    void crearUnEventoEnUnPartidoQueNoEmpezoDevuelve409() throws Exception {
        when(gestor.registrarEvento(eq("101"), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("El partido 101 todavía no empezó"));

        mockMvc.perform(post("/api/partidos/101/eventos")
                        .contentType("application/json")
                        .content("{\"event_type\":\"GOAL\",\"team\":\"home\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void anunciarTiempoAnadidoFueraDeLugarDevuelve400() throws Exception {
        when(gestor.registrarEvento(eq("101"), any(), any(), any(), any()))
                .thenThrow(new MinutoInvalidoException("El tiempo añadido solo se puede anunciar durante el minuto 45 o el 90"));

        mockMvc.perform(post("/api/partidos/101/eventos")
                        .contentType("application/json")
                        .content("{\"event_type\":\"ADDED_TIME\",\"minutos_agregados\":3}"))
                .andExpect(status().isBadRequest());
    }
}
