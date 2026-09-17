package com.inkneko.heimusic.service;

import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.model.entity.UserDetail;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

public interface UserService {


    /**
     * 通过邮箱查询用户
     *
     * @param email
     * @return
     * @throws ServiceException
     */
    UserDetail findUser(String email) throws ServiceException;

    /**
     * 通过用户id查询用户
     *
     * @param uid
     * @return
     * @throws ServiceException
     */
    UserDetail findUser(Integer uid) throws ServiceException;

    /**
     * 更新用户资料（用户名/生日/性别/个性签名）
     * 全量更新：字段传null视为清空（sign 列非空，null 落库为空串）
     *
     * @param userId   用户id
     * @param username 用户名，可为null
     * @param birth    生日，可为null
     * @param gender   性别（m/f），可为null
     * @param sign     个性签名，可为null
     * @throws ServiceException 用户不存在
     */
    void updateUserInfo(Integer userId, String username, Date birth, String gender, String sign) throws ServiceException;

    /**
     * 更新用户头像
     * 上传新头像至对象存储并更新头像字段，随后清理旧头像对象（清理失败仅记录日志）
     *
     * @param userId  用户id
     * @param avatar  头像文件（按文件实际内容探测类型，仅支持 JPEG/PNG/WebP/GIF，最大10MB）
     * @throws ServiceException 用户不存在 / 文件为空 / 文件过大 / 格式不支持 / 上传失败
     */
    void updateAvatar(Integer userId, MultipartFile avatar) throws ServiceException;
}
