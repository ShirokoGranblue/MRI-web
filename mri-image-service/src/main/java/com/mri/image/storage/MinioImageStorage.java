package com.mri.image.storage;

import com.mri.image.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Component
public class MinioImageStorage {
    private static final Logger log = LoggerFactory.getLogger(MinioImageStorage.class);

    public record LoadedObject(byte[] content, String contentType) {
    }

    private final MinioProperties properties;
    private final MinioClient client;

    public MinioImageStorage(MinioProperties properties) {
        this.properties = properties;
        this.client = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @PostConstruct
    void ensureBucket() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
                log.info("Created MinIO bucket {}", properties.bucket());
            }
        } catch (Exception ex) {
            log.warn("MinIO bucket check/create failed (is minio running?): {}", ex.getMessage());
        }
    }

    public void putObject(String objectKey, InputStream in, long size, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(in, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("上传影像到对象存储失败: " + ex.getMessage(), ex);
        }
    }

    public LoadedObject loadObject(String objectKey) {
        try (InputStream in = client.getObject(GetObjectArgs.builder()
                .bucket(properties.bucket())
                .object(objectKey)
                .build());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            String contentType = client.statObject(StatObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build()).contentType();
            return new LoadedObject(out.toByteArray(), contentType == null ? "application/octet-stream" : contentType);
        } catch (Exception ex) {
            throw new IllegalStateException("影像文件不存在或未上传: " + objectKey, ex);
        }
    }

    public void removeObjectQuietly(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            log.warn("删除对象存储对象失败(忽略): {}: {}", objectKey, ex.getMessage());
        }
    }
}
