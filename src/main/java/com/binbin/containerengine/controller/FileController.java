package com.binbin.containerengine.controller;

import com.alibaba.fastjson2.JSONObject;
import com.binbin.containerengine.constant.FileConstants;
import com.binbin.containerengine.entity.dto.ApiResponse;
import com.binbin.containerengine.entity.dto.file.FileDTO;
import com.binbin.containerengine.entity.po.FileInfo;
import com.binbin.containerengine.exception.ServiceException;
import com.binbin.containerengine.service.IFileService;
import com.binbin.containerengine.utils.EncodingUtils;
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
     * @param cover 是否覆盖
     * @param path 上传路径 （项目的相对路径）
     * @return {@link ApiResponse}
     */
    @ApiOperation(value = "上传文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse uploadFiles(
        @RequestParam("file") MultipartFile file,
        @RequestParam("cover") String cover,
        @RequestParam("path") String path) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error("文件不能为空");
        }
        FileDTO fileDTO = new FileDTO();
        fileDTO.setFile(file);
        fileDTO.setPath(path);
        return ApiResponse.success("upload success", fileService.uploadFiles(fileDTO));
    }


    /**
     * 文件下载
     *
     * @param path 远程文件相对路径
     * @param downloadFilename 是否使用传入的文件名
     * @param request  请求
     * @param response 响应
     */
    @ApiOperation(value = "下载文件")
    @GetMapping(value = "/download")
    public void download(@RequestParam String path, @RequestParam(required = false) String downloadFilename, HttpServletRequest request, HttpServletResponse response) {
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            // FileInfo fileDetails = fileService.getFileDetails(id);
            path = savePath + path;
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
