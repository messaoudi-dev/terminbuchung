import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { authGuard } from './auth.guard';
import { IN_EINER_STUNDE, JETZT, erzeugeTestToken } from './test-token';

describe('authGuard', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(JETZT);
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function pruefeGuard(): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as boolean | UrlTree;
  }

  it('authGuard_angemeldet_erlaubt', () => {
    // Arrange
    sessionStorage.setItem('token', erzeugeTestToken({ sub: '42', rolle: 'PATIENT', exp: IN_EINER_STUNDE }));

    // Act
    const ergebnis = pruefeGuard();

    // Assert
    expect(ergebnis).toBe(true);
  });

  it('authGuard_nichtAngemeldet_leitetZumLoginUm', () => {
    // Act
    const ergebnis = pruefeGuard();

    // Assert
    expect(TestBed.inject(Router).serializeUrl(ergebnis as UrlTree)).toBe('/login');
  });

  it('authGuard_tokenAbgelaufen_entferntTokenUndLeitetZumLoginUm', () => {
    // Arrange
    const abgelaufen = JETZT.getTime() / 1000 - 1;
    sessionStorage.setItem('token', erzeugeTestToken({ sub: '42', rolle: 'PATIENT', exp: abgelaufen }));

    // Act
    const ergebnis = pruefeGuard();

    // Assert
    expect(TestBed.inject(Router).serializeUrl(ergebnis as UrlTree)).toBe('/login');
    expect(sessionStorage.getItem('token')).toBeNull();
  });
});
