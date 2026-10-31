package io.github.messaoudidev.terminbuchung.termin;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import io.github.messaoudidev.terminbuchung.arzt.Arzt;
import io.github.messaoudidev.terminbuchung.arzt.ArztNotFoundException;
import io.github.messaoudidev.terminbuchung.arzt.ArztRepository;
import io.github.messaoudidev.terminbuchung.benutzer.Benutzer;
import io.github.messaoudidev.terminbuchung.benutzer.BenutzerRepository;
import io.github.messaoudidev.terminbuchung.benutzer.Rolle;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Gibt nur DTOs zurück: Wegen open-in-view=false muss das Mapping (Arzt ist LAZY) innerhalb der Transaktion passieren.
@Service
public class TerminService {

    // Feste Öffnungszeiten der Praxis (Regel 6). Feiertage sind bewusst nicht berücksichtigt.
    static final LocalTime ERSTER_BEGINN = LocalTime.of(8, 0);
    static final LocalTime LETZTER_BEGINN = LocalTime.of(17, 30);
    static final Duration STORNO_FRIST = Duration.ofHours(24);

    private final TerminRepository terminRepository;
    private final ArztRepository arztRepository;
    private final BenutzerRepository benutzerRepository;
    private final TerminMapper terminMapper;
    private final Clock clock;

    public TerminService(TerminRepository terminRepository, ArztRepository arztRepository,
                         BenutzerRepository benutzerRepository, TerminMapper terminMapper, Clock clock) {
        this.terminRepository = terminRepository;
        this.arztRepository = arztRepository;
        this.benutzerRepository = benutzerRepository;
        this.terminMapper = terminMapper;
        this.clock = clock;
    }

    // Die Vorab-Prüfung liefert eine verständliche Meldung. Bei gleichzeitigen Buchungen
    // sichert der Partial Unique Index in der Datenbank ab (DataIntegrityViolationException → 409).
    @Transactional
    public TerminResponse buchen(TerminRequest request, Long patientId) {
        pruefeTerminzeit(request.beginn());
        Arzt arzt = arztRepository.findById(request.arztId())
                .orElseThrow(() -> new ArztNotFoundException(request.arztId()));
        if (terminRepository.existsByArztIdAndBeginnAndStatus(arzt.getId(), request.beginn(), TerminStatus.GEBUCHT)) {
            throw new TerminBereitsVergebenException();
        }
        // Der Patient kommt aus einem gültigen Token und existiert; eine Abfrage ist nicht nötig.
        Benutzer patient = benutzerRepository.getReferenceById(patientId);
        Termin termin = terminRepository.save(new Termin(arzt, patient, request.beginn()));
        return terminMapper.toResponse(termin);
    }

    @Transactional(readOnly = true)
    public List<TerminResponse> findeAlle(Long benutzerId, Rolle rolle) {
        List<Termin> termine = rolle == Rolle.PRAXIS
                ? terminRepository.findAllByOrderByBeginnAsc()
                : terminRepository.findAllByPatientIdOrderByBeginnAsc(benutzerId);
        return termine.stream()
                .map(terminMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TerminResponse findeNachId(Long id, Long benutzerId, Rolle rolle) {
        return terminMapper.toResponse(ladeTerminMitZugriff(id, benutzerId, rolle));
    }

    // Kein save() nötig: Hibernate schreibt Änderungen an geladenen Entities beim Commit.
    @Transactional
    public TerminResponse stornieren(Long id, Long benutzerId, Rolle rolle) {
        Termin termin = ladeTerminMitZugriff(id, benutzerId, rolle);
        if (rolle == Rolle.PATIENT && LocalDateTime.now(clock).plus(STORNO_FRIST).isAfter(termin.getBeginn())) {
            throw new StornierungNichtMoeglichException("Ein Termin kann nur bis 24 Stunden vor Beginn storniert werden.");
        }
        termin.stornieren();
        return terminMapper.toResponse(termin);
    }

    private Termin ladeTerminMitZugriff(Long id, Long benutzerId, Rolle rolle) {
        Termin termin = terminRepository.findById(id)
                .orElseThrow(() -> new TerminNotFoundException(id));
        if (rolle == Rolle.PATIENT && !termin.gehoertZu(benutzerId)) {
            throw new AccessDeniedException("Fremder Termin");
        }
        return termin;
    }

    private void pruefeTerminzeit(LocalDateTime beginn) {
        if (!beginn.isAfter(LocalDateTime.now(clock))) {
            throw new UngueltigeTerminzeitException("Der Termin darf nicht in der Vergangenheit liegen.");
        }
        boolean volleOderHalbeStunde = beginn.getMinute() == 0 || beginn.getMinute() == 30;
        if (!volleOderHalbeStunde || beginn.getSecond() != 0 || beginn.getNano() != 0) {
            throw new UngueltigeTerminzeitException("Termine beginnen nur zur vollen oder halben Stunde.");
        }
        DayOfWeek tag = beginn.getDayOfWeek();
        LocalTime uhrzeit = beginn.toLocalTime();
        if (tag == DayOfWeek.SATURDAY || tag == DayOfWeek.SUNDAY
                || uhrzeit.isBefore(ERSTER_BEGINN) || uhrzeit.isAfter(LETZTER_BEGINN)) {
            throw new UngueltigeTerminzeitException(
                    "Termine sind nur Montag bis Freitag zwischen 8:00 und 18:00 Uhr möglich (letzter Beginn 17:30).");
        }
    }
}
