package com.binbin.containerengine.service.impl;

import com.binbin.containerengine.constant.FileConstants;
import com.binbin.containerengine.dao.FileInfoDao;
import com.binbin.containerengine.dao.ImageInfoDao;
import com.binbin.containerengine.entity.dto.file.FileDTO;
import com.binbin.containerengine.entity.po.FileInfo;
import com.binbin.containerengine.entity.po.docker.ImageInfo;
import com.binbin.containerengine.service.IDockerService;
import com.binbin.containerengine.service.IFileService;
import com.binbin.containerengine.service.IImageService;
import com.github.dockerjava.api.model.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author 7bin
 * @date 2023/12/13
 */
@Slf4j
@Service
public class ImageServiceImpl implements IImageService {

    @Autowired
    FileInfoDao fileInfoDao;

    @Autowired
    ImageInfoDao imageInfoDao;

    @Autowired
    IFileService fileService;

    @Autowired
    IDockerService dockerService;

    @Value("${file.save-path}")
    String savePath;

    @Override
    public String loadImage(MultipartFile file, String md5) {

        if (imageInfoDao.findByMd5(md5) != null) {
            return imageInfoDao.findByMd5(md5).getId();
        }

        FileDTO fileDTO = new FileDTO();
        fileDTO.setFile(file);
        fileDTO.setPath("/image/" + file.getOriginalFilename());
        fileDTO.setCover(FileConstants.COVER);
        String fileId;
        if (fileInfoDao.findByMd5(md5) == null) {
            fileId = fileService.uploadFiles(fileDTO);
        } else {
            fileId = fileInfoDao.findByMd5(md5).getId();
        }

        fileInfoDao.findById(fileId).ifPresent(fileInfo -> {

            String path = fileInfo.getFilePath();
            String imageName = dockerService.loadImage(savePath + path);

            if (imageName == null) {
                return;
            }

            ImageInfo imageInfo = new ImageInfo();
            imageInfo.setImageName(imageName);
            imageInfo.setMd5(md5);
            // 获取到image的id
            List<Image> images = dockerService.listImages();
            for (Image image : images) {
                if (image.getRepoTags() == null) {
                    continue;
                }
                for (String repoTags : image.getRepoTags()) {
                    if (repoTags.equals(imageName)) {
                        imageInfo.setSha256(image.getId());
                        break;
                    }
                }
            }
            imageInfoDao.insert(imageInfo);
        });

        return imageInfoDao.findByMd5(md5).getId();
    }

    @Override
    public boolean imageExist(String md5) {

        ImageInfo info = imageInfoDao.findByMd5(md5);
        return info != null;

    }
}
