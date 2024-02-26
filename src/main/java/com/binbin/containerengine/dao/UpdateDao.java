package com.binbin.containerengine.dao;

import com.binbin.containerengine.entity.po.docker.ContainerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 用于更新字段的dao（MongoRepository不支持只更新记录中的某些属性）
 *
 * @author 7bin
 * @date 2024/02/26
 */
@Repository
public class UpdateDao {

    @Autowired
    MongoTemplate mongoTemplate;

    public void updateFieldById(String id, String updateField, String updateValue, Class clazz) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update().set(updateField, updateValue);
        mongoTemplate.updateFirst(query, update, clazz);
    }


    public void updateFieldByCusField(String queryField, String queryValue, String updateField, String updateValue, Class clazz) {
        Query query = new Query(Criteria.where(queryField).is(queryValue));
        Update update = new Update().set(updateField, updateValue);
        mongoTemplate.updateFirst(query, update, clazz);
    }

    public ContainerInfo updateDelFlagAndStatusByInsId(String insId, Boolean delFlag, String status) {
        Query query = new Query(Criteria.where("containerInsId").is(insId));
        Update update = new Update()
            .set("delFlag", delFlag)
            .set("status", status);
        return mongoTemplate.findAndModify(query, update, ContainerInfo.class);
    }

}
