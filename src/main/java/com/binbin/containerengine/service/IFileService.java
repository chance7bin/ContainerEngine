package com.binbin.containerengine.service;

import com.binbin.containerengine.entity.dto.file.FileDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * @author 7bin
 * @date 2023/12/01
 */
public interface IFileService {

    /**
     * 小文件上传
     *
     * @param file 文件
     * @return {@link String} 上传后返回的fileId
     */
    String uploadFiles(MultipartFile file);

    /**
     * 小文件上传
     *
     * @param fileDTO 文件传输对象
     * @return {@link String} 上传后返回的fileId
     */
    String uploadFiles(FileDTO fileDTO);

    /**
     * 获取文件输入流
     *
     * @param id id
     * @return {@link InputStream}
     */
    InputStream getFileInputStream(Long id);

    /**
     * 获取文件输入流
     *
     * @param path 文件绝对路径
     * @return {@link InputStream}
     */
    InputStream getFileInputStream(String path);


    /**
     * 将文件拷贝到容器中
     *
     * @param containerId  容器id
     * @param hostPath     宿主机文件路径（全路径）
     * @param remotePath   容器内文件夹路径（不需要带文件名，文件名默认是宿主机文件名）（该路径必须存在）
     */
    void copyFileToContainer(String containerId, String hostPath, String remotePath);

    /**
     * 将容器内的文件拷贝到宿主机
     *
     * @param containerId  容器id
     * @param remotePath   容器内文件路径（全路径）
     * @param hostPath     宿主机文件路径（全路径，父文件夹要先存在）
     */
    void copyFileFromContainer(String containerId, String remotePath, String hostPath);

}
