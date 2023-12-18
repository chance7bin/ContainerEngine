package com.binbin.containerengine.service;

/**
 * @author 7bin
 * @date 2023/12/13
 */
public interface IContainerService {


    /**
     * 启动容器
     *
     * @param imageId 镜像id
     * @return {@link String} 容器id
     */
    String startContainer(String imageId);
}
