package com.binbin.containerengine.entity.po;

import com.binbin.containerengine.constant.TaskStatusConstants;
import com.binbin.containerengine.entity.BaseEntity;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * docker执行脚本信息
 *
 * @author 7bin
 * @date 2023/12/06
 */
@Document
@Data
public class ExecInfo extends BaseEntity {

    private Boolean running = true;

    private Long exitCode;

    private String execId;

    private String containerId;

    private String script;

    private String status = TaskStatusConstants.CREATED;

}
