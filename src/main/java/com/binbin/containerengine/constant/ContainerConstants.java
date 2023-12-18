package com.binbin.containerengine.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 容器常量
 *
 * @author 7bin
 * @date 2023/12/13
 */
public class ContainerConstants {

    /** 容器默认启动命令 */
    public static final List<String> RUN_DEFAULT_CMD = new ArrayList<>(Arrays.asList("tail", "-f", "/dev/null"));


}
