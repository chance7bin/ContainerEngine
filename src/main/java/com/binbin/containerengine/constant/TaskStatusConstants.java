package com.binbin.containerengine.constant;

/**
 * @author 7bin
 * @date 2023/12/04
 */
public class TaskStatusConstants {

    /**
     * 任务已经被创建，但是并未启动
     */
    public static final String CREATED = "created";

    /**
     * 任务正在运行
     */
    public static final String RUNNING = "running";

    /**
     * 任务已经完成
     */
    public static final String FINISHED = "finished";

    /**
     * 任务已经失败
     */
    public static final String FAILED = "failed";

    /**
     * 任务已经被删除
     */
    public static final String DELETED = "deleted";

    /**
     * 其他状态
     */
    public static final String OTHER = "other";

}
