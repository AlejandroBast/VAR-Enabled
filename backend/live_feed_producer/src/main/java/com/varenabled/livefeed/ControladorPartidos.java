package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.futbol.MinutoInvalidoException;
import com.varenabled.dominio.futbol.ReglaPartidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/partidos")
@CrossOrigin(origins = "*")
public class ControladorPartidos {

    private final GestorPartidos gestor;

    public ControladorPartidos(GestorPartidos gestor) {
        this.gestor = gestor;
    }

    @GetMapping
    public List<ResumenPartido> listarPartidos() {
        return gestor.listarPartidos();
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> crearPartido(@RequestBody CrearPartidoRequest request) {
        if (request.matchId() == null || request.matchId().isBlank()) {
            throw new IllegalArgumentException("El matchId no puede ser nulo ni vacio");
        }

        gestor.crearPartido(request.matchId(), request.homeTeam(), request.awayTeam());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("matchId", request.matchId()));
    }

    @PostMapping("/{matchId}/iniciar")
    public EventoPartidoDto iniciarPartido(@PathVariable("matchId") String matchId) {
        return gestor.iniciarPartido(matchId);
    }

    @PostMapping("/{matchId}/segundo-tiempo")
    public EventoPartidoDto iniciarSegundoTiempo(@PathVariable("matchId") String matchId) {
        return gestor.iniciarSegundoTiempo(matchId);
    }

    @GetMapping("/{matchId}/eventos")
    public List<EventoPartidoDto> listarEventos(@PathVariable("matchId") String matchId) {
        return gestor.listarEventos(matchId);
    }

    @PostMapping("/{matchId}/eventos")
    public ResponseEntity<EventoPartidoDto> crearEvento(@PathVariable("matchId") String matchId, @RequestBody CrearEventoRequest request) {
        EventoPartidoDto evento = gestor.registrarEvento(matchId, request.eventType(), request.team(),
                request.annulsEventId(), request.reviewsEventId(), request.minutosAgregados());
        return ResponseEntity.status(HttpStatus.CREATED).body(evento);
    }

    @ExceptionHandler(PartidoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarPartidoNoEncontrado(PartidoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MinutoInvalidoException.class})
    public ResponseEntity<Map<String, String>> manejarSolicitudInvalida(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    /** El evento esta bien formado pero no se puede aplicar ahora (partido sin empezar, revision fuera de tiempo, etc.). */
    @ExceptionHandler({IllegalStateException.class, ReglaPartidoException.class})
    public ResponseEntity<Map<String, String>> manejarEstadoDelPartido(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
