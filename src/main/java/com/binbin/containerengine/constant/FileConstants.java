package com.binbin.containerengine.constant;

/**
 * @author 7bin
 * @date 2022/11/22
 */
public class FileConstants {

    /**
     * 图片类型
     */
    public final static String IMAGE_TYPE = "webp,bmp,pcx,tif,gif,jpg,jpeg,tga,exif,fpx,svg,psd,cdr,pcd,dxf,ufo,eps,ai,png,hdri,raw,wmf,flic,emf,ico,pdf";
    /**
     * 小文件最大大小: 2MB = 1024 * 1024 * 2
     */
    public final static Long MAX_SIZE = 2097152L;


    /**
     * 文件路径分隔符
     */
    public final static String FILE_PATH_SEPARATOR = "/";

    /**
     * 覆盖
     */
    public final static String COVER = "cover";

    /**
     * 不覆盖
     */
    public final static String UNCOVER = "uncover";


    /**
     * 本地存储
     */
    public final static String LOCAL = "local";

    /**
     * 容器存储
     */
    public final static String CONTAINER = "container";

}
