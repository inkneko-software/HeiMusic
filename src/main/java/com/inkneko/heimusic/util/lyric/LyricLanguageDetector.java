package com.inkneko.heimusic.util.lyric;

import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 歌词语言判定器（基于 Tika 封装的 Optimaize 语言检测）
 * <p>
 * 判定结果仅作初始值，混唱歌词的"正确语言"本无唯一答案，人工修正走删旧增新。
 * 实测结论（2026-09-21，见设计文档第 7 节）：整体检测对混唱必误判英文，故逐行投票；
 * 超短纯拉丁行（如 "la la la"）误判率高，按最小行长过滤
 */
@Component
public class LyricLanguageDetector {

    /**
     * 无法判定时的语言标签（BCP 47 undetermined），入库与查询均走 locale 归一化
     */
    public static final String UNDETERMINED = "und";

    /**
     * LRC 时间戳标记，如 [00:17.12]，一行内可重复出现；实测不干扰检测，剥离只为稳妥
     */
    private static final Pattern TIMESTAMP = Pattern.compile("\\[\\d{1,3}:\\d{1,2}(?:[.:]\\d{1,3})]");

    /**
     * 含非 ASCII 字母（中日韩等高信息密度文字）的行最小有效字符数
     */
    private static final int MIN_LINE_CHARS_DENSE = 4;

    /**
     * 纯 ASCII 行（英文/拼音/罗马音）信息密度低，要求更长，低于阈值不参与投票
     */
    private static final int MIN_LINE_CHARS_ASCII = 8;

    private final OptimaizeLangDetector detector = new OptimaizeLangDetector();

    public LyricLanguageDetector() {
        //loadModels 加载内置语言档案，无受检异常；SPI 自动发现在部分类加载环境下不可依赖，直接实例化
        detector.loadModels();
    }

    /**
     * 逐行投票判定歌词语言
     * <p>
     * 剥离时间戳后逐行检测计票，取票数最多者；无有效行、无票或平票时返回 und。
     * 检测器实例有内部状态，synchronized 保证串行使用（消费侧单线程 + 手动单曲，低并发足够）
     *
     * @param content 歌词全文，支持 LRC 时间戳格式
     * @return 归一化小写语言标签（如 ja、zh-cn），无法判定时为 und
     */
    public synchronized String detectLocale(String content) {
        if (content == null || content.isBlank()) {
            return UNDETERMINED;
        }
        Map<String, Integer> votes = new HashMap<>();
        for (String rawLine : content.split("\\r?\\n")) {
            String line = TIMESTAMP.matcher(rawLine).replaceAll(" ").strip();
            if (!isDetectable(line)) {
                continue;
            }
            detector.reset();
            detector.addText(line);
            LanguageResult result = detector.detect();
            if (result.isUnknown()) {
                continue;
            }
            votes.merge(result.getLanguage().toLowerCase(), 1, Integer::sum);
        }
        return decide(votes);
    }

    /**
     * 取票数最多的语言，平票视为无法判定
     */
    private String decide(Map<String, Integer> votes) {
        int max = votes.values().stream().max(Integer::compareTo).orElse(0);
        if (max == 0) {
            return UNDETERMINED;
        }
        String winner = null;
        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            if (entry.getValue() == max) {
                if (winner != null) {
                    return UNDETERMINED;
                }
                winner = entry.getKey();
            }
        }
        return winner;
    }

    /**
     * 行是否具备检测价值：按字母数（含中日韩文字）过滤纯音乐标记、过场符号等噪声行
     */
    private boolean isDetectable(String line) {
        if (line.isEmpty()) {
            return false;
        }
        int letterCount = line.replaceAll("[^\\p{L}]", "").length();
        if (letterCount == 0) {
            return false;
        }
        boolean containsNonAscii = line.chars().anyMatch(c -> c > 0x7F);
        int minLength = containsNonAscii ? MIN_LINE_CHARS_DENSE : MIN_LINE_CHARS_ASCII;
        return letterCount >= minLength;
    }
}
