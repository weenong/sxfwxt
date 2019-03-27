package com.zhexinit.xuelele.controller;

import com.zhexinit.xuelele.config.scheduler.CallBackJob;
import com.zhexinit.xuelele.config.scheduler.JobParameter;
import com.zhexinit.xuelele.model.RespResult;
import com.zhexinit.xuelele.service.XueLeLeService;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/xuelele")
public class XueLeLeController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Value("${callbackurl}")
    private String callBackUrl;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private XueLeLeService xueLeLeService;

    @GetMapping("/authcode")
    public Object sendCode(String accountId){
        RespResult respResult = new RespResult();
        Map retMap = this.xueLeLeService.sendCode(accountId,"验证登录");
        int code = (int) retMap.get("code");
        String info = (String)retMap.get("message");

        if(0 == code){
            Map m = new HashMap();
            m.put("verify",retMap.get("verify"));
            return respResult.setCode("0000").setMsg(info).setData(m);
        }

        return respResult.setCode("0001").setMsg(info);
    }

    @GetMapping("/codelogin")
    public Object codeLoginHandle(String verify,String accountId,String verifyCode){
        RespResult respResult = new RespResult();
        Map retMap = this.xueLeLeService.codeLoginHandle(verify,accountId,verifyCode);
        int code = (int) retMap.get("code");
        String info = (String)retMap.get("message");

        if(0 == code){
            String sessionId = (String)((Map)((Map)retMap.get("data")).get("userInfo")).get("u_session_id");
            Map map = new HashMap();
            map.put("sessionId",sessionId);

            return respResult.setCode("0000").setMsg(info).setData(map);
        }
        return respResult.setCode("0001").setMsg(info);
    }

    @GetMapping("/hasbuy")
    public Object hasBuy(String sessionId,String courseId){
        RespResult respResult = new RespResult();
        String ret = this.xueLeLeService.course(sessionId,courseId);
        if(StringUtils.contains(ret,"course-info-footer")){
            return respResult.setData(false);
        }
        return respResult.setData(true);
    }

    @GetMapping("/prepay")
    public Object prePay(String sessionId,String accountId,String courseId){
        String ret = this.xueLeLeService.course(sessionId,courseId);
        if(!StringUtils.contains(ret,"course-info-footer")){
            return new RespResult().setCode("0001").setMsg("已经购买过");
        }

        Map map = this.xueLeLeService.prePay(sessionId,accountId,courseId);

        return new RespResult().setData(map);
    }

    @GetMapping("/pay")
    public Object pay(String sessionId,String authNo,String checkCode,String clientId,String clientSecret,String productBase,String productType,String accountId,String orderId){
        try {
            boolean ret = this.xueLeLeService.pay(sessionId, authNo, checkCode, clientId, clientSecret, productBase, productType, accountId, orderId);
            if(ret){
                return new RespResult().setMsg("提交成功");
            }
            return new RespResult().setCode("0001").setMsg("提交失败");
        }catch (Exception e){
            e.printStackTrace();
            return new RespResult().setCode("0001").setMsg(e.getMessage());
        }
    }

    @GetMapping("/callback")
    public String callBack(String orderId,int status){
        logger.debug("callback:" +  orderId + " == " + status);
        return "ok";
    }

    @GetMapping("/sendcode")
    public Object sendVerifyCode(String actId,String mobile){
        RespResult respResult = new RespResult();
        Map retMap = this.xueLeLeService.getActInfo(actId);
        int status = (int) retMap.get("status");
        String info = (String)retMap.get("info");
        Map resultData = (Map)retMap.get("resultdata");
        if(1 != status){
            return respResult.setCode("0001").setMsg(info);
        }
        Boolean hasAct = (Boolean) resultData.get("hasAct");
        if(!hasAct){
            info = "加载活动信息出错，请刷新";
            return respResult.setCode("0001").setMsg(info);
        }
        Map actInfo = (Map)resultData.get("info");
        Map course = (Map)resultData.get("course");
        String ocId = (String)course.get("oc_id");
        String isDestroy = (String)actInfo.get("act_is_destroy");
        if(StringUtils.equals(isDestroy,"1")){
            info = "selfDestroy";
            return respResult.setCode("0001").setMsg(info);
        }
        Long startTime = Long.parseLong((String) actInfo.get("act_start_time"));
        Long endTime = Long.parseLong((String) actInfo.get("act_end_time"));
        Long now = System.currentTimeMillis()/1000;
        if(now < startTime){
            info = "活动尚未开始";
            return respResult.setCode("0001").setMsg(info);
        }
        if(now > endTime){
            info = "活动已经结束";
            return respResult.setCode("0001").setMsg(info);
        }

        retMap = this.xueLeLeService.sendVerifyCodeNoLogin(mobile,actId,ocId);
        status = (int) retMap.get("status");
        info = (String)retMap.get("info");
        if(status == 1){
            Map resultMap = new HashMap();
            resultMap.put("ocId",ocId);
            return respResult.setData(resultMap).setMsg(info);
        }else{
            return respResult.setCode("0001").setMsg(info);
        }

    }

    @GetMapping("/channelpay")
    public Object channelPay(String mobile,String verifyCode,String ocId,String actId ){
        RespResult respResult = new RespResult();
        Map retMap = this.xueLeLeService.channelPay(mobile,verifyCode,ocId,actId);
        int status = (int)retMap.get("status");
        String info = (String)retMap.get("info");
        if(1 == status){
            return respResult.setMsg(info).setData(retMap);
        }else{
            return respResult.setMsg(info).setCode("0001");
        }
    }

    @GetMapping("/test")
    public Object test(String jobName) throws Exception{
        JobParameter param = new JobParameter();
        param.setJobName(jobName);
        param.setJobGroup("JobGroup");
//        param.setCronExpression("0/5 * * * * ?");
        param.setCallBackUrl(callBackUrl);
        JobDetail jobDetail = JobBuilder.newJob(CallBackJob.class)
                .withIdentity(param.getJobName(), param.getJobGroup()).build();
        // 表达式调度构建器
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0/"+param.getInterval()+" * * * * ?");
        // 按cronExpression表达式构建trigger
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(param.getJobName(), param.getJobGroup())
                .withSchedule(scheduleBuilder).build();
        // 放入参数，运行时的方法可以获取
        jobDetail.getJobDataMap().put("jobParam", param);
        scheduler.scheduleJob(jobDetail, trigger);
        return null;
    }
}
