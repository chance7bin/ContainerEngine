package com.binbin.containerengine;

import com.binbin.containerengine.constant.TaskStatusConstants;
import com.binbin.containerengine.utils.file.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

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

}
