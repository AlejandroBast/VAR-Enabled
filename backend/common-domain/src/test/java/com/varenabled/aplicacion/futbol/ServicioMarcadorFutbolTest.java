package com.varenabled.aplicacion.futbol;

import com.varenabled.aplicacion.AlertaPartido;
import com.varenabled.aplicacion.PublicadorAlertasPort;
import com.varenabled.aplicacion.PublicadorMarcadorPort;
import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.EventoInexistenteException;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import com.varenabled.dominio.futbol.EstadoPartido;
import com.varenabled.dominio.futbol.PartidoFutbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioMarcadorFutbolTest {

    @Mock
    private RepositorioPartidoFutbolPort repositorio;

    @Mock
    private PublicadorMarcadorPort publicadorMarcador;

    @Mock
    private PublicadorAlertasPort publicadorAlertas;

    @InjectMocks
    private ServicioMarcadorFutbol servicio;

    private final IdPartido idPartido = new IdPartido("123");
    private final IdEvento idEvento = new IdEvento("evt_g1");
    private final IdEvento idAnulacion = new IdEvento("evt_a1");

    @Test
    void procesarGolCreaElPartidoCuandoNoExisteYPublicaElMarcadorConElMinuto() {
        // Arrange
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.empty());

        // Act
        servicio.procesarGol(idPartido, idEvento, Equipo.LOCAL, 23, 0);

        // Assert
        verify(repositorio).guardar(any(PartidoFutbol.class));
        verify(publicadorMarcador).publicarMarcador(eq(idPartido), eq(new Marcador(1, 0)), eq(23), eq(0), eq(EstadoPartido.EN_JUEGO));
        AlertaPartido alerta = alertaPublicada();
        assertEquals("GOAL", alerta.tipoEvento());
        assertEquals(idEvento, alerta.idEvento());
        assertEquals(Equipo.LOCAL, alerta.equipo());
        assertEquals(23, alerta.minuto());
    }

    @Test
    void procesarGolAnuladoRevierteElMarcadorYAvisaQueGolSeAnulo() {
        // Arrange
        PartidoFutbol partidoExistente = new PartidoFutbol(idPartido);
        partidoExistente.anotarGol(idEvento, Equipo.LOCAL);
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.of(partidoExistente));

        // Act
        servicio.procesarGolAnulado(idPartido, idAnulacion, idEvento, 25, 0);

        // Assert
        verify(repositorio).guardar(partidoExistente);
        verify(publicadorMarcador).publicarMarcador(idPartido, Marcador.enCero(), 25, 0, EstadoPartido.EN_JUEGO);
        AlertaPartido alerta = alertaPublicada();
        assertEquals("GOAL_ANNULLED", alerta.tipoEvento());
        assertEquals(idAnulacion, alerta.idEvento());
        assertEquals(idEvento, alerta.idEventoAnulado());
    }

    @Test
    void procesarGolAnuladoDeUnEventoInexistenteNoGuardaNiPublicaNada() {
        // Arrange
        PartidoFutbol partidoSinGoles = new PartidoFutbol(idPartido);
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.of(partidoSinGoles));

        // Act + Assert
        assertThrows(EventoInexistenteException.class,
                () -> servicio.procesarGolAnulado(idPartido, idAnulacion, idEvento, 25, 0));

        verify(repositorio, never()).guardar(any());
        verify(publicadorMarcador, never()).publicarMarcador(any(), any(), anyInt(), anyInt(), any());
        verify(publicadorAlertas, never()).publicarAlerta(any());
    }

    @Test
    void procesarRevisionVarPublicaElMarcadorEnRevisionYAvisa() {
        // Arrange
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.empty());

        // Act
        servicio.procesarRevisionVar(idPartido, idEvento, 30, 0);

        // Assert
        verify(publicadorMarcador).publicarMarcador(idPartido, Marcador.enCero(), 30, 0, EstadoPartido.EN_REVISION);
        assertEquals("VAR_CHECK", alertaPublicada().tipoEvento());
    }

    @Test
    void procesarJugadaConfirmadaVuelveAPublicarElMarcadorEnJuegoYAvisa() {
        // Arrange
        PartidoFutbol partidoEnRevision = new PartidoFutbol(idPartido);
        partidoEnRevision.iniciarRevisionVar();
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.of(partidoEnRevision));

        // Act
        servicio.procesarJugadaConfirmada(idPartido, idEvento, 31, 0);

        // Assert
        verify(publicadorMarcador).publicarMarcador(idPartido, Marcador.enCero(), 31, 0, EstadoPartido.EN_JUEGO);
        assertEquals("MATCH_CONTINUES", alertaPublicada().tipoEvento());
    }

    @Test
    void procesarFaltaNoCambiaElMarcadorPeroAvisaConElEquipo() {
        // Arrange
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.empty());

        // Act
        servicio.procesarFalta(idPartido, idEvento, Equipo.VISITANTE, 11, 0);

        // Assert
        verify(publicadorMarcador).publicarMarcador(idPartido, Marcador.enCero(), 11, 0, EstadoPartido.EN_JUEGO);
        AlertaPartido alerta = alertaPublicada();
        assertEquals("FOUL", alerta.tipoEvento());
        assertEquals(Equipo.VISITANTE, alerta.equipo());
    }

    @Test
    void procesarTiempoAnadidoAvisaCuantosMinutosSeAgregan() {
        // Arrange
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.empty());

        // Act
        servicio.procesarTiempoAnadido(idPartido, idEvento, 45, 3);

        // Assert
        AlertaPartido alerta = alertaPublicada();
        assertEquals("ADDED_TIME", alerta.tipoEvento());
        assertEquals(45, alerta.minuto());
        assertEquals("Se anaden 3 minutos", alerta.mensaje());
    }

    @Test
    void actualizarMinutoCreaElPartidoCuandoNoExisteYPublicaElMarcadorEnCeroSinAvisar() {
        // Arrange
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.empty());

        // Act
        servicio.actualizarMinuto(idPartido, 1, 0);

        // Assert
        verify(repositorio).guardar(any(PartidoFutbol.class));
        verify(publicadorMarcador).publicarMarcador(idPartido, Marcador.enCero(), 1, 0, EstadoPartido.EN_JUEGO);
        verify(publicadorAlertas, never()).publicarAlerta(any());
    }

    @Test
    void actualizarMinutoNoCambiaElMarcadorDeUnPartidoConGoles() {
        // Arrange
        PartidoFutbol partidoExistente = new PartidoFutbol(idPartido);
        partidoExistente.anotarGol(idEvento, Equipo.LOCAL);
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.of(partidoExistente));

        // Act
        servicio.actualizarMinuto(idPartido, 46, 0);

        // Assert
        verify(publicadorMarcador).publicarMarcador(idPartido, new Marcador(1, 0), 46, 0, EstadoPartido.EN_JUEGO);
    }

    private AlertaPartido alertaPublicada() {
        ArgumentCaptor<AlertaPartido> captor = ArgumentCaptor.forClass(AlertaPartido.class);
        verify(publicadorAlertas).publicarAlerta(captor.capture());
        return captor.getValue();
    }
}
