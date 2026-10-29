package io.github.messaoudidev.terminbuchung.arzt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "arzt")
@Getter
public class Arzt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String fachrichtung;

    protected Arzt() {
    }

    public Arzt(String name, String fachrichtung) {
        this.name = name;
        this.fachrichtung = fachrichtung;
    }

    public void aendern(String name, String fachrichtung) {
        this.name = name;
        this.fachrichtung = fachrichtung;
    }
}
