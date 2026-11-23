export type EstadoPartido = 'POR_JUGAR' | 'EN_JUEGO' | 'EN_REVISION' | 'ENTRETIEMPO' | 'FINALIZADO';

/** Lo que manda dashboard_backend cuando cambia el marcador (ActualizacionMarcadorDto). */
export interface Marcador {
  match_id: string;
  local: number;
  visitante: number;
  minuto: number;
  minuto_extra: number;
  estado: EstadoPartido;
  equipo_local: string | null;
  equipo_visitante: string | null;
  faltas_local: number;
  faltas_visitante: number;
  apuestas: 'ABIERTAS' | 'CERRADAS' | 'DETENIDAS' | null;
}

/** Lo que manda dashboard_backend por cada evento del partido (AlertaPartidoDto). */
export interface EventoPartido {
  match_id: string;
  event_id: string | null;
  event_type: string;
  team: 'home' | 'away' | null;
  annuls_event_id: string | null;
  reviews_event_id: string | null;
  minuto: number;
  minuto_extra: number;
  mensaje: string;
}
