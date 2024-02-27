package com.binbin.containerengine.entity.bo.server;

import lombok.Data;

/**
 * 磁盘信息
 *
 * @author 7bin
 * @date 2024/02/26
 */
@Data
public class Disk {

    private String total;
    private String free;
    private String used;
    private Double usage;

}
