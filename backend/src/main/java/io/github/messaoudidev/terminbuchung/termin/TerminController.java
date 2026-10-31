package io.github.messaoudidev.terminbuchung.termin;

import java.net.URI;
import java.util.List;

import io.github.messaoudidev.terminbuchung.benutzer.Rolle;
import io.github.messaoudidev.terminbuchung.security.TokenService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/termine")
@Tag(name = "Termine")
@SecurityRequirement(name = "bearerAuth")
public class TerminController {

    private final TerminService terminService;

    public TerminController(TerminService terminService) {
        this.terminService = terminService;
    }

    @GetMapping
    public List<TerminResponse> findeAlle(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return terminService.findeAlle(benutzerId(jwt), rolle(jwt));
    }

    @GetMapping("/{id}")
    public TerminResponse findeNachId(@PathVariable Long id,
                                      @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return terminService.findeNachId(id, benutzerId(jwt), rolle(jwt));
    }

    @PostMapping
    public ResponseEntity<TerminResponse> buchen(@Valid @RequestBody TerminRequest request,
                                                 @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        TerminResponse response = terminService.buchen(request, benutzerId(jwt));
        URI location = URI.create("/api/termine/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{id}/stornieren")
    public TerminResponse stornieren(@PathVariable Long id,
                                     @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return terminService.stornieren(id, benutzerId(jwt), rolle(jwt));
    }

    // Das Token enthält die Benutzer-ID als "sub" und die Rolle als eigenen Claim (siehe TokenService).
    private Long benutzerId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    private Rolle rolle(Jwt jwt) {
        return Rolle.valueOf(jwt.getClaimAsString(TokenService.ROLLE_CLAIM));
    }
}
