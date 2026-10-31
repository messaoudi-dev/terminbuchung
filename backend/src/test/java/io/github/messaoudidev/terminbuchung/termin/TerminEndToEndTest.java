package io.github.messaoudidev.terminbuchung.termin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

import com.jayway.jsonpath.JsonPath;
import io.github.messaoudidev.terminbuchung.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// Ganzer Ablauf mit echter Security, echtem Service und echtem PostgreSQL (Testcontainers).
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TerminEndToEndTest.FesteUhr.class})
@TestPropertySource(locations = "classpath:test.properties")
class TerminEndToEndTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    // Der JwtDecoder prüft den Ablauf des Tokens gegen die Systemzeit, nicht gegen unsere Clock.
    // Ein fest eingetragenes Datum würde den Test später rot machen. Deshalb: nächster Montag, 10:00 Uhr.
    private static final LocalDateTime JETZT = LocalDate.now(BERLIN)
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .atTime(10, 0);

    // Dienstag 14:00: mehr als 24 Stunden später, der Patient darf also stornieren.
    private static final LocalDateTime BEGINN = JETZT.plusDays(1).withHour(14);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void terminBuchenLesenUndStornieren_alsPatient_funktioniertVonAnfangBisEnde() throws Exception {
        // Arrange: Arzt als Praxis anlegen, Patient registrieren und einloggen
        String praxisToken = login("praxis@test.example", "praxis-test-passwort");
        String arztJson = mockMvc.perform(post("/api/aerzte")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + praxisToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Dr. Müller", "fachrichtung": "Allgemeinmedizin"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer arztId = JsonPath.read(arztJson, "$.id");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "anna@example.com", "passwort": "geheim123"}
                                """))
                .andExpect(status().isCreated());
        String patientToken = login("anna@example.com", "geheim123");

        // Act + Assert: buchen
        String terminJson = mockMvc.perform(post("/api/termine")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"arztId": %d, "beginn": "%s"}
                                """.formatted(arztId, BEGINN)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.ende").value(BEGINN.plusMinutes(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .andExpect(jsonPath("$.status").value("GEBUCHT"))
                .andReturn().getResponse().getContentAsString();
        Integer terminId = JsonPath.read(terminJson, "$.id");

        // Act + Assert: lesen (arztName kommt über die LAZY-Beziehung innerhalb der Transaktion)
        mockMvc.perform(get("/api/termine/" + terminId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arztName").value("Dr. Müller"))
                .andExpect(jsonPath("$.status").value("GEBUCHT"));

        // Act + Assert: stornieren
        mockMvc.perform(post("/api/termine/" + terminId + "/stornieren")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STORNIERT"));
    }

    private String login(String email, String passwort) throws Exception {
        String antwort = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "passwort": "%s"}
                                """.formatted(email, passwort)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(antwort, "$.token");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FesteUhr {

        @Bean
        @Primary
        Clock festeUhr() {
            return Clock.fixed(JETZT.atZone(BERLIN).toInstant(), BERLIN);
        }
    }
}
