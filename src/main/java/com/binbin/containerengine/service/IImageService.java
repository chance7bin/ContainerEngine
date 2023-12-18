package com.binbin.containerengine.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author 7bin
 * @date 2023/12/13
 */
public interface IImageService {

    /**
     * 上传镜像
     *
     * @param file 文件
     * @return {@link String} 上传后返回的image id
     */
    String loadImage(MultipartFile file, String md5);

    /**
     * 根据md5判断镜像是否存在
     *
     * @param md5 md5
     * @return {@link String} 镜像id
     */
    boolean imageExist(String md5);
}
