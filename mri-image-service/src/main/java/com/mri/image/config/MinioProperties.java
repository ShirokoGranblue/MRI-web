package com.mri.image.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mri.image.minio")
public record MinioProperties(String endpoint, String accessKey, String secretKey, String bucket) {
}
