package com.mycompany.clinica.config;

import com.mycompany.clinica.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, AuthenticationProvider authenticationProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                // Deshabilitamos CSRF para las APIs REST para permitir llamadas con JWT / fetch sin token CSRF
                .ignoringRequestMatchers("/api/**")
            )
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas de API REST y páginas web
                .requestMatchers(
                    "/",
                    "/login",
                    "/registro",
                    "/session-expired",
                    "/error",
                    "/error/**",
                    "/api/auth/**",
                    "/css/**",
                    "/js/**",
                    "/img/**",
                    "/images/**",
                    "/fonts/**",
                    "/favicon.ico"
                ).permitAll()

                // Dashboards y vistas
                .requestMatchers("/dashboard").hasAnyRole("ADMIN", "MEDICO", "ENFERMERO", "RECEPCIONISTA", "USUARIO")
                .requestMatchers("/perfil", "/perfil/**").hasAnyRole("ADMIN", "MEDICO", "ENFERMERO", "RECEPCIONISTA", "USUARIO")
                .requestMatchers("/recepcionista", "/recepcionista/**").hasRole("RECEPCIONISTA")
                .requestMatchers("/recepcion", "/recepcion/**").hasAnyRole("ADMIN", "RECEPCIONISTA")
                .requestMatchers("/admin", "/admin/**", "/usuarios", "/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/conversiones", "/conversiones/**").hasAnyRole("ADMIN", "MEDICO", "ENFERMERO")
                .requestMatchers("/medicamentos", "/medicamentos/**").hasAnyRole("ADMIN", "MEDICO", "ENFERMERO")
                .requestMatchers("/pacientes", "/pacientes/**").hasAnyRole("ADMIN", "MEDICO", "ENFERMERO", "RECEPCIONISTA")
                .requestMatchers("/citas", "/citas/**").hasAnyRole("ADMIN", "MEDICO", "RECEPCIONISTA", "USUARIO")
                .requestMatchers("/historial", "/historial/**").hasAnyRole("ADMIN", "MEDICO", "ENFERMERO", "USUARIO")
                .requestMatchers("/notificaciones", "/notificaciones/**").hasAnyRole("ADMIN", "MEDICO", "ENFERMERO", "RECEPCIONISTA", "USUARIO")
                .requestMatchers("/reportes", "/reportes/**").hasAnyRole("ADMIN", "MEDICO")

                // Todo lo demás autenticado
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("CLINGUARDSESSION", "JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/error/403")
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
