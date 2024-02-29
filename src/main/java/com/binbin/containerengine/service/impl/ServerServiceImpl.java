package com.binbin.containerengine.service.impl;

import com.binbin.containerengine.service.IDockerService;
import com.binbin.containerengine.service.IServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 7bin
 * @date 2024/02/28
 */
@Service
public class ServerServiceImpl implements IServerService {

    @Autowired
    IDockerService dockerService;

    @Override
    public void checkDocker() {
        dockerService.ping();
    }
}
