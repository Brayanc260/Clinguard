package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.Cita;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CitaRepository extends MongoRepository<Cita, String> {
        List<Cita> findByUsuarioUsername(String usuarioUsername);

        List<Cita> findByMedicoUsername(String medicoUsername);

        List<Cita> findByEnfermeroUsername(String enfermeroUsername);

        List<Cita> findByFecha(String fecha);

        List<Cita> findByFechaAndHora(String fecha, String hora);

        List<Cita> findByEstado(String estado);

        List<Cita> findByFechaAndEstado(String fecha, String estado);

        List<Cita> findByMedicoUsernameAndFechaOrderByHoraAsc(String medicoUsername, String fecha);

        List<Cita> findByMedicoUsernameAndFechaAndHora(String medicoUsername, String fecha, String hora);

        List<Cita> findAllByOrderByFechaAscHoraAsc();

        List<Cita> findByFechaGreaterThanEqualOrderByFechaAscHoraAsc(String fecha);

        long countByEstado(String estado);

        long countByFecha(String fecha);

        long countByFechaAndEstado(String fecha, String estado);

        long countByMedicoUsernameAndFecha(String medicoUsername, String fecha);

        long countByUsuarioUsername(String usuarioUsername);

        List<Cita> findByMedicoUsernameAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(String medicoUsername,
                        String fecha);

        List<Cita> findByEstadoOrderByFechaAscHoraAsc(String estado);
}
