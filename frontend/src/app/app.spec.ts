import { TestBed } from '@angular/core/testing';
import { App, urlDelMarcador } from './app';

describe('App', () => {
  it('should create', async () => {
    await TestBed.configureTestingModule({ imports: [App] }).compileComponents();
    expect(TestBed.createComponent(App).componentInstance).toBeTruthy();
  });
});

describe('urlDelMarcador', () => {
  it('con ng serve usa el puerto 8081 del backend', () => {
    const url = urlDelMarcador({ protocol: 'http:', hostname: 'localhost', host: 'localhost:4200', port: '4200' });
    expect(url).toBe('ws://localhost:8081/marcador');
  });

  it('abierto como archivo local usa localhost:8081', () => {
    const url = urlDelMarcador({ protocol: 'file:', hostname: '', host: '', port: '' });
    expect(url).toBe('ws://localhost:8081/marcador');
  });

  it('detras de un proxy con HTTPS usa wss por el mismo origen', () => {
    const url = urlDelMarcador({
      protocol: 'https:',
      hostname: 'var-enabled.duckdns.org',
      host: 'var-enabled.duckdns.org',
      port: '',
    });
    expect(url).toBe('wss://var-enabled.duckdns.org/marcador');
  });

  it('detras de un proxy con HTTP usa ws por el mismo origen', () => {
    const url = urlDelMarcador({ protocol: 'http:', hostname: '3.85.10.20', host: '3.85.10.20', port: '' });
    expect(url).toBe('ws://3.85.10.20/marcador');
  });
});