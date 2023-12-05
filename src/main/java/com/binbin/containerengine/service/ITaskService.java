package com.binbin.containerengine.service;

/**
 * 异步任务 / 定时任务接口
 *
 * @author 7bin
 * @date 2023/12/04
 */
public interface ITaskService {

    /**
     * 执行任务
     * @param execId 容器exec任务id
     */
    void taskExecAsync(String execId);

}
