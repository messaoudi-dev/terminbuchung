package io.github.messaoudidev.terminbuchung.termin;

import java.time.Duration;
import java.time.LocalDateTime;

import io.github.messaoudidev.terminbuchung.arzt.Arzt;
import io.github.messaoudidev.terminbuchung.benutzer.Benutzer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

@Entity
@Table(name = "termin")
@Getter
public class Termin {

    public static final Duration DAUER = Duration.ofMinutes(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arzt_id", nullable = false)
    private Arzt arzt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Benutzer patient;

    @Column(nullable = false)
    private LocalDateTime beginn;

    @Column(nullable = false)
    private LocalDateTime ende;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TerminStatus status;

    // Schützt vor gleichzeitigen Änderungen am selben Termin, z. B. zwei Stornierungen.
    @Version
    @Column(nullable = false)
    private Long version;

    protected Termin() {
    }

    public Termin(Arzt arzt, Benutzer patient, LocalDateTime beginn) {
        this.arzt = arzt;
        this.patient = patient;
        this.beginn = beginn;
        this.ende = beginn.plus(DAUER);
        this.status = TerminStatus.GEBUCHT;
    }

    public void stornieren() {
        if (status == TerminStatus.STORNIERT) {
            throw new StornierungNichtMoeglichException("Der Termin ist bereits storniert.");
        }
        status = TerminStatus.STORNIERT;
    }

    public boolean gehoertZu(Long benutzerId) {
        return patient.getId().equals(benutzerId);
    }
}
