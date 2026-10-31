# Terminbuchung für Arztpraxen

> 🚧 **In Arbeit.** Das Projekt wird Schritt für Schritt erweitert. Aktueller Stand siehe [Fortschritt](#fortschritt).

Eine Webanwendung, mit der Patienten Termine bei den Ärzten einer Praxis buchen und stornieren können. Die Praxis verwaltet ihre Ärzte.

Grundprinzip: **wenige Funktionen, aber sauber umgesetzt und getestet.**

## Technik

| Bereich | Technik |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Bean Validation |
| Sicherheit | Spring Security, JWT (OAuth2 Resource Server, HS256), BCrypt |
| Datenbank | PostgreSQL 18 (Docker Compose), Flyway für das Schema |
| API-Dokumentation | OpenAPI / Swagger UI (springdoc) |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc, Spring Security Test |
| Geplant | Angular-Frontend, GitHub Actions |

## Umsetzung

- **Schichten pro Feature:** Controller → Service → Repository, Pakete nach Feature (`arzt`, `benutzer`, `security`).
- **DTOs als Java Records:** Entities werden nie direkt als JSON angenommen oder zurückgegeben.
- **Einheitliche Fehler:** zentral mit `@RestControllerAdvice` als `ProblemDetail` (RFC 9457): 400 mit Meldung pro Feld, 401, 403, 404, 409, nie ein Stacktrace beim Client.
- **Login mit JWT:** stateless, Rollen `PRAXIS` und `PATIENT`. Passwörter nur als BCrypt-Hash, nie in einer Antwort oder im Log.
- **Datenbankschema nur über Flyway**, Hibernate prüft nur (`ddl-auto=validate`).
- **Keine Secrets im Repository:** Datenbank-Passwort, JWT-Secret und Zugangsdaten der Praxis kommen aus einer `.env`-Datei bzw. Umgebungsvariablen.

## Lokal starten

Voraussetzungen: Java 21, Docker Desktop.

```bash
# 1. Konfigurationsdatei anlegen und eigene Werte eintragen
#    (DB_PASSWORD, JWT_SECRET mit mindestens 32 Zeichen, PRAXIS_EMAIL, PRAXIS_PASSWORD)
cp .env.example .env

# 2. Datenbank starten
docker compose up -d

# 3. Backend starten (im Ordner backend/)
cd backend
./mvnw spring-boot:run
```

Unter Windows (PowerShell): `Copy-Item .env.example .env` und `.\mvnw.cmd spring-boot:run`.

Beim ersten Start legt die Anwendung den Praxis-Benutzer aus `PRAXIS_EMAIL` und `PRAXIS_PASSWORD` an.

### Ausprobieren mit Swagger UI

1. http://localhost:8080/swagger-ui.html öffnen
2. `POST /api/auth/login` mit den Praxis-Zugangsdaten aufrufen und das `token` kopieren
3. Oben auf **Authorize** klicken und das Token einfügen
4. Ärzte anlegen, ändern und löschen

### Tests

```bash
cd backend
./mvnw test
```

Die Tests brauchen die laufende Datenbank (`docker compose up -d`) und die Werte aus der `.env`.

## API (bisher)

| Methode | Pfad | Beschreibung | Zugriff | Antworten |
|---|---|---|---|---|
| POST | `/api/auth/register` | als Patient registrieren | offen | 201, 400, 409 |
| POST | `/api/auth/login` | anmelden, liefert ein JWT | offen | 200, 400, 401 |
| GET | `/api/aerzte` | alle Ärzte | angemeldet | 200, 401 |
| GET | `/api/aerzte/{id}` | ein Arzt | angemeldet | 200, 401, 404 |
| POST | `/api/aerzte` | Arzt anlegen | PRAXIS | 201, 400, 401, 403 |
| PUT | `/api/aerzte/{id}` | Arzt ändern | PRAXIS | 200, 400, 401, 403, 404 |
| DELETE | `/api/aerzte/{id}` | Arzt löschen | PRAXIS | 204, 401, 403, 404 |

## Fortschritt

- [x] Projekt-Setup: Spring Boot, PostgreSQL mit Docker Compose, Flyway
- [x] Ärzte verwalten (CRUD) mit Validierung, Fehlerbehandlung, Swagger und Tests
- [x] Login mit JWT und Rollen (Praxis, Patient)
- [ ] Termine buchen und stornieren, inkl. Schutz vor Doppelbuchungen
- [ ] Angular-Frontend
- [ ] GitHub Actions (automatische Tests)
