package com.inkneko.heimusic.service;

import com.inkneko.heimusic.exception.ServiceException;
import org.springframework.web.multipart.MultipartFile;

public interface MinIOService {
    /**
     * 获取上传Url
     * @param bucket 指定的桶
     * @param objectPath 欲存储至的对象路径，如avatar/uid-timestamp.bin
     * @return 预签名的上传链接
     * @throws ServiceException 业务异常
     */
    String getUploadUrl(String bucket, String objectPath) throws ServiceException;

    /**
     * 获取下载url，默认有效期1天
     * @param bucket 指定的桶
     * @param objectPath 对象路径，如avatar/uid-timestamp.bin
     * @return 预签名的下载链接
     * @throws ServiceException 业务异常
     */
    String getDownloadUrl(String bucket, String objectPath) throws ServiceException;

    /**
     * 上传文件
     * @param bucket 指定的桶
     * @param objectPath 欲存储至的对象路径，如avatar/uid-timestamp.bin
     * @param file 文件
     * @throws ServiceException 业务异常
     */
    void upload(String bucket, String objectPath, MultipartFile file) throws ServiceException;

    /**
     *
     * @param bucket 指定的桶
     * @param objectPath 对象路径，如avatar/uid-timestamp.bin
     * @param localPath 欲存储至本地的路径，如/tmp/uid-timestamp.bin
     * @throws ServiceException 业务异常
     */
    void download(String bucket, String objectPath, String localPath) throws ServiceException;
}
