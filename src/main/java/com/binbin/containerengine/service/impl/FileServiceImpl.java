package com.binbin.containerengine.service.impl;

import com.binbin.containerengine.constant.FileConstants;
import com.binbin.containerengine.entity.dto.file.FileDTO;
import com.binbin.containerengine.entity.po.FileInfo;
import com.binbin.containerengine.exception.ServiceException;
import com.binbin.containerengine.service.IDockerService;
import com.binbin.containerengine.service.IFileService;
import com.binbin.containerengine.utils.DateUtils;
import com.binbin.containerengine.utils.DockerUtils;
import com.binbin.containerengine.utils.StringUtils;
import com.binbin.containerengine.utils.file.FileUtils;
import com.binbin.containerengine.utils.uuid.UUID;
import com.github.dockerjava.api.DockerClient;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.constant.FieldConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author 7bin
 * @date 2023/12/01
 */
@Service
@Slf4j
public class FileServiceImpl implements IFileService {

    @Value("${file.save-path}")
    private String savePath;

    @Autowired
    DockerClient dockerClient;

    @Autowired
    IDockerService dockerService;

    @Override
    public Long uploadFiles(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        // 文件名非空校验
        if (StringUtils.isEmpty(fileName)) {
            throw new ServiceException("文件名不能为空");
        }
        // 大文件判定
        if (file.getSize() > FileConstants.MAX_SIZE) {
            // throw new ServiceException("文件过大，请使用大文件传输");
        }
        // 生成新文件名
        String newName = UUID.fastUUID() + "_" + fileName;

        int year = DateUtils.getYear();
        int month = DateUtils.getMonth();
        String separator = FileConstants.FILE_PATH_SEPARATOR;
        // 重命名文件
        String path = savePath + separator + year + separator + month + separator + newName;

        FileDTO fileDTO = new FileDTO();
        fileDTO.setPath(path);
        fileDTO.setFile(file);

        return uploadFiles(fileDTO);
    }

    @Override
    public Long uploadFiles(FileDTO fileDTO){

        // 路径以年月分隔
        int year = DateUtils.getYear();
        int month = DateUtils.getMonth();
        String separator = FileConstants.FILE_PATH_SEPARATOR;
        // String path = savePath + separator + year + separator + month + separator + fileDTO.getPath();
        String path = savePath + fileDTO.getPath();
        // 如果有containerId，把文件上传到containerId文件夹下
        if (StringUtils.isNotEmpty(fileDTO.getContainerId())){
            path = savePath + separator + fileDTO.getContainerId() + separator + fileDTO.getPath();
        }
        File newFile = new File(path);

        // 判断文件是否允许覆盖
        if (newFile.exists() && FileConstants.UNCOVER.equals(fileDTO.getCover())) {
            throw new ServiceException("file already exists");
        }

        // 如果该存储路径不存在则新建存储路径
        if (!newFile.getParentFile().exists()) {
            newFile.getParentFile().mkdirs();
        }
        // 文件写入
        try {
            fileDTO.getFile().transferTo(newFile);
        } catch (IOException e) {
            // log.error("文件写入异常");
            throw new ServiceException("文件写入异常");
        }

        // 保存文件信息
        // FileInfo fileInfo = new FileInfo();
        // ...

        // 拷贝文件到容器里
        if (FileConstants.CONTAINER.equals(fileDTO.getLocation())){
            // 先在容器里把父文件夹创建出来
            String containerPath = FileUtils.getDirPath(fileDTO.getPath());
            dockerService.createFolderInContainer(fileDTO.getContainerId(), containerPath);
            copyFileToContainer(fileDTO.getContainerId(), path, containerPath);
        }


        return 1L;
    }

    @Override
    public void copyFileToContainer(String containerId, String hostPath, String remotePath) {
        DockerUtils.copyArchiveToContainer(dockerClient, containerId, hostPath, remotePath);
    }


    @Override
    public void copyFileFromContainer(String containerId, String remotePath, String hostPath) {
        DockerUtils.copyArchiveFromContainer(dockerClient, containerId, remotePath, hostPath);
    }


    @Override
    public InputStream getFileInputStream(Long id) {
        // FileInfo fileInfo = fileMapper.selectById(id);
        FileInfo fileInfo = new FileInfo();
        String path = savePath + FileConstants.FILE_PATH_SEPARATOR + fileInfo.getFilePath();
        return getFileInputStream(path);

    }
    @Override
    public InputStream getFileInputStream(String path) {
        try {
            File file = new File(path);
            return new FileInputStream(file);
        } catch (Exception e) {
            log.error("获取文件输入流出错", e);
        }
        return null;
    }


}
