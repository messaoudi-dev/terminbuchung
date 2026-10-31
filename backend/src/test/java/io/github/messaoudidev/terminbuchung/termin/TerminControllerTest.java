package io.github.messaoudidev.terminbuchung.termin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import io.github.messaoudidev.terminbuchung.arzt.ArztNotFoundException;
import io.github.messaoudidev.terminbuchung.benutzer.Rolle;
import io.github.messaoudidev.terminbuchung.security.JwtConfig;
import io.github.messaoudidev.terminbuchung.security.ProblemDetailAccessDeniedHandler;
import io.github.messaoudidev.terminbuchung.security.ProblemDetailAuthenticationEntryPoint;
import io.github.messaoudidev.terminbuchung.security.SecurityConfig;
import io.github.messaoudidev.terminbuchung.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(TerminController.class)
@Import({SecurityConfig.class, JwtConfig.class,
        ProblemDetailAuthenticationEntryPoint.class, ProblemDetailAccessDeniedHandler.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-mit-mindestens-32-zeichen-laenge")
class TerminControllerTest {

    private static final Long PATIENT_ID = 5L;
    private static final Long PRAXIS_ID = 1L;
    private static final LocalDateTime BEGINN = LocalDateTime.of(2026, 11, 3, 9, 30);
    private static final TerminRequest GUELTIGER_REQUEST = new TerminRequest(2L, BEGINN);
    private static final String GUELTIGER_REQUEST_JSON = """
            {"arztId": 2, "beginn": "2026-11-03T09:30:00"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TerminService terminService;

    // --- buchen ---

    @Test
    void buchen_gueltigerRequestAlsPatient_liefert201MitLocation() throws Exception {
        when(terminService.buchen(GUELTIGER_REQUEST, PATIENT_ID)).thenReturn(response(TerminStatus.GEBUCHT));

        mockMvc.perform(post("/api/termine").with(patient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/termine/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.arztName").value("Dr. Müller"))
                .andExpect(jsonPath("$.beginn").value("2026-11-03T09:30:00"))
                .andExpect(jsonPath("$.ende").value("2026-11-03T10:00:00"))
                .andExpect(jsonPath("$.status").value("GEBUCHT"));
    }

    @Test
    void buchen_ohneToken_liefert401() throws Exception {
        mockMvc.perform(post("/api/termine")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(terminService, never()).buchen(any(), any());
    }

    @Test
    void buchen_alsPraxis_liefert403AlsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/termine").with(praxis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST_JSON))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(403));

        verify(terminService, never()).buchen(any(), any());
    }

    @Test
    void buchen_pflichtfelderFehlen_liefert400MitFeldfehlern() throws Exception {
        mockMvc.perform(post("/api/termine").with(patient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.errors.arztId").value("Arzt muss angegeben werden"))
                .andExpect(jsonPath("$.errors.beginn").value("Beginn muss angegeben werden"));

        verify(terminService, never()).buchen(any(), any());
    }

    @Test
    void buchen_ungueltigeTerminzeit_liefert400AlsProblemDetail() throws Exception {
        when(terminService.buchen(GUELTIGER_REQUEST, PATIENT_ID))
                .thenThrow(new UngueltigeTerminzeitException("Der Termin darf nicht in der Vergangenheit liegen."));

        mockMvc.perform(post("/api/termine").with(patient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.detail").value("Der Termin darf nicht in der Vergangenheit liegen."));
    }

    @Test
    void buchen_arztUnbekannt_liefert404() throws Exception {
        when(terminService.buchen(GUELTIGER_REQUEST, PATIENT_ID)).thenThrow(new ArztNotFoundException(2L));

        mockMvc.perform(post("/api/termine").with(patient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Arzt mit ID 2 wurde nicht gefunden."));
    }

    @Test
    void buchen_zeitBereitsVergeben_liefert409AlsProblemDetail() throws Exception {
        when(terminService.buchen(GUELTIGER_REQUEST, PATIENT_ID)).thenThrow(new TerminBereitsVergebenException());

        mockMvc.perform(post("/api/termine").with(patient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST_JSON))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.detail").value("Der Arzt hat zu dieser Zeit bereits einen Termin."));
    }

    @Test
    void buchen_gleichzeitigeBuchungVerletztUniqueIndex_liefert409() throws Exception {
        when(terminService.buchen(GUELTIGER_REQUEST, PATIENT_ID))
                .thenThrow(new DataIntegrityViolationException("ux_termin_arzt_beginn_gebucht"));

        mockMvc.perform(post("/api/termine").with(patient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // --- lesen ---

    @Test
    void findeAlle_alsPatient_liefert200MitListe() throws Exception {
        when(terminService.findeAlle(PATIENT_ID, Rolle.PATIENT)).thenReturn(List.of(response(TerminStatus.GEBUCHT)));

        mockMvc.perform(get("/api/termine").with(patient()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].patientId").value(5));
    }

    @Test
    void findeAlle_alsPraxis_ruftServiceMitRollePraxisAuf() throws Exception {
        when(terminService.findeAlle(PRAXIS_ID, Rolle.PRAXIS)).thenReturn(List.of());

        mockMvc.perform(get("/api/termine").with(praxis()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(terminService).findeAlle(PRAXIS_ID, Rolle.PRAXIS);
    }

    @Test
    void findeNachId_eigenerTermin_liefert200() throws Exception {
        when(terminService.findeNachId(10L, PATIENT_ID, Rolle.PATIENT)).thenReturn(response(TerminStatus.GEBUCHT));

        mockMvc.perform(get("/api/termine/10").with(patient()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.arztId").value(2));
    }

    @Test
    void findeNachId_fremderTermin_liefert403AlsProblemDetail() throws Exception {
        when(terminService.findeNachId(10L, PATIENT_ID, Rolle.PATIENT))
                .thenThrow(new AccessDeniedException("Fremder Termin"));

        mockMvc.perform(get("/api/termine/10").with(patient()))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.detail").value("Keine Berechtigung für diese Aktion."));
    }

    @Test
    void findeNachId_unbekannt_liefert404() throws Exception {
        when(terminService.findeNachId(99L, PATIENT_ID, Rolle.PATIENT)).thenThrow(new TerminNotFoundException(99L));

        mockMvc.perform(get("/api/termine/99").with(patient()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Termin mit ID 99 wurde nicht gefunden."));
    }

    // --- stornieren ---

    @Test
    void stornieren_erlaubt_liefert200MitStatusStorniert() throws Exception {
        when(terminService.stornieren(10L, PATIENT_ID, Rolle.PATIENT)).thenReturn(response(TerminStatus.STORNIERT));

        mockMvc.perform(post("/api/termine/10/stornieren").with(patient()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STORNIERT"));
    }

    @Test
    void stornieren_zuSpaet_liefert409AlsProblemDetail() throws Exception {
        when(terminService.stornieren(10L, PATIENT_ID, Rolle.PATIENT)).thenThrow(
                new StornierungNichtMoeglichException("Ein Termin kann nur bis 24 Stunden vor Beginn storniert werden."));

        mockMvc.perform(post("/api/termine/10/stornieren").with(patient()))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.detail").value("Ein Termin kann nur bis 24 Stunden vor Beginn storniert werden."));
    }

    @Test
    void stornieren_gleichzeitigGeaendert_liefert409() throws Exception {
        when(terminService.stornieren(10L, PRAXIS_ID, Rolle.PRAXIS))
                .thenThrow(new OptimisticLockingFailureException("Version veraltet"));

        mockMvc.perform(post("/api/termine/10/stornieren").with(praxis()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Die Daten wurden gleichzeitig geändert. Bitte neu laden und erneut versuchen."));
    }

    @Test
    void stornieren_fremderTermin_liefert403() throws Exception {
        when(terminService.stornieren(10L, PATIENT_ID, Rolle.PATIENT))
                .thenThrow(new AccessDeniedException("Fremder Termin"));

        mockMvc.perform(post("/api/termine/10/stornieren").with(patient()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private static TerminResponse response(TerminStatus status) {
        return new TerminResponse(10L, 2L, "Dr. Müller", PATIENT_ID, BEGINN, BEGINN.plusMinutes(30), status);
    }

    // Wie ein echtes Token aus dem TokenService: Benutzer-ID als "sub", Rolle als eigener Claim.
    private static RequestPostProcessor patient() {
        return jwt().jwt(token -> token.subject(PATIENT_ID.toString()).claim(TokenService.ROLLE_CLAIM, "PATIENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"));
    }

    private static RequestPostProcessor praxis() {
        return jwt().jwt(token -> token.subject(PRAXIS_ID.toString()).claim(TokenService.ROLLE_CLAIM, "PRAXIS"))
                .authorities(new SimpleGrantedAuthority("ROLE_PRAXIS"));
    }
}
