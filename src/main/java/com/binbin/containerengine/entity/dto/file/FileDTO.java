package com.binbin.containerengine.entity.dto.file;

import com.binbin.containerengine.constant.FileConstants;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 7bin
 * @date 2023/12/01
 */
@Data
public class FileDTO {

    /**
     * 必选，文件绝对路径
     */
    String path;

    /**
     * 非必选；参数值：cover:强制覆盖，uncover:不覆盖，默认：uncover
     */
    String cover = FileConstants.UNCOVER;


    /**
     * 必选，文件对象
     */
    MultipartFile file;


}
