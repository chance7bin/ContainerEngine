package com.binbin.containerengine.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * 确保应用退出时能关闭后台线程
 *
 * @author 7bin
 */
@Slf4j
@Component
public class ShutdownManager
{
    // private static final Logger logger = LoggerFactory.getLogger("sys-user");

    @PreDestroy
    public void destroy()
    {
        shutdownAsyncManager();
    }

    /**
     * 停止异步执行任务
     */
    private void shutdownAsyncManager()
    {
        try
        {
            log.info("====关闭后台任务任务线程池====");
            ThreadPoolManager.instance().shutdown();
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }
}
