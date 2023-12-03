package com.binbin.containerengine.service.impl;

import com.binbin.containerengine.constant.FileConstants;
import com.binbin.containerengine.entity.dto.file.FileDTO;
import com.binbin.containerengine.entity.po.FileInfo;
import com.binbin.containerengine.exception.ServiceException;
import com.binbin.containerengine.service.IFileService;
import com.binbin.containerengine.utils.DateUtils;
import com.binbin.containerengine.utils.StringUtils;
import com.binbin.containerengine.utils.uuid.UUID;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.constant.FieldConstant;
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
        String path = savePath + FileConstants.FILE_PATH_SEPARATOR + fileDTO.getPath()
            + FileConstants.FILE_PATH_SEPARATOR + fileDTO.getFile().getOriginalFilename();
        File newFile = new File(path);
        // 如果该存储路径不存在则新建存储路径
        if (!newFile.getParentFile().exists()) {
            newFile.getParentFile().mkdirs();
        }
        // 文件写入
        try {
            fileDTO.getFile().transferTo(newFile);
        } catch (IOException e) {
            // e.printStackTrace();
            log.error("文件写入异常");
            throw new ServiceException("文件写入异常");
        }

        // 保存文件信息
        // FileInfo fileInfo = new FileInfo();

        return 1L;
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
