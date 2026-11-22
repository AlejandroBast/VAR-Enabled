package com.varenabled.aplicacion.futbol;

import com.varenabled.aplicacion.AlertaPartido;
import com.varenabled.aplicacion.PublicadorAlertasPort;
import com.varenabled.aplicacion.PublicadorMarcadorPort;
import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.EventoInexistenteException;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import com.varenabled.dominio.apuestas.EstadoApuestasPartido;
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
    private final IdEvento idOtroEvento = new IdEvento("evt_a1");

    private void sinPartidoGuardado() {
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.empty());
    }

    private void conPartidoGuardado(PartidoFutbol partido) {
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.of(partido));
    }

    private PartidoFutbol partidoPublicado() {
        ArgumentCaptor<PartidoFutbol> captor = ArgumentCaptor.forClass(PartidoFutbol.class);
        verify(publicadorMarcador).publicarMarcador(captor.capture());
        return captor.getValue();
    }

    private AlertaPartido alertaPublicada() {
        ArgumentCaptor<AlertaPartido> captor = ArgumentCaptor.forClass(AlertaPartido.class);
        verify(publicadorAlertas).publicarAlerta(captor.capture());
        return captor.getValue();
    }

    @Test
    void procesarCreacionPublicaElPartidoPorJugarConSusEquiposSinAvisar() {
        sinPartidoGuardado();

        servicio.procesarCreacion(idPartido, "Nacional", "América");

        PartidoFutbol partido = partidoPublicado();
        assertEquals(EstadoPartido.POR_JUGAR, partido.obtenerEstado());
        assertEquals("Nacional", partido.obtenerEquipoLocal());
        assertEquals(Marcador.enCero(), partido.obtenerMarcador());
        verify(publicadorAlertas, never()).publicarAlerta(any());
    }

    @Test
    void procesarInicioPasaElPartidoAEnJuegoYAvisa() {
        PartidoFutbol programado = new PartidoFutbol(idPartido);
        programado.programar();
        conPartidoGuardado(programado);

        servicio.procesarInicio(idPartido, idEvento, 1, 0);

        assertEquals(EstadoPartido.EN_JUEGO, partidoPublicado().obtenerEstado());
        assertEquals("MATCH_STARTED", alertaPublicada().tipoEvento());
    }

    @Test
    void procesarGolSumaYAvisaConElMinuto() {
        sinPartidoGuardado();

        servicio.procesarGol(idPartido, idEvento, Equipo.LOCAL, 23, 0);

        PartidoFutbol partido = partidoPublicado();
        assertEquals(new Marcador(1, 0), partido.obtenerMarcador());
        assertEquals(23, partido.obtenerMinutoActual());
        AlertaPartido alerta = alertaPublicada();
        assertEquals("GOAL", alerta.tipoEvento());
        assertEquals(Equipo.LOCAL, alerta.equipo());
    }

    @Test
    void procesarGolAnuladoRestaYAvisaQueGolSeAnulo() {
        PartidoFutbol partidoExistente = new PartidoFutbol(idPartido);
        partidoExistente.anotarGol(idEvento, Equipo.LOCAL);
        conPartidoGuardado(partidoExistente);

        servicio.procesarGolAnulado(idPartido, idOtroEvento, idEvento, 25, 0);

        assertEquals(Marcador.enCero(), partidoPublicado().obtenerMarcador());
        AlertaPartido alerta = alertaPublicada();
        assertEquals("GOAL_ANNULLED", alerta.tipoEvento());
        assertEquals(idEvento, alerta.idEventoAnulado());
    }

    @Test
    void procesarGolAnuladoDeUnEventoInexistenteNoGuardaNiPublicaNada() {
        conPartidoGuardado(new PartidoFutbol(idPartido));

        assertThrows(EventoInexistenteException.class,
                () -> servicio.procesarGolAnulado(idPartido, idOtroEvento, idEvento, 25, 0));

        verify(repositorio, never()).guardar(any());
        verify(publicadorMarcador, never()).publicarMarcador(any());
        verify(publicadorAlertas, never()).publicarAlerta(any());
    }

    @Test
    void procesarFaltaLaCuentaParaSuEquipo() {
        sinPartidoGuardado();

        servicio.procesarFalta(idPartido, idEvento, Equipo.VISITANTE, 11, 0);

        assertEquals(new Marcador(0, 1), partidoPublicado().obtenerFaltas());
        assertEquals("FOUL", alertaPublicada().tipoEvento());
    }

    @Test
    void procesarFaltaAnuladaLaDescuentaYAvisa() {
        PartidoFutbol partidoExistente = new PartidoFutbol(idPartido);
        partidoExistente.registrarFalta(idEvento, Equipo.LOCAL);
        conPartidoGuardado(partidoExistente);

        servicio.procesarFaltaAnulada(idPartido, idOtroEvento, idEvento, 12, 0);

        assertEquals(Marcador.enCero(), partidoPublicado().obtenerFaltas());
        assertEquals("FOUL_ANNULLED", alertaPublicada().tipoEvento());
    }

    @Test
    void procesarTarjetaAvisaElColorYElEquipo() {
        sinPartidoGuardado();

        servicio.procesarTarjeta(idPartido, idEvento, "RED_CARD", Equipo.LOCAL, 60, 0);

        AlertaPartido alerta = alertaPublicada();
        assertEquals("RED_CARD", alerta.tipoEvento());
        assertEquals("Tarjeta roja para el equipo LOCAL", alerta.mensaje());
    }

    @Test
    void procesarRevisionVarPoneElPartidoEnRevisionYDiceQueSeRevisa() {
        sinPartidoGuardado();

        servicio.procesarRevisionVar(idPartido, idOtroEvento, idEvento, 30, 0);

        assertEquals(EstadoPartido.EN_REVISION, partidoPublicado().obtenerEstado());
        AlertaPartido alerta = alertaPublicada();
        assertEquals("VAR_CHECK", alerta.tipoEvento());
        assertEquals(idEvento, alerta.idEventoRevisado());
    }

    @Test
    void procesarJugadaConfirmadaVuelveAEnJuego() {
        PartidoFutbol enRevision = new PartidoFutbol(idPartido);
        enRevision.iniciarRevisionVar();
        conPartidoGuardado(enRevision);

        servicio.procesarJugadaConfirmada(idPartido, idOtroEvento, idEvento, 31, 0);

        assertEquals(EstadoPartido.EN_JUEGO, partidoPublicado().obtenerEstado());
        assertEquals(idEvento, alertaPublicada().idEventoRevisado());
    }

    @Test
    void procesarEntretiempoYSegundoTiempo() {
        PartidoFutbol partido = new PartidoFutbol(idPartido);
        conPartidoGuardado(partido);

        servicio.procesarEntretiempo(idPartido, idEvento, 45, 2);
        assertEquals(EstadoPartido.ENTRETIEMPO, partido.obtenerEstado());

        servicio.procesarSegundoTiempo(idPartido, idOtroEvento, 46, 0);
        assertEquals(EstadoPartido.EN_JUEGO, partido.obtenerEstado());
        assertEquals(46, partido.obtenerMinutoActual());
    }

    @Test
    void procesarFinalDejaElPartidoFinalizado() {
        sinPartidoGuardado();

        servicio.procesarFinal(idPartido, idEvento, 90, 3);

        assertEquals(EstadoPartido.FINALIZADO, partidoPublicado().obtenerEstado());
        assertEquals("MATCH_ENDED", alertaPublicada().tipoEvento());
    }

    @Test
    void procesarTiempoAnadidoAvisaCuantosMinutosSeAgregan() {
        sinPartidoGuardado();

        servicio.procesarTiempoAnadido(idPartido, idEvento, 45, 3);

        assertEquals("Se añaden 3 minutos", alertaPublicada().mensaje());
    }

    @Test
    void actualizarMinutoPublicaElMarcadorSinAvisar() {
        sinPartidoGuardado();

        servicio.actualizarMinuto(idPartido, 1, 0);

        assertEquals(1, partidoPublicado().obtenerMinutoActual());
        verify(publicadorAlertas, never()).publicarAlerta(any());
    }

    @Test
    void lasApuestasSeAbrenAlEmpezarSeCierranConUnGolYSeDetienenEnLaRevision() {
        PartidoFutbol partido = new PartidoFutbol(idPartido);
        conPartidoGuardado(partido);

        servicio.procesarInicio(idPartido, idEvento, 1, 0);
        assertEquals(EstadoApuestasPartido.ABIERTAS, partido.obtenerApuestas());

        servicio.procesarGol(idPartido, idEvento, Equipo.LOCAL, 10, 0);
        assertEquals(EstadoApuestasPartido.CERRADAS, partido.obtenerApuestas());

        servicio.procesarRevisionVar(idPartido, idOtroEvento, idEvento, 11, 0);
        assertEquals(EstadoApuestasPartido.DETENIDAS, partido.obtenerApuestas());

        servicio.procesarJugadaConfirmada(idPartido, new IdEvento("evt_c1"), idEvento, 11, 0);
        assertEquals(EstadoApuestasPartido.ABIERTAS, partido.obtenerApuestas());
    }

    @Test
    void unGolQueQuedaFirmeReabreLasApuestasSinAvisar() {
        PartidoFutbol partido = new PartidoFutbol(idPartido);
        partido.cambiarApuestas(EstadoApuestasPartido.CERRADAS);
        conPartidoGuardado(partido);

        servicio.procesarGolConfirmado(idPartido, 13, 0);

        assertEquals(EstadoApuestasPartido.ABIERTAS, partidoPublicado().obtenerApuestas());
        verify(publicadorAlertas, never()).publicarAlerta(any());
    }
}
