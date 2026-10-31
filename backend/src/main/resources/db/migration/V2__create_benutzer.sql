CREATE TABLE benutzer (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    -- Ein BCrypt-Hash ist immer 60 Zeichen lang.
    passwort_hash VARCHAR(60)  NOT NULL,
    rolle         VARCHAR(20)  NOT NULL CHECK (rolle IN ('PRAXIS', 'PATIENT'))
);
