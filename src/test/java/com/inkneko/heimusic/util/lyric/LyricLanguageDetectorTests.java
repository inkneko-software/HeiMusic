package com.inkneko.heimusic.util.lyric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * LyricLanguageDetector 纯单元测试：无基础设施依赖。
 * 期望值均来自 2026-09-21 的 Optimaize 实测（见设计文档第 7 节），检测过程确定无随机性。
 */
class LyricLanguageDetectorTests {

    LyricLanguageDetector detector = new LyricLanguageDetector();

    @Test
    void detectSimplifiedChinese() {
        String lyric = "夜空中最亮的星，能否听清，那仰望的人，心底的孤独和叹息。oh 夜空中最亮的星，能否记起，曾与我同行，消失在风里的身影。";
        assertEquals("zh-cn", detector.detectLocale(lyric));
    }

    @Test
    void detectTraditionalChinese() {
        String lyric = "還記得多年前你我曾一起走過那條老街，歲月無聲地流過，把青春都寫成了信箋。燈火闌珊處，誰在等待誰的歸來。";
        assertEquals("zh-tw", detector.detectLocale(lyric));
    }

    @Test
    void detectEnglish() {
        String lyric = "I feel your breath upon my neck, a soft caress as cold as death. I didn't know you well back then, I blame it all on luck and vain. The clock won't stop and this is what we get, the aftermath of all the things we did.";
        assertEquals("en", detector.detectLocale(lyric));
    }

    @Test
    void detectJapanese() {
        //日文因汉字干扰置信度为 MEDIUM（第二名 zh-CN），单行整体检测仍判 ja
        String lyric = "沈むように溶けてゆくように、二人だけの空が広がる夜に。さよならとあなたに手を振る、哀しみの色に変わりゆく。";
        assertEquals("ja", detector.detectLocale(lyric));
    }

    @Test
    void mixedChineseEnglishVoteForChinese() {
        //中英混唱整体检测必误判英文，逐行投票修正（实测 zh-CN 6 票 : en 1 票）
        String lyric = String.join("\n",
                "这是个问题 怎么去面对",
                "I want to fly",
                "想要飞得更高",
                "梦想的翅膀",
                "take me higher",
                "不会再后退",
                "夜空中最亮的星",
                "能否听清 那仰望的人");
        assertEquals("zh-cn", detector.detectLocale(lyric));
    }

    @Test
    void lrcTimestampsStripped() {
        //时间戳不干扰检测，剥离后逐行判定
        String lyric = String.join("\n",
                "[00:17.12] I feel your breath upon my neck",
                "[00:21.40] A soft caress as cold as death",
                "[00:25.88] I didn't know you well back then",
                "[00:30.15] I blame it all on luck and vain");
        assertEquals("en", detector.detectLocale(lyric));
    }

    @Test
    void shortGarbageLinesUndetermined() {
        //纯音乐标记/过场符号/超短拉丁行（如 "la la la"）不参与投票，无有效票返回 und
        String lyric = String.join("\n",
                "[00:00.00]",
                "[00:05.00] (music)",
                "[00:10.00] ...",
                "la la la");
        assertEquals(LyricLanguageDetector.UNDETERMINED, detector.detectLocale(lyric));
    }

    @Test
    void blankContentUndetermined() {
        assertEquals(LyricLanguageDetector.UNDETERMINED, detector.detectLocale(null));
        assertEquals(LyricLanguageDetector.UNDETERMINED, detector.detectLocale(""));
        assertEquals(LyricLanguageDetector.UNDETERMINED, detector.detectLocale("   \n  "));
    }
}
