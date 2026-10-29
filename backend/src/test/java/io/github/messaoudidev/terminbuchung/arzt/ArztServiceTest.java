package io.github.messaoudidev.terminbuchung.arzt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArztServiceTest {

    @Mock
    private ArztRepository arztRepository;

    private ArztService arztService;

    @BeforeEach
    void setUp() {
        arztService = new ArztService(arztRepository, new ArztMapper());
    }

    @Test
    void findeAlle_zweiAerzte_liefertBeideSortiertNachName() {
        // Arrange
        Arzt mueller = arztMitId(1L, "Dr. Müller", "Allgemeinmedizin");
        Arzt schmidt = arztMitId(2L, "Dr. Schmidt", "Kardiologie");
        when(arztRepository.findAll(Sort.by("name"))).thenReturn(List.of(mueller, schmidt));

        // Act
        List<ArztResponse> ergebnis = arztService.findeAlle();

        // Assert
        assertThat(ergebnis).containsExactly(
                new ArztResponse(1L, "Dr. Müller", "Allgemeinmedizin"),
                new ArztResponse(2L, "Dr. Schmidt", "Kardiologie"));
    }

    @Test
    void findeNachId_vorhanden_liefertArzt() {
        // Arrange
        Arzt arzt = arztMitId(1L, "Dr. Müller", "Allgemeinmedizin");
        when(arztRepository.findById(1L)).thenReturn(Optional.of(arzt));

        // Act
        ArztResponse ergebnis = arztService.findeNachId(1L);

        // Assert
        assertThat(ergebnis).isEqualTo(new ArztResponse(1L, "Dr. Müller", "Allgemeinmedizin"));
    }

    @Test
    void findeNachId_unbekannt_wirftArztNotFoundException() {
        // Arrange
        when(arztRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> arztService.findeNachId(99L))
                .isInstanceOf(ArztNotFoundException.class)
                .hasMessage("Arzt mit ID 99 wurde nicht gefunden.");
    }

    @Test
    void anlegen_gueltigerRequest_speichertArztUndLiefertIhnMitId() {
        // Arrange
        ArztRequest request = new ArztRequest("Dr. Müller", "Allgemeinmedizin");
        Arzt gespeichert = arztMitId(1L, "Dr. Müller", "Allgemeinmedizin");
        when(arztRepository.save(any(Arzt.class))).thenReturn(gespeichert);

        // Act
        ArztResponse ergebnis = arztService.anlegen(request);

        // Assert
        ArgumentCaptor<Arzt> captor = ArgumentCaptor.forClass(Arzt.class);
        verify(arztRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Dr. Müller");
        assertThat(captor.getValue().getFachrichtung()).isEqualTo("Allgemeinmedizin");
        assertThat(ergebnis).isEqualTo(new ArztResponse(1L, "Dr. Müller", "Allgemeinmedizin"));
    }

    @Test
    void aendern_vorhanden_aendertNameUndFachrichtung() {
        // Arrange
        Arzt arzt = arztMitId(1L, "Dr. Müller", "Allgemeinmedizin");
        when(arztRepository.findById(1L)).thenReturn(Optional.of(arzt));
        ArztRequest request = new ArztRequest("Dr. Müller-Lang", "Innere Medizin");

        // Act
        ArztResponse ergebnis = arztService.aendern(1L, request);

        // Assert
        assertThat(arzt.getName()).isEqualTo("Dr. Müller-Lang");
        assertThat(arzt.getFachrichtung()).isEqualTo("Innere Medizin");
        assertThat(ergebnis).isEqualTo(new ArztResponse(1L, "Dr. Müller-Lang", "Innere Medizin"));
    }

    @Test
    void aendern_unbekannt_wirftArztNotFoundException() {
        // Arrange
        when(arztRepository.findById(99L)).thenReturn(Optional.empty());
        ArztRequest request = new ArztRequest("Dr. Müller", "Allgemeinmedizin");

        // Act + Assert
        assertThatThrownBy(() -> arztService.aendern(99L, request))
                .isInstanceOf(ArztNotFoundException.class);
    }

    @Test
    void loeschen_vorhanden_loeschtArzt() {
        // Arrange
        Arzt arzt = arztMitId(1L, "Dr. Müller", "Allgemeinmedizin");
        when(arztRepository.findById(1L)).thenReturn(Optional.of(arzt));

        // Act
        arztService.loeschen(1L);

        // Assert
        verify(arztRepository).delete(arzt);
    }

    @Test
    void loeschen_unbekannt_wirftArztNotFoundExceptionUndLoeschtNichts() {
        // Arrange
        when(arztRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> arztService.loeschen(99L))
                .isInstanceOf(ArztNotFoundException.class);
        verify(arztRepository, never()).delete(any(Arzt.class));
    }

    // Die ID vergibt sonst die Datenbank; Arzt hat bewusst keinen Setter dafür.
    private Arzt arztMitId(Long id, String name, String fachrichtung) {
        Arzt arzt = new Arzt(name, fachrichtung);
        ReflectionTestUtils.setField(arzt, "id", id);
        return arzt;
    }
}
