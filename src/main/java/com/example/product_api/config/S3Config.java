package com.example.product_api.config;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3Config {

    private static final Logger logger = LoggerFactory.getLogger(S3Config.class);

    public static final String UNCONFIGURED_URL = "http://storage-s3-nao-configurado.invalid";

    @Value("${storage.s3.url:}")
    private String url;

    @Value("${storage.s3.key-id:}")
    private String keyId;

    @Value("${storage.s3.secret-key:}")
    private String secretKey;

    @Value("${storage.s3.region:garage}")
    private String region;

    @Bean
    public S3Client s3Client() {
        if (isBlank(url) || isBlank(keyId) || isBlank(secretKey)) {
            logger.warn("Storage S3 nao configurado (storage.s3.url / key-id / secret-key). "
                    + "A aplicacao sobe normalmente, mas qualquer operacao com imagens vai falhar. "
                    + "Defina S3_URL, S3_KEY_ID e S3_SECRET_KEY no ambiente.");
            return build(UNCONFIGURED_URL, "unconfigured", "unconfigured", region);
        }
        logger.info("Storage S3 configurado: url={}, region={}", url, region);
        return build(url, keyId, secretKey, region);
    }

    public static S3Client build(String url, String keyId, String secretKey, String region) {
        S3Configuration serviceConfig = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        ClientOverrideConfiguration override = ClientOverrideConfiguration.builder()
                .addExecutionInterceptor(new ExecutionInterceptor() {
                    @Override
                    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest ctx,
                                                            ExecutionAttributes attrs) {
                        return ctx.httpRequest().toBuilder()
                                .removeHeader("Expect")
                                .build();
                    }
                })
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(url))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(keyId, secretKey)))
                .serviceConfiguration(serviceConfig)
                .overrideConfiguration(override)
                .build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
