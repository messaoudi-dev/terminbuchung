package io.github.messaoudidev.terminbuchung.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

// Security-Fehler entstehen im Filter vor dem Controller. Die Weitergabe an Spring MVC sorgt dafür,
// dass der GlobalExceptionHandler sie wie alle anderen Fehler als ProblemDetail beantwortet.
@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public ProblemDetailAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        handlerExceptionResolver.resolveException(request, response, null, authException);
    }
}
