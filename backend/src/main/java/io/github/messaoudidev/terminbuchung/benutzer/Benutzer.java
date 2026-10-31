package io.github.messaoudidev.terminbuchung.benutzer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

// Bewusst ohne toString(), damit der Passwort-Hash nie im Log landet.
@Entity
@Table(name = "benutzer")
@Getter
public class Benutzer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "passwort_hash", nullable = false, length = 60)
    private String passwortHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rolle rolle;

    protected Benutzer() {
    }

    public Benutzer(String email, String passwortHash, Rolle rolle) {
        this.email = email;
        this.passwortHash = passwortHash;
        this.rolle = rolle;
    }
}
