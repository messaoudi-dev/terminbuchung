import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TerminResponse } from './termin.models';
import { TerminService } from './termin.service';

const TERMIN: TerminResponse = {
  id: 5,
  arztId: 1,
  arztName: 'Dr. Weber',
  patientId: 42,
  beginn: '2026-11-02T09:30:00',
  ende: '2026-11-02T10:00:00',
  status: 'GEBUCHT',
};

describe('TerminService', () => {
  let httpTesting: HttpTestingController;
  let service: TerminService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    service = TestBed.inject(TerminService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('findeAlle_termineVorhanden_liefertListe', () => {
    // Arrange
    let ergebnis: TerminResponse[] = [];

    // Act
    service.findeAlle().subscribe((response) => (ergebnis = response));
    const request = httpTesting.expectOne('/api/termine');
    request.flush([TERMIN]);

    // Assert
    expect(request.request.method).toBe('GET');
    expect(ergebnis).toEqual([TERMIN]);
  });

  it('buchen_gueltigeDaten_sendetPostMitLokalemZeitstring', () => {
    // Arrange
    let ergebnis: TerminResponse | null = null;

    // Act
    service.buchen({ arztId: 1, beginn: '2026-11-02T09:30:00' }).subscribe((response) => (ergebnis = response));
    const request = httpTesting.expectOne('/api/termine');
    request.flush(TERMIN, { status: 201, statusText: 'Created' });

    // Assert
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ arztId: 1, beginn: '2026-11-02T09:30:00' });
    expect(ergebnis).toEqual(TERMIN);
  });

  it('buchen_terminBereitsVergeben_gibtFehler409Weiter', () => {
    // Arrange
    let status = 0;

    // Act
    service.buchen({ arztId: 1, beginn: '2026-11-02T09:30:00' }).subscribe({
      error: (error) => (status = error.status),
    });
    httpTesting
      .expectOne('/api/termine')
      .flush({ status: 409, detail: 'Der Termin ist bereits vergeben.' }, { status: 409, statusText: 'Conflict' });

    // Assert
    expect(status).toBe(409);
  });

  it('stornieren_gebuchterTermin_sendetPostAnStornierenUrl', () => {
    // Arrange
    let ergebnis: TerminResponse | null = null;

    // Act
    service.stornieren(5).subscribe((response) => (ergebnis = response));
    const request = httpTesting.expectOne('/api/termine/5/stornieren');
    request.flush({ ...TERMIN, status: 'STORNIERT' });

    // Assert
    expect(request.request.method).toBe('POST');
    expect(ergebnis).toEqual({ ...TERMIN, status: 'STORNIERT' });
  });

  it('stornieren_wenigerAls24Stunden_gibtFehler409Weiter', () => {
    // Arrange
    let status = 0;

    // Act
    service.stornieren(5).subscribe({ error: (error) => (status = error.status) });
    httpTesting
      .expectOne('/api/termine/5/stornieren')
      .flush({ status: 409, detail: 'Stornieren ist nur bis 24 Stunden vor Beginn möglich.' }, { status: 409, statusText: 'Conflict' });

    // Assert
    expect(status).toBe(409);
  });
});
