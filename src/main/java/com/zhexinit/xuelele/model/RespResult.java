package com.zhexinit.xuelele.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class RespResult<T> {
    private int code ;
    private String messagestr;
    private List<T> data;
    private int totalCount;
}
