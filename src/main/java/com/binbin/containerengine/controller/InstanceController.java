package com.binbin.containerengine.controller;

import com.binbin.containerengine.entity.dto.ApiResponse;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 7bin
 * @date 2023/11/30
 */

@RestController()
@RequestMapping("/instance")
public class InstanceController {

    // 测试接口
    @ApiOperation(value = "测试接口")
    @GetMapping("/test")
    public ApiResponse test() {
        return ApiResponse.success("test");
    }



}
