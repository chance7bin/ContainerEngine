package com.binbin.containerengine.entity.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 终端的执行输出
 *
 * @author 7bin
 * @date 2022/12/14
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TerminalRsp {
    Integer code;
    String msg;

    public static final Integer SUCCESS_CODE = 1;

    public static final Integer ERROR_CODE = -1;

    public static TerminalRsp success(String msg){
        return new TerminalRsp(SUCCESS_CODE, msg);
    }

    public static TerminalRsp error(String msg){
        return new TerminalRsp(ERROR_CODE, msg);
    }


}
