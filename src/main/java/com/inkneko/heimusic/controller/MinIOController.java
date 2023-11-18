package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.annotation.auth.UserAuth;
import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.service.MinIOService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/minio")
public class MinIOController {
    MinIOService minIOService;

    public MinIOController(MinIOService minIOService) {
        this.minIOService = minIOService;
    }

    @PostMapping("/getUploadLink")
    @UserAuth
    @Operation(summary = "获取上传授权链接")
    public Response<String> generateUploadUrl(HttpServletRequest req) {
        Integer uid = (Integer) req.getAttribute("userId");
        Date now = new Date();
        return new Response<>(0, "ok", minIOService.getUploadUrl("heimusic", String.format("upload/%d-%d-%s", uid, now.getTime(), UUID.randomUUID().toString())));
    }
}
