import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { IN_EINER_STUNDE, JETZT, erzeugeTestToken } from './test-token';

describe('authInterceptor', () => {
  const token = erzeugeTestToken({ sub: '42', rolle: 'PATIENT', exp: IN_EINER_STUNDE });
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(JETZT);
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.useRealTimers();
  });

  function anmelden(): void {
    sessionStorage.setItem('token', token);
  }

  it('apiRequest_mitToken_setztAuthorizationHeader', () => {
    // Arrange
    anmelden();

    // Act
    http.get('/api/aerzte').subscribe();
    const request = httpTesting.expectOne('/api/aerzte');
    request.flush([]);

    // Assert
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
  });

  it('apiRequest_ohneToken_keinAuthorizationHeader', () => {
    // Act
    http.get('/api/aerzte').subscribe();
    const request = httpTesting.expectOne('/api/aerzte');
    request.flush([]);

    // Assert
    expect(request.request.headers.has('Authorization')).toBe(false);
  });

  it('authRequest_mitToken_keinAuthorizationHeader', () => {
    // Arrange
    anmelden();

    // Act
    http.post('/api/auth/login', {}).subscribe();
    const request = httpTesting.expectOne('/api/auth/login');
    request.flush({});

    // Assert
    expect(request.request.headers.has('Authorization')).toBe(false);
  });

  it('antwort401_meldetAbUndNavigiertZumLogin', () => {
    // Arrange
    anmelden();
    let status = 0;

    // Act
    http.get('/api/termine').subscribe({ error: (error) => (status = error.status) });
    httpTesting
      .expectOne('/api/termine')
      .flush({ detail: 'Anmeldung erforderlich.' }, { status: 401, statusText: 'Unauthorized' });

    // Assert
    expect(status).toBe(401);
    expect(sessionStorage.getItem('token')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('antwort401BeimLogin_bleibtAufSeiteUndGibtFehlerWeiter', () => {
    // Arrange
    let status = 0;

    // Act
    http.post('/api/auth/login', {}).subscribe({ error: (error) => (status = error.status) });
    httpTesting
      .expectOne('/api/auth/login')
      .flush({ detail: 'E-Mail oder Passwort ist falsch.' }, { status: 401, statusText: 'Unauthorized' });

    // Assert
    expect(status).toBe(401);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('antwort403_bleibtAngemeldet', () => {
    // Arrange
    anmelden();

    // Act
    http.get('/api/termine/1').subscribe({ error: () => undefined });
    httpTesting
      .expectOne('/api/termine/1')
      .flush({ detail: 'Keine Berechtigung.' }, { status: 403, statusText: 'Forbidden' });

    // Assert
    expect(sessionStorage.getItem('token')).toBe(token);
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
