package com.binbin.containerengine.controller.common;

import com.binbin.containerengine.entity.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 7bin
 * @date 2023/11/30
 */
@Slf4j
public class BaseController {

    /**
     * 返回成功
     */
    public ApiResponse success()
    {
        return ApiResponse.success();
    }

    /**
     * 返回失败消息
     */
    public ApiResponse error()
    {
        return ApiResponse.error();
    }

    /**
     * 返回成功消息
     */
    public ApiResponse success(String message)
    {
        return ApiResponse.success(message);
    }

    /**
     * 返回成功消息
     */
    public ApiResponse success(Object data)
    {
        return ApiResponse.success(data);
    }

    /**
     * 返回失败消息
     */
    public ApiResponse error(String message)
    {
        return ApiResponse.error(message);
    }


}
