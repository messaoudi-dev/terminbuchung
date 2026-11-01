# Terminbuchung für Arztpraxen

[![CI](https://github.com/messaoudi-dev/terminbuchung/actions/workflows/ci.yml/badge.svg)](https://github.com/messaoudi-dev/terminbuchung/actions/workflows/ci.yml)

> 🚧 **In Arbeit.** Das Projekt wird Schritt für Schritt erweitert. Aktueller Stand siehe [Fortschritt](#fortschritt).

Eine Webanwendung, mit der Patienten Termine bei den Ärzten einer Praxis buchen und stornieren können. Die Praxis verwaltet ihre Ärzte und sieht alle Termine.

Grundprinzip: **wenige Funktionen, aber sauber umgesetzt und getestet.**

## Technik

| Bereich | Technik |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Bean Validation |
| Frontend | Angular 22, TypeScript, Angular Material, Vitest |
| Sicherheit | Spring Security, JWT (OAuth2 Resource Server, HS256), BCrypt |
| Datenbank | PostgreSQL 18 (Docker Compose), Flyway für das Schema |
| API-Dokumentation | OpenAPI / Swagger UI (springdoc) |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc, Spring Security Test, Testcontainers |
| CI | GitHub Actions (Backend- und Frontend-Tests bei jedem Push und Pull Request) |

## Umsetzung

- **Schichten pro Feature:** Controller → Service → Repository, Pakete nach Feature (`arzt`, `benutzer`, `termin`, `security`).
- **DTOs als Java Records:** Entities werden nie direkt als JSON angenommen oder zurückgegeben.
- **Einheitliche Fehler:** zentral mit `@RestControllerAdvice` als `ProblemDetail` (RFC 9457): 400, 401, 403, 404, 409, nie ein Stacktrace beim Client.
- **Login mit JWT:** stateless, Rollen `PRAXIS` und `PATIENT`. Passwörter nur als BCrypt-Hash, nie in einer Antwort oder im Log.
- **Geschäftsregeln nur im Backend:** keine Termine in der Vergangenheit, 30-Minuten-Raster, feste Öffnungszeiten, Stornieren für Patienten nur bis 24 Stunden vorher. Die aktuelle Zeit kommt über eine injizierte `Clock`, damit die Regeln testbar sind.
- **Keine Doppelbuchungen, auch bei gleichzeitigen Anfragen:** Ein Partial Unique Index in PostgreSQL erlaubt pro Arzt und Zeitpunkt nur einen gebuchten Termin; stornierte Termine geben den Zeitpunkt wieder frei. Gleichzeitige Änderungen am selben Termin verhindert Optimistic Locking (`@Version`).
- **Stornieren statt Löschen:** Ein Termin bekommt den Status `STORNIERT` und bleibt erhalten.
- **Datenbankschema nur über Flyway**, Hibernate prüft nur (`ddl-auto=validate`), `open-in-view=false`.
- **Frontend:** Im Entwicklungsmodus leitet ein Proxy `/api` an das Backend weiter. Das JWT liegt nur im `sessionStorage`; ein Interceptor hängt das Token an jede Anfrage. Guards prüfen Anmeldung und Rolle. Geschäftsregeln prüft nur das Backend, das Frontend zeigt dessen Fehlermeldungen an.
- **Keine Secrets im Repository:** Datenbank-Passwort, JWT-Secret und Zugangsdaten der Praxis kommen aus einer `.env`-Datei bzw. Umgebungsvariablen.

## Geschäftsregeln

| Regel | Bei Verstoß |
|---|---|
| Kein Termin in der Vergangenheit | 400 |
| Termine dauern 30 Minuten und beginnen zur vollen oder halben Stunde | 400 |
| Öffnungszeiten: Montag bis Freitag, 8:00 bis 18:00 Uhr (letzter Beginn 17:30) | 400 |
| Ein Arzt hat zu einer Zeit nur einen gebuchten Termin | 409 |
| Patienten sehen und stornieren nur eigene Termine | 403 |
| Patienten stornieren nur bis 24 Stunden vor Beginn (die Praxis jederzeit) | 409 |

Bewusst vereinfacht: Feiertage und Sprechzeiten pro Arzt sind nicht Teil von Version 1.

## Lokal starten

Voraussetzungen: Java 21, Node.js 26, Docker Desktop.

```bash
# 1. Konfigurationsdatei anlegen und eigene Werte eintragen
#    (DB_PASSWORD, JWT_SECRET mit mindestens 32 Zeichen, PRAXIS_EMAIL, PRAXIS_PASSWORD)
cp .env.example .env

# 2. Datenbank starten
docker compose up -d

# 3. Backend starten (im Ordner backend/)
cd backend
./mvnw spring-boot:run

# 4. Frontend starten (in einem zweiten Terminal, im Ordner frontend/)
cd frontend
npm install
npm start
# danach http://localhost:4200 öffnen
```

Unter Windows (PowerShell): `Copy-Item .env.example .env` und `.\mvnw.cmd spring-boot:run`.

Beim ersten Start legt die Anwendung den Praxis-Benutzer aus `PRAXIS_EMAIL` und `PRAXIS_PASSWORD` an.

### Ausprobieren mit Swagger UI

1. http://localhost:8080/swagger-ui.html öffnen
2. Als Praxis anmelden (`POST /api/auth/login`), Token über **Authorize** einfügen und einen Arzt anlegen
3. Einen Patienten registrieren (`POST /api/auth/register`), anmelden und das neue Token über **Authorize** einfügen
4. Einen Termin buchen (`POST /api/termine`), zum Beispiel am nächsten Werktag um 09:30 Uhr, und wieder stornieren

### Tests

```bash
cd backend
./mvnw test
```

Die Tests starten mit Testcontainers eine eigene PostgreSQL-Datenbank. Docker muss laufen; die lokale Datenbank bleibt unberührt.

Frontend-Tests (im Ordner frontend/):

```bash
cd frontend
npm test -- --watch=false
```

## API (bisher)

| Methode | Pfad | Beschreibung | Zugriff | Antworten |
|---|---|---|---|---|
| POST | `/api/auth/register` | als Patient registrieren | offen | 201, 400, 409 |
| POST | `/api/auth/login` | anmelden, liefert ein JWT | offen | 200, 400, 401 |
| GET | `/api/aerzte` | alle Ärzte | angemeldet | 200, 401 |
| GET | `/api/aerzte/{id}` | ein Arzt | angemeldet | 200, 401, 404 |
| POST | `/api/aerzte` | Arzt anlegen | PRAXIS | 201, 400, 401, 403 |
| PUT | `/api/aerzte/{id}` | Arzt ändern | PRAXIS | 200, 400, 401, 403, 404 |
| DELETE | `/api/aerzte/{id}` | Arzt löschen (nicht, wenn er Termine hat) | PRAXIS | 204, 401, 403, 404, 409 |
| POST | `/api/termine` | Termin buchen | PATIENT | 201, 400, 401, 403, 404, 409 |
| GET | `/api/termine` | Termine (Patient: eigene, Praxis: alle) | angemeldet | 200, 401 |
| GET | `/api/termine/{id}` | ein Termin | angemeldet | 200, 401, 403, 404 |
| POST | `/api/termine/{id}/stornieren` | Termin stornieren | angemeldet | 200, 401, 403, 404, 409 |

## Fortschritt

- [x] Projekt-Setup: Spring Boot, PostgreSQL mit Docker Compose, Flyway
- [x] Ärzte verwalten (CRUD) mit Validierung, Fehlerbehandlung, Swagger und Tests
- [x] Login mit JWT und Rollen (Praxis, Patient)
- [x] Termine buchen und stornieren, inkl. Schutz vor Doppelbuchungen
- [x] Angular-Frontend
- [x] GitHub Actions (automatische Tests)
