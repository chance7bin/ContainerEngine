package com.binbin.containerengine.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * docker配置信息
 *
 * @author bin
 * @date 2022/10/12
 */
@Slf4j
@Configuration
public class DockerConfig {

    @Value("${docker.server-host}")
    private String serverHost;

    @Value("${docker.server-port}")
    private String serverPort;

    @Bean(name = "dockerClient")
    DockerClient dockerClient(){
        return connect();
    }

    private static final int _3DAYS = 3 * 24 * 60 * 60;

    /** 连接docker */
    private DockerClient connect() {
        String host = "tcp://" + serverHost + ":" + serverPort;
        // String apiVersion = "1.38";
        //创建DefaultDockerClientConfig
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(host)
            // .withApiVersion(apiVersion)
            .build();
        //创建DockerHttpClient
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(_3DAYS)) // 执行时间超过responseTimeout会报错：Read timed out
            .build();
        //创建DockerClient
        DockerClient client = DockerClientImpl.getInstance(config, httpClient);

        try {
            client.pingCmd().exec();
            log.info("docker client connection successfully!");
        } catch (Exception e) {
            log.error("docker client connection failed!");
            // throw new RuntimeException("docker client connection failed!");
        }

        return client;
    }

}
