package cl.duoc.guia_service.producer.controller;

import cl.duoc.guia_service.producer.dto.CrearGuiaRequest;
import cl.duoc.guia_service.producer.dto.CrearGuiaResponse;
import cl.duoc.guia_service.producer.model.Documento;
import cl.duoc.guia_service.producer.service.DocumentoService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    // 1. ENDPOINT: SUBIR / GENERAR
    @PostMapping("/generar")
    public ResponseEntity<CrearGuiaResponse> generarDocumento(@Valid @RequestBody CrearGuiaRequest request) {
        try {
            byte[] mockPdf = "Contenido simulado del PDF".getBytes(); 
            
            Documento doc = documentoService.registrarDocumento(request.transportista(), request.nombreArchivo(), mockPdf);

            // Generar el ID de evento para la respuesta del cliente REST
            String eventId = UUID.randomUUID().toString().substring(0, 8).toUpperCase(); 

            // Construir la respuesta formal estructurada en el record
            CrearGuiaResponse response = new CrearGuiaResponse(
                    "La guía se recibió exitosamente, se subió a S3 y está en cola para su procesamiento.",
                    doc.getS3Key(), 
                    eventId
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

// 2. ENDPOINT: MODIFICAR / ACTUALIZAR
    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizarDocumento(
            @PathVariable Long id,
            @RequestBody CrearGuiaRequest requestDto) { 
        try {
            Documento docActualizado = documentoService.actualizarDocumentoCompleto(id, requestDto);
            return ResponseEntity.ok(docActualizado);
            
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error al actualizar objeto en AWS S3: " + e.awsErrorDetails().errorMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error de negocio: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno inesperado al actualizar: " + e.getMessage());
        }
    }

    // 3. ENDPOINT: DESCARGAR
    @GetMapping("/descargar/{id}")
    public ResponseEntity<?> descargarDocumento(@PathVariable Long id) {
        try {
            Documento doc = documentoService.obtenerPorId(id);
            byte[] archivoBytes = documentoService.descargarArchivoS3(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", doc.getNombreArchivo());

            return new ResponseEntity<>(archivoBytes, headers, HttpStatus.OK);

        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error en el almacenamiento AWS S3: " + e.awsErrorDetails().errorMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error de consulta: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error de estado: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado al intentar descargar el archivo: " + e.getMessage());
        }
    }

    // 4. ENDPOINT: BORRAR / ELIMINAR
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarDocumento(@PathVariable Long id) {
        try {
            documentoService.eliminarDocumentoFisicoYLogico(id);
            return ResponseEntity.ok("Archivo físico en S3 y registros en Oracle eliminados con éxito.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error de eliminación: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el archivo: " + e.getMessage());
        }
    }

    // 5. ENDPOINT: HISTORIAL 
    @GetMapping("/historial")
    public ResponseEntity<?> consultarHistorial(
            @RequestParam String transportista,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        try {
            List<Documento> historial = documentoService.consultarHistorial(transportista, inicio, fin);
            
            if (historial.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            return ResponseEntity.ok(historial);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al consultar el historial en la base de datos: " + e.getMessage());
        }
    }
}