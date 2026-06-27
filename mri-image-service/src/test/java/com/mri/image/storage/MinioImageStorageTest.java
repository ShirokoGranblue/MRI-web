package com.mri.image.storage;

import com.mri.image.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioImageStorageTest {
    @Test
    void createsMissingBucketBeforeUploading() throws Exception {
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        MinioProperties properties = new MinioProperties(
                "http://localhost:9000",
                "mri",
                "mri123456",
                "mri-images"
        );
        MinioImageStorage storage = new MinioImageStorage(properties, client);

        storage.putObject(
                "series/1/test.png",
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                3,
                "image/png"
        );

        verify(client).makeBucket(any(MakeBucketArgs.class));
        verify(client).putObject(any(PutObjectArgs.class));
    }
}
