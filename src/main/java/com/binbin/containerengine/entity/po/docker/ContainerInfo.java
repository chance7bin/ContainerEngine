package com.binbin.containerengine.entity.po.docker;

import com.binbin.containerengine.entity.BaseEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * docker容器实例信息
 *
 * @author bin
 * @date 2022/10/5
 */
@Data
public class ContainerInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 容器id */
    // Long containerId;

    /** 对应docker中的容器实例id */
    String containerInsId;

    /** 容器名*/
    String containerName;

    /** 镜像名 pull imageName */
    String imageName;

    /** 镜像id */
    String imageId;

    /** 容器对外暴露端口 */
    Integer containerExportPort;

    /** 容器绑定端口 */
    Integer hostBindPort;

    /** 主机ip */
    String hostIP;

    /** 主机MAC地址（ip可能会变，mac不会变，所以识别一个主机用mac） */
    String hostMAC;

    /** 挂载数据卷列表 宿主机目录中表示的只是相对路径 实际路径需加上${repository} */
    List<String> volumeList;

    /** 启动命令 */
    List<String> cmd;

    /** 容器状态 */
    String status;

    /** 容器是否被删除 */
    Boolean delFlag;

    /** 容器大类 */
    // ContainerType type;



}
