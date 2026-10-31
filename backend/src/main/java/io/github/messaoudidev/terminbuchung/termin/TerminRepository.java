package io.github.messaoudidev.terminbuchung.termin;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminRepository extends JpaRepository<Termin, Long> {

    boolean existsByArztIdAndBeginnAndStatus(Long arztId, LocalDateTime beginn, TerminStatus status);

    // Lädt den Arzt gleich mit, sonst gäbe es für den Arztnamen eine eigene Abfrage pro Termin.
    @EntityGraph(attributePaths = "arzt")
    List<Termin> findAllByOrderByBeginnAsc();

    @EntityGraph(attributePaths = "arzt")
    List<Termin> findAllByPatientIdOrderByBeginnAsc(Long patientId);
}
