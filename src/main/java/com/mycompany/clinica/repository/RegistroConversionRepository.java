package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.RegistroConversion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RegistroConversionRepository extends MongoRepository<RegistroConversion, String> {
    List<RegistroConversion> findByUsuarioNombreOrderByFechaHoraDesc(String usuarioNombre);
    Page<RegistroConversion> findByUsuarioNombreOrderByFechaHoraDesc(String usuarioNombre, Pageable pageable);
    Page<RegistroConversion> findAllByOrderByFechaHoraDesc(Pageable pageable);
}