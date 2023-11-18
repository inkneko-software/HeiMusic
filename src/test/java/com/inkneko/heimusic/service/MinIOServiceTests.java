package com.inkneko.heimusic.service;

import com.inkneko.heimusic.config.MinIOConfig;
import com.inkneko.heimusic.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
public class MinIOServiceTests {
    @Autowired
    MinIOService minIOService;

    @Test
    void uploadObject(){

        minIOService.upload("testbucket-1253989510", "testobject.bin", new File("C:\\Users\\41740\\Desktop\\testobject.bin"));
    }

    @Test
    void deleteObject(){
        minIOService.delete("testbucket-1253989510", "testobject.bin");
    }

    @Test
    void download(){
        File file = minIOService.download("heimusic", "music/103_source");
        System.out.println(file.getAbsolutePath());
    }
}
