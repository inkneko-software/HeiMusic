package com.inkneko.heimusic.service.impl;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务提交后回调的包内工具
 * <p>
 * 用于缓存驱逐等"必须在提交后生效"的动作，覆盖两个问题：
 * ① 同类内部调用不走 Spring 代理，目标方法上的 @CacheEvict 根本不生效
 * （实测事故根因：拉取编排内调 addLyric，lyricList 从不驱逐，扫描前缓存的空列表永久驻留）；
 * ② 注解驱逐在经代理的路径上于事务提交前执行，窗口内并发读会把旧值回填缓存。
 * 无活动事务时直接执行（防御性兜底）
 */
final class TransactionAfterCommit {

    private TransactionAfterCommit() {
    }

    /**
     * 注册事务提交后执行的动作；无活动事务时立即执行
     *
     * @param action 提交后执行的动作
     */
    static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
