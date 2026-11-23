import { NgTemplateOutlet } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { EstadoPartido, EventoPartido, Marcador } from './partido.model';
import { SimulacionCancha } from './simulacion/simulacion-cancha';

interface MensajeDashboard {
  tipo: 'marcador' | 'evento';
  datos: Marcador | EventoPartido;
}

interface Partido {
  marcador: Marcador;
  eventos: EventoPartido[];
}

interface Estadistica {
  nombre: string;
  local: number;
  visitante: number;
  porcentajeLocal: number;
}

interface FilaLineaDeTiempo {
  minuto: string;
  icono: string;
  clase: string;
  titulo: string;
  descripcion: string;
  anulado: boolean;
  /** Lado de la linea de tiempo: local a la izquierda, visitante a la derecha, null al centro. */
  lado: 'home' | 'away' | null;
}

type Pestana = 'campo' | 'eventos' | 'estadisticas';
type FiltroJugadas = 'todas' | 'goles' | 'tarjetas' | 'var';

const CLASES_FILTRO: Record<FiltroJugadas, string[] | null> = {
  todas: null,
  goles: ['gol'],
  tarjetas: ['amarilla', 'roja'],
  var: ['revision', 'confirmada', 'anulacion'],
};

const ETIQUETA_ESTADO: Record<EstadoPartido, string> = {
  POR_JUGAR: 'Por jugar',
  EN_JUEGO: 'En juego',
  EN_REVISION: 'Revisión VAR',
  ENTRETIEMPO: 'Entretiempo',
  FINALIZADO: 'Finalizado',
};

/**
 * Con ng serve (o con el build abierto en otro puerto) el WebSocket esta en el 8081 local.
 * Si la pagina se sirve en el puerto estandar (80 o 443) hay un proxy delante, como Caddy
 * en AWS, y el WebSocket va por el mismo origen: asi tambien funciona con HTTPS (wss).
 */
export function urlDelMarcador(
  ubicacion: Pick<Location, 'protocol' | 'hostname' | 'host' | 'port'>,
): string {
  const protocolo = ubicacion.protocol === 'https:' ? 'wss' : 'ws';
  const destino =
    ubicacion.hostname && ubicacion.port === ''
      ? ubicacion.host
      : `${ubicacion.hostname || 'localhost'}:8081`;
  return `${protocolo}://${destino}/marcador`;
}

@Component({
  selector: 'app-root',
  imports: [NgTemplateOutlet, SimulacionCancha],
  templateUrl: './app.html',
  styleUrls: ['./app.css', './app-paneles.css']
})
export class App implements OnInit, OnDestroy {
  protected readonly partidos = signal<Partido[]>([]);
  protected readonly idSeleccionado = signal<string | null>(null);
  protected readonly conectado = signal(false);
  protected readonly pestana = signal<Pestana>('campo');
  protected readonly soloEnVivo = signal(false);
  protected readonly filtroJugadas = signal<FiltroJugadas>('todas');
  protected readonly enVivo = computed(() => this.partidos().filter((p) => ['EN_JUEGO', 'EN_REVISION'].includes(p.marcador.estado)).length);

  protected readonly partidoSeleccionado = computed(() => {
    const partidos = this.partidos();
    return partidos.find((p) => p.marcador.match_id === this.idSeleccionado()) ?? partidos[0];
  });

  /** Lista de la izquierda, con el filtro "En vivo" aplicado. */
  protected readonly partidosVisibles = computed(() => this.soloEnVivo()
    ? this.partidos().filter((p) => ['EN_JUEGO', 'EN_REVISION'].includes(p.marcador.estado))
    : this.partidos());

  /** Lo mas reciente arriba, como en una transmision en vivo. */
  protected readonly lineaDeTiempo = computed(() => {
    const partido = this.partidoSeleccionado();
    return partido ? partido.eventos.map((evento) => this.describir(evento, partido)).reverse() : [];
  });

  /** Ultimas jugadas importantes para la tarjeta "Jugadas destacadas". */
  protected readonly jugadasDestacadas = computed(() => {
    const clases = CLASES_FILTRO[this.filtroJugadas()] ?? ['gol', 'amarilla', 'roja', 'revision', 'confirmada', 'anulacion', 'falta'];
    return this.lineaDeTiempo().filter((f) => clases.includes(f.clase)).slice(0, 5);
  });

  /** Comparativa local vs visitante para el panel de estadisticas. */
  protected readonly estadisticas = computed<Estadistica[]>(() => {
    const partido = this.partidoSeleccionado();
    if (!partido) return [];
    const { marcador, eventos } = partido;
    const contar = (tipo: string, team: 'home' | 'away') => eventos.filter((e) => e.event_type === tipo && e.team === team).length;
    const fila = (nombre: string, local: number, visitante: number): Estadistica => {
      const total = local + visitante;
      return { nombre, local, visitante, porcentajeLocal: total ? (local / total) * 100 : 50 };
    };
    return [
      fila('Goles', marcador.local, marcador.visitante),
      fila('Faltas', marcador.faltas_local, marcador.faltas_visitante),
      fila('Amarillas', contar('YELLOW_CARD', 'home'), contar('YELLOW_CARD', 'away')),
      fila('Rojas', contar('RED_CARD', 'home'), contar('RED_CARD', 'away')),
    ];
  });

