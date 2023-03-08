package com.inkneko.heimusic.service.impl;

import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.service.MinIOService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
public class MinIOServiceImpl implements MinIOService {
    MinIOConfig minioConfig;

    MinioClient minioClient;

    @Autowired
    public MinIOServiceImpl(MinIOConfig minIOConfig) {
        this.minioConfig = minIOConfig;
        System.out.println(minioConfig.getEndpoint());
        minioClient = MinioClient.builder().endpoint(minioConfig.getEndpoint()).credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey()).build();
    }

    @Override
    public String getUploadUrl(String bucket, String object) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(object)
                            .expiry(1, TimeUnit.DAYS)
                            .build());
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | MinioException e) {
            System.out.println(e);
        }
        return null;
    }

    @Override
    public String getDownloadUrl(String bucket, String object) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(object)
                            .expiry(1, TimeUnit.DAYS)
                            .build());
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | MinioException e) {
            System.out.println(e);
        }
        return null;
    }

    @Override
    public void upload(String bucket, String objectPath, MultipartFile file) throws ServiceException {

    }

    @Override
    public void download(String bucket, String objectPath, String localPath) throws ServiceException {

    }


}
