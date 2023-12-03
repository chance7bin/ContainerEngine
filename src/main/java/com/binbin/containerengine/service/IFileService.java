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
     * @return {@link Long} 上传后返回的fileId
     */
    Long uploadFiles(MultipartFile file);

    /**
     * 小文件上传
     *
     * @param fileDTO 文件传输对象
     * @return {@link Long} 上传后返回的fileId
     */
    Long uploadFiles(FileDTO fileDTO);

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

}
