package com.zhexinit.xuelele.config.scheduler;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class JobParameter {
    public static final String JOB_PARAM = "jobParam";
    private String jobName;
    private String jobGroup;
    private String jobTrigger;
    private String status;
//    private String cronExpression;
    private Boolean isSync = false;
    private String description;
    private String extra;
    private String accountId;
    private Date updatedTime = new Date();

    private int cnt = 0;
    private String callBackUrl;

    private int interval = 5;
}
