package com.binbin.containerengine.service.impl;

import com.binbin.containerengine.constant.Constants;
import com.binbin.containerengine.constant.ContainerStatus;
import com.binbin.containerengine.constant.FileConstants;
import com.binbin.containerengine.constant.TaskStatusConstants;
import com.binbin.containerengine.dao.ExecInfoDao;
import com.binbin.containerengine.entity.bo.ExecResponse;
import com.binbin.containerengine.entity.bo.TerminalRsp;
import com.binbin.containerengine.entity.po.ExecInfo;
import com.binbin.containerengine.entity.po.docker.ContainerInfo;
import com.binbin.containerengine.entity.po.docker.ImageInfo;
import com.binbin.containerengine.exception.ServiceException;
import com.binbin.containerengine.manager.ThreadPoolManager;
import com.binbin.containerengine.service.IDockerService;
import com.binbin.containerengine.utils.CmdUtils;
import com.binbin.containerengine.utils.StringUtils;
import com.binbin.containerengine.utils.TerminalUtils;
import com.binbin.containerengine.utils.Threads;
import com.binbin.containerengine.utils.file.FileUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author 7bin
 * @date 2023/11/30
 */
@Slf4j
@Service
public class DockerServiceImpl implements IDockerService {

    @Autowired
    DockerClient dockerClient;

    @Autowired
    ExecInfoDao execInfoDao;

    @Value(value = "${docker.registry-url}")
    String dockerRegistryUrl;

    @Value(value = "${file.save-path}")
    String savePath;

    // exec 正常退出状态码
    private static final Long NORMAL_EXIT_CODE = 0L;

    private final static String CONTAINER_DIR = "container";

    @Override
    public void listContainer() {
        List<Container> list =  dockerClient.listContainersCmd()
            .withShowAll(true)
            .exec();
    }

    @Override
    public ContainerInfo createContainer(ContainerInfo containerInfo) {

        // DockerClient client = connect();

        CreateContainerResponse response = initContainer(dockerClient, containerInfo);
        String containerInsId = response.getId();
        //启动
        dockerClient.startContainerCmd(containerInsId).exec();

        containerInfo.setContainerInsId(containerInsId);

        return containerInfo;
    }


    @Override
    public int updateContainer(ContainerInfo containerInfo) {
        return 0;
    }

    @Override
    public String getContainerStatusByContainerInsId(String containerInsId) {

        try {
            InspectContainerCmd cmd = dockerClient.inspectContainerCmd(containerInsId);
            InspectContainerResponse res = cmd.exec();
            String status = res.getState().getStatus();
            return status;
        } catch (Exception e){
            // throw new ServiceException("容器不存在");
            return ContainerStatus.DELETED;
        }
    }

    @Override
    public ImageInfo inspectImage(String sha256) {
        InspectImageResponse res = dockerClient.inspectImageCmd(sha256).exec();
        ImageInfo imageInfo = new ImageInfo();
        // imageInfo.setSize(res.getSize());
        return imageInfo;
    }


    @Override
    public List<Image> listImages() {

        List<Image> images = dockerClient.listImagesCmd().exec();

        // List<ImageInfo> imageInfoDTOS = new ArrayList<>();
        // for (Image image : images) {
        //     imageInfoDTOS.add(formatImageInfo(image));
        // }

        return images;

    }


    /**
     * docker中的image信息转换成项目中的image信息
     * @param image docker中的image信息
     * @return {@link ImageInfo}
     * @author 7bin
     **/
    private ImageInfo formatImageInfo(Image image){

        ImageInfo imageInfo = new ImageInfo();
        // imageInfo.setRepoTags(image.getRepoTags()[0]);
        // Long size = image.getSize();
        // imageInfo.setSize(size);

        return imageInfo;

    }

    @Override
    public List<ContainerInfo> listContainers() {

        List<Container> containers = dockerClient.listContainersCmd().exec();
        List<ContainerInfo> infoList = new ArrayList<>();
        for (Container container : containers) {
            infoList.add(formatContainerInfo(container));
        }
        return infoList;

    }

