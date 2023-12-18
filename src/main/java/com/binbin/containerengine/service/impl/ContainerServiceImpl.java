package com.binbin.containerengine.service.impl;

import com.binbin.containerengine.constant.ContainerConstants;
import com.binbin.containerengine.dao.ContainerInfoDao;
import com.binbin.containerengine.dao.ImageInfoDao;
import com.binbin.containerengine.entity.po.docker.ContainerInfo;
import com.binbin.containerengine.entity.po.docker.ImageInfo;
import com.binbin.containerengine.exception.ServiceException;
import com.binbin.containerengine.service.IContainerService;
import com.binbin.containerengine.service.IDockerService;
import com.binbin.containerengine.utils.uuid.IdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 7bin
 * @date 2023/12/13
 */
@Service
@Slf4j
public class ContainerServiceImpl implements IContainerService {

    @Autowired
    IDockerService dockerService;

    @Autowired
    ImageInfoDao imageInfoDao;

    @Autowired
    ContainerInfoDao containerInfoDao;

    @Override
    public String startContainer(String imageId) {

        ImageInfo imageInfo = imageInfoDao.findById(imageId).orElseThrow(() -> new ServiceException("image not found"));

        ContainerInfo containerInfo = new ContainerInfo();
        containerInfo.setContainerName(IdUtils.fastUUID());
        containerInfo.setImageName(imageInfo.getImageName());
        containerInfo.setImageId(imageInfo.getId());
        containerInfo.setCmd(ContainerConstants.RUN_DEFAULT_CMD);
        try {

            dockerService.createContainer(containerInfo);
            String status = dockerService.getContainerStatusByContainerInsId(containerInfo.getContainerInsId());
            containerInfo.setStatus(status);
            containerInfoDao.insert(containerInfo);
            return containerInfo.getContainerInsId();

        } catch (Exception e) {

            // 如果容器创建成功，但是因为其他原因导致try中代码抛异常，删除容器
            if (containerInfo.getContainerInsId() != null) {
                dockerService.removeContainer(containerInfo.getContainerInsId());
            }

            throw new ServiceException(e.getMessage());

        }

    }
}
