package cl.duoc.guia_service.consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.guia_service.consumer.model.Documento;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findByTransportistaEntityAndFechaCreacionBetween(String transportistaEntity, LocalDateTime inicio, LocalDateTime fin);
}