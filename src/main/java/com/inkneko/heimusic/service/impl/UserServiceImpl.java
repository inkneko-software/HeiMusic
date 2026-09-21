package com.inkneko.heimusic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.errorcode.MinIOServiceErrorCode;
import com.inkneko.heimusic.errorcode.UserServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.UserAuthMapper;
import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.UserAuth;
import com.inkneko.heimusic.model.entity.UserDetail;
import com.inkneko.heimusic.service.AuthService;
import com.inkneko.heimusic.service.MinIOService;
import com.inkneko.heimusic.service.UserService;
import com.inkneko.heimusic.util.mail.AsyncMailSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private static final Set<String> ALLOWED_AVATAR_MIME = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private static final long MAX_AVATAR_SIZE = 10L * 1024 * 1024;

    /**
     * 防刷：每用户每小时最多5次头像上传请求（含校验失败的请求，超限请求不落盘不探测）
     */
    private static final int MAX_AVATAR_UPLOADS_PER_HOUR = 5;
    private static final Duration AVATAR_UPLOAD_LIMIT_DURATION = Duration.ofHours(1);

    private final SecureRandom secureRandom = new SecureRandom();

    private final Tika tika = new Tika();

    private final UserDetailMapper userDetailMapper;

    private final MinIOService minIOService;

    private final MinIOConfig minIOConfig;

    private final RedissonClient redissonClient;

    @Autowired
    public UserServiceImpl(UserDetailMapper userDetailMapper, MinIOService minIOService, MinIOConfig minIOConfig, RedissonClient redissonClient) {
        this.userDetailMapper = userDetailMapper;
        this.minIOService = minIOService;
        this.minIOConfig = minIOConfig;
        this.redissonClient = redissonClient;
    }

    @Override
    public UserDetail findUser(String email) {
        return userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email));
    }

    @Override
    public UserDetail findUser(Integer uid) {
        return userDetailMapper.selectOne(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getUserId, uid));
    }

    @Override
    public void updateUserInfo(Integer userId, String username, LocalDate birth, String gender, String sign) {
        if (findUser(userId) == null) {
            throw new ServiceException(UserServiceErrorCode.USER_NOT_EXISTS);
        }
        //允许清空至NULL，须显式set，不能走updateById（其默认跳过null字段）
        userDetailMapper.update(null, new LambdaUpdateWrapper<UserDetail>()
                .eq(UserDetail::getUserId, userId)
                .set(UserDetail::getUsername, username)
                .set(UserDetail::getBirth, birth)
                .set(UserDetail::getGender, gender)
                .set(UserDetail::getSign, sign == null ? "" : sign));
    }

    @Override
    public void updateAvatar(Integer userId, MultipartFile avatar) {
        UserDetail userDetail = findUser(userId);
        if (userDetail == null) {
            throw new ServiceException(UserServiceErrorCode.USER_NOT_EXISTS);
        }
        //防刷限频：自首次上传请求起一小时窗口内最多5次，超限请求不落盘不探测
        RAtomicLong uploadCount = redissonClient.getAtomicLong("user_avatar_upload_count:" + userId);
        long count = uploadCount.incrementAndGet();
        if (count == 1) {
            uploadCount.expire(AVATAR_UPLOAD_LIMIT_DURATION);
        }
        if (count > MAX_AVATAR_UPLOADS_PER_HOUR) {
            throw new ServiceException(UserServiceErrorCode.AVATAR_UPDATE_OVER_LIMIT);
        }
        if (avatar == null || avatar.isEmpty()) {
            throw new ServiceException(UserServiceErrorCode.AVATAR_FILE_EMPTY);
        }
        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new ServiceException(UserServiceErrorCode.AVATAR_FILE_TOO_LARGE);
        }
        File tempFile = null;
        try {
            tempFile = File.createTempFile("heimusic_avatar_", ".tmp");
            avatar.transferTo(tempFile);
            //以文件实际内容探测类型，不信任客户端声明的ContentType
            String mimeType = tika.detect(tempFile);
            if (!ALLOWED_AVATAR_MIME.contains(mimeType)) {
                throw new ServiceException(UserServiceErrorCode.AVATAR_FORMAT_UNSUPPORTED);
            }
            String objectKey = String.format("avatar/%d-%d-%s", userId, System.currentTimeMillis(), UUID.randomUUID());
            minIOService.upload(minIOConfig.getBucket(), objectKey, tempFile, mimeType);
            userDetailMapper.update(null, new LambdaUpdateWrapper<UserDetail>()
                    .eq(UserDetail::getUserId, userId)
                    .set(UserDetail::getAvatarBucket, minIOConfig.getBucket())
                    .set(UserDetail::getAvatarObjectKey, objectKey));
            //清理旧头像对象，失败仅记录日志（遗留孤儿对象不影响业务）
            if (userDetail.getAvatarObjectKey() != null && !userDetail.getAvatarObjectKey().isEmpty()) {
                try {
                    minIOService.delete(userDetail.getAvatarBucket(), userDetail.getAvatarObjectKey());
                } catch (Exception e) {
                    log.warn("清理用户{}旧头像对象{}/{}失败", userId, userDetail.getAvatarBucket(), userDetail.getAvatarObjectKey(), e);
                }
            }
        } catch (IOException e) {
            log.error("处理用户{}头像上传失败", userId, e);
            throw new ServiceException(MinIOServiceErrorCode.UPLOAD_FAILED);
        } finally {
            if (tempFile != null) {
                //upload成功路径会自行删除临时文件，此处兜底失败路径
                boolean ignored = tempFile.delete();
            }
        }
    }
}
