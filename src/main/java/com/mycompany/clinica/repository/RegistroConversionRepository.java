package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.RegistroConversion;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RegistroConversionRepository extends MongoRepository<RegistroConversion, String> {
    List<RegistroConversion> findByUsuarioNombreOrderByFechaHoraDesc(String usuarioNombre);
}