  private socket?: WebSocket;
  private reintento?: ReturnType<typeof setTimeout>;

  ngOnInit(): void { this.conectar(); }

  ngOnDestroy(): void {
    if (this.reintento) clearTimeout(this.reintento);
    this.socket?.close();
  }

  protected seleccionar(matchId: string): void {
    this.idSeleccionado.set(matchId);
  }

  protected cambiarPestana(pestana: Pestana): void {
    this.pestana.set(pestana);
  }

  protected alternarEnVivo(): void {
    this.soloEnVivo.update((v) => !v);
  }

  protected filtrarJugadas(filtro: FiltroJugadas): void {
    this.filtroJugadas.set(filtro);
  }

  /** Flechas del partido destacado: pasa al anterior / siguiente de la lista. */
  protected moverDestacado(paso: 1 | -1): void {
    const partidos = this.partidos();
    const actual = this.partidoSeleccionado();
    if (!actual || partidos.length < 2) return;
    const i = partidos.indexOf(actual);
    this.idSeleccionado.set(partidos[(i + paso + partidos.length) % partidos.length].marcador.match_id);
  }

  /** Texto corto de estado para la lista: minuto en vivo, "Final", "Desc."... */
  protected estadoCorto(marcador: Marcador): string {
    switch (marcador.estado) {
      case 'POR_JUGAR': return 'Por jugar';
      case 'ENTRETIEMPO': return 'Descanso';
      case 'FINALIZADO': return 'Final';
      case 'EN_REVISION': return 'VAR';
      default: return this.minutoTexto(marcador.minuto, marcador.minuto_extra);
    }
  }

  protected textoApuestas(marcador: Marcador): string {
    switch (marcador.apuestas) {
      case 'ABIERTAS': return 'Se aceptan apuestas en este partido.';
      case 'DETENIDAS': return 'Suspendidas mientras el VAR revisa la jugada.';
      case 'CERRADAS': return 'El mercado de este partido está cerrado.';
      default: return 'Todavía no hay información de apuestas.';
    }
  }

  protected reconectar(): void {
    if (this.reintento) clearTimeout(this.reintento);
    this.socket?.close();
    this.conectar();
  }

  protected minutoTexto(minuto: number, minutoExtra: number): string {
    return minutoExtra ? `${minuto}+${minutoExtra}'` : `${minuto}'`;
  }

  protected nombreLocal(marcador: Marcador): string {
    return marcador.equipo_local || 'Local';
  }

  protected nombreVisitante(marcador: Marcador): string {
    return marcador.equipo_visitante || 'Visitante';
  }

  protected titulo(marcador: Marcador): string {
    return marcador.equipo_local && marcador.equipo_visitante
      ? `${marcador.equipo_local} vs ${marcador.equipo_visitante}`
      : `Partido ${marcador.match_id}`;
  }

  /** "Atlético Nacional" -> "ATL" */
  protected abreviatura(nombre: string): string {
    return nombre.normalize('NFD').replace(/[^A-Za-z]/g, '').slice(0, 3).toUpperCase();
  }

  protected etiquetaEstado(estado: EstadoPartido): string {
    return ETIQUETA_ESTADO[estado] ?? estado;
  }

  /** "Primer tiempo" / "Segundo tiempo" segun lo que ya paso en el partido. */
  protected fase(partido: Partido): string {
    const estado = partido.marcador.estado;
    if (estado === 'POR_JUGAR') return 'Por jugar';
    if (estado === 'ENTRETIEMPO') return 'Entretiempo';
    if (estado === 'FINALIZADO') return 'Finalizado';
    return partido.eventos.some((e) => e.event_type === 'SECOND_HALF_STARTED') ? 'Segundo tiempo' : 'Primer tiempo';
  }

  protected claseEstado(estado: EstadoPartido): string {
    return { POR_JUGAR: 'por-jugar', EN_JUEGO: 'en-juego', EN_REVISION: 'revision', ENTRETIEMPO: 'entretiempo', FINALIZADO: 'finalizado' }[estado] ?? '';
  }

  private conectar(): void {
    this.socket = new WebSocket(urlDelMarcador(window.location));
    this.socket.onopen = () => {
      // Al conectarse, el backend vuelve a mandar todo lo que ya paso: se arranca de cero.
      this.partidos.set([]);
      this.conectado.set(true);
    };
    this.socket.onmessage = (mensaje) => this.recibir(mensaje.data);
    this.socket.onerror = () => this.socket?.close();
    this.socket.onclose = () => {
      this.conectado.set(false);
      this.reintento = setTimeout(() => this.conectar(), 3000);
    };
  }

