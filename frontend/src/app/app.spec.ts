import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { IN_EINER_STUNDE, JETZT, erzeugeTestToken } from './auth/test-token';

describe('App', () => {
  beforeEach(async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(JETZT);
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('navigation_nichtAngemeldet_keineToolbar', async () => {
    // Act
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    // Assert
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('mat-toolbar')).toBeNull();
  });

  it('navigation_patient_zeigtTerminBuchen', async () => {
    // Arrange
    sessionStorage.setItem('token', erzeugeTestToken({ sub: '42', rolle: 'PATIENT', exp: IN_EINER_STUNDE }));

    // Act
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    // Assert
    const text = (fixture.nativeElement as HTMLElement).querySelector('mat-toolbar')?.textContent;
    expect(text).toContain('Meine Termine');
    expect(text).toContain('Termin buchen');
    expect(text).toContain('Abmelden');
  });

  it('navigation_praxis_zeigtAlleTermineOhneBuchen', async () => {
    // Arrange
    sessionStorage.setItem('token', erzeugeTestToken({ sub: '1', rolle: 'PRAXIS', exp: IN_EINER_STUNDE }));

    // Act
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    // Assert
    const text = (fixture.nativeElement as HTMLElement).querySelector('mat-toolbar')?.textContent;
    expect(text).toContain('Alle Termine');
    expect(text).not.toContain('Termin buchen');
  });

  it('benutzerInfo_patient_zeigtEmailUndRolle', async () => {
    // Arrange
    sessionStorage.setItem(
      'token',
      erzeugeTestToken({ sub: '42', rolle: 'PATIENT', email: 'patient@example.com', exp: IN_EINER_STUNDE }),
    );

    // Act
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    // Assert
    const info = (fixture.nativeElement as HTMLElement).querySelector('.benutzer-info')?.textContent;
    expect(info?.trim()).toBe('patient@example.com · Patient');
  });

  it('benutzerInfo_praxis_zeigtEmailUndRolle', async () => {
    // Arrange
    sessionStorage.setItem(
      'token',
      erzeugeTestToken({ sub: '1', rolle: 'PRAXIS', email: 'praxis@example.com', exp: IN_EINER_STUNDE }),
    );

    // Act
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    // Assert
    const info = (fixture.nativeElement as HTMLElement).querySelector('.benutzer-info')?.textContent;
    expect(info?.trim()).toBe('praxis@example.com · Praxis');
  });
});
