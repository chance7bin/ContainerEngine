package com.binbin.containerengine.service;

import com.binbin.containerengine.constant.TaskStatusConstants;
import com.binbin.containerengine.entity.bo.ExecResponse;
import com.binbin.containerengine.entity.bo.TerminalRsp;
import com.binbin.containerengine.entity.po.ExecInfo;
import com.binbin.containerengine.entity.po.docker.ContainerInfo;
import com.binbin.containerengine.entity.po.docker.ImageInfo;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.model.Image;

import java.io.IOException;
import java.util.List;

/**
 * @author 7bin
 * @date 2023/11/30
 */
public interface IDockerService {

    void listContainer();

    ContainerInfo createContainer(ContainerInfo containerInfo);

    int updateContainer(ContainerInfo containerInfo);

    String getContainerStatusByContainerInsId(String containerInsId);


    ImageInfo inspectImage(String sha256);

    //获取docker镜像列表
    List<Image> listImages();

    //获取docker容器列表
    List<ContainerInfo> listContainers();

    ContainerInfo selectContainerByInsId(String insId);


    /**
     * 执行脚本 默认异步调用
     * @param insId 容器实例id
     * @param script 脚本
     * @return 脚本执行的任务id
     */
    String exec(String insId, String script);

    /**
     * 执行脚本
     * @param insId 容器实例id
     * @param script 脚本
     * @param mode 调用方式 异步/同步
     * @return 脚本执行的任务id
     */
    String exec(String insId, String script, String mode);

    void execAsync(String execId);


    /**
     * 导出镜像
     *
     * @param containerInsId docker中的容器实例id
     * @param imageName 镜像名
     * @param tag 镜像版本
     * @return {@link String} 镜像的sha256
     * @author 7bin
     **/
    String commitContainer(String containerInsId, String imageName, String tag);

    void saveContainer();

    TerminalRsp exportContainer(String container, String outputPath);

    TerminalRsp importContainer(String inputPath, String imageName);

    void startContainer(String containerInsId);

    void stopContainer(String containerInsId);

    void removeContainer(String containerInsId);

    boolean isContainerRunning(String containerInsId);

    void pushImage(String imageName, String tag) throws InterruptedException;

    void pullImage(String imageName, String tag) throws InterruptedException;

    ExecResponse execWithTerminal(String[] cmdArr) throws IOException, InterruptedException;

    /**
     * 查看脚本执行状态
     * @param execId 脚本执行的任务id
     * @return 脚本执行状，状态的情况在{@link TaskStatusConstants}类中
     */
    String getExecStatus(String execId);

    /**
     * 查看脚本执行信息
     * @param execId 脚本执行的任务id
     * @return 脚本执行信息
     */
    ExecInfo getExecInfoByExecId(String execId);

    /**
     * 查看docker内脚本执行信息
     * @param execId 脚本执行的任务id
     * @return 脚本执行信息
     */
    InspectExecResponse inspectExecCmd(String execId);

    boolean getExecIfDone(String execId);

    // 测试入口
    void test();


    // 在容器内部创建文件夹
    void createFolderInContainer(String containerId, String folderPath);


    /**
     * 将镜像导入到docker中
     *
     * @param path 容器压缩包路径
     * @return {@link String} 导入的镜像名 [name:tag]
     * @author 7bin
     **/
    String loadImage(String path);

    void ping();

}
