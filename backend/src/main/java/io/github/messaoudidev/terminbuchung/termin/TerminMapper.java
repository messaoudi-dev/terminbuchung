package io.github.messaoudidev.terminbuchung.termin;

import org.springframework.stereotype.Component;

@Component
public class TerminMapper {

    public TerminResponse toResponse(Termin termin) {
        return new TerminResponse(
                termin.getId(),
                termin.getArzt().getId(),
                termin.getArzt().getName(),
                termin.getPatient().getId(),
                termin.getBeginn(),
                termin.getEnde(),
                termin.getStatus());
    }
}
