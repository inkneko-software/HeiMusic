package com.inkneko.heimusic.service.impl;

import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.errorcode.MinIOServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.service.MinIOService;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinIOServiceImpl implements MinIOService {
    MinIOConfig minioConfig;

    MinioClient minioClient;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public MinIOServiceImpl(MinIOConfig minIOConfig) {
        this.minioConfig = minIOConfig;
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
    public void upload(String bucket, String objectPath, MultipartFile multipartFile) throws ServiceException {
        try {
            String filePrefix = String.format("%s_%s", bucket, new String(Base64.getEncoder().encode(objectPath.getBytes()))).toLowerCase();
            File file = File.createTempFile(filePrefix, ".tmp");
            multipartFile.transferTo(file);
            minioClient.uploadObject(UploadObjectArgs.builder().bucket(bucket).object(objectPath).filename(file.getAbsolutePath()).build());
            boolean ignored = file.delete();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | MinioException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void upload(String bucket, String objectPath, File file) throws ServiceException {
        try {
            minioClient.uploadObject(UploadObjectArgs.builder().bucket(bucket).object(objectPath).filename(file.getAbsolutePath()).build());
            boolean ignored = file.delete();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | MinioException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void download(String bucket, String objectPath, String localPath) throws ServiceException {
        try {
            minioClient.downloadObject(DownloadObjectArgs.builder().bucket(bucket).object(objectPath).filename(localPath).build());
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | MinioException e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载文件至临时存储目录
     *
     * @param objectPath 对象完整的路径，包括桶名称，如/heimusic/avatar/uid-timestamp.bin
     * @return 若下载成功则返回文件路径
     * @throws ServiceException 业务异常
     */
    @Override
    public File download(String bucket, String objectPath) throws ServiceException {
        try {
            File tempFile = File.createTempFile("heimusic_download_", UUID.randomUUID().toString());
            boolean ignored = tempFile.delete();
            download(bucket, objectPath, tempFile.getAbsolutePath());
            return tempFile;
        } catch (IOException e) {
            logger.error("文件下载失败", e);
            throw new ServiceException(500, "服务端处理文件时下载失败");
        }
    }

    /**
     * 删除对象
     *
     * @param bucket     指定的桶
     * @param objectPath 对象路径，如avatar/uid-timestamp.bin
     * @throws ServiceException 业务异常
     */
    @Override
    public void delete(String bucket, String objectPath) throws ServiceException {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectPath).build());
        } catch (Exception e) {
            logger.error("error in deleting object, caused by: ", e);
            throw new ServiceException(MinIOServiceErrorCode.DELETE_FAILED);
        }
    }
}
