package com.binbin.containerengine.entity;

import com.binbin.containerengine.utils.uuid.SnowFlake;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity基类
 *
 * @author bin
 * @date 2022/08/22
 */
@Data
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // @org.springframework.data.annotation.Id
    // @Field(targetType = FieldType.STRING)
    @Id
    private String id = String.valueOf(SnowFlake.nextId());

    /** 搜索值 */
    private String searchValue;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime = new Date();

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /** 备注 */
    private String remark;

    /** 请求参数 */
    private Map<String, Object> params;

}
