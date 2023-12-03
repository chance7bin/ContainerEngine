package com.binbin.containerengine.controller;

import com.binbin.containerengine.controller.common.BaseController;
import com.binbin.containerengine.entity.dto.ApiResponse;
import com.binbin.containerengine.service.IDockerService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 7bin
 * @date 2023/11/30
 */

@RestController
@RequestMapping("/instance")
public class InstanceController extends BaseController {

    @Autowired
    IDockerService dockerService;

    // 测试接口
    @ApiOperation(value = "测试接口")
    @GetMapping("/test")
    public ApiResponse test() {
        return ApiResponse.success("test");
    }

    @ApiOperation(value = "容器列表")
    @GetMapping("/list")
    public ApiResponse list() {
        dockerService.listContainer();
        return ApiResponse.success();
    }


}
