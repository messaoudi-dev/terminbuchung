import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Rolle } from './auth.models';
import { roleGuard } from './role.guard';
import { IN_EINER_STUNDE, JETZT, erzeugeTestToken } from './test-token';

describe('roleGuard', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(JETZT);
    sessionStorage.clear();
    sessionStorage.setItem('token', erzeugeTestToken({ sub: '42', rolle: 'PATIENT', exp: IN_EINER_STUNDE }));
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function pruefeGuard(rolle: Rolle): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      roleGuard(rolle)({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as boolean | UrlTree;
  }

  it('roleGuard_passendeRolle_erlaubt', () => {
    // Act
    const ergebnis = pruefeGuard('PATIENT');

    // Assert
    expect(ergebnis).toBe(true);
  });

  it('roleGuard_andereRolle_leitetZurStartseiteUm', () => {
    // Act
    const ergebnis = pruefeGuard('PRAXIS');

    // Assert
    expect(TestBed.inject(Router).serializeUrl(ergebnis as UrlTree)).toBe('/start');
  });
});
