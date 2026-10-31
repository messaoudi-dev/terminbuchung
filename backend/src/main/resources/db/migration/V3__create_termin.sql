CREATE TABLE termin (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    arzt_id    BIGINT      NOT NULL REFERENCES arzt (id),
    patient_id BIGINT      NOT NULL REFERENCES benutzer (id),
    beginn     TIMESTAMP   NOT NULL,
    ende       TIMESTAMP   NOT NULL,
    status     VARCHAR(20) NOT NULL CHECK (status IN ('GEBUCHT', 'STORNIERT')),
    version    BIGINT      NOT NULL
);

-- Letzte Sicherung gegen Doppelbuchungen. Stornierte Termine geben den Zeitpunkt wieder frei.
CREATE UNIQUE INDEX ux_termin_arzt_beginn_gebucht ON termin (arzt_id, beginn) WHERE status = 'GEBUCHT';
