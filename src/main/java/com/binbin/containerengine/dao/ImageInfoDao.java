package com.binbin.containerengine.dao;

import com.binbin.containerengine.entity.po.FileInfo;
import com.binbin.containerengine.entity.po.docker.ImageInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author 7bin
 * @date 2023/12/06
 */
public interface ImageInfoDao extends MongoRepository<ImageInfo, String> {

    ImageInfo findByMd5(String md5);

}
