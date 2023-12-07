package com.binbin.containerengine.config;

import com.binbin.containerengine.utils.Threads;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Description 线程池配置
 * @Author bin
 * @Date 2021/10/11
 */
@Configuration
// @EnableScheduling  //同步
// @EnableAsync  //异步
public class ThreadPoolConfig {

    // 核心线程池大小
    private static final int CORE_POOL_SIZE = 50;

    // 最大可创建的线程数
    private static final int MAX_POOL_SIZE = 200;

    // 队列最大长度
    private static final int QUEUE_CAPACITY = 1000;

    // 线程池维护线程所允许的空闲时间
    private static final int KEEP_ALIVE_SECONDS = 300;

    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数 核心线程数线程数定义了最小可以同时运行的线程数量
        executor.setCorePoolSize(CORE_POOL_SIZE);
        // 设置最大线程数 当队列中存放的任务达到队列容量的时候，当前可以同时运行的线程数量变为最大线程数
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        // 设置队列容量 当新任务来的时候会先判断当前运行的线程数量是否达到核心线程数，如果达到的话，新任务就会被存放在队列中
        executor.setQueueCapacity(QUEUE_CAPACITY);
        // 设置线程活跃时间（秒） 针对救急线程
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        // 线程池对拒绝任务(无线程可用)的处理策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 设置默认线程名称
        executor.setThreadNamePrefix("async-pool-");
        // 等待所有任务结束后再关闭线程池
        // executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    /**
     * 执行周期性或定时任务
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService()
    {
        return new ScheduledThreadPoolExecutor(CORE_POOL_SIZE,
            new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build(),
            new ThreadPoolExecutor.CallerRunsPolicy())
        {
            @Override
            protected void afterExecute(Runnable r, Throwable t)
            {
                super.afterExecute(r, t);
                Threads.printException(r, t);
            }
        };
    }

    // @Bean
    // public TaskExecutor taskExecutor() {
    //     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    //     // 设置核心线程数 核心线程数线程数定义了最小可以同时运行的线程数量
    //     executor.setCorePoolSize(5);
    //     // 设置最大线程数 当队列中存放的任务达到队列容量的时候，当前可以同时运行的线程数量变为最大线程数
    //     executor.setMaxPoolSize(8);
    //     // 设置队列容量 当新任务来的时候会先判断当前运行的线程数量是否达到核心线程数，如果达到的话，新任务就会被存放在队列中
    //     executor.setQueueCapacity(200);
    //     // 设置线程活跃时间（秒） 针对救急线程
    //     executor.setKeepAliveSeconds(60);
    //     // 设置默认线程名称
    //     executor.setThreadNamePrefix("async-pool-");
    //     // 设置拒绝策略 当最大池被填满时，此策略为我们提供可伸缩队列
    //     executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    //     // 等待所有任务结束后再关闭线程池
    //     executor.setWaitForTasksToCompleteOnShutdown(true);
    //     return executor;
    // }

}
