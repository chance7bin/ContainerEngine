package com.binbin.containerengine.dao;

import com.binbin.containerengine.entity.po.FileInfo;
import com.binbin.containerengine.entity.po.docker.ContainerInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author 7bin
 * @date 2023/12/06
 */
public interface ContainerInfoDao extends MongoRepository<ContainerInfo, String> {

    ContainerInfo findFirstByImageId(String imageId);

}
