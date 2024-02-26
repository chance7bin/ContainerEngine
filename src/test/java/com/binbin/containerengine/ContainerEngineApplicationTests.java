package com.binbin.containerengine;

import com.binbin.containerengine.constant.TaskStatusConstants;
import com.binbin.containerengine.dao.ExecInfoDao;
import com.binbin.containerengine.dao.UpdateDao;
import com.binbin.containerengine.entity.po.ExecInfo;
import com.binbin.containerengine.service.IDockerService;
import com.binbin.containerengine.service.IFileService;
import com.binbin.containerengine.utils.StringUtils;
import com.binbin.containerengine.utils.Threads;
import com.binbin.containerengine.utils.file.FileUtils;
import com.binbin.containerengine.utils.uuid.IdUtils;
import com.github.dockerjava.api.DockerClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

    @Autowired
    IDockerService dockerService;

    @Test
    void testCmd(){

        // String insId = "f8d4db1b87e1e2be84ae51d9ac7dd90dfaf181aa052ea2ed552b68a75f4ed027";
        // // String script = "echo 123";
        // String script = CmdUtils.latestScriptPidCmd("grep python");
        // String rsp = execSimpleCmd(insId, script);
        // if (!StringUtils.isEmpty(rsp)){
        //     String pid = StringUtils.matchNumber(rsp);
        //     System.out.println(pid);
        //     script = CmdUtils.killScriptCmd(pid);
        //     rsp = execSimpleCmd(insId, script);
        //     System.out.println(rsp);
        // }

    }

    @Test
    void testUtils(){

        String s = StringUtils.matchNumber("1999\n111");
        System.out.println(s);

    }


    @Autowired
    DockerClient dockerClient;

    @Test
    void copyArchiveFromContainerCmd() throws IOException {

        // List<InspectVolumeResponse> volumes = dockerClient.listVolumesCmd().exec().getVolumes();
        // System.out.println();

        String uuid = IdUtils.fastUUID();
        String containerId = "d1c15a8e74fa40aae22014815143b53b8a8dbd567a6b655b5c8261de3ada02ba";
        InputStream inputStream = dockerClient.copyArchiveFromContainerCmd(containerId, "/home/tmpdir/123_out.pdf").exec();
        // inputStream写入到path文件中
        String hostFile = "E:\\container-engine\\file\\test1\\123_out1.pdf";
        TarArchiveInputStream tarStream = new TarArchiveInputStream(inputStream);
        unTar(tarStream, new File(hostFile));


    }

    @Autowired
    IFileService fileService;

    @Test
    void copyArchiveToContainerCmd() {

        // String containerId = "d1c15a8e74fa40aae22014815143b53b8a8dbd567a6b655b5c8261de3ada02ba";
        // dockerClient.copyArchiveToContainerCmd(containerId)
        //     .withHostResource("E:\\container-engine\\file\\test1\\123_out.pdf")
        //     .withRemotePath("/home/tmpdir2") // 容器内文件夹路径（该路径必须存在），文件名默认为宿主机的文件名
        //     .exec();
        // System.out.println("success");

        fileService.copyFileToContainer(
            "f8d4db1b87e1e2be84ae51d9ac7dd90dfaf181aa052ea2ed552b68a75f4ed027",
            "E:\\ModelServiceContainer\\seims\\SEIMS-master\\data\\youwuzhen\\data_prepare\\climate\\climate",
            "/home/climate");

    }

    public void unTar(TarArchiveInputStream tis, File destFile)
        throws IOException {
        TarArchiveEntry tarEntry = null;
        while ((tarEntry = tis.getNextTarEntry()) != null) {
            if (tarEntry.isDirectory()) {
                if (!destFile.exists()) {
                    destFile.mkdirs();
                }
            } else {
                FileOutputStream fos = new FileOutputStream(destFile);
                IOUtils.copy(tis, fos);
                fos.close();
            }
        }
        tis.close();
    }

    @Test
    void testLoadImage(){
        dockerService.loadImage("E:\\container-engine\\file\\image\\cw_env.tar");
    }

    @Test
    void testUncompress(){
        FileUtils.unCompress("E:\\ModelServiceContainer\\seims\\SEIMS-master\\data\\youwuzhen\\data_prepare\\climate\\climate.zip");
    }

    @Autowired
    UpdateDao updateDao;

    @Test
    void testUpdateDao(){
        // updateDao.updateDelFlagByInsId("f6e8513025324ac6439a0314c3eb59fa72efcdff0b60a1616e107276d3339d25", Boolean.TRUE);
        String status = dockerService.getContainerStatusByContainerInsId("3123");
        System.out.println(status);
    }

}
