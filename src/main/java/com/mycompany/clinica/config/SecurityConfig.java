package com.mycompany.clinica.config;

import com.mycompany.clinica.security.JwtAuthenticationFilter;
import com.mycompany.clinica.security.JwtCookieService;
import com.mycompany.clinica.security.JwtRevocacionService;
import com.mycompany.clinica.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final JwtService jwtService;
    private final JwtCookieService jwtCookieService;
    private final JwtRevocacionService jwtRevocacionService;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          AuthenticationProvider authenticationProvider,
                          JwtService jwtService,
                          JwtCookieService jwtCookieService,
                          JwtRevocacionService jwtRevocacionService,
                          UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
        this.jwtService = jwtService;
        this.jwtCookieService = jwtCookieService;
        this.jwtRevocacionService = jwtRevocacionService;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers(request -> {
                String path = request.getServletPath();
                String authorization = request.getHeader("Authorization");
                return "/api/auth/login".equals(path)
                    || (path.startsWith("/api/")
                        && authorization != null
                        && authorization.startsWith("Bearer "));
            }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                .requestMatchers("/api/auth/login", "/api/auth/registro").permitAll()
                .requestMatchers(
                    "/", "/login", "/registro", "/session-expired",
                    "/error", "/error/**", "/css/**", "/js/**",
                    "/img/**", "/images/**", "/fonts/**", "/favicon.ico"
                ).permitAll()
                .requestMatchers("/dashboard")
                    .hasAnyRole("ADMIN", "MEDICO", "ENFERMERO", "RECEPCIONISTA", "USUARIO")
                .requestMatchers("/perfil", "/perfil/**")
                    .hasAnyRole("ADMIN", "MEDICO", "ENFERMERO", "RECEPCIONISTA", "USUARIO")
                .requestMatchers("/recepcionista", "/recepcionista/**").hasRole("RECEPCIONISTA")
                .requestMatchers("/recepcion", "/recepcion/**").hasAnyRole("ADMIN", "RECEPCIONISTA")
                .requestMatchers("/admin", "/admin/**", "/usuarios", "/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/conversiones", "/conversiones/**")
                    .hasAnyRole("ADMIN", "MEDICO", "ENFERMERO")
                .requestMatchers("/medicamentos", "/medicamentos/**")
                    .hasAnyRole("ADMIN", "MEDICO", "ENFERMERO")
                .requestMatchers("/pacientes", "/pacientes/**")
                    .hasAnyRole("ADMIN", "MEDICO", "ENFERMERO", "RECEPCIONISTA")
                .requestMatchers("/citas", "/citas/**")
                    .hasAnyRole("ADMIN", "MEDICO", "ENFERMERO", "RECEPCIONISTA", "USUARIO")
                .requestMatchers("/historial", "/historial/**")
                    .hasAnyRole("ADMIN", "MEDICO", "ENFERMERO", "USUARIO")
                .requestMatchers("/notificaciones", "/notificaciones/**")
                    .hasAnyRole("ADMIN", "MEDICO", "ENFERMERO", "RECEPCIONISTA", "USUARIO")
                .requestMatchers("/reportes", "/reportes/**").hasAnyRole("ADMIN", "MEDICO")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    UserDetails userDetails;
                    if (authentication.getPrincipal() instanceof UserDetails details) {
                        userDetails = details;
                    } else {
                        userDetails = userDetailsService.loadUserByUsername(authentication.getName());
                    }
                    String token = jwtService.generateToken(userDetails);
                    jwtCookieService.addJwtCookie(response, token);
                    response.sendRedirect(request.getContextPath() + "/dashboard");
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler((request, response, authentication) -> {
                    Cookie[] cookies = request.getCookies();
                    if (cookies != null) {
                        for (Cookie cookie : cookies) {
                            if (JwtAuthenticationFilter.JWT_COOKIE_NAME.equals(cookie.getName())
                                    && cookie.getValue() != null
                                    && !cookie.getValue().isBlank()) {
                                jwtRevocacionService.revocarToken(cookie.getValue());
                                break;
                            }
                        }
                    }
                    jwtCookieService.clearJwtCookie(response);
                })
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("CLINGUARDSESSION", "JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                // Responder directamente 403: un FORWARD de POST a /error/403
                // generaba HttpRequestMethodNotSupportedException y terminaba en 500.
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado");
                })
                .authenticationEntryPoint((request, response, ex) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"mensaje\":\"No autenticado\"}");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login");
                    }
                })
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
