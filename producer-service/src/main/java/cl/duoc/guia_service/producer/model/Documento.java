package cl.duoc.guia_service.producer.model;

import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "documentos_v3")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "tipo_documento", length = 100)
    private String tipoDocumento;

    @Column(name = "transportista_entity", length = 255)
    private String transportistaEntity;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Column(name = "estado", length = 50)
    private String estado;
}