package io.github.messaoudidev.terminbuchung.arzt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArztRequest(

        @NotBlank(message = "Name darf nicht leer sein")
        @Size(max = 100, message = "Name darf höchstens 100 Zeichen lang sein")
        String name,

        @NotBlank(message = "Fachrichtung darf nicht leer sein")
        @Size(max = 100, message = "Fachrichtung darf höchstens 100 Zeichen lang sein")
        String fachrichtung
) {
}
