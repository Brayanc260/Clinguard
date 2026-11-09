package com.mycompany.clinica.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Crea y elimina la cookie que transporta el JWT en el navegador.
 * La autenticacion web tradicional de ClinGuard no se modifica al agregar
 * este servicio: se usara cuando se adapte el controlador del login web.
 */
@Service
public class JwtCookieService {

    private static final String COOKIE_NAME = JwtAuthenticationFilter.JWT_COOKIE_NAME;

    private final boolean cookieSecure;
    private final Duration tokenDuration;

    public JwtCookieService(
            @Value("${app.jwt.cookie-secure:false}") boolean cookieSecure,
            @Value("${app.jwt.expiration-ms}") long expirationMillis) {

        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("app.jwt.expiration-ms debe ser mayor que cero");
        }
        this.cookieSecure = cookieSecure;
        this.tokenDuration = Duration.ofMillis(expirationMillis);
    }

    /** Entrega una cookie HttpOnly con el JWT despues de un login valido. */
    public void addJwtCookie(HttpServletResponse response, String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("No se puede crear una cookie JWT sin token");
        }

        ResponseCookie cookie = baseCookie(token)
                .maxAge(tokenDuration)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** Pide al navegador eliminar la cookie JWT_TOKEN. No revoca el JWT. */
    public void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie("")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/");
    }
}
