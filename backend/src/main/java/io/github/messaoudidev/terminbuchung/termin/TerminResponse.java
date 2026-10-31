package io.github.messaoudidev.terminbuchung.termin;

import java.time.LocalDateTime;

public record TerminResponse(
        Long id,
        Long arztId,
        String arztName,
        Long patientId,
        LocalDateTime beginn,
        LocalDateTime ende,
        TerminStatus status
) {
}