    @Override
    public ContainerInfo selectContainerByInsId(String insId) {

        List<ContainerInfo> containers = listContainers();
        for (ContainerInfo container : containers) {
            if (container.getContainerInsId().equals(insId)){
                return container;
            }
        }

        return null;
    }

    private ContainerInfo formatContainerInfo(Container container){
        ContainerInfo containerInfo = new ContainerInfo();
        containerInfo.setContainerInsId(container.getId());
        containerInfo.setImageName(container.getImage());
        containerInfo.setStatus(container.getState()); //running
        // container.getCreated() 的时间戳位数是10位 now.getTime()是13位
        // containerInfo.setCreated(DateUtils.getTime2Now(DateUtils.toDate(container.getCreated() * 1000)));
        return containerInfo;
    }


    @Override
    public String exec(String insId, String script){
        return exec(insId, script, Constants.ASYNC);
    }

    @Override
    public String exec(String insId, String script, String mode) {

        // 多行命令时需在每行末尾加入\n 换行转义符 （使用&&时curl无法识别，需\&转义，所以规定使用\n，后端将\n转换为&&)
        script = script.replaceAll("\n", "&&");
        // ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(insId)
        //     .withAttachStdout(true)
        //     .withAttachStderr(true)
        //     .withCmd("/bin/bash", "-c", script)
        //     .exec();
        //
        // String execId = execCreateCmdResponse.getId();
        String execId = execCreateCmd(insId, script);

        // 记录 exec 状态
        ExecInfo execInfo = new ExecInfo();
        execInfo.setExecId(execId);
        execInfo.setContainerId(insId);
        execInfo.setScript(script);
        execInfoDao.insert(execInfo);

        // 脚本执行时间过长时，会导致回调异常：Error during callback
        // ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        // ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        // dockerClient.execStartCmd(execId)
        //     // .withDetach(true) // 直接返回，不等了
        //     .exec(new ExecStartResultCallback(stdout, stderr));
        // execInfo.setStatus(TaskStatusConstants.RUNNING);
        // execInfoDao.save(execInfo);
        // 执行脚本后设置定时任务检查脚本执行结果
        // scheduledCheckTask(execId);

        // 同步调用还是异步调用
        log.info("exec: [ {} ]", script);
        if (Constants.SYNC.equals(mode)){

            execSync(execId);

        } else {

            // SpringUtils.getAopProxy(this).execAsync(execId); // 使用该方法报错
            // IDockerService proxy = (IDockerService) AopContext.currentProxy(); // 使用获取代理的方式调用异步方法

            // 使用getBean获取代理对象，实现异步调用
            // SpringUtils.getBean(IDockerService.class).execAsync(execId);

            execAsync(execId);

        }

        return execId;

    }

    @Override
    public void test(){

    }

    @Override
    public void createFolderInContainer(String containerId, String folderPath){
        String script = CmdUtils.createDirCmd(folderPath);
        execSimpleCmd(containerId, script);
    }

    @Override
    public String loadImage(String path) {

        File file = new File(path);
        if (!file.exists()){
            throw new ServiceException("file not exist");
        }

        final String[] imageName = {null};

        try {

            // dockerClient.loadImageCmd(new FileInputStream(file)).exec();
            // return true;

            dockerClient.loadImageAsyncCmd(new FileInputStream(file))
                .exec(new ResultCallback.Adapter<LoadResponseItem>() {
                    @Override
                    public void onNext(LoadResponseItem item) {
                        String stream = item.getStream();
                        // 截取"imageName:tag"字符串，并且把末尾的换行符去掉
                        if (stream != null){
                            imageName[0] = stream.substring(stream.indexOf("Loaded image: ") + 14, stream.length() - 1);
                        }
                    }
                })
                .awaitCompletion();


            return imageName[0];

        } catch (Exception e) {
            e.printStackTrace();
            return imageName[0];
        }


    }

    @Override
    public void ping() {
       dockerClient.pingCmd().exec();
    }


