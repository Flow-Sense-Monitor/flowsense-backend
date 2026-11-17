package com.project.flowsense.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.project.flowsense.entity.EventosImportantes;

public interface EventosImportantesRepository extends JpaRepository<EventosImportantes, Long> {

    /** Eventos mais recentes primeiro; o Pageable limita a quantidade. */
    List<EventosImportantes> findByDispositivoOrderByMomentoDescIdDesc(String dispositivo, Pageable pagina);

    @Modifying
    @Transactional
    long deleteByMomentoBefore(Instant limite);
}
