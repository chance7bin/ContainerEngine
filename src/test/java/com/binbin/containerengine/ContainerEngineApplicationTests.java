package com.binbin.containerengine;

import com.binbin.containerengine.constant.TaskStatusConstants;
import com.binbin.containerengine.dao.ExecInfoDao;
import com.binbin.containerengine.entity.po.ExecInfo;
import com.binbin.containerengine.utils.Threads;
import com.binbin.containerengine.utils.file.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Optional;

@SpringBootTest
class ContainerEngineApplicationTests {

    @Test
    void contextLoads() {
    }


    @Test
    void writeTest() {

        String path = "E:\\container-engine\\container\\tmp\\123333";
        String content = TaskStatusConstants.FINISHED;

        FileUtils.write(path,content);


    }


    @Test
    void readTest() {

        String path = "E:\\container-engine\\container\\tmp\\123333";

        String content = null;
        try {
            content = FileUtils.readTxtFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(content);
    }

    @Autowired
    ExecInfoDao execInfoDao;

    @Test
    void testDaoFind() {

        ExecInfo execInfo = new ExecInfo();
        execInfoDao.insert(execInfo);

        Threads.sleep(1000);

        execInfoDao.findById(execInfo.getId()).ifPresent(value -> {
            System.out.println(value);
        });

        System.out.println();

    }
}
