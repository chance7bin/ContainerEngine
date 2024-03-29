package com.binbin.containerengine.controller;

import com.binbin.containerengine.constant.ContainerStatus;
import com.binbin.containerengine.controller.common.BaseController;
import com.binbin.containerengine.entity.dto.ApiResponse;
import com.binbin.containerengine.entity.dto.StartContainerDTO;
import com.binbin.containerengine.entity.dto.docker.ExecDTO;
import com.binbin.containerengine.entity.po.ExecInfo;
import com.binbin.containerengine.entity.po.docker.ContainerInfo;
import com.binbin.containerengine.service.IContainerService;
import com.binbin.containerengine.service.IDockerService;
import com.github.dockerjava.api.command.InspectExecResponse;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author 7bin
 * @date 2023/11/30
 */

@RestController
@RequestMapping("/instance")
public class InstanceController extends BaseController {

    @Autowired
    IDockerService dockerService;

    @Autowired
    IContainerService containerService;


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

    @ApiOperation(value = "执行脚本")
    @PostMapping("/task/actions/exec")
    public ApiResponse exec(@RequestBody ExecDTO execDTO) {
        String execId = dockerService.exec(execDTO.getInsId(), execDTO.getScript());
        return ApiResponse.success(execId);
    }

    @ApiOperation(value = "查看脚本执行状态")
    @GetMapping("/task/actions/exec/status")
    public ApiResponse getExecStatus(@RequestParam String execId) {
        String status = dockerService.getExecStatus(execId);
        return ApiResponse.success(status);
    }


    @ApiOperation(value = "查看脚本执行信息")
    @GetMapping("/task/actions/exec/info")
    public ApiResponse getExecInfo(@RequestParam String execId) {
        ExecInfo info = dockerService.getExecInfoByExecId(execId);
        return ApiResponse.success(info);
    }

    @ApiOperation(value = "查看脚本是否执行完成")
    @GetMapping("/task/actions/exec/ifdone")
    public ApiResponse getExecIfDone(@RequestParam String execId) {
        boolean ifdone = dockerService.getExecIfDone(execId);
        return ApiResponse.success(ifdone);
    }


    @ApiOperation(value = "查看脚本执行信息")
    @GetMapping("/task/actions/exec/inspect")
    public ApiResponse inspectExecCmd(@RequestParam String execId) {
        InspectExecResponse response = dockerService.inspectExecCmd(execId);
        return ApiResponse.success(response);
    }

    @ApiOperation(value = "启动容器实例")
    @PostMapping("/task/actions/start")
    public ApiResponse startContainer(@RequestBody StartContainerDTO dto) {

        if (!dto.isNewContainer()){
            ContainerInfo containerInfo = containerService.findFirstByImageIdAndStatus(dto.getImageId(), ContainerStatus.RUNNING);
            if (containerInfo != null) {
                return ApiResponse.success(containerInfo.getContainerInsId());
            }
        }

        String insId = containerService.startContainer(dto);
        return ApiResponse.success(insId);
    }

    @ApiOperation(value = "删除容器实例")
    @PostMapping("/task/actions/delete")
    public ApiResponse deleteContainer(@RequestParam String insId) {
        containerService.deleteContainer(insId);
        return ApiResponse.success();
    }
}
