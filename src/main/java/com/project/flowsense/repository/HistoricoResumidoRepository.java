package com.project.flowsense.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.project.flowsense.entity.HistoricoResumido;

public interface HistoricoResumidoRepository extends JpaRepository<HistoricoResumido, Long> {

    Optional<HistoricoResumido> findByDispositivoAndMinuto(String dispositivo, Instant minuto);

    /** Minutos do período [de, ate), em ordem cronológica. */
    List<HistoricoResumido> findByDispositivoAndMinutoGreaterThanEqualAndMinutoLessThanOrderByMinutoAsc(
            String dispositivo, Instant de, Instant ate);

    /** Soma do consumo (kWh) no período [de, ate). */
    @Query("""
            select coalesce(sum(h.consumoKwh), 0)
            from HistoricoResumido h
            where h.dispositivo = :dispositivo and h.minuto >= :de and h.minuto < :ate
            """)
    double somarConsumo(String dispositivo, Instant de, Instant ate);

    @Modifying
    @Transactional
    long deleteByMinutoBefore(Instant limite);
}
