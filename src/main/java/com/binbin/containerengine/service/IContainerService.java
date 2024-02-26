package com.binbin.containerengine.service;

import com.binbin.containerengine.entity.dto.StartContainerDTO;
import com.binbin.containerengine.entity.po.docker.ContainerInfo;

/**
 * @author 7bin
 * @date 2023/12/13
 */
public interface IContainerService {


    /**
     * 启动容器
     *
     * @param dto 启动参数
     * @return {@link String} 容器id
     */
    String startContainer(StartContainerDTO dto);


    ContainerInfo findFirstByImageId(String imageId);

    ContainerInfo findFirstByImageIdAndStatus(String imageId, String status);

    void deleteContainer(String insId);
}
