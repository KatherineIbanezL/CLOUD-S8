package cl.duoc.guia_service.producer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class S3ObjectDto {
    private String nombreArchivo;
    private String bucket;
    private String s3Key;
    private long tamanioBytes;
    private String contentType;
    private String estado;
}