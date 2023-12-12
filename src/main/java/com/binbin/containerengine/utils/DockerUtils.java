package com.binbin.containerengine.utils;

import com.binbin.containerengine.utils.uuid.IdUtils;
import com.github.dockerjava.api.DockerClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Docker相关工具类
 *
 * @author 7bin
 * @date 2023/12/12
 */
public class DockerUtils {

    /**
     * 将容器内的文件拷贝到宿主机
     *
     * @param dockerClient docker客户端
     * @param containerId  容器id
     * @param remotePath   容器内文件路径（全路径）
     * @param hostPath     宿主机文件路径（全路径，父文件夹要先存在）
     */
    public static void copyArchiveFromContainer(
        DockerClient dockerClient, String containerId, String remotePath, String hostPath) {

        InputStream inputStream = dockerClient
            .copyArchiveFromContainerCmd(containerId, remotePath)
            .exec();
        // inputStream写入到path文件中
        TarArchiveInputStream tarStream = new TarArchiveInputStream(inputStream);
        unTar(tarStream, new File(hostPath));

    }


    /**
     * 将宿主机文件拷贝到容器内
     *
     * @param dockerClient docker客户端
     * @param containerId  容器id
     * @param hostPath     宿主机文件路径（全路径）
     * @param remotePath   容器内文件夹路径（不需要带文件名，文件名默认是宿主机文件名）（该路径必须存在）
     */
    public static void copyArchiveToContainer(DockerClient dockerClient, String containerId, String hostPath, String remotePath) {

        dockerClient.copyArchiveToContainerCmd(containerId)
            .withHostResource(hostPath)
            .withRemotePath(remotePath)
            .exec();

    }

    private static void unTar(TarArchiveInputStream tis, File destFile) {
        try {
            TarArchiveEntry tarEntry = null;
            while ((tarEntry = tis.getNextTarEntry()) != null) {
                if (tarEntry.isDirectory()) {
                    if (!destFile.exists()) {
                        destFile.mkdirs();
                    }
                } else {
                    FileOutputStream fos = new FileOutputStream(destFile);
                    IOUtils.copy(tis, fos);
                    fos.close();
                }
            }
            tis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