    private String execCreateCmd(String insId, String script){
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(insId)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd("/bin/bash", "-c", script)
            .exec();
        return execCreateCmdResponse.getId();
    }

    // 调用简单的cmd命令获取一些系统信息
    private String execSimpleCmd(String insId, String script){
        String execId = execCreateCmd(insId, script);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        try {
            dockerClient.execStartCmd(execId)
                .exec(new ExecStartResultCallback(stdout, stderr))
                .awaitCompletion();
            return stdout.toString();
        } catch (InterruptedException | RuntimeException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 创建定时任务检查任务的执行状态
    private void scheduledCheckTask(String execId){

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                InspectExecResponse info = inspectExecCmd(execId);
                // log.info("检查脚本执行结果 - 是否正在运行 : " + info.isRunning());
                if (!info.isRunning()){

                    // 更新 task info 信息
                    ExecInfo res = getExecInfoByExecId(execId);
                    res.setExitCode(info.getExitCodeLong());
                    res.setRunning(info.isRunning());
                    res.setStatus(Objects.equals(res.getExitCode(), NORMAL_EXIT_CODE) ? TaskStatusConstants.FINISHED : TaskStatusConstants.FAILED);
                    execInfoDao.save(res);

                    // !!! 各种操作要放在cancelTask之前，后续的代码都不会执行的！！！
                    ThreadPoolManager.instance().cancelTask(execId);

                }
            }
        };

        ScheduledFuture scheduledFuture = ThreadPoolManager.instance().scheduleWithFixedDelay(task, 1000);
        ThreadPoolManager.instance().recordTask(execId, scheduledFuture);
    }

    // 使用countDownLatch阻塞线程，等待结果返回
    // 现在直接用 ExecStartResultCallback 的 awaitCompletion 方法阻塞线程就行了
    private void execSyncWithResult(String execId){

        // 异步 -> 同步
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final long SECONDS_OF_WAIT_TIME = 300L;// 等待时间 5分钟

        List<String> result = new ArrayList<>();

        ResultCallback<Frame> callback = new ResultCallbackTemplate<ResultCallback<Frame>, Frame>() {

            //3.结果返回后进行回调，解除阻塞
            @Override
            public void onNext(Frame frame) {

                // 如果调用的脚本没输出东西到终端，那么不会触发onNext方法
                result.add(new String(frame.getPayload()).trim());
                System.out.println();
                countDownLatch.countDown();

            }
        };

        // 1.异步调用
        dockerClient.execStartCmd(execId)
            .withDetach(false) // true 直接返回， false会回调callback方法
            .exec(callback);

        // 2.阻塞等待异步响应
        try {
            countDownLatch.await(SECONDS_OF_WAIT_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //4.超时或结果正确返回，对结果进行处理
        System.out.println(result);

    }


    // 异步调用任务
    // @Async
    @Override
    public void execAsync(String execId){
        // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // dockerClient.execStartCmd(execId)
        //     .exec(new ExecStartResultCallback(outputStream, null));
        // log.info("execAsync execId: {}", execId);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                execSync(execId);
            }
        };
        ThreadPoolManager.instance().execute(task);


        // 延迟一秒记录当前脚本在容器内部的pid
        TimerTask task2 = new TimerTask() {
            @Override
            public void run() {
                // log.info("record pid");

                // 这里为什么要阻塞一下
                // 比如说前面那个异步任务如果在线程池满了的时候加入，会导致前面那个任务被阻塞，
                // 那么这个延迟一面的定时任务会先于前面执行，获取到的pid就是前一个脚本的pid了
                // 解决方法：当前定时任务先阻塞住，如果异步任务更新为running的话再唤醒当前的定时任务，如果异步任务终止的话直接退出该方法
                while (true){
                    String status = getExecInfoByExecId(execId).getStatus();
                    // 如果异步任务更新为running的话再唤醒当前的定时任务
                    if (status.equals(TaskStatusConstants.RUNNING)){
                        break;
                    }
                    // 如果异步任务终止的话直接退出该方法
                    if (!status.equals(TaskStatusConstants.CREATED)){
                        return;
                    }
                    Threads.sleep(1000);
                }

                // 获取脚本容器内的pid
                String script = CmdUtils.latestScriptPidCmd("grep python");
                String containerId = getExecInfoByExecId(execId).getContainerId();
                String rsp = execSimpleCmd(containerId, script);
                if (!StringUtils.isEmpty(rsp)){
                    String pid = StringUtils.matchNumber(rsp);
                    ExecInfo info = getExecInfoByExecId(execId);
                    info.setPid(pid);
                    execInfoDao.save(info);
                    // System.out.println(pid);
                    // script = CmdUtils.killScriptCmd(pid);
                    // rsp = execSimpleCmd(execId, script);
                }
            }
        };

        ThreadPoolManager.instance().schedule(task2);
    }


