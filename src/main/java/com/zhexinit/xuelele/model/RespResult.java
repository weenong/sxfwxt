package com.zhexinit.xuelele.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RespResult {
    private String code = "0000";
    private String msg;
    private Object data;
}
