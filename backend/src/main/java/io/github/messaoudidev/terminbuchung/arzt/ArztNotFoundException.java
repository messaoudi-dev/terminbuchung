package io.github.messaoudidev.terminbuchung.arzt;

public class ArztNotFoundException extends RuntimeException {

    public ArztNotFoundException(Long id) {
        super("Arzt mit ID " + id + " wurde nicht gefunden.");
    }
}
