
package com.mycompany.clinica.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.mycompany.clinica.entity.Notificacion;

public interface NotificacionRepository
        extends MongoRepository<Notificacion, String> {

    List<Notificacion>
        findByDestinatarioUsernameOrderByFechaCreacionDesc(
            String username
        );

    List<Notificacion>
        findByDestinatarioUsernameAndLeidaOrderByFechaCreacionDesc(
            String username,
            boolean leida
        );

    Optional<Notificacion>
        findByIdAndDestinatarioUsername(
            String id,
            String username
        );

    long countByDestinatarioUsername(
        String username
    );

    long countByDestinatarioUsernameAndLeida(
        String username,
        boolean leida
    );
}

