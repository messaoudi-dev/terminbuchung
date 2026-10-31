package io.github.messaoudidev.terminbuchung.benutzer;

public record LoginResponse(String token, long expiresIn) {
}
