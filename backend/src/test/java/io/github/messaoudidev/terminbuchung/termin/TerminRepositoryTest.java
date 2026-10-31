package io.github.messaoudidev.terminbuchung.termin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import io.github.messaoudidev.terminbuchung.TestcontainersConfiguration;
import io.github.messaoudidev.terminbuchung.arzt.Arzt;
import io.github.messaoudidev.terminbuchung.benutzer.Benutzer;
import io.github.messaoudidev.terminbuchung.benutzer.Rolle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

// Prüft den Partial Unique Index aus V3 gegen ein echtes PostgreSQL; ein Mock kann das nicht.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(locations = "classpath:test.properties")
class TerminRepositoryTest {

    private static final LocalDateTime BEGINN = LocalDateTime.of(2026, 11, 3, 9, 30);

    @Autowired
    private TerminRepository terminRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Arzt arzt;
    private Benutzer patient;
    private Benutzer andererPatient;

    @BeforeEach
    void setUp() {
        arzt = entityManager.persist(new Arzt("Dr. Müller", "Allgemeinmedizin"));
        patient = entityManager.persist(new Benutzer("anna@example.com", "hash", Rolle.PATIENT));
        andererPatient = entityManager.persist(new Benutzer("ben@example.com", "hash", Rolle.PATIENT));
    }

    @Test
    void speichern_zweiGebuchteTermineGleicherArztGleicheZeit_wirftDataIntegrityViolationException() {
        // Arrange
        terminRepository.saveAndFlush(new Termin(arzt, patient, BEGINN));
        Termin doppelt = new Termin(arzt, andererPatient, BEGINN);

        // Act + Assert
        assertThatThrownBy(() -> terminRepository.saveAndFlush(doppelt))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void speichern_nachStornoGleicheZeit_istWiederBuchbar() {
        // Arrange
        Termin ersterTermin = terminRepository.saveAndFlush(new Termin(arzt, patient, BEGINN));
        ersterTermin.stornieren();
        terminRepository.saveAndFlush(ersterTermin);

        // Act
        Termin neuerTermin = terminRepository.saveAndFlush(new Termin(arzt, andererPatient, BEGINN));

        // Assert
        assertThat(neuerTermin.getId()).isNotNull();
        assertThat(terminRepository.existsByArztIdAndBeginnAndStatus(arzt.getId(), BEGINN, TerminStatus.GEBUCHT))
                .isTrue();
    }
}
