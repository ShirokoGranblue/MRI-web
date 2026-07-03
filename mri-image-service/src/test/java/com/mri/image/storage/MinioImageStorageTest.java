package com.mri.image.storage;

import com.mri.image.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void objectReadFailureDoesNotExposeStorageKey() throws Exception {
        MinioClient client = mock(MinioClient.class);
        when(client.getObject(any(GetObjectArgs.class))).thenThrow(new IllegalStateException("missing"));
        MinioImageStorage storage = new MinioImageStorage(new MinioProperties(
                "http://localhost:9000",
                "mri",
                "mri123456",
                "mri-images"
        ), client);

        assertThatThrownBy(() -> storage.loadObject("series/51/private-object.png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("影像文件不存在或未上传")
                .hasMessageNotContaining("series/51");
    }
}
