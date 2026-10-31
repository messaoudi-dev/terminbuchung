package io.github.messaoudidev.terminbuchung.benutzer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "E-Mail darf nicht leer sein")
        String email,

        @NotBlank(message = "Passwort darf nicht leer sein")
        @Size(max = 72, message = "Passwort darf höchstens 72 Zeichen lang sein")
        String passwort
) {
}
