package com.binbin.containerengine;

import cn.hutool.Hutool;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HashUtil;
import com.binbin.containerengine.constant.TaskStatusConstants;
import com.binbin.containerengine.dao.ExecInfoDao;
import com.binbin.containerengine.entity.po.ExecInfo;
import com.binbin.containerengine.service.IDockerService;
import com.binbin.containerengine.utils.StringUtils;
import com.binbin.containerengine.utils.Threads;
import com.binbin.containerengine.utils.file.FileUtils;
import com.binbin.containerengine.utils.uuid.IdUtils;
import com.binbin.containerengine.utils.uuid.UUID;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.awt.datatransfer.Transferable;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    IDockerService dockerService;

    @Test
    void testCmd(){

        dockerService.test();

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

    @Test
    void copyArchiveToContainerCmd() {

        String containerId = "d1c15a8e74fa40aae22014815143b53b8a8dbd567a6b655b5c8261de3ada02ba";
        dockerClient.copyArchiveToContainerCmd(containerId)
            .withHostResource("E:\\container-engine\\file\\test1\\123_out.pdf")
            .withRemotePath("/home/tmpdir2") // 容器内文件夹路径（该路径必须存在），文件名默认为宿主机的文件名
            .exec();
        System.out.println("success");


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

    // @SneakyThrows(IOException.class)
    // public void copyFileToContainer(Transferable transferable, String containerPath) {
    //     try (
    //         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    //         TarArchiveOutputStream tarArchive = new TarArchiveOutputStream(byteArrayOutputStream)
    //     ) {
    //         tarArchive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
    //         int lastSlashIndex = StringUtils.removeEnd(containerPath, "/").lastIndexOf("/");
    //         String extractArchiveTo = containerPath.substring(0, lastSlashIndex + 1);
    //         String pathInArchive = containerPath.substring(lastSlashIndex + 1);
    //         transferable.transferTo(tarArchive, pathInArchive);
    //         tarArchive.finish();
    //         dockerClient
    //             .copyArchiveToContainerCmd(containerId)
    //             .withTarInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))
    //             .withRemotePath(extractArchiveTo)
    //             .exec();
    //     }
    // }
}
