package com.binbin.containerengine.dao;

import com.binbin.containerengine.entity.po.ExecInfo;
import com.binbin.containerengine.entity.po.FileInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * @author 7bin
 * @date 2023/12/06
 */
public interface FileInfoDao extends MongoRepository<FileInfo, String> {

    FileInfo findByMd5(String md5);

}
