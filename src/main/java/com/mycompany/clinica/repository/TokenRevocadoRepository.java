package com.mycompany.clinica.repository;

import com.mycompany.clinica.security.TokenRevocado;
import org.springframework.data.mongodb.repository.MongoRepository;

/** El identificador de cada documento es el jti del JWT. */
public interface TokenRevocadoRepository extends MongoRepository<TokenRevocado, String> {
}
