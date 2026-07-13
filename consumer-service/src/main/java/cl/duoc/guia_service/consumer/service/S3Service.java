package cl.duoc.guia_service.consumer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String awsS3BucketName;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // Recibe la llave pre-armada "resumenes/{id_resumen}/{nombre_archivo}.pdf" desde el service 
    public String subirArchivoConKey(String s3Key, File archivo) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3BucketName)
                .key(s3Key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(archivo));
        return s3Key;
    }

    // Subir archivos
    public String subirArchivo(String entidad, File archivo) {
        String fechaCarpeta = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String s3Key = fechaCarpeta + "/" + entidad + "/" + archivo.getName();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3BucketName)
                .key(s3Key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(archivo));
        return s3Key;
    }

    // Modificar y actualizar los archivos en AWS S3 (Sobrescribe la Key existente)
    public void actualizarArchivo(String s3Key, File nuevoArchivo) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3BucketName)
                .key(s3Key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(nuevoArchivo));
    }

    // Descargar los archivos desde AWS S3 asegurando integridad
    public byte[] descargarArchivo(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsS3BucketName)
                .key(s3Key)
                .build();
                
        return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
    }

    // Eliminar un archivo de S3 automáticamente cuando se borre en el sistema
    public void eliminarArchivo(String s3Key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(awsS3BucketName)
                .key(s3Key)
                .build();
                
        s3Client.deleteObject(deleteObjectRequest);
    }
}