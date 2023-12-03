package com.binbin.containerengine.entity.po;

import lombok.Data;

/**
 * @author 7bin
 * @date 2023/12/01
 */
@Data
public class FileInfo {

    /** 文件ID */
    private Long fileId;

    /** 文件名 */
    private String fileName;

    /** 文件路径 真实的文件路径的文件名为 [{md5}_{fileName}] */
    private String filePath;

    /** 文件后缀 */
    private String suffix;

    /** md5 */
    private String md5;

    /** 文件大小 (单位: 字节) */
    private String size;

    /** 删除标志 0false  1true */
    private Boolean delFlag;

}
