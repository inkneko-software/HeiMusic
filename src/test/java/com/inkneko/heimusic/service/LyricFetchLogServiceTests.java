package com.inkneko.heimusic.service;

import com.inkneko.heimusic.model.entity.LyricFetchLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LyricFetchLogService 集成测试：日志写入（detail 截断）、分页查询与 musicId/outcome 过滤。
 * record 走 REQUIRES_NEW 独立事务提交，不受测试事务回滚保护，
 * 清理须用独立 auto-commit 连接执行，且先于测试事务内任何读取完成
 */
@SpringBootTest
@Transactional
class LyricFetchLogServiceTests {

    @Autowired
    LyricFetchLogService lyricFetchLogService;

    @Autowired
    DataSource dataSource;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            connection.prepareStatement("DELETE FROM lyric_fetch_log").executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void recordTruncatesOverlongDetail() {
        String overlong = "错".repeat(2000);
        lyricFetchLogService.record(LyricFetchLog.SOURCE_MQ, 1, LyricFetchLog.OUTCOME_FAILED, overlong);

        LyricFetchLog log = lyricFetchLogService.getLogs(1, 10, 1, LyricFetchLog.OUTCOME_FAILED).getRecords().get(0);
        assertEquals(512, log.getDetail().length());
        assertEquals(LyricFetchLog.SOURCE_MQ, log.getSource());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void getLogsPaginatesAndFilters() {
        lyricFetchLogService.record(LyricFetchLog.SOURCE_MQ, 101, LyricFetchLog.OUTCOME_CREATED, "d1");
        lyricFetchLogService.record(LyricFetchLog.SOURCE_MQ, 102, LyricFetchLog.OUTCOME_NOT_FOUND, "d2");
        lyricFetchLogService.record(LyricFetchLog.SOURCE_MANUAL, 101, LyricFetchLog.OUTCOME_SKIPPED, "d3");

        //倒序分页：第一页2条为最新的两条（skipped、not_found），总数3
        var page1 = lyricFetchLogService.getLogs(1, 2, null, null);
        assertEquals(3, page1.getTotal());
        assertEquals(2, page1.getRecords().size());
        assertEquals(LyricFetchLog.OUTCOME_SKIPPED, page1.getRecords().get(0).getOutcome());

        //第二页为最旧一条（created）
        var page2 = lyricFetchLogService.getLogs(2, 2, null, null);
        assertEquals(1, page2.getRecords().size());
        assertEquals(LyricFetchLog.OUTCOME_CREATED, page2.getRecords().get(0).getOutcome());

        //musicId 与 outcome 过滤
        assertEquals(2, lyricFetchLogService.getLogs(1, 10, 101, null).getTotal());
        assertEquals(1, lyricFetchLogService.getLogs(1, 10, 101, LyricFetchLog.OUTCOME_SKIPPED).getTotal());
        assertEquals(1, lyricFetchLogService.getLogs(1, 10, null, LyricFetchLog.OUTCOME_NOT_FOUND).getTotal());
        assertEquals(0, lyricFetchLogService.getLogs(1, 10, null, LyricFetchLog.OUTCOME_INSTRUMENTAL).getTotal());
    }
}
