package io.github.messaoudidev.terminbuchung.termin;

public class TerminBereitsVergebenException extends RuntimeException {

    public TerminBereitsVergebenException() {
        super("Der Arzt hat zu dieser Zeit bereits einen Termin.");
    }
}