    private void execSync(String execId){
        // log.info("execSync");

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        recordExecProcessing(execId, TaskStatusConstants.RUNNING);

        try {
            dockerClient.execStartCmd(execId)
                // .withDetach(false) // true 直接返回， false会回调callback方法
                .exec(new ExecStartResultCallback(stdout, stderr))
                .awaitCompletion(); // 如果执行太久会报错：Read timed out
            // System.out.println(stdout.toString());
            // System.out.println(stderr.toString());
            // if ("".equals(stderr.toString())){
            //     recordExecProcessing(execId, TaskStatusConstants.FINISHED);
            // } else {
            //     recordExecProcessing(execId, TaskStatusConstants.FAILED);
            // }
            InspectExecResponse inspectInfo = inspectExecCmd(execId);
            // 更新 task info 信息
            ExecInfo execInfo = getExecInfoByExecId(execId);
            execInfo.setExitCode(inspectInfo.getExitCodeLong());
            execInfo.setRunning(inspectInfo.isRunning());
            execInfo.setStatus(Objects.equals(inspectInfo.getExitCodeLong(), NORMAL_EXIT_CODE) ? TaskStatusConstants.FINISHED : TaskStatusConstants.FAILED);
            // execInfo.setStderr(stderr.toString());
            if (stderr.toString().length() > 0){
                execInfo.setStderr("[ container inner exception ] " + stderr.toString());
            }
            execInfoDao.save(execInfo);

        } catch (InterruptedException | RuntimeException e) {
            ExecInfo execInfo = getExecInfoByExecId(execId);
            execInfo.setExitCode(1L);
            execInfo.setRunning(false);
            execInfo.setStatus(TaskStatusConstants.FAILED);
            execInfo.setStderr("[ container inner exception ] " + e.getMessage());
            execInfoDao.save(execInfo);

            e.printStackTrace();
        }
    }


    @Override
    public String getExecStatus(String execId) {
        ExecInfo info = getExecInfoByExecId(execId);
        return info.getStatus();
    }

    public String getExecStatusByTxt(String execId) {
        String tmpFilePath = getExecProcessingFilePath(execId);
        String result = TaskStatusConstants.OTHER;
        try {
            result = FileUtils.readTxtFile(tmpFilePath);
        } catch (IOException e) {
            throw new ServiceException("file not found: " + tmpFilePath);
        }

        return result;
    }

    @Override
    public ExecInfo getExecInfoByExecId(String execId){
        ExecInfo execInfo = execInfoDao.findByExecId(execId).orElseThrow(() -> new ServiceException("execInfo[execId=" + execId + "] not found"));
        return execInfo;
    }

    @Override
    public InspectExecResponse inspectExecCmd(String execId){

        // exec instance 是临时的，运行的一定时间才有效，时间太长就失效了
        // exitCode 正常退出为0，异常退出不为0
        InspectExecResponse rsp = dockerClient.inspectExecCmd(execId).exec();
        // System.out.println(rsp);

        return rsp;
    }

    @Override
    public boolean getExecIfDone(String execId){
        ExecInfo info = getExecInfoByExecId(execId);
        return !info.getRunning();
    }

