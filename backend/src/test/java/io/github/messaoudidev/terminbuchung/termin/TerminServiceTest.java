package io.github.messaoudidev.terminbuchung.termin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import io.github.messaoudidev.terminbuchung.arzt.Arzt;
import io.github.messaoudidev.terminbuchung.arzt.ArztNotFoundException;
import io.github.messaoudidev.terminbuchung.arzt.ArztRepository;
import io.github.messaoudidev.terminbuchung.benutzer.Benutzer;
import io.github.messaoudidev.terminbuchung.benutzer.BenutzerRepository;
import io.github.messaoudidev.terminbuchung.benutzer.Rolle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TerminServiceTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    // Montag, 2. November 2026, 10:00 Uhr in Berlin.
    private static final LocalDateTime JETZT = LocalDateTime.of(2026, 11, 2, 10, 0);

    private static final Long ARZT_ID = 2L;
    private static final Long PATIENT_ID = 5L;
    private static final Long ANDERER_PATIENT_ID = 6L;
    private static final Long PRAXIS_ID = 1L;

    @Mock
    private TerminRepository terminRepository;

    @Mock
    private ArztRepository arztRepository;

    @Mock
    private BenutzerRepository benutzerRepository;

    private TerminService terminService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(JETZT.atZone(BERLIN).toInstant(), BERLIN);
        terminService = new TerminService(terminRepository, arztRepository, benutzerRepository,
                new TerminMapper(), clock);
    }

    // --- buchen ---

    @Test
    void buchen_gueltigerTermin_speichertTerminMitEndeNach30MinutenUndStatusGebucht() {
        // Arrange
        LocalDateTime beginn = LocalDateTime.of(2026, 11, 3, 9, 30);
        stubGueltigeBuchung(beginn);

        // Act
        TerminResponse ergebnis = terminService.buchen(new TerminRequest(ARZT_ID, beginn), PATIENT_ID);

        // Assert
        ArgumentCaptor<Termin> captor = ArgumentCaptor.forClass(Termin.class);
        verify(terminRepository).save(captor.capture());
        Termin gespeichert = captor.getValue();
        assertThat(gespeichert.getArzt().getId()).isEqualTo(ARZT_ID);
        assertThat(gespeichert.getPatient().getId()).isEqualTo(PATIENT_ID);
        assertThat(gespeichert.getEnde()).isEqualTo(LocalDateTime.of(2026, 11, 3, 10, 0));
        assertThat(gespeichert.getStatus()).isEqualTo(TerminStatus.GEBUCHT);
        assertThat(ergebnis).isEqualTo(new TerminResponse(1L, ARZT_ID, "Dr. Müller", PATIENT_ID,
                beginn, LocalDateTime.of(2026, 11, 3, 10, 0), TerminStatus.GEBUCHT));
    }

    @Test
    void buchen_beginnInVergangenheit_wirftUngueltigeTerminzeitException() {
        // Arrange
        TerminRequest request = new TerminRequest(ARZT_ID, LocalDateTime.of(2026, 11, 2, 9, 30));

        // Act + Assert
        assertThatThrownBy(() -> terminService.buchen(request, PATIENT_ID))
                .isInstanceOf(UngueltigeTerminzeitException.class)
                .hasMessage("Der Termin darf nicht in der Vergangenheit liegen.");
        verify(terminRepository, never()).save(any());
    }

    @Test
    void buchen_beginnGenauJetzt_wirftUngueltigeTerminzeitException() {
        // Arrange
        TerminRequest request = new TerminRequest(ARZT_ID, JETZT);

        // Act + Assert
        assertThatThrownBy(() -> terminService.buchen(request, PATIENT_ID))
                .isInstanceOf(UngueltigeTerminzeitException.class)
                .hasMessage("Der Termin darf nicht in der Vergangenheit liegen.");
    }

    @Test
    void buchen_beginnNichtZurVollenOderHalbenStunde_wirftUngueltigeTerminzeitException() {
        // Arrange
        TerminRequest request = new TerminRequest(ARZT_ID, LocalDateTime.of(2026, 11, 3, 9, 15));

        // Act + Assert
        assertThatThrownBy(() -> terminService.buchen(request, PATIENT_ID))
                .isInstanceOf(UngueltigeTerminzeitException.class)
                .hasMessage("Termine beginnen nur zur vollen oder halben Stunde.");
        verify(terminRepository, never()).save(any());
    }

    @Test
    void buchen_beginnMitSekunden_wirftUngueltigeTerminzeitException() {
        // Arrange
        TerminRequest request = new TerminRequest(ARZT_ID, LocalDateTime.of(2026, 11, 3, 9, 30, 15));

        // Act + Assert
        assertThatThrownBy(() -> terminService.buchen(request, PATIENT_ID))
                .isInstanceOf(UngueltigeTerminzeitException.class)
                .hasMessage("Termine beginnen nur zur vollen oder halben Stunde.");
    }

    @Test
    void buchen_beginnUm8Uhr_istErlaubt() {
        // Arrange
        LocalDateTime beginn = LocalDateTime.of(2026, 11, 3, 8, 0);
        stubGueltigeBuchung(beginn);

        // Act
        TerminResponse ergebnis = terminService.buchen(new TerminRequest(ARZT_ID, beginn), PATIENT_ID);

        // Assert
        assertThat(ergebnis.beginn()).isEqualTo(beginn);
    }

    @Test
    void buchen_beginnUm17Uhr30_istErlaubt() {
        // Arrange
        LocalDateTime beginn = LocalDateTime.of(2026, 11, 3, 17, 30);
        stubGueltigeBuchung(beginn);

        // Act
        TerminResponse ergebnis = terminService.buchen(new TerminRequest(ARZT_ID, beginn), PATIENT_ID);

        // Assert
        assertThat(ergebnis.ende()).isEqualTo(LocalDateTime.of(2026, 11, 3, 18, 0));
    }

    @Test
    void buchen_beginnUm7Uhr30_wirftUngueltigeTerminzeitException() {
        assertAusserhalbDerOeffnungszeiten(LocalDateTime.of(2026, 11, 3, 7, 30));
    }

    @Test
    void buchen_beginnUm18Uhr_wirftUngueltigeTerminzeitException() {
        assertAusserhalbDerOeffnungszeiten(LocalDateTime.of(2026, 11, 3, 18, 0));
    }

    @Test
    void buchen_beginnAmSamstag_wirftUngueltigeTerminzeitException() {
        assertAusserhalbDerOeffnungszeiten(LocalDateTime.of(2026, 11, 7, 10, 0));
    }

    @Test
    void buchen_beginnAmSonntag_wirftUngueltigeTerminzeitException() {
        assertAusserhalbDerOeffnungszeiten(LocalDateTime.of(2026, 11, 8, 10, 0));
    }

    @Test
    void buchen_arztUnbekannt_wirftArztNotFoundException() {
        // Arrange
        when(arztRepository.findById(99L)).thenReturn(Optional.empty());
        TerminRequest request = new TerminRequest(99L, LocalDateTime.of(2026, 11, 3, 9, 30));

        // Act + Assert
        assertThatThrownBy(() -> terminService.buchen(request, PATIENT_ID))
                .isInstanceOf(ArztNotFoundException.class);
        verify(terminRepository, never()).save(any());
    }

    @Test
    void buchen_zeitBeimArztBereitsVergeben_wirftTerminBereitsVergebenException() {
        // Arrange
        LocalDateTime beginn = LocalDateTime.of(2026, 11, 3, 9, 30);
        when(arztRepository.findById(ARZT_ID)).thenReturn(Optional.of(arzt()));
        when(terminRepository.existsByArztIdAndBeginnAndStatus(ARZT_ID, beginn, TerminStatus.GEBUCHT))
                .thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> terminService.buchen(new TerminRequest(ARZT_ID, beginn), PATIENT_ID))
                .isInstanceOf(TerminBereitsVergebenException.class)
                .hasMessage("Der Arzt hat zu dieser Zeit bereits einen Termin.");
        verify(terminRepository, never()).save(any());
    }

    // --- findeAlle ---

    @Test
    void findeAlle_alsPatient_liefertNurEigeneTermine() {
        // Arrange
        Termin eigener = termin(10L, PATIENT_ID, LocalDateTime.of(2026, 11, 3, 9, 30));
        when(terminRepository.findAllByPatientIdOrderByBeginnAsc(PATIENT_ID)).thenReturn(List.of(eigener));

        // Act
        List<TerminResponse> ergebnis = terminService.findeAlle(PATIENT_ID, Rolle.PATIENT);

        // Assert
        assertThat(ergebnis).extracting(TerminResponse::id).containsExactly(10L);
        verify(terminRepository, never()).findAllByOrderByBeginnAsc();
    }

    @Test
    void findeAlle_alsPraxis_liefertAlleTermine() {
        // Arrange
        Termin ersterTermin = termin(10L, PATIENT_ID, LocalDateTime.of(2026, 11, 3, 9, 30));
        Termin zweiterTermin = termin(11L, ANDERER_PATIENT_ID, LocalDateTime.of(2026, 11, 3, 10, 0));
        when(terminRepository.findAllByOrderByBeginnAsc()).thenReturn(List.of(ersterTermin, zweiterTermin));

        // Act
        List<TerminResponse> ergebnis = terminService.findeAlle(PRAXIS_ID, Rolle.PRAXIS);

        // Assert
        assertThat(ergebnis).extracting(TerminResponse::id).containsExactly(10L, 11L);
    }

    // --- findeNachId ---

    @Test
    void findeNachId_eigenerTerminAlsPatient_liefertTermin() {
        // Arrange
        Termin termin = termin(10L, PATIENT_ID, LocalDateTime.of(2026, 11, 3, 9, 30));
        when(terminRepository.findById(10L)).thenReturn(Optional.of(termin));

        // Act
        TerminResponse ergebnis = terminService.findeNachId(10L, PATIENT_ID, Rolle.PATIENT);

        // Assert
        assertThat(ergebnis.id()).isEqualTo(10L);
        assertThat(ergebnis.arztName()).isEqualTo("Dr. Müller");
    }

    @Test
    void findeNachId_fremderTerminAlsPatient_wirftAccessDeniedException() {
        // Arrange
        Termin termin = termin(10L, ANDERER_PATIENT_ID, LocalDateTime.of(2026, 11, 3, 9, 30));
        when(terminRepository.findById(10L)).thenReturn(Optional.of(termin));

        // Act + Assert
        assertThatThrownBy(() -> terminService.findeNachId(10L, PATIENT_ID, Rolle.PATIENT))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findeNachId_fremderTerminAlsPraxis_liefertTermin() {
        // Arrange
        Termin termin = termin(10L, PATIENT_ID, LocalDateTime.of(2026, 11, 3, 9, 30));
        when(terminRepository.findById(10L)).thenReturn(Optional.of(termin));

        // Act
        TerminResponse ergebnis = terminService.findeNachId(10L, PRAXIS_ID, Rolle.PRAXIS);

        // Assert
        assertThat(ergebnis.patientId()).isEqualTo(PATIENT_ID);
    }

    @Test
    void findeNachId_unbekannt_wirftTerminNotFoundException() {
        // Arrange
        when(terminRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> terminService.findeNachId(99L, PATIENT_ID, Rolle.PATIENT))
                .isInstanceOf(TerminNotFoundException.class)
                .hasMessage("Termin mit ID 99 wurde nicht gefunden.");
    }

    // --- stornieren ---

    @Test
    void stornieren_alsPatientMehrAls24StundenVorher_setztStatusStorniert() {
        // Arrange
        Termin termin = termin(10L, PATIENT_ID, LocalDateTime.of(2026, 11, 4, 10, 0));
        when(terminRepository.findById(10L)).thenReturn(Optional.of(termin));

        // Act
        TerminResponse ergebnis = terminService.stornieren(10L, PATIENT_ID, Rolle.PATIENT);

        // Assert
        assertThat(termin.getStatus()).isEqualTo(TerminStatus.STORNIERT);
        assertThat(ergebnis.status()).isEqualTo(TerminStatus.STORNIERT);
    }

    @Test
    void stornieren_alsPatientGenau24StundenVorher_setztStatusStorniert() {
        // Arrange
        Termin termin = termin(10L, PATIENT_ID, JETZT.plusHours(24));
        when(terminRepository.findById(10L)).thenReturn(Optional.of(termin));

        // Act
        terminService.stornieren(10L, PATIENT_ID, Rolle.PATIENT);

        // Assert
        assertThat(termin.getStatus()).isEqualTo(TerminStatus.STORNIERT);
    }

    @Test
    void stornieren_alsPatientWenigerAls24StundenVorher_wirftStornierungNichtMoeglichException() {
        // Arrange
        Termin termin = termin(10L, PATIENT_ID, LocalDateTime.of(2026, 11, 3, 9, 30));
        when(terminRepository.findById(10L)).thenReturn(Optional.of(termin));

        // Act + Assert
        assertThatThrownBy(() -> terminService.stornieren(10L, PATIENT_ID, Rolle.PATIENT))
                .isInstanceOf(StornierungNichtMoeglichException.class)
                .hasMessage("Ein Termin kann nur bis 24 Stunden vor Beginn storniert werden.");
        assertThat(termin.getStatus()).isEqualTo(TerminStatus.GEBUCHT);
    }

    @Test
    void stornieren_fremderTerminAlsPatient_wirftAccessDeniedException() {
        // Arrange
        Termin termin = termin(10L, ANDERER_PATIENT_ID, LocalDateTime.of(2026, 11, 4, 10, 0));
        when(terminRepository.findById(10L)).thenReturn(Optional.of(termin));

        // Act + Assert
        assertThatThrownBy(() -> terminService.stornieren(10L, PATIENT_ID, Rolle.PATIENT))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(termin.getStatus()).isEqualTo(TerminStatus.GEBUCHT);
    }

    @Test
    void stornieren_alsPraxisWenigerAls24StundenVorher_setztStatusStorniert() {
        // Arrange
        Termin termin = termin(10L, PATIENT_ID, LocalDateTime.of(2026, 11, 2, 14, 0));
        when(terminRepository.findById(10L)).thenReturn(Optional.of(termin));

        // Act
        terminService.stornieren(10L, PRAXIS_ID, Rolle.PRAXIS);

        // Assert
        assertThat(termin.getStatus()).isEqualTo(TerminStatus.STORNIERT);
    }

    @Test
    void stornieren_bereitsStorniert_wirftStornierungNichtMoeglichException() {
        // Arrange
        Termin termin = termin(10L, PATIENT_ID, LocalDateTime.of(2026, 11, 4, 10, 0));
        termin.stornieren();
        when(terminRepository.findById(10L)).thenReturn(Optional.of(termin));

        // Act + Assert
        assertThatThrownBy(() -> terminService.stornieren(10L, PATIENT_ID, Rolle.PATIENT))
                .isInstanceOf(StornierungNichtMoeglichException.class)
                .hasMessage("Der Termin ist bereits storniert.");
    }

    @Test
    void stornieren_unbekannt_wirftTerminNotFoundException() {
        // Arrange
        when(terminRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> terminService.stornieren(99L, PRAXIS_ID, Rolle.PRAXIS))
                .isInstanceOf(TerminNotFoundException.class);
    }

    // --- Hilfsmethoden ---

    private void assertAusserhalbDerOeffnungszeiten(LocalDateTime beginn) {
        // Arrange
        TerminRequest request = new TerminRequest(ARZT_ID, beginn);

        // Act + Assert
        assertThatThrownBy(() -> terminService.buchen(request, PATIENT_ID))
                .isInstanceOf(UngueltigeTerminzeitException.class)
                .hasMessage("Termine sind nur Montag bis Freitag zwischen 8:00 und 18:00 Uhr möglich (letzter Beginn 17:30).");
        verify(terminRepository, never()).save(any());
    }

    private void stubGueltigeBuchung(LocalDateTime beginn) {
        when(arztRepository.findById(ARZT_ID)).thenReturn(Optional.of(arzt()));
        when(terminRepository.existsByArztIdAndBeginnAndStatus(ARZT_ID, beginn, TerminStatus.GEBUCHT))
                .thenReturn(false);
        when(benutzerRepository.getReferenceById(PATIENT_ID)).thenReturn(patient(PATIENT_ID));
        // Die ID vergibt sonst die Datenbank beim Speichern.
        when(terminRepository.save(any(Termin.class))).thenAnswer(aufruf -> {
            Termin termin = aufruf.getArgument(0);
            ReflectionTestUtils.setField(termin, "id", 1L);
            return termin;
        });
    }

    private Arzt arzt() {
        Arzt arzt = new Arzt("Dr. Müller", "Allgemeinmedizin");
        ReflectionTestUtils.setField(arzt, "id", ARZT_ID);
        return arzt;
    }

    private Benutzer patient(Long id) {
        Benutzer patient = new Benutzer("patient" + id + "@example.com", "hash", Rolle.PATIENT);
        ReflectionTestUtils.setField(patient, "id", id);
        return patient;
    }

    private Termin termin(Long id, Long patientId, LocalDateTime beginn) {
        Termin termin = new Termin(arzt(), patient(patientId), beginn);
        ReflectionTestUtils.setField(termin, "id", id);
        return termin;
    }
}
