package io.github.messaoudidev.terminbuchung.common;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.messaoudidev.terminbuchung.arzt.ArztNotFoundException;
import io.github.messaoudidev.terminbuchung.benutzer.EmailBereitsVergebenException;
import io.github.messaoudidev.terminbuchung.benutzer.UngueltigeAnmeldedatenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Die Basisklasse liefert für Spring-MVC-Fehler (z. B. kaputtes JSON, falscher Typ im Pfad) bereits ProblemDetail.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail problemDetail = ex.getBody();
        problemDetail.setDetail("Die Eingaben sind ungültig.");
        problemDetail.setProperty("errors", errors);
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
    }

    @ExceptionHandler(ArztNotFoundException.class)
    public ProblemDetail handleArztNotFound(ArztNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmailBereitsVergebenException.class)
    public ProblemDetail handleEmailBereitsVergeben(EmailBereitsVergebenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UngueltigeAnmeldedatenException.class)
    public ProblemDetail handleUngueltigeAnmeldedaten(UngueltigeAnmeldedatenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // Die Datenbank ist die letzte Sicherung, z. B. bei zwei gleichzeitigen Registrierungen mit derselben E-Mail.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Die Anfrage steht im Konflikt mit vorhandenen Daten.");
    }

    // Feste Meldungen, weil die Texte von Spring Security englisch sind und Details zum Token verraten können.
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Anmeldung erforderlich. Bitte ein gültiges Token mitsenden.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Keine Berechtigung für diese Aktion.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnerwarteterFehler(Exception ex) {
        log.error("Unerwarteter Fehler", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ein unerwarteter Fehler ist aufgetreten.");
    }
}
