package io.github.messaoudidev.terminbuchung.benutzer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BenutzerRepository extends JpaRepository<Benutzer, Long> {

    Optional<Benutzer> findByEmail(String email);

    boolean existsByEmail(String email);
}