    // 记录exec执行的状态
    private void recordExecProcessing(String execId, String status){
        // 记录到文件中（deprecated）
        // String tmpFilePath = getExecProcessingFilePath(execId);
        // FileUtils.write(tmpFilePath, status);

        // 记录到数据库中
        ExecInfo info = getExecInfoByExecId(execId);
        info.setStatus(status);
        execInfoDao.save(info);

    }

    private String getExecProcessingFilePath(String execId){
        return savePath
            + FileConstants.FILE_PATH_SEPARATOR + CONTAINER_DIR
            + FileConstants.FILE_PATH_SEPARATOR + execId;
    }

    @Override
    public String commitContainer(String containerInsId, String imageName, String tag) {
        // CommitCmd commitCmd = dockerClient.commitCmd("413a283cfa2a2e0cfcd2a70a77d63f9c524d9f59f65f9f1ca682fd7423c4e1d6");
        // commitCmd.withRepository("java-generate");
        // commitCmd.withTag("1.0");
        // String exec = commitCmd.exec();

        String imageId = dockerClient.commitCmd(containerInsId)
            .withRepository(imageName)
            .withTag(tag)
            .exec();

        // System.out.println(exec);

        // String[] cmd = new String[] {"docker", "commit", "53c516af510e17d5f3ae93475849e93749da0cd1e25b31c87232a4553f044120", "java-commit:2.0"};
        // TerminalRes exec = TerminalUtils.exec(cmd);
        // System.out.println(exec);

        return imageId;

    }

    @Override
    public void saveContainer() {
        SaveImageCmd saveImageCmd = dockerClient.saveImageCmd("java-generate:1.0");

        InputStream exec = saveImageCmd.exec();
    }

    @Override
    public TerminalRsp exportContainer(String container, String outputPath) {
        String[] cmd = new String[] {"docker", "export", "-o", outputPath, container};
        return TerminalUtils.exec(cmd);
        // System.out.println(exec);
    }

    @Override
    public TerminalRsp importContainer(String inputPath, String imageName) {
        String[] cmd = new String[] {"docker", "import", inputPath, imageName};
        return TerminalUtils.exec(cmd);
        // System.out.println(exec);
    }

    @Override
    public void startContainer(String containerInsId) {
    }

    @Override
    public void stopContainer(String containerInsId) {

    }

    @Override
    public void removeContainer(String containerInsId) {
        RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(containerInsId);
        removeContainerCmd.withForce(true);
        removeContainerCmd.exec();
    }

    @Override
    public boolean isContainerRunning(String containerInsId) {
        String status = getContainerStatusByContainerInsId(containerInsId);
        if (ContainerStatus.RUNNING.equals(status)){
            return true;
        }
        return false;
    }

    @Override
    public ExecResponse execWithTerminal(String[] cmdArr) throws IOException, InterruptedException {

        //这个方法是类似隐形开启了命令执行器，输入指令执行python脚本
        Process process = Runtime.getRuntime()
            .exec(cmdArr); // "python解释器位置（这里一定要用python解释器所在位置不要用python这个指令）+ python脚本所在路径（一定绝对路径）"

        String response = TerminalUtils.getInputMsg(process);
        String error = TerminalUtils.getErrorMsg(process);

        int exitVal = process.waitFor(); // 阻塞程序，跑完了才输出结果
        // long end = System.currentTimeMillis();

        // TODO: 2022/11/7 封装脚本中的错误该如何处理
        // currentMsrIns = msInsService.getCurrentMsrIns(msInsId);

        return new ExecResponse(exitVal, response, error);

    }



