package io.github.messaoudidev.terminbuchung.termin;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record TerminRequest(

        @NotNull(message = "Arzt muss angegeben werden")
        Long arztId,

        @NotNull(message = "Beginn muss angegeben werden")
        LocalDateTime beginn
) {
}
