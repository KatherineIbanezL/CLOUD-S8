package cl.duoc.guia_service.producer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoDto {
    private Long id;
    private String nombreArchivo;
    private String tipoDocumento;
    private String transportistaEntity; 
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion; 
    private String s3Key; 
    private String estado;
}