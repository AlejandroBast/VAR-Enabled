package com.varenabled.aplicacion.futbol;

import com.varenabled.aplicacion.PublicadorAlertasPort;
import com.varenabled.aplicacion.PublicadorMarcadorPort;
import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.EventoInexistenteException;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import com.varenabled.dominio.futbol.PartidoFutbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    void procesarGolCreaElPartidoCuandoNoExisteYPublicaElMarcador() {
        // Arrange
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.empty());

        // Act
        servicio.procesarGol(idPartido, idEvento, Equipo.LOCAL);

        // Assert
        verify(repositorio).guardar(any(PartidoFutbol.class));
        verify(publicadorMarcador).publicarMarcador(eq(idPartido), eq(new Marcador(1, 0)));
        verify(publicadorAlertas).publicarAlerta(eq(idPartido), anyString());
    }

    @Test
    void procesarGolAnuladoRevierteElMarcadorYLoPublica() {
        // Arrange
        PartidoFutbol partidoExistente = new PartidoFutbol(idPartido);
        partidoExistente.anotarGol(idEvento, Equipo.LOCAL);
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.of(partidoExistente));

        // Act
        servicio.procesarGolAnulado(idPartido, idEvento);

        // Assert
        verify(repositorio).guardar(partidoExistente);
        verify(publicadorMarcador).publicarMarcador(idPartido, Marcador.enCero());
        verify(publicadorAlertas).publicarAlerta(eq(idPartido), anyString());
    }

    @Test
    void procesarGolAnuladoDeUnEventoInexistenteNoGuardaNiPublicaNada() {
        // Arrange
        PartidoFutbol partidoSinGoles = new PartidoFutbol(idPartido);
        when(repositorio.buscarPorId(idPartido)).thenReturn(Optional.of(partidoSinGoles));

        // Act + Assert
        assertThrows(EventoInexistenteException.class,
                () -> servicio.procesarGolAnulado(idPartido, idEvento));

        verify(repositorio, never()).guardar(any());
        verify(publicadorMarcador, never()).publicarMarcador(any(), any());
        verify(publicadorAlertas, never()).publicarAlerta(any(), anyString());
    }
}
