package com.inkneko.heimusic.service;

import com.inkneko.heimusic.exception.ServiceException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

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
     * 上传文件
     * @param bucket 指定的桶
     * @param objectPath 欲存储至的对象路径，如avatar/uid-timestamp.bin
     * @param file 文件
     * @throws ServiceException 业务异常
     */
    void upload(String bucket, String objectPath, File file) throws ServiceException;


    /**
     *
     * @param bucket 指定的桶
     * @param objectPath 对象路径，如avatar/uid-timestamp.bin
     * @param localPath 欲存储至本地的文件路径，该路径不能存在文件。
     * @throws ServiceException 业务异常
     */
    void download(String bucket, String objectPath, String localPath) throws ServiceException;

    /**
     * 下载文件至临时存储目录
     *
     * @param bucket 指定的桶
     * @param objectPath 对象路径，如avatar/uid-timestamp.bin
     * @throws ServiceException 业务异常
     * @return 若下载成功则返回文件路径
     */
    File download(String bucket, String objectPath) throws ServiceException;

    /**
     * 删除对象
     *
     * @param bucket 指定的桶
     * @param objectPath 对象路径，如avatar/uid-timestamp.bin
     * @throws ServiceException 业务异常
     */
    void delete(String bucket, String objectPath) throws ServiceException;

}
