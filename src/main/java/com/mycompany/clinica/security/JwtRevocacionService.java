package com.mycompany.clinica.security;

import java.util.Date;

import com.mycompany.clinica.repository.TokenRevocadoRepository;
import org.springframework.stereotype.Service;

/**
 * Permite marcar un JWT como revocado y comprobar su revocación.
 * Este servicio debe integrarse con el filtro JWT y el cierre de sesión.
 */
@Service
public class JwtRevocacionService {

    private final TokenRevocadoRepository tokenRevocadoRepository;
    private final JwtService jwtService;

    public JwtRevocacionService(TokenRevocadoRepository tokenRevocadoRepository,
            JwtService jwtService) {
        this.tokenRevocadoRepository = tokenRevocadoRepository;
        this.jwtService = jwtService;
    }

    /**
     * Registra la revocación de un JWT previamente verificado como auténtico.
     * No guardamos el token completo, solamente su jti y la expiración.
     */
    public void revocarToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El JWT no puede estar vacío");
        }

        // extractTokenId y extractExpiration verifican la firma y expiración
        // mediante el parser de JwtService.
        String jti = jwtService.extractTokenId(token);
        Date expiracion = jwtService.extractExpiration(token);

        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("El JWT no contiene un identificador jti");
        }
        if (expiracion == null) {
            throw new IllegalArgumentException("El JWT no contiene una expiración");
        }
        if (expiracion.after(new Date())) {
            tokenRevocadoRepository.save(new TokenRevocado(jti, expiracion));
        }
    }

    /** Devuelve true si MongoDB tiene registrado el jti como revocado. */
    public boolean estaRevocado(String jti) {
        return jti != null && !jti.isBlank()
                && tokenRevocadoRepository.existsById(jti);
    }

    /** Consulta la revocación a partir de un JWT válido. */
    public boolean estaRevocadoToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return estaRevocado(jwtService.extractTokenId(token));
    }
}
