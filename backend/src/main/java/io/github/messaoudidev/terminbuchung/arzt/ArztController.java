package io.github.messaoudidev.terminbuchung.arzt;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aerzte")
@Tag(name = "Ärzte")
public class ArztController {

    private final ArztService arztService;

    public ArztController(ArztService arztService) {
        this.arztService = arztService;
    }

    @GetMapping
    public List<ArztResponse> findeAlle() {
        return arztService.findeAlle();
    }

    @GetMapping("/{id}")
    public ArztResponse findeNachId(@PathVariable Long id) {
        return arztService.findeNachId(id);
    }

    @PostMapping
    public ResponseEntity<ArztResponse> anlegen(@Valid @RequestBody ArztRequest request) {
        ArztResponse response = arztService.anlegen(request);
        URI location = URI.create("/api/aerzte/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ArztResponse aendern(@PathVariable Long id, @Valid @RequestBody ArztRequest request) {
        return arztService.aendern(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> loeschen(@PathVariable Long id) {
        arztService.loeschen(id);
        return ResponseEntity.noContent().build();
    }
}
