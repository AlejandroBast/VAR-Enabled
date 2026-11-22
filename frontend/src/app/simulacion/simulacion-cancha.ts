import { AfterViewInit, Component, ElementRef, OnDestroy, effect, input, untracked, viewChild } from '@angular/core';
import { EstadoPartido, EventoPartido, Marcador } from '../partido.model';

/**
 * Cancha animada, puramente visual. No genera eventos ni habla con el backend:
 * solo se mueve mientras el partido esta EN_JUEGO (es decir, despues de pulsar
 * "Iniciar partido" en el EventMaker) y anima los eventos reales que llegan por
 * el WebSocket (goles, faltas, tarjetas, VAR, tiempo añadido...).
 */

const W = 1000;
const H = 570;
const L = 80, R = 920, T = 78, B = 490;
const CX = 500, CY = (T + B) / 2;
const ARCO = 38;
const COLOR_EQUIPO = ['#1f7cec', '#ee662e'];
const AMARILLA = '#facc15';
const ROJA = '#dc2626';
const VERDE = '#4ade80';
const AMBAR = '#fbbf24';

/** Formacion base (proporciones de la cancha) de local y visitante. */
const FORMACION: [number, number][][] = [
  [[.08, .50], [.26, .20], [.26, .41], [.26, .60], [.25, .80], [.43, .17], [.43, .39], [.44, .61], [.44, .82], [.56, .39], [.57, .61]],
  [[.92, .50], [.74, .20], [.74, .41], [.74, .60], [.75, .80], [.57, .17], [.57, .39], [.56, .61], [.56, .82], [.44, .34], [.43, .67]],
];

interface Jugador {
  equipo: 0 | 1; dorsal: number;
  x: number; y: number; baseX: number; baseY: number; fase: number;
  alpha: number; expulsado: boolean;
}

interface Particula { x: number; y: number; vx: number; vy: number; rot: number; vr: number; color: string; vida: number; }

type TipoEfecto = 'gol' | 'falta' | 'tarjeta' | 'tiempo' | 'var' | 'confirmada' | 'anulada' | 'silbato';

interface Efecto {
  tipo: TipoEfecto;
  inicio: number;
  dur: number;
  equipo: 0 | 1 | null;
  titulo: string;
  sub: string;
  color: string;
  x: number;
  y: number;
  jugador?: Jugador;
  lanzado?: boolean;
}

const DURACION: Record<TipoEfecto, number> = {
  gol: 3400, falta: 1800, tarjeta: 2800, tiempo: 3800, var: 2600, confirmada: 2400, anulada: 2600, silbato: 2200,
};

const OVERLAY: Partial<Record<EstadoPartido, { titulo: string; color: string }>> = {
  POR_JUGAR: { titulo: 'Esperando el inicio del partido', color: '#e5e7eb' },
  EN_REVISION: { titulo: 'Revisión VAR · Juego detenido', color: '#fcd34d' },
  ENTRETIEMPO: { titulo: 'Entretiempo', color: '#93c5fd' },
  FINALIZADO: { titulo: 'Final del partido', color: '#e5e7eb' },
};

const clamp = (v: number, a: number, b: number) => Math.min(b, Math.max(a, v));
const azar = (min: number, max: number) => min + Math.random() * (max - min);
/** Progreso 0..1 de `ms` dentro de la ventana [a, b]. */
const tramo = (ms: number, a: number, b: number) => clamp((ms - a) / (b - a), 0, 1);
const salidaCubica = (p: number) => 1 - Math.pow(1 - p, 3);
const rebote = (p: number) => { const c = 1.70158; return 1 + (c + 1) * Math.pow(p - 1, 3) + c * Math.pow(p - 1, 2); };

@Component({
  selector: 'app-simulacion-cancha',
  template: `<canvas #lienzo class="cancha" [attr.width]="ancho" [attr.height]="alto" role="img"
    aria-label="Simulación visual del partido con jugadores y balón"></canvas>`,
  styles: `
    :host { display: block; margin: 24px auto 0; max-width: 880px; }
    .cancha { aspect-ratio: 1000 / 570; background: #0b1510; border: 2px solid var(--borde-fuerte); border-radius: 10px; display: block; height: auto; width: 100%; }
  `,
})
export class SimulacionCancha implements AfterViewInit, OnDestroy {
  readonly marcador = input.required<Marcador>();
  readonly eventos = input<EventoPartido[]>([]);

  protected readonly ancho = W;
  protected readonly alto = H;

  private readonly lienzo = viewChild.required<ElementRef<HTMLCanvasElement>>('lienzo');
  private ctx!: CanvasRenderingContext2D;
  private raf = 0;
  private ultimoFrame = 0;
  private readonly reducido = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;

  private accion = '';
  private jugadores: Jugador[] = [];
  private balon = { x: CX, y: CY, tx: CX, ty: CY, proximoPase: 0 };
  private arbitro = { x: CX - 50, y: CY - 50 };
  private poseedor: 0 | 1 = 0;
  private balonQuietoHasta = 0;
  private gol: { equipo: 0 | 1; inicio: number; hasta: number; desdeX: number; desdeY: number } | null = null;
  private falta: { agresor: Jugador; victima: Jugador; x: number; y: number; inicio: number; hasta: number } | null = null;
  private expulsados = new Set<string>();
  private anadido: number | null = null;
  private efectos: Efecto[] = [];
  private particulas: Particula[] = [];

  private partidoId: string | null = null;
  private estadoPrevio: EstadoPartido | null = null;
  private vistoDesde = 0;
  private vistos = new Set<string>();

  constructor() {
    this.reiniciar();
    effect(() => {
      const marcador = this.marcador();
      const eventos = this.eventos();
      untracked(() => this.sincronizar(marcador, eventos));
    });
  }

