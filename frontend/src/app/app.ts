import { NgTemplateOutlet } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';

/** Lo que manda dashboard_backend cuando cambia el marcador (ActualizacionMarcadorDto). */
interface Marcador {
  match_id: string;
  local: number;
  visitante: number;
  minuto: number;
  minuto_extra: number;
  estado: 'EN_JUEGO' | 'EN_REVISION' | 'FINALIZADO';
}

/** Lo que manda dashboard_backend por cada evento del partido (AlertaPartidoDto). */
interface EventoPartido {
  match_id: string;
  event_id: string | null;
  event_type: string;
  team: 'home' | 'away' | null;
  annuls_event_id: string | null;
  minuto: number;
  minuto_extra: number;
  mensaje: string;
}

interface MensajeDashboard {
  tipo: 'marcador' | 'evento';
  datos: Marcador | EventoPartido;
}

interface Partido {
  marcador: Marcador;
  eventos: EventoPartido[];
}

interface FilaLineaDeTiempo {
  minuto: string;
  icono: string;
  clase: string;
  titulo: string;
  descripcion: string;
  anulado: boolean;
}

@Component({
  selector: 'app-root',
  imports: [NgTemplateOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  protected readonly partidos = signal<Partido[]>([]);
  protected readonly idSeleccionado = signal<string | null>(null);
  protected readonly conectado = signal(false);

  protected readonly partidoSeleccionado = computed(() => {
    const partidos = this.partidos();
    return partidos.find((p) => p.marcador.match_id === this.idSeleccionado()) ?? partidos[0];
  });

  protected readonly lineaDeTiempo = computed(() => {
    const partido = this.partidoSeleccionado();
    return partido ? partido.eventos.map((evento) => this.describir(evento, partido.eventos)) : [];
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

  protected reconectar(): void {
    if (this.reintento) clearTimeout(this.reintento);
    this.socket?.close();
    this.conectar();
  }

  protected minutoTexto(minuto: number, minutoExtra: number): string {
    return minutoExtra ? `${minuto}+${minutoExtra}'` : `${minuto}'`;
  }

  private conectar(): void {
    const protocolo = window.location.protocol === 'https:' ? 'wss' : 'ws';
    this.socket = new WebSocket(`${protocolo}://${window.location.hostname || 'localhost'}:8081/marcador`);
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

  private describir(evento: EventoPartido, eventos: EventoPartido[]): FilaLineaDeTiempo {
    const minuto = this.minutoTexto(evento.minuto, evento.minuto_extra);
    const equipo = evento.team === 'home' ? 'local' : evento.team === 'away' ? 'visitante' : null;

    switch (evento.event_type) {
      case 'MATCH_STARTED':
        return { minuto, icono: '▶', clase: 'inicio', titulo: 'Comienza el partido', descripcion: 'El reloj empieza a correr.', anulado: false };
      case 'MATCH_ENDED':
        return { minuto, icono: '■', clase: 'final', titulo: 'Final del partido', descripcion: 'El árbitro pita el final.', anulado: false };
      case 'GOAL': {
        const anulado = eventos.some((e) => e.event_type === 'GOAL_ANNULLED' && e.annuls_event_id === evento.event_id);
        return { minuto, icono: '⚽', clase: 'gol', titulo: anulado ? 'Gol (anulado)' : 'Gol',
          descripcion: `Gol del equipo ${equipo}.`, anulado };
      }
      case 'GOAL_ANNULLED': {
        const gol = eventos.find((e) => e.event_type === 'GOAL' && e.event_id === evento.annuls_event_id);
        const detalle = gol
          ? `Se anula el gol del ${gol.team === 'home' ? 'local' : 'visitante'} del minuto ${this.minutoTexto(gol.minuto, gol.minuto_extra)}.`
          : 'Se anula el gol.';
        return { minuto, icono: '✕', clase: 'anulacion', titulo: 'Gol anulado por el VAR', descripcion: detalle, anulado: false };
      }
      case 'VAR_CHECK':
        return { minuto, icono: 'VAR', clase: 'revision', titulo: 'Revisión VAR',
          descripcion: 'El árbitro revisa la jugada. Apuestas suspendidas.', anulado: false };
      case 'MATCH_CONTINUES':
        return { minuto, icono: '✓', clase: 'confirmada', titulo: 'El VAR confirma la jugada',
          descripcion: 'El partido sigue. Apuestas reanudadas.', anulado: false };
      case 'FOUL':
        return { minuto, icono: '!', clase: 'falta', titulo: 'Falta',
          descripcion: equipo ? `Falta del equipo ${equipo}.` : 'Falta.', anulado: false };
      case 'ADDED_TIME':
        return { minuto, icono: '+', clase: 'tiempo', titulo: 'Tiempo añadido', descripcion: `${evento.mensaje}.`, anulado: false };
      default:
        return { minuto, icono: '•', clase: 'otro', titulo: evento.event_type, descripcion: evento.mensaje, anulado: false };
    }
  }
}
