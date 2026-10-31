package io.github.messaoudidev.terminbuchung.benutzer;

// Eine Meldung für unbekannte E-Mail und falsches Passwort, damit niemand testen kann, welche E-Mails existieren.
public class UngueltigeAnmeldedatenException extends RuntimeException {

    public UngueltigeAnmeldedatenException() {
        super("E-Mail oder Passwort ist falsch.");
    }
}
