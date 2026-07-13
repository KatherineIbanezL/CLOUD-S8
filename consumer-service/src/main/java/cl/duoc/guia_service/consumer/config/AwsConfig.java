package cl.duoc.guia_service.consumer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${spring.cloud.aws.credentials.session-token:}")
    private String sessionToken;

    @Value("${spring.cloud.aws.region.static:us-east-1}")
    private String region;

    @Bean
    @Primary
    public S3Client s3Client() {
        AwsCredentials credentials;

        // Si AWS provee un Session Token, se utiliza la firma extendida
        if (StringUtils.hasText(sessionToken)) {
            credentials = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
        } else {
            credentials = AwsBasicCredentials.create(accessKey, secretKey);
        }

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}