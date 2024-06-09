package com.inkneko.heimusic.util.music;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inkneko.heimusic.util.music.model.ProbeResult;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * 音乐信息提取
 */
@Slf4j
public class MusicProber {
    public static ProbeResult probe(File file) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-of", "json",
                "-show_format",
                "-show_streams",
                file.getAbsolutePath()
        );
        log.debug("扫描文件：{}", file.getAbsolutePath());
        ObjectMapper objectMapper = JsonMapper.builder().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true).build();;
        Process process = processBuilder.start();
        StringBuilder stdoutStringBuilder = new StringBuilder();
        StringBuilder stderrStringBuilder = new StringBuilder();
        Thread stdoutReadingThead = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = process.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String tmp;
                try{
                    while ((tmp = bufferedReader.readLine()) != null) {
                        stdoutStringBuilder.append(tmp);
                    }
                    inputStream.close();
                }catch (IOException e){
                    log.error("读取ffprobe输出时发生异常", e);
                }
            }
        });
        Thread stderrReadingThead = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = process.getErrorStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String tmp;
                try{
                    while ((tmp = bufferedReader.readLine()) != null) {
                        stderrStringBuilder.append(tmp);
                    }
                    inputStream.close();
                }catch (IOException e){
                    log.error("读取ffprobe stderr输出时发生异常", e);
                }
            }
        });
        stdoutReadingThead.start();
        stderrReadingThead.start();

        int retCode = process.waitFor();
        stdoutReadingThead.join();
        stderrReadingThead.join();

        if (retCode == 0) {
            return objectMapper.readValue(stdoutStringBuilder.toString(), ProbeResult.class);
        }
        throw new IOException(String.format("ffprobe执行失败，命令：%s，stderr输出：%s", processBuilder, stderrStringBuilder));
    }
}
