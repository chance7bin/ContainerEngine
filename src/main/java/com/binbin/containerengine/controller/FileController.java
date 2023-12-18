package com.binbin.containerengine.controller;

import com.alibaba.fastjson2.JSONObject;
import com.binbin.containerengine.constant.FileConstants;
import com.binbin.containerengine.entity.dto.ApiResponse;
import com.binbin.containerengine.entity.dto.file.FileDTO;
import com.binbin.containerengine.entity.po.FileInfo;
import com.binbin.containerengine.exception.ServiceException;
import com.binbin.containerengine.service.IFileService;
import com.binbin.containerengine.utils.EncodingUtils;
import com.binbin.containerengine.utils.StringUtils;
import com.binbin.containerengine.utils.file.FileUtils;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author 7bin
 * @date 2023/12/01
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    IFileService fileService;

    @Value("${file.save-path}")
    String savePath;


    /**
     * 上传文件
     *
     * @param file 文件
     * @param path 上传路径 （项目的相对路径）
     * @param cover 是否覆盖
     * @param location 上传到项目位置还是容器位置
     * @param containerId 容器id
     * @return {@link ApiResponse}
     */
    @ApiOperation(value = "上传文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse uploadFiles(
        @RequestParam("file") MultipartFile file,
        @RequestParam("path") String path,
        @RequestParam(value = "cover", required = false) String cover,
        @RequestParam(value = "location", required = false) String location,
        @RequestParam(value = "containerId", required = false) String containerId
    ) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error("文件不能为空");
        }
        FileDTO fileDTO = new FileDTO();
        fileDTO.setFile(file);
        fileDTO.setPath(path);
        fileDTO.setCover(cover);
        if (cover != null){
            fileDTO.setCover(cover);
        }
        if (location != null){
            fileDTO.setLocation(location);
        }
        if (containerId != null){
            fileDTO.setContainerId(containerId);
        }
        // 如果有containerId，把文件上传到containerId文件夹下
        if (StringUtils.isNotEmpty(fileDTO.getContainerId())){
            String separator = FileConstants.FILE_PATH_SEPARATOR;
            path = separator + fileDTO.getContainerId() + fileDTO.getPath();
        }
        fileDTO.setPath(path);
        fileService.uploadFiles(fileDTO);
        return ApiResponse.success("upload success");
    }


    /**
     * 文件下载
     *
     * @param path 远程文件相对路径
     * @param downloadFilename 是否使用传入的文件名
     * @param location 从项目位置下载还是容器里面下载
     * @param containerId 容器id
     * @param request  请求
     * @param response 响应
     */
    @ApiOperation(value = "下载文件")
    @GetMapping(value = "/download")
    public void download(
        @RequestParam String path,
        @RequestParam(value = "downloadFilename", required = false) String downloadFilename,
        @RequestParam(value = "location", required = false) String location,
        @RequestParam(value = "containerId", required = false) String containerId,
        HttpServletRequest request,
        HttpServletResponse response) {

        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            // FileInfo fileDetails = fileService.getFileDetails(id);


            String tmpPath = savePath + path;

            if (FileConstants.CONTAINER.equals(location)){

                // 把数据从容器里面拷贝出来
                String separator = FileConstants.FILE_PATH_SEPARATOR;
                tmpPath = savePath + separator + containerId + separator + path;
                // 判断tmpPath的父文件夹是否存在，不存在则创建
                FileUtils.createParentDir(tmpPath);
                fileService.copyFileFromContainer(containerId, path, tmpPath);

            }

            path = tmpPath;

            FileInfo fileDetails = new FileInfo();
            if (FileUtils.exist(path)) {
                fileDetails.setFileName(FileUtils.getName(path));
            } else {
                throw new ServiceException("未找到该文件");
            }
            // if (fileDetails == null){
            //     throw new ServiceException("未找到该文件");
            // }
            String filename = (downloadFilename != null && !downloadFilename.isEmpty()) ? downloadFilename : fileDetails.getFileName();

            inputStream = fileService.getFileInputStream(path);
            response.setHeader("Content-Disposition", "attachment;filename=" + EncodingUtils.convertToFileName(request, filename));
            // 获取输出流
            outputStream = response.getOutputStream();
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            log.error("文件下载出错", e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    @ApiOperation(value = "判断文件是否存在")
    @GetMapping(value = "/exist")
    public ApiResponse exist(@RequestParam String path) {
        path = savePath + path;
        // 创建res对象
        JSONObject res = new JSONObject();
        res.put("exist", FileUtils.exist(path));
        return ApiResponse.success(res);
    }
}
