import { Component, OnDestroy, OnInit, signal } from '@angular/core';

interface Marcador {
  match_id: string;
  local: number;
  visitante: number;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  protected readonly partidos = signal<Marcador[]>([]);
  protected readonly conectado = signal(false);
  protected readonly mensajeEstado = signal('Conectando al marcador en vivo...');

  private socket?: WebSocket;
  private reintento?: ReturnType<typeof setTimeout>;

  ngOnInit(): void { this.conectar(); }

  ngOnDestroy(): void {
    if (this.reintento) clearTimeout(this.reintento);
    this.socket?.close();
  }

  protected reconectar(): void {
    if (this.reintento) clearTimeout(this.reintento);
    this.socket?.close();
    this.conectar();
  }

  private conectar(): void {
    const protocolo = window.location.protocol === 'https:' ? 'wss' : 'ws';
    this.mensajeEstado.set('Conectando al marcador en vivo...');
    this.socket = new WebSocket(`${protocolo}://${window.location.hostname || 'localhost'}:8081/marcador`);
    this.socket.onopen = () => {
      this.conectado.set(true);
      this.mensajeEstado.set('Actualizaciones en tiempo real activas');
    };
    this.socket.onmessage = (evento) => this.actualizarMarcador(evento.data);
    this.socket.onerror = () => this.socket?.close();
    this.socket.onclose = () => {
      this.conectado.set(false);
      this.mensajeEstado.set('Esperando conexión con el backend');
      this.reintento = setTimeout(() => this.conectar(), 3000);
    };
  }

  private actualizarMarcador(mensaje: string): void {
    try {
      const marcador = JSON.parse(mensaje) as Marcador;
      if (!marcador.match_id || marcador.local < 0 || marcador.visitante < 0) return;
      this.partidos.update((partidos) => {
        const indice = partidos.findIndex((partido) => partido.match_id === marcador.match_id);
        if (indice === -1) return [...partidos, marcador];
        return partidos.map((partido, i) => i === indice ? marcador : partido);
      });
    } catch { /* Ignora mensajes fuera del contrato. */ }
  }
}
