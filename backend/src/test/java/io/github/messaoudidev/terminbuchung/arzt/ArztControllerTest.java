package io.github.messaoudidev.terminbuchung.arzt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import io.github.messaoudidev.terminbuchung.security.JwtConfig;
import io.github.messaoudidev.terminbuchung.security.ProblemDetailAccessDeniedHandler;
import io.github.messaoudidev.terminbuchung.security.ProblemDetailAuthenticationEntryPoint;
import io.github.messaoudidev.terminbuchung.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ArztController.class)
@Import({SecurityConfig.class, JwtConfig.class,
        ProblemDetailAuthenticationEntryPoint.class, ProblemDetailAccessDeniedHandler.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-mit-mindestens-32-zeichen-laenge")
class ArztControllerTest {

    private static final String GUELTIGER_REQUEST = """
            {"name": "Dr. Müller", "fachrichtung": "Allgemeinmedizin"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArztService arztService;

    @Test
    void findeAlle_zweiAerzte_liefert200MitListe() throws Exception {
        when(arztService.findeAlle()).thenReturn(List.of(
                new ArztResponse(1L, "Dr. Müller", "Allgemeinmedizin"),
                new ArztResponse(2L, "Dr. Schmidt", "Kardiologie")));

        mockMvc.perform(get("/api/aerzte").with(patient()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Dr. Müller"))
                .andExpect(jsonPath("$[1].fachrichtung").value("Kardiologie"));
    }

    @Test
    void findeAlle_ohneToken_liefert401AlsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/aerzte"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Anmeldung erforderlich. Bitte ein gültiges Token mitsenden."));

        verify(arztService, never()).findeAlle();
    }

    @Test
    void findeAlle_ungueltigesToken_liefert401AlsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/aerzte").header(HttpHeaders.AUTHORIZATION, "Bearer kein-gueltiges-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(401));

        verify(arztService, never()).findeAlle();
    }

    @Test
    void findeNachId_vorhanden_liefert200MitArzt() throws Exception {
        when(arztService.findeNachId(1L)).thenReturn(new ArztResponse(1L, "Dr. Müller", "Allgemeinmedizin"));

        mockMvc.perform(get("/api/aerzte/1").with(patient()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Dr. Müller"))
                .andExpect(jsonPath("$.fachrichtung").value("Allgemeinmedizin"));
    }

    @Test
    void findeNachId_unbekannt_liefert404AlsProblemDetail() throws Exception {
        when(arztService.findeNachId(99L)).thenThrow(new ArztNotFoundException(99L));

        mockMvc.perform(get("/api/aerzte/99").with(patient()))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Arzt mit ID 99 wurde nicht gefunden."));
    }

    @Test
    void findeNachId_idKeineZahl_liefert400() throws Exception {
        mockMvc.perform(get("/api/aerzte/abc").with(patient()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void anlegen_gueltigerRequest_liefert201MitLocation() throws Exception {
        when(arztService.anlegen(new ArztRequest("Dr. Müller", "Allgemeinmedizin")))
                .thenReturn(new ArztResponse(1L, "Dr. Müller", "Allgemeinmedizin"));

        mockMvc.perform(post("/api/aerzte").with(praxis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/aerzte/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Dr. Müller"));
    }

    @Test
    void anlegen_ohneToken_liefert401() throws Exception {
        mockMvc.perform(post("/api/aerzte")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(arztService, never()).anlegen(any());
    }

    @Test
    void anlegen_alsPatient_liefert403AlsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/aerzte").with(patient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Keine Berechtigung für diese Aktion."));

        verify(arztService, never()).anlegen(any());
    }

    @Test
    void anlegen_leereFelder_liefert400MitFeldfehlern() throws Exception {
        mockMvc.perform(post("/api/aerzte").with(praxis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "fachrichtung": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").value("Name darf nicht leer sein"))
                .andExpect(jsonPath("$.errors.fachrichtung").value("Fachrichtung darf nicht leer sein"));

        verify(arztService, never()).anlegen(any());
    }

    @Test
    void anlegen_nameLaengerAls100Zeichen_liefert400() throws Exception {
        String zuLangerName = "a".repeat(101);

        mockMvc.perform(post("/api/aerzte").with(praxis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "fachrichtung": "Allgemeinmedizin"}
                                """.formatted(zuLangerName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name darf höchstens 100 Zeichen lang sein"));

        verify(arztService, never()).anlegen(any());
    }

    @Test
    void anlegen_ungueltigesJson_liefert400() throws Exception {
        mockMvc.perform(post("/api/aerzte").with(praxis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{kein json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(arztService, never()).anlegen(any());
    }

    @Test
    void aendern_gueltigerRequest_liefert200MitGeaendertemArzt() throws Exception {
        when(arztService.aendern(1L, new ArztRequest("Dr. Müller", "Allgemeinmedizin")))
                .thenReturn(new ArztResponse(1L, "Dr. Müller", "Allgemeinmedizin"));

        mockMvc.perform(put("/api/aerzte/1").with(praxis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fachrichtung").value("Allgemeinmedizin"));
    }

    @Test
    void aendern_alsPatient_liefert403() throws Exception {
        mockMvc.perform(put("/api/aerzte/1").with(patient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(arztService, never()).aendern(any(), any());
    }

    @Test
    void aendern_unbekannt_liefert404() throws Exception {
        when(arztService.aendern(eq(99L), any(ArztRequest.class))).thenThrow(new ArztNotFoundException(99L));

        mockMvc.perform(put("/api/aerzte/99").with(praxis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGER_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void aendern_leererName_liefert400() throws Exception {
        mockMvc.perform(put("/api/aerzte/1").with(praxis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "fachrichtung": "Allgemeinmedizin"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name darf nicht leer sein"));

        verify(arztService, never()).aendern(any(), any());
    }

    @Test
    void loeschen_vorhanden_liefert204() throws Exception {
        mockMvc.perform(delete("/api/aerzte/1").with(praxis()))
                .andExpect(status().isNoContent());

        verify(arztService).loeschen(1L);
    }

    @Test
    void loeschen_alsPatient_liefert403() throws Exception {
        mockMvc.perform(delete("/api/aerzte/1").with(patient()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(arztService, never()).loeschen(any());
    }

    @Test
    void loeschen_unbekannt_liefert404() throws Exception {
        doThrow(new ArztNotFoundException(99L)).when(arztService).loeschen(99L);

        mockMvc.perform(delete("/api/aerzte/99").with(praxis()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // jwt() erzeugt ein bereits geprüftes Token, ohne Secret und ohne Login.
    private static RequestPostProcessor praxis() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_PRAXIS"));
    }

    private static RequestPostProcessor patient() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_PATIENT"));
    }
}
