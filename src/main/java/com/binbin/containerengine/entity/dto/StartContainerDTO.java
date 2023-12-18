package com.binbin.containerengine.entity.dto;

import lombok.Data;

/**
 * @author 7bin
 * @date 2023/12/13
 */
@Data
public class StartContainerDTO {

    /** 镜像id */
    String imageId;

    /** 是否需要创建新的容器 */
    boolean newContainer;

}
