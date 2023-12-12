package com.binbin.containerengine.manager;


import com.binbin.containerengine.utils.Threads;
import com.binbin.containerengine.utils.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 线程池管理器
 *
 * @author 7bin
 */
@Slf4j
public class ThreadPoolManager {
    /**
     * 操作延迟10毫秒
     */
    private static final int OPERATE_DELAY_TIME = 1000;


    /**
     * 定时任务集合
     */
    private static final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();


    /**
     * 异步/定时操作任务调度线程池
     */
    private ScheduledExecutorService scheduledExecutor = SpringUtils.getBean("scheduledExecutorService");
    private ThreadPoolTaskExecutor asyncExecutor = SpringUtils.getBean("threadPoolTaskExecutor");

    /**
     * 单例模式
     */
    private ThreadPoolManager() {
    }

    private static ThreadPoolManager instance = new ThreadPoolManager();

    public static ThreadPoolManager instance() {
        return instance;
    }

    /**
     * 执行异步任务
     *
     * @param task 任务
     */
    public void execute(TimerTask task) {
        asyncExecutor.execute(task);
    }

    /**
     * 执行定时任务
     *
     * @param task 任务
     */
    public void schedule(TimerTask task) {
        scheduledExecutor.schedule(task, OPERATE_DELAY_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * 执行周期性或定时任务
     */
    public ScheduledFuture scheduleWithFixedDelay(TimerTask task, long period) {
        ScheduledFuture<?> scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(task, OPERATE_DELAY_TIME, period, TimeUnit.MILLISECONDS);
        return scheduledFuture;
    }


    /**
     * 停止scheduled任务线程池
     */
    public void shutdown() {
        Threads.shutdownAndAwaitTermination(scheduledExecutor);
    }


    /**
     * 记录定时任务
     */
    public void recordTask(String taskId, ScheduledFuture<?> scheduledFuture) {
        tasks.put(taskId, scheduledFuture);
        log.info("record schedule task: {}", taskId.substring(0, 5) + "..." + taskId.substring(taskId.length() - 5));

    }

    /**
     * 取消定时任务
     */
    public void cancelTask(String taskId) {
        ScheduledFuture<?> scheduledFuture = tasks.get(taskId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            tasks.remove(taskId);
            log.info("cancel schedule task: {}", taskId.substring(0, 5) + "..." + taskId.substring(taskId.length() - 5));
        }
    }


}
