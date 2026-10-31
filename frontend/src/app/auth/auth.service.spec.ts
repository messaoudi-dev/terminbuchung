import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { IN_EINER_STUNDE, JETZT, erzeugeTestToken } from './test-token';

describe('AuthService', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(JETZT);
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.useRealTimers();
  });

  it('login_erfolgreich_speichertNurToken', () => {
    // Arrange
    const token = erzeugeTestToken({ sub: '42', rolle: 'PATIENT', exp: IN_EINER_STUNDE });
    const service = TestBed.inject(AuthService);

    // Act
    service.login({ email: 'patient@example.com', passwort: 'geheim123' }).subscribe();
    const request = httpTesting.expectOne('/api/auth/login');
    request.flush({ token, expiresIn: 3600 });

    // Assert
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'patient@example.com', passwort: 'geheim123' });
    expect(sessionStorage.length).toBe(1);
    expect(sessionStorage.getItem('token')).toBe(token);
    expect(service.istAngemeldet()).toBe(true);
  });

  it('login_falscheDaten_speichertKeinTokenUndGibtFehlerWeiter', () => {
    // Arrange
    const service = TestBed.inject(AuthService);
    let status = 0;

    // Act
    service.login({ email: 'patient@example.com', passwort: 'falsch' }).subscribe({
      error: (error) => (status = error.status),
    });
    httpTesting
      .expectOne('/api/auth/login')
      .flush({ detail: 'E-Mail oder Passwort ist falsch.' }, { status: 401, statusText: 'Unauthorized' });

    // Assert
    expect(status).toBe(401);
    expect(sessionStorage.length).toBe(0);
    expect(service.istAngemeldet()).toBe(false);
  });

  it('registrieren_erfolgreich_sendetDatenUndSpeichertKeinToken', () => {
    // Arrange
    const service = TestBed.inject(AuthService);

    // Act
    service.registrieren({ email: 'neu@example.com', passwort: 'geheim123' }).subscribe();
    const request = httpTesting.expectOne('/api/auth/register');
    request.flush({ id: 7, email: 'neu@example.com', rolle: 'PATIENT' }, { status: 201, statusText: 'Created' });

    // Assert
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'neu@example.com', passwort: 'geheim123' });
    expect(sessionStorage.length).toBe(0);
  });

  it('rolleUndBenutzerId_gueltigesToken_liestClaims', () => {
    // Arrange
    sessionStorage.setItem(
      'token',
      erzeugeTestToken({ sub: '42', rolle: 'PRAXIS', email: 'praxis@example.com', exp: IN_EINER_STUNDE }),
    );

    // Act
    const service = TestBed.inject(AuthService);

    // Assert
    expect(service.istAngemeldet()).toBe(true);
    expect(service.rolle()).toBe('PRAXIS');
    expect(service.benutzerId()).toBe(42);
    expect(service.email()).toBe('praxis@example.com');
  });

  it('istAngemeldet_tokenAbgelaufen_false', () => {
    // Arrange
    const abgelaufen = JETZT.getTime() / 1000 - 1;
    sessionStorage.setItem(
      'token',
      erzeugeTestToken({ sub: '42', rolle: 'PATIENT', email: 'patient@example.com', exp: abgelaufen }),
    );

    // Act
    const service = TestBed.inject(AuthService);

    // Assert
    expect(service.istAngemeldet()).toBe(false);
    expect(service.token()).toBeNull();
    expect(service.rolle()).toBeNull();
    expect(service.benutzerId()).toBeNull();
    expect(service.email()).toBeNull();
  });

  it('istAngemeldet_ohneToken_false', () => {
    // Act
    const service = TestBed.inject(AuthService);

    // Assert
    expect(service.istAngemeldet()).toBe(false);
    expect(service.rolle()).toBeNull();
  });

  it('istAngemeldet_kaputtesToken_false', () => {
    // Arrange
    sessionStorage.setItem('token', 'kein-jwt');

    // Act
    const service = TestBed.inject(AuthService);

    // Assert
    expect(service.istAngemeldet()).toBe(false);
  });

  it('abmelden_angemeldet_entferntToken', () => {
    // Arrange
    sessionStorage.setItem('token', erzeugeTestToken({ sub: '42', rolle: 'PATIENT', exp: IN_EINER_STUNDE }));
    const service = TestBed.inject(AuthService);

    // Act
    service.abmelden();

    // Assert
    expect(sessionStorage.getItem('token')).toBeNull();
    expect(service.istAngemeldet()).toBe(false);
  });
});
