package cl.duoc.guia_service.producer.service;

import cl.duoc.guia_service.producer.dto.CrearGuiaRequest;
import cl.duoc.guia_service.producer.model.Documento;
import cl.duoc.guia_service.producer.repository.DocumentoRepository;
import cl.duoc.guia_service.shared.events.GuiaCreadaEvent;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final GuiaProducerService guiaProducerService; // Para despachar a RabbitMQ
    private final S3Service s3Service;

    public DocumentoService(DocumentoRepository documentoRepository, GuiaProducerService guiaProducerService, S3Service s3Service) {
        this.documentoRepository = documentoRepository;
        this.guiaProducerService = guiaProducerService;
        this.s3Service = s3Service;
    }

    // 1. REGISTRAR, SUBIR A S3 Y ENVIAR A LA COLA
    public Documento registrarDocumento(String transportista, String nombre, byte[] mockPdf) throws Exception {
        String eventId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String numeroGuia = "GD-" + eventId;

        // 1. Guardar en Oracle Cloud (Tabla Productor)
        Documento doc = new Documento();
        doc.setNombreArchivo(nombre);
        doc.setTipoDocumento("GUIA_DESPACHO");
        doc.setTransportistaEntity(transportista);
        doc.setFechaCreacion(LocalDateTime.now());
        doc.setEstado("PENDIENTE");
        doc = documentoRepository.saveAndFlush(doc);

        // 2. Subir directamente a S3
        String s3KeyEstructurada = "resumenes/" + doc.getId() + "/" + nombre;
        
        String s3Key = s3Service.subirArchivoBytes(s3KeyEstructurada, mockPdf); 
        doc.setS3Key(s3Key);
        doc = documentoRepository.saveAndFlush(doc);

        // 3. Crear el evento estructurado (record)
        GuiaCreadaEvent evento = new GuiaCreadaEvent(
                eventId,
                nombre,
                transportista,
                numeroGuia,
                doc.getEstado()
        );

        // 4. Despachamos a la Cola 1 mediante RabbitMQ
        guiaProducerService.enviarGuia(evento);

        return doc;
    }

    // 2. MODIFICAR / ACTUALIZAR
    public Documento actualizarDocumentoCompleto(Long id, CrearGuiaRequest dto) { // <-- Cambiado a CrearGuiaRequest
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        // Los records de Java no usan "get", se llaman directo como funciones:
        if (dto.transportista() != null) {
            documento.setTransportistaEntity(dto.transportista());
        }
        if (dto.nombreArchivo() != null) {
            documento.setNombreArchivo(dto.nombreArchivo());
        }

        documento.setEstado("MODIFICADO");
        documento.setFechaModificacion(LocalDateTime.now());

        return documentoRepository.save(documento);
    }

    // 3. DESCARGAR
    public byte[] descargarArchivoS3(Long id) throws Exception {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el documento para descargar con el ID: " + id));
        
        if (doc.getS3Key() == null || doc.getS3Key().isEmpty()) {
            throw new IllegalStateException("El documento no posee una llave válida de AWS S3 asociada.");
        }
        
        return s3Service.descargarArchivo(doc.getS3Key());
    }

    // 4. BORRAR / ELIMINAR
    public void eliminarDocumentoFisicoYLogico(Long id) throws Exception {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se puede eliminar un registro inexistente con ID: " + id));

        // Eliminar el objeto almacenado en AWS S3
        if (doc.getS3Key() != null && !doc.getS3Key().isEmpty()) {
            try {
                s3Service.eliminarArchivo(doc.getS3Key());
            } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
                System.err.println("Advertencia de AWS: Falló remoción física en S3: " + e.awsErrorDetails().errorMessage());
            }
        }

        // Eliminar el registro en la base de datos de Oracle Cloud
        documentoRepository.delete(doc);
    } 

    // 5. CONSULTAR HISTORIAL
    public List<Documento> consultarHistorial(String transportista, LocalDateTime inicio, LocalDateTime fin) {
        return documentoRepository.findByTransportistaEntityAndFechaCreacionBetween(transportista, inicio, fin);
    }

    public Documento obtenerPorId(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de documento no encontrada: " + id));
    }
}