    //初始化容器
    private CreateContainerResponse initContainer(DockerClient client, ContainerInfo containerInfo){

        //数据卷 Bind.parse
        List<Bind> binds = new ArrayList<>();
        if (containerInfo.getVolumeList() != null){
            for (String volume : containerInfo.getVolumeList()) {
                // volume = formatPathSupportDocker(repository + volume);
                volume = formatPathSupportDocker(volume);
                binds.add(Bind.parse(volume));
            }
        }

        //容器启动配置
        HostConfig hostConfig = new HostConfig();

        if (containerInfo.getContainerExportPort() != null && containerInfo.getHostBindPort() != null){
            hostConfig
                //端口映射
                .withPortBindings(new Ports(
                    new ExposedPort(containerInfo.getContainerExportPort()),
                    Ports.Binding.bindPort(containerInfo.getHostBindPort())));

        }

        // .withPortBindings(
        //     new PortBinding(Ports.Binding.bindPort(containerInfo.getContainerExportPort()), new ExposedPort(containerInfo.getContainerExportPort())),
        //     new PortBinding(Ports.Binding.bindPort(socketPort), new ExposedPort(socketPort)));

        if (binds.size() != 0){
            //挂载
            hostConfig.withBinds(binds);
        }

        CreateContainerCmd containerCmd = client.createContainerCmd(containerInfo.getImageName())
            //容器名
            .withName(containerInfo.getContainerName())
            //端口映射 内部80端口与外部81端口映射
            // .withHostConfig(new HostConfig().withPortBindings(new Ports(new ExposedPort(80), Ports.Binding.bindPort(81))))
            .withHostConfig(hostConfig);

        // 添加启动命令
        if (!StringUtils.isEmpty(containerInfo.getCmd())){
            //启动命令
            containerCmd.withCmd(containerInfo.getCmd());
        }

        //创建
        CreateContainerResponse response = containerCmd.exec();
        // log.info("container instance id: " + response.getId());
        return response;

    }


    /**
     * 将数据卷目录修改成适合docker的格式 [ /e/... ]
     * @param path
     * @return java.lang.String
     * @Author bin
     **/
    private String formatPathSupportDocker(String path){
        // E:\opengms-lab\container\workspace\test:/opt/notebooks
        // String path = "E:\\opengms-lab\\container\\workspace\\test:/opt/notebooks";
        path = path.replaceAll("\\\\","/");
        int index = path.indexOf(":");
        String outputPath = path;
        if (index == 1){
            // 只有 E: 这种形式的才要进行处理
            outputPath = "/" + Character.toString(path.charAt(0)).toLowerCase() + path.substring(index + 1);
        }
        return outputPath;
    }

    private String tagImage(String imageNameWithTag, String newImageNameWithRepository, String newTag) {

        final String id = dockerClient.inspectImageCmd(imageNameWithTag)
            .exec()
            .getId();

        // push the image to the registry
        dockerClient.tagImageCmd(id, newImageNameWithRepository, newTag).exec();

        return newImageNameWithRepository + ":" + newTag;
    }

    @Override
    public void pushImage(String imageName, String tag) throws InterruptedException {

        // 1.推送本地镜像到仓库前都必须重命名(docker tag)镜像，以镜像仓库地址为前缀
        String newImageName = tagImage(imageName + ":" + tag, dockerRegistryUrl + "/" + imageName, tag);

        // 2.使用dockerclient推送镜像
        dockerClient.pushImageCmd(newImageName)
            .start()
            .awaitCompletion(1, TimeUnit.MINUTES);


        // 3.删除以镜像仓库地址为前缀的镜像（因为该镜像只是用于推送到指定docker registry的临时tag）
        // 不用删除
        // dockerClient.removeImageCmd(newImageName).withForce(true).exec();

    }

    @Override
    public void pullImage(String imageName, String tag) throws InterruptedException {

        String newImageWithTag = dockerRegistryUrl + "/" + imageName + ":" + tag;
        // String newImageWithTag = imageName + ":" + tag;

        // 1.使用dockerclient拉取镜像
        dockerClient.pullImageCmd(newImageWithTag)
            .start()
            .awaitCompletion(1, TimeUnit.MINUTES);

        // 2.重命名(docker tag)镜像，删除镜像仓库地址前缀
        // tagImage(newImageWithTag, imageName, tag);


        // 3.删除以镜像仓库地址为前缀的镜像
        // dockerClient.removeImageCmd(newImageWithTag).withForce(true).exec();
    }
}
