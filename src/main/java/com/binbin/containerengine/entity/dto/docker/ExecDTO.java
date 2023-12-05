package com.binbin.containerengine.entity.dto.docker;

import lombok.Data;

/**
 * @author 7bin
 * @date 2023/12/03
 */
@Data
public class ExecDTO {

    /**
     * 容器id
     */
    String insId;

    /**
     * 脚本内容
     * 多行命令时需在每行末尾加入\n 换行转义符
     */
    String script;

}
