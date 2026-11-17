package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.Cita;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CitaRepository extends MongoRepository<Cita, String> {
    
    @Query("{ 'usuarioUsername': ?0 }")
    List<Cita> findByUsuarioUsername(String usuarioUsername);

    @Query("{ 'medicoUsername': ?0 }")
    List<Cita> findByMedicoUsername(String medicoUsername);

    @Query("{ 'enfermeroUsername': ?0 }")
    List<Cita> findByEnfermeroUsername(String enfermeroUsername);

    @Query("{ 'fecha': ?0 }")
    List<Cita> findByFecha(String fecha);

    @Query("{ 'fecha': ?0, 'hora': ?1 }")
    List<Cita> findByFechaAndHora(String fecha, String hora);

    @Query("{ 'estado': ?0 }")
    List<Cita> findByEstado(String estado);

    @Query("{ 'fecha': ?0, 'estado': ?1 }")
    List<Cita> findByFechaAndEstado(String fecha, String estado);

    @Query("{ 'medicoUsername': ?0, 'fecha': ?1 }")
    List<Cita> findByMedicoUsernameAndFechaOrderByHoraAsc(String medicoUsername, String fecha);

    @Query("{ 'medicoUsername': ?0, 'fecha': ?1, 'hora': ?2 }")
    List<Cita> findByMedicoUsernameAndFechaAndHora(String medicoUsername, String fecha, String hora);

    List<Cita> findAllByOrderByFechaAscHoraAsc();

    @Query("{ 'fecha': { $gte: ?0 } }")
    List<Cita> findByFechaGreaterThanEqualOrderByFechaAscHoraAsc(String fecha);

    long countByEstado(String estado);
    long countByFecha(String fecha);
    long countByFechaAndEstado(String fecha, String estado);
    long countByMedicoUsernameAndFecha(String medicoUsername, String fecha);
    long countByUsuarioUsername(String usuarioUsername);

    @Query("{ 'medicoUsername': ?0, 'fecha': { $gte: ?1 } }")
    List<Cita> findByMedicoUsernameAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(String medicoUsername, String fecha);

    @Query("{ 'estado': ?0 }")
    List<Cita> findByEstadoOrderByFechaAscHoraAsc(String estado);
}