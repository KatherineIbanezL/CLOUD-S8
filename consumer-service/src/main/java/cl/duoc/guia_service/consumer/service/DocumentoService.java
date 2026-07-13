package cl.duoc.guia_service.consumer.service;

import cl.duoc.guia_service.consumer.model.Documento;
import cl.duoc.guia_service.consumer.repository.DocumentoRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class DocumentoService {

    private final DocumentoRepository documentoRepository;

    public DocumentoService(DocumentoRepository documentoRepository) {
        this.documentoRepository = documentoRepository;
    }

    public void procesarEstadoGuia(String s3Key) {
        // Buscamos el documento en la BD local usando el s3Key del evento
        Documento documento = documentoRepository.findAll().stream()
                .filter(d -> s3Key.equals(d.getS3Key()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Guía no encontrada en base de datos para la llave S3: " + s3Key));

        // Actualizamos su estado para cerrar el ciclo asíncrono
        documento.setEstado("PROCESADO");
        documento.setFechaModificacion(LocalDateTime.now());

        documentoRepository.save(documento);
        System.out.println(" Base de Datos Oracle Cloud actualizada: Guía con S3Key [" + s3Key + "] marcada como PROCESADA.");
    }
}