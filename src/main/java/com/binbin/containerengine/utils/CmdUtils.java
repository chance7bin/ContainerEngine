package com.binbin.containerengine.utils;

/**
 * 一些终端的执行脚本
 *
 * @author 7bin
 * @date 2023/12/11
 */
public class CmdUtils {

    // 列出最近的一个脚本进程的pid
    // ps -eo pid,lstart,cmd | grep python | grep -v 'grep python' | sort -k2 -r | head -n 1 | awk '{print $1}'
    private static final String LATEST_SCRIPT_PID = "ps -eo pid,lstart,cmd | %s | grep -v '%s' | sort -k2 -r | head -n 1 | awk '{print $1}'";

    // 杀死进程
    private static final String KILL_SCRIPT = "kill -9 %s";

    // 创建文件夹
    private static final String CREATE_DIR = "mkdir -p %s";

    public static String latestScriptPidCmd(String grepPattern) {
        return String.format(LATEST_SCRIPT_PID, grepPattern, grepPattern);
    }

    public static String killScriptCmd(String pid) {
        return String.format(KILL_SCRIPT, pid);
    }


    public static String createDirCmd(String path) {
        return String.format(CREATE_DIR, path);
    }
}
