package io.github.messaoudidev.terminbuchung.benutzer;

public class EmailBereitsVergebenException extends RuntimeException {

    public EmailBereitsVergebenException() {
        super("Diese E-Mail ist bereits vergeben.");
    }
}
