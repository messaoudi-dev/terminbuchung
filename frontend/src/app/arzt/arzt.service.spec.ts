import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ArztResponse } from './arzt.models';
import { ArztService } from './arzt.service';

describe('ArztService', () => {
  let httpTesting: HttpTestingController;
  let service: ArztService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    service = TestBed.inject(ArztService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('findeAlle_aerzteVorhanden_liefertListe', () => {
    // Arrange
    const aerzte: ArztResponse[] = [{ id: 1, name: 'Dr. Weber', fachrichtung: 'Allgemeinmedizin' }];
    let ergebnis: ArztResponse[] = [];

    // Act
    service.findeAlle().subscribe((response) => (ergebnis = response));
    const request = httpTesting.expectOne('/api/aerzte');
    request.flush(aerzte);

    // Assert
    expect(request.request.method).toBe('GET');
    expect(ergebnis).toEqual(aerzte);
  });

  it('anlegen_gueltigeDaten_sendetPost', () => {
    // Arrange
    const arzt = { name: 'Dr. Weber', fachrichtung: 'Allgemeinmedizin' };
    let ergebnis: ArztResponse | null = null;

    // Act
    service.anlegen(arzt).subscribe((response) => (ergebnis = response));
    const request = httpTesting.expectOne('/api/aerzte');
    request.flush({ id: 1, ...arzt }, { status: 201, statusText: 'Created' });

    // Assert
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(arzt);
    expect(ergebnis).toEqual({ id: 1, ...arzt });
  });

  it('aendern_gueltigeDaten_sendetPutMitId', () => {
    // Arrange
    const arzt = { name: 'Dr. Weber', fachrichtung: 'Kardiologie' };

    // Act
    service.aendern(3, arzt).subscribe();
    const request = httpTesting.expectOne('/api/aerzte/3');
    request.flush({ id: 3, ...arzt });

    // Assert
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(arzt);
  });

  it('loeschen_vorhandenerArzt_sendetDeleteMitId', () => {
    // Arrange
    let fertig = false;

    // Act
    service.loeschen(3).subscribe({ complete: () => (fertig = true) });
    const request = httpTesting.expectOne('/api/aerzte/3');
    request.flush(null, { status: 204, statusText: 'No Content' });

    // Assert
    expect(request.request.method).toBe('DELETE');
    expect(fertig).toBe(true);
  });

  it('loeschen_arztHatTermine_gibtFehler409Weiter', () => {
    // Arrange
    let status = 0;
    let detail = '';

    // Act
    service.loeschen(3).subscribe({
      error: (error) => {
        status = error.status;
        detail = error.error.detail;
      },
    });
    httpTesting
      .expectOne('/api/aerzte/3')
      .flush({ status: 409, detail: 'Arzt hat noch Termine.' }, { status: 409, statusText: 'Conflict' });

    // Assert
    expect(status).toBe(409);
    expect(detail).toBe('Arzt hat noch Termine.');
  });
});
