package com.inkneko.heimusic.service.impl;

import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.service.MinIOService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MinIOServiceImplTests {
    @Autowired
    MinIOService minIOService;

    @Autowired
    MinIOConfig minIOConfig;

    @Test
    void testReadConf(){
        System.out.println(minIOConfig.getEndpoint());
    }

    @Test
    void getUploadUrlTestCase(){
        System.out.println(minIOService.getUploadUrl("heimusic", "uid-timestamp-fileid.bin"));
    }

    @Test
    void getDownloadUrlTestCase(){
        System.out.println(minIOService.getDownloadUrl("heimusic", "uid-timestamp-fileid.bin"));
    }
}
