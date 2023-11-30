package com.binbin.containerengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 7bin
 * @date 2023/11/30
 */
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "app")
@Data
public class AppPropsConfig {

    /** 项目名称 */
    private String name;

    /** 版本 */
    private String version;

}
