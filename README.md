# Terminbuchung für Arztpraxen

> 🚧 **In Arbeit.** Das Projekt wird Schritt für Schritt erweitert. Aktueller Stand siehe [Fortschritt](#fortschritt).

Eine Webanwendung, mit der Patienten Termine bei den Ärzten einer Praxis buchen und stornieren können. Die Praxis verwaltet ihre Ärzte.

Grundprinzip: **wenige Funktionen, aber sauber umgesetzt und getestet.**

## Technik

| Bereich | Technik |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Bean Validation |
| Datenbank | PostgreSQL 18 (Docker Compose), Flyway für das Schema |
| API-Dokumentation | OpenAPI / Swagger UI (springdoc) |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc |
| Geplant | Spring Security mit JWT, Angular-Frontend, GitHub Actions |

## Umsetzung

- **Schichten pro Feature:** Controller → Service → Repository, Pakete nach Feature (`arzt`, …).
- **DTOs als Java Records:** Entities werden nie direkt als JSON angenommen oder zurückgegeben.
- **Einheitliche Fehler:** zentral mit `@RestControllerAdvice` als `ProblemDetail` (RFC 9457): 400 mit Meldung pro Feld, 404, nie ein Stacktrace beim Client.
- **Datenbankschema nur über Flyway**, Hibernate prüft nur (`ddl-auto=validate`).
- **Keine Secrets im Repository:** Das Datenbank-Passwort kommt aus einer `.env`-Datei bzw. Umgebungsvariable.

## Lokal starten

Voraussetzungen: Java 21, Docker Desktop.

```bash
# 1. Passwort-Datei anlegen und ein eigenes Passwort eintragen
cp .env.example .env

# 2. Datenbank starten
docker compose up -d

# 3. Backend starten (im Ordner backend/)
cd backend
./mvnw spring-boot:run
```

Unter Windows (PowerShell): `Copy-Item .env.example .env` und `.\mvnw.cmd spring-boot:run`.

Swagger UI: http://localhost:8080/swagger-ui.html

### Tests

```bash
cd backend
./mvnw test
```

Die Tests brauchen die laufende Datenbank (`docker compose up -d`).

## API (bisher)

| Methode | Pfad | Beschreibung | Antworten |
|---|---|---|---|
| GET | `/api/aerzte` | alle Ärzte | 200 |
| GET | `/api/aerzte/{id}` | ein Arzt | 200, 404 |
| POST | `/api/aerzte` | Arzt anlegen | 201, 400 |
| PUT | `/api/aerzte/{id}` | Arzt ändern | 200, 400, 404 |
| DELETE | `/api/aerzte/{id}` | Arzt löschen | 204, 404 |

## Fortschritt

- [x] Projekt-Setup: Spring Boot, PostgreSQL mit Docker Compose, Flyway
- [x] Ärzte verwalten (CRUD) mit Validierung, Fehlerbehandlung, Swagger und Tests
- [ ] Login mit JWT und Rollen (Praxis, Patient)
- [ ] Termine buchen und stornieren, inkl. Schutz vor Doppelbuchungen
- [ ] Angular-Frontend
- [ ] GitHub Actions (automatische Tests)
