package com.inkneko.heimusic.util.music;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 测试用媒体文件工具：在测试运行时动态生成真实可被 ffprobe 解析的 WAV 文件，
 * 避免 git 中提交二进制样例，也不依赖 ffmpeg 生成。
 */
public final class TestMediaFiles {

    private TestMediaFiles() {
    }

    /**
     * 生成 8kHz 单声道 16bit PCM 正弦波 WAV。
     */
    public static void writeSineWav(Path path, int seconds) throws IOException {
        int sampleRate = 8000;
        int frames = seconds * sampleRate;
        int dataSize = frames * 2;
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
            out.writeBytes("RIFF");
            writeIntLe(out, 36 + dataSize);
            out.writeBytes("WAVE");
            out.writeBytes("fmt ");
            writeIntLe(out, 16);
            writeShortLe(out, (short) 1);   // PCM
            writeShortLe(out, (short) 1);   // mono
            writeIntLe(out, sampleRate);
            writeIntLe(out, sampleRate * 2);
            writeShortLe(out, (short) 2);   // block align
            writeShortLe(out, (short) 16);  // bits per sample
            out.writeBytes("data");
            writeIntLe(out, dataSize);
            for (int i = 0; i < frames; i++) {
                writeShortLe(out, (short) (Math.sin(2 * Math.PI * 440 * i / sampleRate) * 8000));
            }
        }
    }

    /**
     * 当前环境是否能调用 ffprobe（MusicProber 依赖它）。不可用时相关测试按 Assumptions 跳过。
     */
    public static boolean ffprobeAvailable() {
        try {
            Process process = new ProcessBuilder("ffprobe", "-version").start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void writeIntLe(DataOutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private static void writeShortLe(DataOutputStream out, short value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }
}
