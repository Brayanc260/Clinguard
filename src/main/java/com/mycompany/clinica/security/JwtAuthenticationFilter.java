package com.mycompany.clinica.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Valida JWT de Bearer o cookie y consulta su revocacion en MongoDB. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String JWT_COOKIE_NAME = "JWT_TOKEN";

    private final JwtService jwtService;
    private final JwtRevocacionService jwtRevocacionService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   JwtRevocacionService jwtRevocacionService,
                                   UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.jwtRevocacionService = jwtRevocacionService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        boolean bearerPresent = authorization != null && authorization.startsWith("Bearer ");
        String token = bearerPresent ? authorization.substring(7).trim() : readJwtCookie(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (token.isBlank()) {
                throw new IllegalArgumentException("JWT vacio");
            }

            String nombreUsuario = jwtService.extractUsername(token);
            String jti = jwtService.extractTokenId(token);
            if (nombreUsuario == null || nombreUsuario.isBlank()) {
                throw new IllegalArgumentException("JWT sin usuario");
            }
            if (jti == null || jti.isBlank()) {
                throw new IllegalArgumentException("JWT sin jti");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(nombreUsuario);
            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new IllegalArgumentException("JWT no valido");
            }
            if (jwtRevocacionService.estaRevocado(jti)) {
                // No dejar que un JWT revocado reutilice la autenticacion de sesion.
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                reject(response, bearerPresent || request.getRequestURI().startsWith("/api/"));
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
            if (bearerPresent || request.getRequestURI().startsWith("/api/")) {
                writeUnauthorized(response);
                return;
            }
            clearJwtCookie(response);
            // Se conserva la compatibilidad con el inicio de sesion tradicional
            // cuando hay una cookie invalida/expirada, no revocada.
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, boolean apiRequest) throws IOException {
        clearJwtCookie(response);
        if (apiRequest) {
            writeUnauthorized(response);
        } else {
            response.sendRedirect("/login?revocado=true");
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensaje\":\"JWT invalido, expirado o revocado\"}");
    }

    private void clearJwtCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie",
                JWT_COOKIE_NAME + "=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax");
    }

    private String readJwtCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
