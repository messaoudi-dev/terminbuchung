package io.github.messaoudidev.terminbuchung.termin;

public class TerminNotFoundException extends RuntimeException {

    public TerminNotFoundException(Long id) {
        super("Termin mit ID " + id + " wurde nicht gefunden.");
    }
}
