package io.github.messaoudidev.terminbuchung.benutzer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.messaoudidev.terminbuchung.security.JwtConfig;
import io.github.messaoudidev.terminbuchung.security.ProblemDetailAccessDeniedHandler;
import io.github.messaoudidev.terminbuchung.security.ProblemDetailAuthenticationEntryPoint;
import io.github.messaoudidev.terminbuchung.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class,
        ProblemDetailAuthenticationEntryPoint.class, ProblemDetailAccessDeniedHandler.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-mit-mindestens-32-zeichen-laenge")
class AuthControllerTest {

    private static final String GUELTIGE_REGISTRIERUNG = """
            {"email": "anna@example.com", "passwort": "geheim123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registrieren_gueltigerRequestOhneToken_liefert201OhnePasswort() throws Exception {
        when(authService.registrieren(new RegisterRequest("anna@example.com", "geheim123")))
                .thenReturn(new BenutzerResponse(1L, "anna@example.com", Rolle.PATIENT));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGE_REGISTRIERUNG))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("anna@example.com"))
                .andExpect(jsonPath("$.rolle").value("PATIENT"))
                .andExpect(jsonPath("$.passwort").doesNotExist())
                .andExpect(jsonPath("$.passwortHash").doesNotExist());
    }

    @Test
    void registrieren_ungueltigeEmailUndKurzesPasswort_liefert400MitFeldfehlern() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "keine-email", "passwort": "kurz"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.errors.email").value("E-Mail ist ungültig"))
                .andExpect(jsonPath("$.errors.passwort").value("Passwort muss zwischen 8 und 72 Zeichen lang sein"));

        verify(authService, never()).registrieren(any());
    }

    @Test
    void registrieren_emailVergeben_liefert409AlsProblemDetail() throws Exception {
        when(authService.registrieren(any())).thenThrow(new EmailBereitsVergebenException());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGE_REGISTRIERUNG))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Diese E-Mail ist bereits vergeben."));
    }

    @Test
    void login_korrekteDaten_liefert200MitToken() throws Exception {
        when(authService.login(new LoginRequest("anna@example.com", "geheim123")))
                .thenReturn(new LoginResponse("token-123", 3600));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGE_REGISTRIERUNG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-123"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void login_falscheDaten_liefert401AlsProblemDetail() throws Exception {
        when(authService.login(any())).thenThrow(new UngueltigeAnmeldedatenException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUELTIGE_REGISTRIERUNG))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("E-Mail oder Passwort ist falsch."));
    }

    @Test
    void login_leereFelder_liefert400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "", "passwort": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("E-Mail darf nicht leer sein"))
                .andExpect(jsonPath("$.errors.passwort").value("Passwort darf nicht leer sein"));

        verify(authService, never()).login(any());
    }
}
