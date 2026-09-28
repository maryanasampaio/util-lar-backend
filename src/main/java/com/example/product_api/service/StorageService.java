package com.example.product_api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.product_api.exception.StorageUnavailableException;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class StorageService {

    private final S3Client s3;
    private final String bucket;
    private final String url;
    private final boolean configured;

    public StorageService(S3Client s3,
                          @Value("${storage.s3.bucket:}") String bucket,
                          @Value("${storage.s3.url:}") String url,
                          @Value("${storage.s3.key-id:}") String keyId,
                          @Value("${storage.s3.secret-key:}") String secretKey) {
        this.s3 = s3;
        this.bucket = bucket;
        this.url = url;
        this.configured = !isBlank(bucket) && !isBlank(url) && !isBlank(keyId) && !isBlank(secretKey);
    }

    public record StoredFile(byte[] bytes, String contentType) {}

    public boolean isConfigured() {
        return configured;
    }

    public String describeTarget() {
        return configured ? url + "/" + bucket : "storage nao configurado";
    }

    public String upload(String key, byte[] bytes, String contentType) {
        requireConfigured();
        s3.putObject(req -> req
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType != null ? contentType : "application/octet-stream")
                        .contentLength((long) bytes.length),
                RequestBody.fromBytes(bytes));
        return key;
    }

    public Optional<StoredFile> download(String key) {
        requireConfigured();
        try {
            ResponseBytes<GetObjectResponse> obj = s3.getObjectAsBytes(req -> req.bucket(bucket).key(key));
            return Optional.of(new StoredFile(obj.asByteArray(), obj.response().contentType()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    public void delete(String key) {
        requireConfigured();
        s3.deleteObject(req -> req.bucket(bucket).key(key));
    }

    public List<String> list(String prefix) {
        requireConfigured();
        return s3.listObjectsV2(req -> req.bucket(bucket).prefix(prefix))
                .contents().stream().map(S3Object::key).toList();
    }

    public boolean exists(String key) {
        requireConfigured();
        try {
            s3.headObject(req -> req.bucket(bucket).key(key));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    private void requireConfigured() {
        if (!configured) {
            throw new StorageUnavailableException(
                    "Storage S3 nao configurado neste ambiente. "
                    + "Defina S3_URL, S3_BUCKET, S3_KEY_ID e S3_SECRET_KEY.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
