package com.binbin.containerengine.dao;

import com.binbin.containerengine.entity.po.FileInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author 7bin
 * @date 2023/12/06
 */
public interface FileInfoDao extends MongoRepository<FileInfo, String> {

    FileInfo findFirstByMd5(String md5);

}
