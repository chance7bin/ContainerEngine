package com.binbin.containerengine.controller;

import com.alibaba.fastjson2.JSONObject;
import com.binbin.containerengine.entity.dto.ApiResponse;
import com.binbin.containerengine.service.IImageService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 镜像接口
 *
 * @author 7bin
 * @date 2023/12/13
 */
@RestController
@RequestMapping("/image")
public class ImageController {

    @Autowired
    IImageService imageService;


    @ApiOperation(value = "导入镜像")
    @PostMapping(value = "/load", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse loadImage(
        @RequestParam("file") MultipartFile file,
        @RequestParam("md5") String md5
        ) {

        String imageId = imageService.loadImage(file, md5);

        return ApiResponse.success(imageId);
    }


    // 根据md5判断镜像是否存在
    @ApiOperation(value = "根据md5或者imageName判断镜像是否存在")
    @GetMapping(value = "/exist")
    public ApiResponse imageExist(
        @RequestParam(name = "md5", required = false) String md5,
        @RequestParam(name = "imageName", required = false) String imageName) {

        String imageId;

        if (imageName != null) {
            imageId = imageService.imageExistByName(imageName);
        } else if (md5 != null){
            imageId = imageService.imageExistByMd5(md5);
        } else {
            return ApiResponse.error("md5和imageName不能同时为空");
        }

        JSONObject result = new JSONObject();
        result.put("exist", imageId != null);
        result.put("imageId", imageId);

        return ApiResponse.success(result);
    }

}