  private recibir(texto: string): void {
    try {
      const mensaje = JSON.parse(texto) as MensajeDashboard;
      if (mensaje.tipo === 'marcador') this.actualizarMarcador(mensaje.datos as Marcador);
      if (mensaje.tipo === 'evento') this.agregarEvento(mensaje.datos as EventoPartido);
    } catch { /* Ignora mensajes fuera del contrato. */ }
  }

  private actualizarMarcador(marcador: Marcador): void {
    if (!marcador.match_id) return;
    this.partidos.update((partidos) => {
      const existe = partidos.some((p) => p.marcador.match_id === marcador.match_id);
      if (!existe) return [...partidos, { marcador, eventos: [] }];
      return partidos.map((p) => p.marcador.match_id === marcador.match_id ? { ...p, marcador } : p);
    });
  }

  private agregarEvento(evento: EventoPartido): void {
    if (!evento.match_id) return;
    this.partidos.update((partidos) => partidos.map((p) =>
      p.marcador.match_id === evento.match_id ? { ...p, eventos: [...p.eventos, evento] } : p));
  }

  private nombreDe(team: string | null, marcador: Marcador): string {
    if (team === 'home') return this.nombreLocal(marcador);
    if (team === 'away') return this.nombreVisitante(marcador);
    return '';
  }

  /** "el gol de Nacional del 38'" a partir del id de un gol o una falta. */
  private jugada(eventId: string | null, partido: Partido): string {
    const evento = partido.eventos.find((e) => e.event_id === eventId);
    if (!evento) return 'la jugada';
    const que = evento.event_type === 'GOAL' ? 'el gol' : 'la falta';
    return `${que} de ${this.nombreDe(evento.team, partido.marcador)} del ${this.minutoTexto(evento.minuto, evento.minuto_extra)}`;
  }

  private fila(evento: EventoPartido, icono: string, clase: string, titulo: string, descripcion: string, anulado = false): FilaLineaDeTiempo {
    const conLado = ['GOAL', 'FOUL', 'YELLOW_CARD', 'RED_CARD'].includes(evento.event_type);
    return { minuto: this.minutoTexto(evento.minuto, evento.minuto_extra), icono, clase, titulo, descripcion, anulado,
      lado: conLado ? evento.team : null };
  }

  private describir(evento: EventoPartido, partido: Partido): FilaLineaDeTiempo {
    const equipo = this.nombreDe(evento.team, partido.marcador);
    const fueAnulado = partido.eventos.some((e) => e.annuls_event_id && e.annuls_event_id === evento.event_id);

    switch (evento.event_type) {
      case 'MATCH_STARTED':
        return this.fila(evento, '▶', 'inicio', 'Comienza el partido', 'El reloj empieza a correr.');
      case 'HALF_TIME':
        return this.fila(evento, '⏸', 'inicio', 'Entretiempo', 'Termina el primer tiempo.');
      case 'SECOND_HALF_STARTED':
        return this.fila(evento, '▶', 'inicio', 'Comienza el segundo tiempo', 'El reloj vuelve a correr.');
      case 'MATCH_ENDED':
        return this.fila(evento, '■', 'final', 'Final del partido', 'El árbitro pita el final.');
      case 'GOAL':
        return this.fila(evento, '⚽', 'gol', fueAnulado ? 'Gol (anulado)' : 'Gol', `Gol de ${equipo}.`, fueAnulado);
      case 'FOUL':
        return this.fila(evento, '!', 'falta', fueAnulado ? 'Falta (anulada)' : 'Falta', `Falta de ${equipo}.`, fueAnulado);
      case 'YELLOW_CARD':
        return this.fila(evento, '', 'amarilla', 'Tarjeta amarilla', `Amonestación para ${equipo}.`);
      case 'RED_CARD':
        return this.fila(evento, '', 'roja', 'Tarjeta roja', `Expulsión para ${equipo}.`);
      case 'VAR_CHECK':
        return this.fila(evento, 'VAR', 'revision', 'Revisión VAR',
          `Se revisa ${this.jugada(evento.reviews_event_id, partido)}. Apuestas suspendidas.`);
      case 'MATCH_CONTINUES':
        return this.fila(evento, '✓', 'confirmada', 'El VAR confirma la jugada',
          `Se mantiene ${this.jugada(evento.reviews_event_id, partido)}. El partido sigue.`);
      case 'GOAL_ANNULLED':
        return this.fila(evento, '✕', 'anulacion', 'Gol anulado por el VAR', `Se anula ${this.jugada(evento.annuls_event_id, partido)}.`);
      case 'FOUL_ANNULLED':
        return this.fila(evento, '✕', 'anulacion', 'Falta anulada por el VAR', `Se anula ${this.jugada(evento.annuls_event_id, partido)}.`);
      case 'ADDED_TIME':
        return this.fila(evento, '+', 'tiempo', 'Tiempo añadido', `${evento.mensaje}.`);
      default:
        return this.fila(evento, '•', 'otro', evento.event_type, evento.mensaje);
    }
  }
}
