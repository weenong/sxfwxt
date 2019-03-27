package com.zhexinit.xuelele.config.scheduler;

import com.zhexinit.xuelele.utils.JackJson;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class CallBackJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        try {
            JobParameter jobParam = (JobParameter) context.getJobDetail().getJobDataMap()
                    .get(JobParameter.JOB_PARAM);
            String callbackurl = jobParam.getCallBackUrl();
            int cnt = jobParam.getCnt();
            if (jobParam != null) {
                String url = "http://i.xuelele.10155.com/Pay/queryOrder?orderId=" + jobParam.getJobName();

                String line = httpSend(url,"POST");
                Map map = JackJson.fromJsonToObject(line,Map.class);
                int status = (Integer) map.get("status");
                System.out.println(line);
                if(status == 1){
                    //call成功
                    String ret = httpSend(callbackurl + "?orderId=" + jobParam.getJobName() + "&status=1","GET");
                    System.out.println(ret);
                    if(StringUtils.equalsAnyIgnoreCase(ret,"ok")){
                        stopJob(context);
                    }
                }
            } else {
                System.out.println("Hey, can't find job parameter ...:)");
            }
            cnt++;
            jobParam.setCnt(cnt);
            jobParam.setInterval(jobParam.getInterval()*2);
            if (cnt > 10) {
                System.out.println("重试大于10次,终止");
                stopJob(context);
                String ret = httpSend(callbackurl + "?orderId=" + jobParam.getJobName() + "&status=0","GET");
//                call失败
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void stopJob(JobExecutionContext context) throws Exception{
        JobParameter jobParam = (JobParameter) context.getJobDetail().getJobDataMap()
                .get(JobParameter.JOB_PARAM);
        Scheduler sched = context.getScheduler();
        TriggerKey triggerKey = TriggerKey.triggerKey(jobParam.getJobName(), jobParam.getJobGroup());
        sched.pauseTrigger(triggerKey);// 停止触发器
        sched.unscheduleJob(triggerKey);// 移除触发器
        sched.deleteJob(JobKey.jobKey(jobParam.getJobName(), jobParam.getJobGroup()));// 删除任务
    }

    private void updateJob(JobExecutionContext context) throws Exception{
        JobParameter jobParam = (JobParameter) context.getJobDetail().getJobDataMap()
                .get(JobParameter.JOB_PARAM);
        Scheduler scheduler = context.getScheduler();
        TriggerKey triggerKey = TriggerKey.triggerKey(jobParam.getJobName(), jobParam.getJobGroup());
        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0/"+jobParam.getInterval()+" * * * * ?");
        trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
        scheduler.rescheduleJob(triggerKey, trigger);

    }

    private String httpSend(String url,String method) throws Exception{

        URL postUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
        connection.setDoInput(true);
        connection.setRequestMethod(method);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        connection.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line = reader.readLine();

        reader.close();
        connection.disconnect();
        return line;
    }
}
