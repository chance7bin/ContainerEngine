package com.binbin.containerengine.controller;

import com.alibaba.fastjson2.JSONObject;
import com.binbin.containerengine.dao.ImageInfoDao;
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

    @Autowired
    ImageInfoDao imageInfoDao;

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
    @ApiOperation(value = "根据md5判断镜像是否存在")
    @GetMapping(value = "/exist")
    public ApiResponse imageExist(@RequestParam("md5") String md5) {

        boolean exist = imageService.imageExist(md5);

        JSONObject result = new JSONObject();
        result.put("exist", exist);
        if (exist) {
            result.put("imageId", imageInfoDao.findByMd5(md5).getId());
        }

        return ApiResponse.success(result);
    }

}