  ngAfterViewInit(): void {
    const ctx = this.lienzo().nativeElement.getContext('2d');
    if (!ctx) return;
    this.ctx = ctx;
    this.raf = requestAnimationFrame(this.frame);
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.raf);
  }

  // =================== Estado ===================

  private reiniciar(): void {
    this.jugadores = FORMACION.flatMap((equipo, e) => equipo.map(([px, py], i) => {
      const x = L + px * (R - L), y = T + py * (B - T);
      const expulsado = this.expulsados.has(`${e}-${i + 1}`);
      return { equipo: e as 0 | 1, dorsal: i + 1, x, y, baseX: x, baseY: y, fase: Math.random() * Math.PI * 2, alpha: expulsado ? 0 : 1, expulsado };
    }));
    this.balon = { x: CX, y: CY, tx: CX, ty: CY, proximoPase: 0 };
    this.gol = null;
    this.falta = null;
  }

  private nombre(team: EventoPartido['team'], m: Marcador): string {
    if (team === 'home') return m.equipo_local || 'Local';
    if (team === 'away') return m.equipo_visitante || 'Visitante';
    return '';
  }

  private textoEstado(m: Marcador): string {
    return {
      POR_JUGAR: 'Inicia el partido desde el EventMaker',
      EN_JUEGO: 'Balón en juego',
      EN_REVISION: 'El VAR está revisando la jugada',
      ENTRETIEMPO: 'Entretiempo',
      FINALIZADO: 'Final del partido',
    }[m.estado];
  }

  private sincronizar(m: Marcador, eventos: EventoPartido[]): void {
    const clave = (e: EventoPartido, i: number) => e.event_id ?? `${i}-${e.event_type}-${e.minuto}`;

    // Cambio de partido: se reconstruye el estado (expulsados, tiempo añadido) sin animar.
    if (m.match_id !== this.partidoId) {
      this.partidoId = m.match_id;
      this.estadoPrevio = m.estado;
      this.vistoDesde = performance.now();
      this.expulsados.clear();
      this.efectos = [];
      this.particulas = [];
      this.anadido = null;
      this.reiniciar();
      this.accion = this.textoEstado(m);
      this.vistos = new Set();
      eventos.forEach((e, i) => { this.vistos.add(clave(e, i)); this.reaccionar(e, m, false); });
      return;
    }

    eventos.forEach((e, i) => {
      const k = clave(e, i);
      if (this.vistos.has(k)) return;
      this.vistos.add(k);
      this.reaccionar(e, m, true);
    });

    if (m.estado !== this.estadoPrevio) {
      if (m.estado === 'ENTRETIEMPO' || m.estado === 'POR_JUGAR') this.reiniciar();
      if (this.estadoPrevio === 'POR_JUGAR' && !this.efectos.length) this.accion = this.textoEstado(m);
      this.estadoPrevio = m.estado;
    }
  }

  private efecto(tipo: TipoEfecto, datos: Partial<Efecto> = {}): void {
    // Un efecto "grande" reemplaza al anterior para que no se encimen.
    // Una falta convive con un efecto grande (y viceversa), pero no con otra falta.
    this.efectos = this.efectos.filter((e) => tipo === 'falta' ? e.tipo !== 'falta' : e.tipo === 'falta');
    this.efectos.push({ tipo, inicio: performance.now(), dur: DURACION[tipo], equipo: null, titulo: '', sub: '', color: '#fff', x: CX, y: CY, ...datos });
  }

  /** Reacciona a un evento real que llego por el WebSocket. */
  private reaccionar(e: EventoPartido, m: Marcador, permitirAnimacion: boolean): void {
    // Al reconectar, el backend reenvia el historial en rafaga: eso no se anima.
    const animar = permitirAnimacion && performance.now() - this.vistoDesde > 1200 && e.minuto >= m.minuto - 1;
    const equipo = this.nombre(e.team, m);
    const eq: 0 | 1 | null = e.team === 'home' ? 0 : e.team === 'away' ? 1 : null;
    const local = m.equipo_local || 'Local', visitante = m.equipo_visitante || 'Visitante';

    switch (e.event_type) {
      case 'MATCH_STARTED':
        this.reiniciar(); this.poseedor = 0; this.anadido = null;
        this.accion = 'Comienza el encuentro';
        if (animar) this.efecto('silbato', { titulo: '¡Comienza el partido!', sub: `${local} vs ${visitante}` });
        break;
      case 'HALF_TIME':
        this.reiniciar(); this.anadido = null;
        this.accion = 'Entretiempo';
        if (animar) this.efecto('silbato', { titulo: 'Fin del primer tiempo', sub: `${local} ${m.local} - ${m.visitante} ${visitante}` });
        break;
      case 'SECOND_HALF_STARTED':
        this.reiniciar(); this.poseedor = 1; this.anadido = null;
        this.accion = 'Comienza el segundo tiempo';
        if (animar) this.efecto('silbato', { titulo: '¡Arranca el segundo tiempo!', sub: `${local} vs ${visitante}` });
        break;
      case 'MATCH_ENDED':
        this.anadido = null;
        this.accion = 'Final del partido';
        if (animar) this.efecto('silbato', { titulo: '¡Final del partido!', sub: `${local} ${m.local} - ${m.visitante} ${visitante}` });
        break;
      case 'GOAL':
        this.accion = `¡GOOOL! ${equipo}`;
        if (animar && eq !== null) this.iniciarGol(eq, equipo);
        break;
      case 'FOUL':
        this.accion = `Falta de ${equipo}`;
        if (animar && eq !== null) this.iniciarFalta(eq, equipo);
        break;
      case 'YELLOW_CARD':
      case 'RED_CARD': {
        const roja = e.event_type === 'RED_CARD';
        this.accion = `Tarjeta ${roja ? 'roja' : 'amarilla'} para ${equipo}`;
        if (eq === null) break;
        const jugador = this.elegirJugador(eq);
        if (roja && jugador) this.expulsar(jugador, animar);
        if (animar) {
          this.efecto('tarjeta', {
            equipo: eq, jugador, color: roja ? ROJA : AMARILLA,
            titulo: roja ? 'TARJETA ROJA' : 'TARJETA AMARILLA',
            sub: `${equipo}${jugador ? ` · Dorsal ${jugador.dorsal}` : ''}${roja ? ' · Expulsado' : ''}`,
            x: jugador?.x ?? this.balon.x, y: jugador?.y ?? this.balon.y,
          });
        }
        break;
      }
      case 'VAR_CHECK':
        this.accion = 'Revisión VAR en curso';
        if (animar) this.efecto('var', { titulo: 'VAR', sub: 'Revisando la jugada' });
        break;
      case 'MATCH_CONTINUES':
        this.accion = 'El VAR confirma la jugada';
        if (animar) this.efecto('confirmada', { titulo: 'EL VAR CONFIRMA', sub: 'Se mantiene la jugada · el partido sigue' });
        break;
      case 'GOAL_ANNULLED':
      case 'FOUL_ANNULLED': {
        const esGol = e.event_type === 'GOAL_ANNULLED';
        this.accion = `${esGol ? 'Gol anulado' : 'Falta anulada'} por el VAR`;
        if (animar) this.efecto('anulada', { titulo: esGol ? 'GOL ANULADO' : 'FALTA ANULADA', sub: 'Decisión del VAR' });
        break;
      }
      case 'ADDED_TIME': {
        const n = Number((e.mensaje || '').match(/\d+/)?.[0]) || null;
        this.anadido = n;
        this.accion = n ? `Se añaden ${n} minutos` : (e.mensaje || 'Tiempo añadido');
        if (animar) this.efecto('tiempo', { titulo: n ? `+${n}` : '+', sub: 'TIEMPO AÑADIDO' });
        break;
      }
      default:
        if (e.mensaje) this.accion = e.mensaje;
    }
  }

  private iniciarGol(eq: 0 | 1, nombre: string): void {
    const ahora = performance.now();
    this.falta = null;
    if (!this.reducido) this.gol = { equipo: eq, inicio: ahora, hasta: ahora + DURACION.gol, desdeX: this.balon.x, desdeY: this.balon.y };
    this.efecto('gol', { equipo: eq, titulo: '¡GOOOL!', sub: nombre, color: COLOR_EQUIPO[eq] });
  }

  private iniciarFalta(eq: 0 | 1, nombre: string): void {
    const ahora = performance.now();
    const { x, y } = this.balon;
    const agresor = this.masCercano(eq, x, y);
    const victima = this.masCercano(eq === 0 ? 1 : 0, x, y);
    if (agresor && victima && !this.reducido) this.falta = { agresor, victima, x, y, inicio: ahora, hasta: ahora + DURACION.falta };
    this.balonQuietoHasta = ahora + DURACION.falta;
    this.efecto('falta', { equipo: eq, titulo: 'FALTA', sub: nombre, color: ROJA, x, y });
  }

  private masCercano(eq: 0 | 1, x: number, y: number): Jugador | undefined {
    const candidatos = this.jugadores.filter((p) => p.equipo === eq && !p.expulsado && p.dorsal > 1);
    return candidatos.sort((a, b) => Math.hypot(a.x - x, a.y - y) - Math.hypot(b.x - x, b.y - y))[0];
  }

  private elegirJugador(eq: 0 | 1): Jugador | undefined {
    return this.masCercano(eq, this.balon.x, this.balon.y);
  }

  private expulsar(j: Jugador, animar: boolean): void {
    this.expulsados.add(`${j.equipo}-${j.dorsal}`);
    j.expulsado = true;
    if (!animar || this.reducido) j.alpha = 0;
  }

  // =================== Bucle de animacion ===================

  private readonly frame = (t: number): void => {
    const dt = this.ultimoFrame ? Math.min(45, t - this.ultimoFrame) : 16;
    this.ultimoFrame = t;
    if (!this.reducido) {
      if (this.marcador().estado === 'EN_JUEGO') this.mover(dt, t);
      this.animarSiempre(dt, t);
    }
    this.dibujar(t);
    this.raf = requestAnimationFrame(this.frame);
  };

  /** Lo que se anima aunque el reloj este detenido: arbitro, expulsados, confeti. */
  private animarSiempre(dt: number, t: number): void {
    const foco = this.efectos.find((e) => e.tipo === 'tarjeta' || e.tipo === 'falta');
    const objetivo = foco
      ? { x: (foco.jugador?.x ?? foco.x) + 34, y: (foco.jugador?.y ?? foco.y) - 24 }
      : { x: this.balon.x - 55, y: this.balon.y - 48 };
    const k = Math.min(1, dt * .003);
    this.arbitro.x += (clamp(objetivo.x, L + 10, R - 10) - this.arbitro.x) * k;
    this.arbitro.y += (clamp(objetivo.y, T + 10, B - 10) - this.arbitro.y) * k;

    for (const p of this.jugadores) {
      if (!p.expulsado || p.alpha <= 0) continue;
      p.y += dt * .06;
      p.alpha = Math.max(0, p.alpha - dt * .00045);
    }

    const f = dt / 16;
    for (const c of this.particulas) {
      c.x += c.vx * f; c.vy += .16 * f; c.vx *= .99; c.y += c.vy * f; c.rot += c.vr * f; c.vida -= dt;
    }
    this.particulas = this.particulas.filter((c) => c.vida > 0 && c.y < H + 20);

    // El confeti del gol sale cuando el balon toca la red.
    for (const e of this.efectos) {
      if (e.tipo === 'gol' && !e.lanzado && t - e.inicio > 650) {
        e.lanzado = true;
        this.lanzarConfeti(e.equipo ?? 0);
      }
    }
  }

  private lanzarConfeti(eq: 0 | 1): void {
    const x0 = eq === 0 ? R : L;
    const dir = eq === 0 ? -1 : 1;
    const colores = [COLOR_EQUIPO[eq], '#ffffff', AMARILLA, COLOR_EQUIPO[eq]];
    for (let i = 0; i < 150; i++) {
      const desdeArriba = i % 3 === 0;
      this.particulas.push({
        x: desdeArriba ? azar(L, R) : x0, y: desdeArriba ? T - 10 : CY + azar(-30, 30),
        vx: desdeArriba ? azar(-1.5, 1.5) : dir * azar(2, 11), vy: desdeArriba ? azar(0, 2) : azar(-9, -2),
        rot: azar(0, 6), vr: azar(-.3, .3), color: colores[i % colores.length], vida: azar(1400, 2600),
      });
    }
  }

  private mover(dt: number, t: number): void {
    const b = this.balon;
    const gol = this.gol;
    if (gol && t > gol.hasta) {
      // Saque del medio para el equipo que recibio el gol.
      this.gol = null;
      this.poseedor = gol.equipo === 0 ? 1 : 0;
      b.x = b.tx = CX; b.y = b.ty = CY;
      b.proximoPase = t + 900;
    }
    const falta = this.falta && t < this.falta.hasta ? this.falta : (this.falta = null);

    const ataque = this.poseedor === 0 ? 1 : -1;
    const k = Math.min(1, dt * .0025);
    for (const p of this.jugadores) {
      if (p.expulsado) continue;
      let tx = p.baseX + Math.sin(t * .0013 + p.fase) * 15;
      let ty = p.baseY + Math.sin(t * .0019 + p.fase) * 18;
      let rapidez = k;

      if (this.gol) {
        const g = this.gol;
        const ms = t - g.inicio;
        if (p.equipo === g.equipo && p.dorsal > 1) {
          if (ms > 900) {
            // Festejo: el equipo corre a la esquina y salta.
            const ex = g.equipo === 0 ? R - 70 : L + 70;
            const ey = g.desdeY < CY ? T + 60 : B - 60;
            tx = ex + Math.cos(p.fase) * 40;
            ty = ey + Math.sin(p.fase) * 30 + Math.abs(Math.sin(t * .012 + p.fase)) * -8;
            rapidez = Math.min(1, dt * .004);
          } else {
            tx += 60 * (g.equipo === 0 ? 1 : -1);
          }
        }
      } else if (falta && p === falta.agresor) {
        tx = falta.x - 14; ty = falta.y - 6; rapidez = Math.min(1, dt * .008);
      } else if (falta && p === falta.victima) {
        tx = falta.x + 8; ty = falta.y + 8; rapidez = Math.min(1, dt * .008);
      } else if (p.equipo === this.poseedor && p.dorsal === 11) {
        tx = b.x - 18 * ataque;
        ty = b.y + 16;
      }
      p.x += (clamp(tx, L + 8, R - 8) - p.x) * rapidez;
      p.y += (clamp(ty, T + 8, B - 8) - p.y) * rapidez;
    }

    if (this.gol) {
      const g = this.gol;
      const p = tramo(t - g.inicio, 0, 650);
      const metaX = g.equipo === 0 ? R + 16 : L - 16;
      const metaY = CY + clamp(g.desdeY - CY, -ARCO + 10, ARCO - 10) * .5;
      b.x = g.desdeX + (metaX - g.desdeX) * p * p;
      b.y = g.desdeY + (metaY - g.desdeY) * p * p;
      return;
    }
    if (t < this.balonQuietoHasta) return;

    if (t > b.proximoPase) {
      b.proximoPase = t + azar(800, 2200);
      if (Math.random() < .22) this.poseedor = this.poseedor === 0 ? 1 : 0;
      const dir = this.poseedor === 0 ? 1 : -1;
      b.tx = clamp(b.x + azar(55, 155) * dir, L + 30, R - 30);
      b.ty = clamp(b.y + azar(-110, 110), T + 20, B - 20);
      if (Math.random() < .13) { b.tx = azar(L + 160, R - 160); b.ty = azar(T + 40, B - 40); }
    }
    b.x += (b.tx - b.x) * Math.min(1, dt * .003);
    b.y += (b.ty - b.y) * Math.min(1, dt * .003);
    if (b.x > R - 38 || b.x < L + 38) { b.tx = CX; b.ty = CY; }
  }

  // =================== Dibujo ===================

  private dibujar(t: number): void {
    const ctx = this.ctx;
    const m = this.marcador();
    this.efectos = this.efectos.filter((e) => t < e.inicio + e.dur);

    // Sacudida de camara al entrar el gol o al "sellar" una anulacion.
    let sacudida = 0;
    for (const e of this.efectos) {
      const ms = t - e.inicio;
      if (e.tipo === 'gol' && ms > 650 && ms < 1100) sacudida = 7 * (1 - tramo(ms, 650, 1100));
      if (e.tipo === 'anulada' && ms > 200 && ms < 520) sacudida = 6 * (1 - tramo(ms, 200, 520));
    }
    ctx.save();
    if (sacudida && !this.reducido) ctx.translate(azar(-sacudida, sacudida), azar(-sacudida, sacudida));

    this.dibujarCancha(ctx, m, t);
    this.dibujarJugadores(ctx, t);
    this.dibujarHud(ctx, m, t);
    this.dibujarOverlay(ctx, m, t);
    for (const e of this.efectos) this.dibujarEfecto(ctx, e, t);
    this.dibujarConfeti(ctx);
    ctx.restore();
  }

  /** Cuanto se infla la red de cada arco (0 = local defiende a la izquierda). */
  private bultoRed(lado: 'izq' | 'der', t: number): number {
    const e = this.efectos.find((x) => x.tipo === 'gol');
    if (!e || this.reducido) return 0;
    if ((lado === 'der') !== (e.equipo === 0)) return 0;
    const ms = t - e.inicio - 650;
    return ms > 0 ? 20 * Math.exp(-ms / 420) * Math.abs(Math.cos(ms * .014)) : 0;
  }

  private dibujarCancha(ctx: CanvasRenderingContext2D, m: Marcador, t: number): void {
    ctx.clearRect(0, 0, W, H);
    const pasto = ctx.createLinearGradient(0, 0, 0, H);
    pasto.addColorStop(0, '#12372a'); pasto.addColorStop(.25, '#174b30'); pasto.addColorStop(1, '#0a2218');
    ctx.fillStyle = pasto; ctx.fillRect(-10, -10, W + 20, H + 20);
    ctx.fillStyle = '#182521'; ctx.fillRect(-10, -10, W + 20, 85); ctx.fillRect(-10, 490, W + 20, 90);
    ctx.fillStyle = '#0c2e20'; ctx.fillRect(-10, 75, L + 10, 415); ctx.fillRect(R, 75, W - R + 10, 415);
    for (let i = 0; i < 12; i++) {
      ctx.fillStyle = i % 2 ? '#2c8547' : '#287840';
      ctx.fillRect(L + i * (R - L) / 12, T, (R - L) / 12, B - T);
    }
    ctx.strokeStyle = 'rgba(241,255,243,.82)'; ctx.lineWidth = 2.6;
    ctx.strokeRect(L, T, R - L, B - T);
    ctx.beginPath(); ctx.moveTo(CX, T); ctx.lineTo(CX, B); ctx.stroke();
    ctx.beginPath(); ctx.arc(CX, CY, 61, 0, Math.PI * 2); ctx.stroke();
    ctx.beginPath(); ctx.arc(CX, CY, 3, 0, Math.PI * 2); ctx.fillStyle = '#e9f6e9'; ctx.fill();
    ctx.strokeRect(L, CY - 110, 145, 220); ctx.strokeRect(R - 145, CY - 110, 145, 220);
    ctx.strokeRect(L, CY - 59, 52, 118); ctx.strokeRect(R - 52, CY - 59, 52, 118);
    ctx.beginPath(); ctx.arc(L + 100, CY, 3, 0, Math.PI * 2); ctx.arc(R - 100, CY, 3, 0, Math.PI * 2); ctx.fill();

    this.dibujarArco(ctx, L, -1, this.bultoRed('izq', t));
    this.dibujarArco(ctx, R, 1, this.bultoRed('der', t));

    // Nombres de los equipos en la banda superior.
    ctx.font = 'bold 16px system-ui, sans-serif'; ctx.textBaseline = 'middle';
    const nombres = [m.equipo_local || 'Local', m.equipo_visitante || 'Visitante'];
    ctx.textAlign = 'left';
    ctx.fillStyle = COLOR_EQUIPO[0]; ctx.beginPath(); ctx.arc(L + 6, 38, 7, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#e9fff0'; ctx.fillText(nombres[0], L + 20, 38);
    ctx.textAlign = 'right';
    ctx.fillStyle = COLOR_EQUIPO[1]; ctx.beginPath(); ctx.arc(R - 6, 38, 7, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#e9fff0'; ctx.fillText(nombres[1], R - 20, 38);
  }

  /** Arco con red; `bulto` infla la red hacia afuera cuando entra el balon. */
  private dibujarArco(ctx: CanvasRenderingContext2D, x0: number, dir: 1 | -1, bulto: number): void {
    const arriba = CY - ARCO, abajo = CY + ARCO, fondo = 24;
    const fondoX = (y: number) => x0 + dir * (fondo + bulto * (1 - Math.pow((y - CY) / ARCO, 2)));

    ctx.fillStyle = 'rgba(255,255,255,.06)';
    ctx.beginPath(); ctx.moveTo(x0, arriba);
    for (let y = arriba; y <= abajo; y += 4) ctx.lineTo(fondoX(y), y);
    ctx.lineTo(x0, abajo); ctx.closePath(); ctx.fill();

    ctx.strokeStyle = 'rgba(255,255,255,.28)'; ctx.lineWidth = 1;
    for (let y = arriba + 7; y < abajo; y += 7) { ctx.beginPath(); ctx.moveTo(x0, y); ctx.lineTo(fondoX(y), y); ctx.stroke(); }
    for (const f of [.33, .66]) {
      ctx.beginPath();
      for (let y = arriba; y <= abajo; y += 4) ctx[y === arriba ? 'moveTo' : 'lineTo'](x0 + (fondoX(y) - x0) * f, y);
      ctx.stroke();
    }

    ctx.strokeStyle = '#dde8e0'; ctx.lineWidth = 4; ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(x0, arriba); ctx.lineTo(fondoX(arriba), arriba);
    for (let y = arriba; y <= abajo; y += 4) ctx.lineTo(fondoX(y), y);
    ctx.lineTo(x0, abajo);
    ctx.stroke();
    ctx.lineCap = 'butt';
  }

  private dibujarJugadores(ctx: CanvasRenderingContext2D, t: number): void {
    const falta = this.falta;
    for (const p of this.jugadores) {
      if (p.alpha <= 0) continue;
      ctx.save();
      ctx.globalAlpha = p.alpha;
      const caido = falta && p === falta.victima && t - falta.inicio > 350;
      ctx.beginPath(); ctx.ellipse(p.x + 2, p.y + 7, 12, 5, 0, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(0,0,0,.25)'; ctx.fill();
      ctx.beginPath();
      if (caido) ctx.ellipse(p.x, p.y + 3, 14, 7, .4, 0, Math.PI * 2);
      else ctx.arc(p.x, p.y, 10.5, 0, Math.PI * 2);
      ctx.fillStyle = COLOR_EQUIPO[p.equipo]; ctx.fill();
      ctx.lineWidth = 2; ctx.strokeStyle = p.expulsado ? ROJA : '#f2fff5'; ctx.stroke();
      ctx.font = 'bold 10px system-ui, sans-serif'; ctx.fillStyle = '#fff';
      ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.fillText(String(p.dorsal), p.x, caido ? p.y + 3 : p.y);
      ctx.restore();
    }

    this.dibujarArbitro(ctx, t);

    const b = this.balon;
    if (this.gol && t - this.gol.inicio < 700) {
      // Estela del remate.
      const g = this.gol;
      for (let i = 1; i <= 4; i++) {
        ctx.beginPath();
        ctx.arc(b.x - (b.x - g.desdeX) * i * .07, b.y - (b.y - g.desdeY) * i * .07, 6.5 - i, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(255,255,255,${.35 - i * .07})`; ctx.fill();
      }
    }
    ctx.beginPath(); ctx.ellipse(b.x + 3, b.y + 8, 7, 3, 0, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(0,0,0,.35)'; ctx.fill();
    ctx.beginPath(); ctx.arc(b.x, b.y, 6.5, 0, Math.PI * 2);
    ctx.fillStyle = '#fff'; ctx.fill(); ctx.strokeStyle = '#14201b'; ctx.lineWidth = 1; ctx.stroke();
    ctx.beginPath(); ctx.arc(b.x, b.y, 2.5, 0, Math.PI * 2); ctx.fillStyle = '#1d3027'; ctx.fill();
  }

  private dibujarArbitro(ctx: CanvasRenderingContext2D, t: number): void {
    const { x, y } = this.arbitro;
    const tarjeta = this.efectos.find((e) => e.tipo === 'tarjeta');
    ctx.beginPath(); ctx.ellipse(x + 2, y + 6, 10, 4, 0, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(0,0,0,.25)'; ctx.fill();
    if (tarjeta) {
      // Brazo en alto mostrando la tarjeta.
      const sube = this.reducido ? 1 : salidaCubica(tramo(t - tarjeta.inicio, 200, 600));
      const cy = y - 12 - 20 * sube;
      ctx.strokeStyle = '#111827'; ctx.lineWidth = 3;
      ctx.beginPath(); ctx.moveTo(x + 5, y - 4); ctx.lineTo(x + 8, cy + 6); ctx.stroke();
      ctx.save(); ctx.translate(x + 9, cy); ctx.rotate(.15);
      ctx.fillStyle = tarjeta.color; ctx.fillRect(-5, -8, 10, 14);
      ctx.strokeStyle = 'rgba(0,0,0,.4)'; ctx.lineWidth = 1; ctx.strokeRect(-5, -8, 10, 14);
      ctx.restore();
    }
    ctx.beginPath(); ctx.arc(x, y, 9, 0, Math.PI * 2);
    ctx.fillStyle = '#111827'; ctx.fill();
    ctx.lineWidth = 2; ctx.strokeStyle = AMARILLA; ctx.stroke();
  }

  private dibujarHud(ctx: CanvasRenderingContext2D, m: Marcador, t: number): void {
    const gol = this.efectos.find((e) => e.tipo === 'gol');
    ctx.fillStyle = 'rgba(3,18,11,.77)'; ctx.fillRect(20, 513, 560, 40);
    ctx.textAlign = 'left'; ctx.textBaseline = 'middle';
    ctx.fillStyle = gol ? VERDE : '#e9fff0';
    ctx.font = 'bold 17px system-ui, sans-serif';
    ctx.fillText(this.accion, 34, 533, 532);

    // Marcador real, con "latido" cuando entra un gol.
    const resultado = `${m.local} - ${m.visitante}`;
    ctx.font = 'bold 18px system-ui, sans-serif';
    const anchoResultado = ctx.measureText(resultado).width;
    const pulso = gol && !this.reducido ? Math.sin(Math.PI * tramo(t - gol.inicio, 650, 1350)) : 0;
    ctx.save();
    ctx.translate(970 - anchoResultado / 2, 533);
    ctx.scale(1 + pulso * .45, 1 + pulso * .45);
    ctx.textAlign = 'center';
    ctx.fillStyle = pulso > .05 && gol ? gol.color : '#bcf3d4';
    ctx.fillText(resultado, 0, 0);
    ctx.restore();

    const minuto = m.estado === 'POR_JUGAR' ? "--'" : m.minuto_extra ? `${m.minuto}+${m.minuto_extra}'` : `${m.minuto}'`;
    let x = 970 - anchoResultado - 22;
    ctx.textAlign = 'right'; ctx.font = 'bold 16px system-ui, sans-serif'; ctx.fillStyle = '#bcf3d4';
    ctx.fillText('·', x + 10, 533);
    ctx.fillText(minuto, x - 4, 533);
    if (this.anadido) {
      // Insignia del tiempo añadido junto al reloj.
      x -= ctx.measureText(minuto).width + 14;
      const texto = `+${this.anadido}`;
      ctx.font = 'bold 13px ui-monospace, Consolas, monospace';
      const w = ctx.measureText(texto).width + 12;
      ctx.fillStyle = '#052e16'; ctx.beginPath(); ctx.roundRect(x - w, 522, w, 22, 5); ctx.fill();
      ctx.strokeStyle = VERDE; ctx.lineWidth = 1.5; ctx.stroke();
      ctx.fillStyle = VERDE; ctx.textAlign = 'center'; ctx.fillText(texto, x - w / 2, 534);
    }
  }

  private dibujarOverlay(ctx: CanvasRenderingContext2D, m: Marcador, t: number): void {
    const info = OVERLAY[m.estado];
    if (!info) return;
    ctx.fillStyle = 'rgba(3,12,8,.55)'; ctx.fillRect(L, T, R - L, B - T);

    if (m.estado === 'EN_REVISION') {
      // Barrido de "escaneo" y esquinas de pantalla de TV.
      if (!this.reducido) {
        const sx = L + ((t * .28) % (R - L));
        const barrido = ctx.createLinearGradient(sx - 70, 0, sx, 0);
        barrido.addColorStop(0, 'rgba(251,191,36,0)'); barrido.addColorStop(1, 'rgba(251,191,36,.28)');
        ctx.fillStyle = barrido; ctx.fillRect(Math.max(L, sx - 70), T, Math.min(70, sx - L), B - T);
        ctx.fillStyle = 'rgba(251,191,36,.8)'; ctx.fillRect(sx - 1, T, 2, B - T);
      }
      ctx.strokeStyle = AMBAR; ctx.lineWidth = 4;
      const c = 34, e = 12;
      for (const [x, y, dx, dy] of [[L + e, T + e, 1, 1], [R - e, T + e, -1, 1], [L + e, B - e, 1, -1], [R - e, B - e, -1, -1]]) {
        ctx.beginPath(); ctx.moveTo(x, y + dy * c); ctx.lineTo(x, y); ctx.lineTo(x + dx * c, y); ctx.stroke();
      }
    }

    if (this.efectos.some((e) => e.tipo !== 'falta')) return; // el efecto activo manda
    const pulso = m.estado === 'EN_REVISION' && !this.reducido ? .75 + Math.sin(t * .005) * .25 : 1;
    ctx.globalAlpha = pulso;
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    ctx.font = '800 30px system-ui, sans-serif'; ctx.fillStyle = info.color;
    ctx.fillText(info.titulo, CX, CY - 14);
    ctx.globalAlpha = 1;
    ctx.font = '15px system-ui, sans-serif'; ctx.fillStyle = '#cbd5e1';
    const sub = m.estado === 'POR_JUGAR' ? 'La simulación arranca al pulsar «Iniciar partido» en el EventMaker'
      : m.estado === 'ENTRETIEMPO' ? 'Se reanuda con «Iniciar segundo tiempo»'
      : m.estado === 'EN_REVISION' ? 'Apuestas suspendidas hasta la decisión del VAR'
      : `Resultado final ${m.local} - ${m.visitante}`;
    ctx.fillText(sub, CX, CY + 22);
  }

  private dibujarConfeti(ctx: CanvasRenderingContext2D): void {
    for (const c of this.particulas) {
      ctx.save();
      ctx.globalAlpha = Math.min(1, c.vida / 400);
      ctx.translate(c.x, c.y); ctx.rotate(c.rot);
      ctx.fillStyle = c.color; ctx.fillRect(-4, -2, 8, 4);
      ctx.restore();
    }
  }

  // =================== Efectos por evento ===================

  private dibujarEfecto(ctx: CanvasRenderingContext2D, e: Efecto, t: number): void {
    const ms = Math.max(0, t - e.inicio);
    const p = ms / e.dur;
    /** Progreso de entrada; con movimiento reducido todo aparece ya en su sitio. */
    const r = (a: number, b: number) => this.reducido ? 1 : tramo(ms, a, b);
    ctx.save();
    ctx.globalAlpha = p > .85 ? (1 - p) / .15 : 1;
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    switch (e.tipo) {
      case 'gol': this.efectoGol(ctx, e, ms, r); break;
      case 'falta': this.efectoFalta(ctx, e, ms, r); break;
      case 'tarjeta': this.efectoTarjeta(ctx, e, t, r); break;
      case 'tiempo': this.efectoTiempo(ctx, e, ms, r); break;
      case 'var': this.efectoVar(ctx, e, r); break;
      case 'confirmada': this.efectoConfirmada(ctx, e, r); break;
      case 'anulada': this.efectoAnulada(ctx, e, r); break;
      case 'silbato': this.efectoSilbato(ctx, e, ms, r); break;
    }
    ctx.restore();
  }

  private banda(ctx: CanvasRenderingContext2D, y: number, alto: number, k: number, color = 'rgba(3,12,8,.72)'): void {
    const ancho = (R - L) * k;
    ctx.fillStyle = color;
    ctx.fillRect(CX - ancho / 2, y - alto / 2, ancho, alto);
  }

  private efectoGol(ctx: CanvasRenderingContext2D, e: Efecto, ms: number, r: (a: number, b: number) => number): void {
    if (ms < 650 && !this.reducido) return; // primero vuela el balon
    if (!this.reducido && ms < 1050) {
      ctx.save();
      ctx.globalAlpha = .55 * (1 - tramo(ms, 650, 1050));
      ctx.fillStyle = '#fff'; ctx.fillRect(-10, -10, W + 20, H + 20);
      ctx.restore();
    }
    const k = salidaCubica(r(650, 950));
    this.banda(ctx, CY, 150, k, 'rgba(3,12,8,.55)');
    ctx.save();
    ctx.globalAlpha *= .9;
    this.banda(ctx, CY, 118, k, e.color);
    ctx.restore();

    const s = rebote(r(750, 1150));
    ctx.save();
    ctx.translate(CX, CY - 14);
    ctx.rotate(this.reducido ? 0 : Math.sin(ms * .012) * .03);
    ctx.scale(s, s);
    ctx.font = '900 78px system-ui, sans-serif';
    ctx.lineWidth = 8; ctx.strokeStyle = 'rgba(0,0,0,.35)'; ctx.lineJoin = 'round';
    ctx.strokeText(e.titulo, 0, 0);
    ctx.fillStyle = '#fff'; ctx.fillText(e.titulo, 0, 0);
    ctx.restore();

    ctx.globalAlpha *= r(1000, 1300);
    ctx.font = '800 22px system-ui, sans-serif'; ctx.fillStyle = '#fff';
    ctx.fillText(`${e.sub} marca`, CX, CY + 40);
  }

  private efectoFalta(ctx: CanvasRenderingContext2D, e: Efecto, ms: number, r: (a: number, b: number) => number): void {
    const x = clamp(e.x, L + 90, R - 90), y = clamp(e.y, T + 80, B - 10);
    if (!this.reducido) {
      for (const d of [0, 220]) {
        const k = tramo(ms, d, d + 700);
        if (k <= 0 || k >= 1) continue;
        ctx.save(); ctx.globalAlpha *= 1 - k;
        ctx.strokeStyle = ROJA; ctx.lineWidth = 3;
        ctx.beginPath(); ctx.arc(e.x, e.y, 10 + 42 * k, 0, Math.PI * 2); ctx.stroke();
        ctx.restore();
      }
    }
    // Burbuja "!" que rebota sobre la jugada.
    const s = rebote(r(100, 420));
    const salto = this.reducido ? 0 : Math.abs(Math.sin(ms * .009)) * -5;
    ctx.save();
    ctx.translate(x, y - 42 + salto); ctx.scale(s, s);
    ctx.fillStyle = ROJA; ctx.beginPath(); ctx.arc(0, 0, 16, 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = '#fff'; ctx.lineWidth = 2.5; ctx.stroke();
    ctx.fillStyle = '#fff'; ctx.font = '900 20px system-ui, sans-serif'; ctx.fillText('!', 0, 1);
    ctx.restore();

    ctx.globalAlpha *= r(250, 500);
    const texto = `FALTA · ${e.sub}`;
    ctx.font = '800 14px system-ui, sans-serif';
    const w = ctx.measureText(texto).width + 22;
    ctx.fillStyle = 'rgba(3,12,8,.85)'; ctx.beginPath(); ctx.roundRect(x - w / 2, y - 86, w, 26, 13); ctx.fill();
    ctx.strokeStyle = ROJA; ctx.lineWidth = 2; ctx.stroke();
    ctx.fillStyle = '#fecaca'; ctx.fillText(texto, x, y - 72);
  }

  private efectoTarjeta(ctx: CanvasRenderingContext2D, e: Efecto, t: number, r: (a: number, b: number) => number): void {
    const ms = Math.max(0, t - e.inicio);
    // Aro sobre el jugador amonestado.
    const j = e.jugador;
    if (j && j.alpha > 0) {
      ctx.save();
      ctx.strokeStyle = e.color; ctx.lineWidth = 3;
      ctx.beginPath(); ctx.arc(j.x, j.y, 17 + (this.reducido ? 0 : Math.sin(ms * .012) * 3), 0, Math.PI * 2); ctx.stroke();
      ctx.restore();
    }

    // Rotulo tipo TV en el tercio inferior.
    const yb = B - 62;
    const k = salidaCubica(r(0, 300));
    ctx.fillStyle = 'rgba(3,12,8,.82)';
    ctx.fillRect(L + 20, yb - 42, (R - L - 40) * k, 84);
    ctx.fillStyle = e.color; ctx.fillRect(L + 20, yb - 42, 6, 84 * k);

    // La tarjeta "gira" al aparecer.
    const giro = rebote(r(150, 650));
    ctx.save();
    ctx.translate(L + 78, yb);
    ctx.rotate(-.12);
    ctx.scale(Math.max(.02, giro), 1);
    ctx.shadowColor = e.color; ctx.shadowBlur = 18;
    ctx.fillStyle = e.color; ctx.beginPath(); ctx.roundRect(-24, -34, 48, 68, 5); ctx.fill();
    ctx.shadowBlur = 0;
    const brillo = ctx.createLinearGradient(-24, -34, 24, 34);
    brillo.addColorStop(0, 'rgba(255,255,255,.45)'); brillo.addColorStop(.5, 'rgba(255,255,255,0)');
    ctx.fillStyle = brillo; ctx.fill();
    ctx.restore();

    const entra = salidaCubica(r(300, 700));
    ctx.globalAlpha *= entra;
    ctx.textAlign = 'left';
    ctx.font = '900 30px system-ui, sans-serif'; ctx.fillStyle = e.color;
    ctx.fillText(e.titulo, L + 128 + (1 - entra) * 50, yb - 12);
    ctx.font = '600 17px system-ui, sans-serif'; ctx.fillStyle = '#e5e7eb';
    ctx.fillText(e.sub, L + 128 + (1 - entra) * 80, yb + 20);
  }

  private efectoTiempo(ctx: CanvasRenderingContext2D, e: Efecto, ms: number, r: (a: number, b: number) => number): void {
    // Cartel luminoso del cuarto arbitro que sube desde abajo.
    const y = CY + (1 - rebote(r(0, 650))) * 280;
    ctx.save();
    ctx.translate(CX, y);
    ctx.fillStyle = '#05090c'; ctx.strokeStyle = '#1f2937'; ctx.lineWidth = 8;
    ctx.beginPath(); ctx.roundRect(-140, -86, 280, 172, 16); ctx.fill(); ctx.stroke();
    ctx.strokeStyle = VERDE; ctx.lineWidth = 2; ctx.shadowColor = VERDE; ctx.shadowBlur = 14;
    ctx.beginPath(); ctx.roundRect(-126, -72, 252, 144, 10); ctx.stroke();
    // Mango del cartel.
    ctx.shadowBlur = 0; ctx.fillStyle = '#374151'; ctx.fillRect(-12, 86, 24, 50);

    ctx.font = '700 15px system-ui, sans-serif'; ctx.fillStyle = '#9ca3af';
    ctx.fillText(e.sub, 0, -50);
    const parpadeo = !this.reducido && ms > 600 && ms < 1800 && Math.floor(ms / 180) % 2 === 0;
    ctx.globalAlpha *= parpadeo ? .35 : 1;
    ctx.shadowColor = VERDE; ctx.shadowBlur = 22;
    ctx.font = '900 76px ui-monospace, Consolas, monospace'; ctx.fillStyle = VERDE;
    ctx.fillText(e.titulo, 0, 8);
    ctx.shadowBlur = 0;
    ctx.font = '700 14px system-ui, sans-serif'; ctx.fillStyle = '#86efac';
    ctx.fillText('MINUTOS', 0, 56);
    ctx.restore();
  }

  private efectoVar(ctx: CanvasRenderingContext2D, e: Efecto, r: (a: number, b: number) => number): void {
    // El arbitro "dibuja" la pantalla de TV en el aire.
    const w = 300, h = 170, x0 = CX - w / 2, y0 = CY - h / 2 - 10;
    const puntos: [number, number][] = [[x0, y0], [x0 + w, y0], [x0 + w, y0 + h], [x0, y0 + h], [x0, y0]];
    const total = 2 * (w + h);
    let resto = total * (this.reducido ? 1 : salidaCubica(r(0, 1000)));
    ctx.fillStyle = 'rgba(3,12,8,.45)'; ctx.fillRect(x0, y0, w, h);
    ctx.strokeStyle = '#fff'; ctx.lineWidth = 7; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    ctx.shadowColor = AMBAR; ctx.shadowBlur = 16;
    ctx.beginPath(); ctx.moveTo(x0, y0);
    let punta = puntos[0];
    for (let i = 1; i < puntos.length && resto > 0; i++) {
      const [ax, ay] = puntos[i - 1], [bx, by] = puntos[i];
      const largo = Math.hypot(bx - ax, by - ay);
      const f = Math.min(1, resto / largo);
      punta = [ax + (bx - ax) * f, ay + (by - ay) * f];
      ctx.lineTo(punta[0], punta[1]);
      resto -= largo;
    }
    ctx.stroke();
    if (resto < 0) { ctx.fillStyle = AMBAR; ctx.beginPath(); ctx.arc(punta[0], punta[1], 6, 0, Math.PI * 2); ctx.fill(); }
    ctx.shadowBlur = 0;

    const s = rebote(r(850, 1200));
    ctx.save();
    ctx.translate(CX, CY - 24); ctx.scale(s, s);
    ctx.font = '900 64px system-ui, sans-serif'; ctx.fillStyle = AMBAR;
    ctx.fillText(e.titulo, 0, 0);
    ctx.restore();
    ctx.globalAlpha *= r(1050, 1350);
    ctx.font = '700 18px system-ui, sans-serif'; ctx.fillStyle = '#fef3c7';
    ctx.fillText(e.sub, CX, CY + 32);
  }

  private efectoConfirmada(ctx: CanvasRenderingContext2D, e: Efecto, r: (a: number, b: number) => number): void {
    this.banda(ctx, CY, 200, salidaCubica(r(0, 300)));
    const s = rebote(r(0, 450));
    ctx.save();
    ctx.translate(CX, CY - 30); ctx.scale(s, s);
    ctx.fillStyle = '#166534'; ctx.strokeStyle = VERDE; ctx.lineWidth = 5;
    ctx.beginPath(); ctx.arc(0, 0, 44, 0, Math.PI * 2); ctx.fill(); ctx.stroke();
    // Palomita que se traza sola.
    const k = r(300, 750);
    const a: [number, number] = [-20, 2], b: [number, number] = [-5, 17], c: [number, number] = [22, -14];
    ctx.strokeStyle = '#fff'; ctx.lineWidth = 8; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    ctx.beginPath(); ctx.moveTo(a[0], a[1]);
    const k1 = Math.min(1, k / .4), k2 = clamp((k - .4) / .6, 0, 1);
    ctx.lineTo(a[0] + (b[0] - a[0]) * k1, a[1] + (b[1] - a[1]) * k1);
    if (k2 > 0) ctx.lineTo(b[0] + (c[0] - b[0]) * k2, b[1] + (c[1] - b[1]) * k2);
    ctx.stroke();
    ctx.restore();

    ctx.globalAlpha *= r(450, 750);
    ctx.font = '900 30px system-ui, sans-serif'; ctx.fillStyle = VERDE;
    ctx.fillText(e.titulo, CX, CY + 40);
    ctx.font = '600 16px system-ui, sans-serif'; ctx.fillStyle = '#d1fae5';
    ctx.fillText(e.sub, CX, CY + 72);
  }

  private efectoAnulada(ctx: CanvasRenderingContext2D, e: Efecto, r: (a: number, b: number) => number): void {
    this.banda(ctx, CY, 200, salidaCubica(r(0, 250)), 'rgba(40,6,6,.6)');
    // Sello que "golpea" la pantalla.
    const s = 2.4 - 1.4 * salidaCubica(r(0, 260));
    ctx.save();
    ctx.globalAlpha *= r(0, 180);
    ctx.translate(CX, CY); ctx.rotate(-.12); ctx.scale(s, s);
    ctx.fillStyle = 'rgba(127,29,29,.6)'; ctx.strokeStyle = ROJA; ctx.lineWidth = 7;
    ctx.beginPath(); ctx.roundRect(-220, -58, 440, 116, 12); ctx.fill(); ctx.stroke();
    ctx.lineWidth = 2; ctx.beginPath(); ctx.roundRect(-206, -44, 412, 88, 8); ctx.stroke();
    ctx.font = '900 50px system-ui, sans-serif'; ctx.fillStyle = '#fecaca';
    ctx.fillText(e.titulo, 0, -4);
    ctx.font = '700 15px system-ui, sans-serif'; ctx.fillStyle = '#fca5a5';
    ctx.fillText(e.sub.toUpperCase(), 0, 30);
    ctx.restore();
  }

  private efectoSilbato(ctx: CanvasRenderingContext2D, e: Efecto, ms: number, r: (a: number, b: number) => number): void {
    this.banda(ctx, CY, 170, salidaCubica(r(0, 300)));
    const wx = CX, wy = CY - 42;
    // Ondas de sonido del silbato.
    if (!this.reducido) {
      for (let i = 0; i < 3; i++) {
        const k = ((ms * .0012) + i / 3) % 1;
        ctx.save(); ctx.globalAlpha *= (1 - k) * .8;
        ctx.strokeStyle = '#fff'; ctx.lineWidth = 3;
        ctx.beginPath(); ctx.arc(wx, wy, 18 + k * 46, -Math.PI * .85, -Math.PI * .15); ctx.stroke();
        ctx.restore();
      }
    }
    ctx.fillStyle = '#e5e7eb';
    ctx.beginPath(); ctx.arc(wx, wy, 14, 0, Math.PI * 2); ctx.fill();
    ctx.fillRect(wx + 6, wy - 12, 22, 9);
    ctx.fillStyle = '#111827'; ctx.beginPath(); ctx.arc(wx, wy, 5, 0, Math.PI * 2); ctx.fill();

    const entra = salidaCubica(r(150, 550));
    ctx.globalAlpha *= entra;
    ctx.font = '900 34px system-ui, sans-serif'; ctx.fillStyle = '#fff';
    ctx.fillText(e.titulo, CX, CY + 14 + (1 - entra) * 20);
    ctx.font = '600 17px system-ui, sans-serif'; ctx.fillStyle = '#cbd5e1';
    ctx.fillText(e.sub, CX, CY + 48 + (1 - entra) * 20);
  }
}
