package com.binbin.containerengine.controller;

import com.binbin.containerengine.entity.bo.Server;
import com.binbin.containerengine.entity.dto.ApiResponse;
import com.binbin.containerengine.service.IServerService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务器监控
 *
 * @author 7bin
 * @date 2024/02/26
 */
@RestController
@RequestMapping("/monitor/server")
public class ServerController {

    @Autowired
    IServerService serverService;

    @ApiOperation("获取服务器信息")
    @GetMapping("/info")
    public ApiResponse getInfo() throws Exception {
        Server server = new Server();
        server.copyTo();

        return ApiResponse.success(server);
    }

    @ApiOperation("检查是否可以使用Docker")
    @GetMapping("/docker/check")
    public ApiResponse checkDocker() {
        serverService.checkDocker();
        return ApiResponse.success();
    }

}
