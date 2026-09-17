package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.model.entity.UserDetail;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBasicVo {
    private Integer userId;
    private String username;
    private String avatarUrl;

    public UserBasicVo(UserDetail userDetail, MinIOConfig minIOConfig) {
        this.userId = userDetail.getUserId();
        this.username = userDetail.getUsername();
        this.avatarUrl = assembleAvatarUrl(userDetail, minIOConfig);
    }

    /**
     * 拼装头像url：未设置头像时返回空串，由客户端使用默认头像
     */
    static String assembleAvatarUrl(UserDetail userDetail, MinIOConfig minIOConfig) {
        if (userDetail.getAvatarObjectKey() == null || userDetail.getAvatarObjectKey().isEmpty()) {
            return "";
        }
        //若不使用CDN，则url为 endpoint + bucket + objectKey
        if (minIOConfig.getCdn().isEmpty()) {
            return String.format("%s/%s/%s", minIOConfig.getEndpoint(), userDetail.getAvatarBucket(), userDetail.getAvatarObjectKey());
        }
        //若使用CDN，则默认该CDN指向相应的桶
        return String.format("%s/%s", minIOConfig.getCdn(), userDetail.getAvatarObjectKey());
    }
}
