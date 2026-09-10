package com.inkneko.heimusic.service;

import com.inkneko.heimusic.config.MinIOConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * MinIOService 集成测试：连接 WSL2 中的 MinIO 实例，做上传/下载/删除回环。
 * 每次运行使用独立的一次性桶，测试结束清理，不影响已有数据。
 */
@SpringBootTest
class MinIOServiceTests {

    @Autowired
    MinIOService minIOService;

    @Autowired
    MinIOConfig minIOConfig;

    @TempDir
    Path tempDir;

    String bucket;
    MinioClient minioClient;

    @BeforeEach
    void setUpBucket() throws Exception {
        bucket = "it-" + UUID.randomUUID();
        minioClient = MinioClient.builder()
                .endpoint(minIOConfig.getEndpoint())
                .credentials(minIOConfig.getAccessKey(), minIOConfig.getSecretKey())
                .build();
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    @AfterEach
    void tearDownBucket() {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
        } catch (Exception ignored) {
            //测试失败遗留对象时桶删不掉，可忽略（桶名随机不影响后续运行）
        }
    }

    @Test
    void uploadDownloadDeleteRoundtrip() throws Exception {
        //注意：upload 上传完成后会删除传入的本地文件，比对内容需先在内存留一份
        byte[] content = new byte[4096];
        new SecureRandom().nextBytes(content);
        Path source = tempDir.resolve("roundtrip.bin");
        Files.write(source, content);

        String objectKey = "test/roundtrip-" + UUID.randomUUID() + ".bin";
        minIOService.upload(bucket, objectKey, source.toFile(), "application/octet-stream");

        File downloaded = minIOService.download(bucket, objectKey);
        assertArrayEquals(content, Files.readAllBytes(downloaded.toPath()));

        minIOService.delete(bucket, objectKey);
    }
}
