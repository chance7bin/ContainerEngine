package com.binbin.containerengine.service.impl;

import com.binbin.containerengine.constant.ContainerConstants;
import com.binbin.containerengine.dao.ContainerInfoDao;
import com.binbin.containerengine.dao.ImageInfoDao;
import com.binbin.containerengine.dao.UpdateDao;
import com.binbin.containerengine.entity.dto.StartContainerDTO;
import com.binbin.containerengine.entity.po.docker.ContainerInfo;
import com.binbin.containerengine.entity.po.docker.ImageInfo;
import com.binbin.containerengine.exception.ServiceException;
import com.binbin.containerengine.service.IContainerService;
import com.binbin.containerengine.service.IDockerService;
import com.binbin.containerengine.utils.StringUtils;
import com.binbin.containerengine.utils.uuid.IdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Autowired
    UpdateDao updateDao;

    @Override
    public String startContainer(StartContainerDTO dto) {

        ImageInfo imageInfo = imageInfoDao.findById(dto.getImageId()).orElseThrow(() -> new ServiceException("image not found"));

        ContainerInfo containerInfo = new ContainerInfo();
        containerInfo.setContainerName(IdUtils.fastUUID());
        containerInfo.setImageName(imageInfo.getImageName());
        containerInfo.setImageId(imageInfo.getId());
        // 设置容器启动参数
        if (!StringUtils.isEmpty(dto.getCmd())){
            String[] arr = dto.getCmd().split(" ");
            List<String> arrList = new ArrayList<>(Arrays.asList(arr));
            containerInfo.setCmd(arrList);
        } else {
            containerInfo.setCmd(ContainerConstants.RUN_DEFAULT_CMD);
        }

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

    @Override
    public ContainerInfo findFirstByImageId(String imageId) {
        return containerInfoDao.findFirstByImageId(imageId);
    }

    @Override
    public ContainerInfo findFirstByImageIdAndStatus(String imageId, String status) {
        // 按照createTime排序，取最近创建的
        Sort sort = Sort.by(Sort.Order.desc("createTime"));
        return containerInfoDao.findFirstByImageIdAndStatus(imageId, status, sort);
    }

    @Override
    public void deleteContainer(String insId) {
        dockerService.removeContainer(insId);
        String status = dockerService.getContainerStatusByContainerInsId(insId);
        updateDao.updateDelFlagAndStatusByInsId(insId, true, status);
    }
}
