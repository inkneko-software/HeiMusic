package com.inkneko.heimusic.service;

import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.errorcode.UserServiceErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import com.inkneko.heimusic.mapper.UserDetailMapper;
import com.inkneko.heimusic.model.entity.UserDetail;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserServiceTests {

    @Autowired
    UserService userService;

    @Autowired
    UserDetailMapper userDetailMapper;

    @Test
    void findUserByEmailAndByUid() {
        String email = UUID.randomUUID() + "@test.example.com";
        UserDetail detail = new UserDetail();
        detail.setEmail(email);
        detail.setUsername("测试用户");
        userDetailMapper.insert(detail);

        UserDetail byEmail = userService.findUser(email);
        assertEquals(detail.getUserId(), byEmail.getUserId());
        assertEquals("测试用户", byEmail.getUsername());

        UserDetail byUid = userService.findUser(detail.getUserId());
        assertEquals(email, byUid.getEmail());
    }

    @Test
    void findUserReturnsNullWhenMissing() {
        assertNull(userService.findUser("nobody-" + UUID.randomUUID() + "@test.example.com"));
        assertNull(userService.findUser(-1));
    }

    @Test
    void updateUserInfo() {
        UserDetail detail = new UserDetail();
        detail.setEmail(UUID.randomUUID() + "@test.example.com");
        userDetailMapper.insert(detail);

        Date birth = new Date(0);
        userService.updateUserInfo(detail.getUserId(), "测试用户", birth, "m", "测试签名");
        UserDetail updated = userService.findUser(detail.getUserId());
        assertEquals("测试用户", updated.getUsername());
        assertEquals(birth, updated.getBirth());
        assertEquals("m", updated.getGender());
        assertEquals("测试签名", updated.getSign());

        //全量更新：null清空字段，sign列非空故落库为空串
        userService.updateUserInfo(detail.getUserId(), null, null, null, null);
        UserDetail cleared = userService.findUser(detail.getUserId());
        assertNull(cleared.getUsername());
        assertNull(cleared.getBirth());
        assertNull(cleared.getGender());
        assertEquals("", cleared.getSign());
    }

    //1x1透明PNG
    private static final byte[] PNG_BYTES = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Autowired
    MinIOService minIOService;

    @Autowired
    MinIOConfig minIOConfig;

    @Autowired
    RedissonClient redissonClient;

    //头像用例创建的用户id，用于AfterEach定向清理限频计数键
    private final List<Integer> usedUids = new ArrayList<>();

    /**
     * @return 已插入数据库的随机邮箱测试用户
     */
    private UserDetail newTestUser() {
        UserDetail detail = new UserDetail();
        detail.setEmail(UUID.randomUUID() + "@test.example.com");
        userDetailMapper.insert(detail);
        usedUids.add(detail.getUserId());
        return detail;
    }

    @AfterEach
    void cleanRedis() {
        //DB写入由事务回滚，Redis限频计数键需定向清理
        for (Integer uid : usedUids) {
            redissonClient.getAtomicLong("user_avatar_upload_count:" + uid).delete();
        }
        usedUids.clear();
    }

    @BeforeEach
    void setUpBucket() throws Exception {
        //头像用例上传到配置桶（test profile为heimusic-test），测试环境自建，幂等
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minIOConfig.getEndpoint())
                .credentials(minIOConfig.getAccessKey(), minIOConfig.getSecretKey())
                .build();
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(minIOConfig.getBucket()).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minIOConfig.getBucket()).build());
        }
    }

    @Test
    void updateAvatar() {
        UserDetail detail = newTestUser();

        MultipartFile avatar = new MockMultipartFile("avatar", "a.png", "image/png", PNG_BYTES);
        userService.updateAvatar(detail.getUserId(), avatar);
        UserDetail updated = userService.findUser(detail.getUserId());
        assertEquals(minIOConfig.getBucket(), updated.getAvatarBucket());
        assertTrue(updated.getAvatarObjectKey().startsWith("avatar/" + detail.getUserId() + "-"));
        String firstKey = updated.getAvatarObjectKey();

        //重复上传：objectKey变化（旧对象由服务清理），DB指向新对象
        userService.updateAvatar(detail.getUserId(), avatar);
        UserDetail updatedAgain = userService.findUser(detail.getUserId());
        assertNotEquals(firstKey, updatedAgain.getAvatarObjectKey());
        //清理测试产生的最后一个对象（前一个已被服务清理）
        minIOService.delete(updatedAgain.getAvatarBucket(), updatedAgain.getAvatarObjectKey());
    }

    @Test
    void updateAvatarRejectsNonImage() {
        UserDetail detail = newTestUser();

        MultipartFile notImage = new MockMultipartFile("avatar", "a.txt", "text/plain", "不是图片".getBytes(StandardCharsets.UTF_8));
        ServiceException exception = assertThrows(ServiceException.class, () -> userService.updateAvatar(detail.getUserId(), notImage));
        assertEquals(UserServiceErrorCode.AVATAR_FORMAT_UNSUPPORTED.getCode(), exception.getCode().intValue());
    }

    @Test
    void updateAvatarRateLimited() {
        UserDetail detail = newTestUser();

        //预置满额计数（与实现中MAX_AVATAR_UPLOADS_PER_HOUR对齐），下一次请求应被拒绝
        RAtomicLong uploadCount = redissonClient.getAtomicLong("user_avatar_upload_count:" + detail.getUserId());
        uploadCount.set(5);
        uploadCount.expire(Duration.ofMinutes(1));

        MultipartFile avatar = new MockMultipartFile("avatar", "a.png", "image/png", PNG_BYTES);
        ServiceException exception = assertThrows(ServiceException.class, () -> userService.updateAvatar(detail.getUserId(), avatar));
        assertEquals(UserServiceErrorCode.AVATAR_UPDATE_OVER_LIMIT.getCode(), exception.getCode().intValue());
        //超限请求不落盘不写库
        UserDetail unchanged = userService.findUser(detail.getUserId());
        assertEquals("", unchanged.getAvatarObjectKey());
    }
}
