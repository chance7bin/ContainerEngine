package com.binbin.containerengine.dao;

import com.binbin.containerengine.entity.po.ExecInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * @author 7bin
 * @date 2023/12/06
 */
public interface ExecInfoDao extends MongoRepository<ExecInfo, String> {

    // 根据execId找
    Optional<ExecInfo> findByExecId(String execId);

    ExecInfo findFirstById(String id);

}
