package com.zhexinit.xuelele.service;

import com.zhexinit.xuelele.config.scheduler.CallBackJob;
import com.zhexinit.xuelele.config.scheduler.JobParameter;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class XueLeLeService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Value("${callbackurl}")
    private String callBackUrl;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private RestTemplate restTemplate;

    public Map getActInfo(String actId){
        String url = "https://xuelele.10155.com/Activity/Common/getActInfo";

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("act_id",actId);
        map.add("p","");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("x-requested-with","XMLHttpRequest");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url,request, Map.class);
        Map retMap = response.getBody();
        return retMap;
    }

    public Map sendVerifyCodeNoLogin(String mobile, String actId, String ocId) {
        String url = "https://xuelele.10155.com/Activity/Order/sendVerifyCodeNoLogin";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("x-requested-with","XMLHttpRequest");

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("accountID", mobile);
        map.add("oc_id",ocId);
        map.add("act_id",actId);
        map.add("p","");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity( url, request , Map.class );
        Map retMap = response.getBody();
        return retMap;
    }

    public Map channelPay(String mobile,String verifyCode,String ocId,String actId){
        String url = "https://xuelele.10155.com/Activity/Order/channelPay";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("x-requested-with","XMLHttpRequest");

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("accountID", mobile);
        map.add("verifyCode",verifyCode);
        map.add("payType","tcpay");
        map.add("backUrl","");
        map.add("oc_id",ocId);
        map.add("act_id",actId);
        map.add("p","");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity( url, request , Map.class );
        Map retMap = response.getBody();
        return retMap;
    }

    public Map sendCode(String accountId, String type) {
        String url = "https://xuelele.10155.com/Api/Login/sendCode?jrPlatform=WAP";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> map= new HashMap<>();
        map.put("accountID", accountId);
        map.put("type",type);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity( url, request , Map.class );
        Map retMap = response.getBody();
        List<String> cookieList = response.getHeaders().get("Set-Cookie");
        String verify = "";
        if(null != cookieList && cookieList.size()>0){
            for(String cookieStr:cookieList) {
                String[] cookies = cookieStr.split(";");
                for (String cookie : cookies) {
                    String[] kv = cookie.split("=");
                    if (kv[0].equalsIgnoreCase("verify")) {
                        verify = kv[1].trim();
                        break;
                    }
                }
            }
        }
        retMap.put("verify",verify);
        return retMap;
    }

    public Map codeLoginHandle(String verify,String accountId, String verifyCode) {
        String url = "https://xuelele.10155.com/Api/Login/codeLoginHandle?jrPlatform=WAP";
        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.COOKIE,new ArrayList<String>(){{
            add("verify=" + verify);
        }});
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> map= new HashMap<>();
        map.put("accountID", accountId);
        map.put("verifyCode",verifyCode);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity( url, request , Map.class );
        Map retMap = response.getBody();
        return retMap;
    }

    public String course(String sessionId, String courseId) {
        String url = "https://xuelele.10155.com/course/" + courseId;
        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.COOKIE,new ArrayList<String>(){{
            add("PHPSESSID=" + sessionId);
        }});
        headers.setAccept(new ArrayList<MediaType>(){{
            add(MediaType.TEXT_HTML);
        }});

        HttpEntity entity = new HttpEntity(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        String retStr = response.getBody();
        return retStr;
    }

    public Map prePay(String sessionId,String accountId, String courseId){
        String url = "https://xuelele.10155.com/Api/PaymentOrder/getPaymentList?id="+courseId+"&productType=0&jrPlatform=WAP";
        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.COOKIE,new ArrayList<String>(){{
            add("PHPSESSID=" + sessionId);
        }});
//        headers.set("Cookie","PHPSESSID=" + sessionId);
        headers.setAccept(new ArrayList<MediaType>(){{
            add(MediaType.APPLICATION_JSON_UTF8);
        }});
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity entity = new HttpEntity(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);

        Map retMap = response.getBody();

        Map data = (Map)retMap.get("data");
        String payId = (String)((Map)((List<Map>)data.get("list")).get(0)).get("ps_id");

        url = "https://xuelele.10155.com/Api/PaymentOrder/pay?jrPlatform=WAP";

        Map<String, String> map= new HashMap<>();
        map.put("accountID", accountId);
        map.put("jumpUrl","https://xuelele.10155.com/course/" + courseId);
        map.put("payType","CU");
        map.put("payId",payId);
        map.put("productType","0");
        map.put("id",courseId);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(map, headers);

        response = restTemplate.postForEntity( url, request , Map.class );
        retMap = response.getBody();
        data = (Map)retMap.get("data");
        url = (String)data.get("url");

//        map.put("payId",payId);
        ResponseEntity<String> pageResp = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        String retStr = pageResp.getBody();
        Map capMap = new HashMap();
        Document doc = Jsoup.parse(retStr);
        Elements e = doc.getElementsByTag("script").eq(2);
        for (Element element : e) {
            String[] els = element.data().toString().split("var");
            for (String variable : els) {
                if (variable.contains("=")) {
                    String[] kvp = variable.split("=");
                    String key = kvp[0].trim();
                    String val = kvp[1].split(";")[0].replaceAll("'","");
                    capMap.put(key, StringEscapeUtils.unescapeJava(val.trim()));
                }

            }
        }

        url = "https://cap.chinaunicom.cn/cap/productquality/";
        map= new HashMap<>();
        map.put("PRODUCT_ID", (String)capMap.get("productId"));
        map.put("PRODUCT_NAME",(String)capMap.get("productName"));

        request = new HttpEntity<>(map, headers);
        Long initTime = System.currentTimeMillis();
        response = restTemplate.postForEntity( url, request , Map.class );

        url = "https://cap.chinaunicom.cn/cap/statistics/init";
        map = new HashMap<>();
        map.put("APPOS","3");
        map.put("DEVICE_ID","h5");
        map.put("DEVICE_TYPE","Mozilla/5.0 (iPhone; CPU iPhone OS 12_1_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Mobile/15E148 Safari/604.1");
        map.put("INITTIME","" + initTime);
        map.put("AUTH_NO",(String)capMap.get("authNo"));
        map.put("PRODUCT_ID",(String)capMap.get("productId"));
        map.put("SERVICE_ID",(String)capMap.get("serviceId"));
        map.put("PRODUCT_NAME",(String)capMap.get("productName"));
        map.put("PRODUCT_PRICE",(String)capMap.get("productPrice"));
        map.put("SEQUENCE_ID",initTime + (String)capMap.get("clientId") + StringUtils.rightPad("" + (int)Math.floor(Math.random() * 10000),4,'0'));
        map.put("VERSION","1.0.0");

        request = new HttpEntity<>(map, headers);
        response = restTemplate.postForEntity( url, request , Map.class );

        url = "https://cap.chinaunicom.cn/cap/sms/";

        map = new HashMap<>();
        map.put("CLIENT","3");
        map.put("CLIENT_ID",(String)capMap.get("clientId"));
        map.put("AUTH_NO",(String)capMap.get("authNo"));
        map.put("CLIENT_SECRET",(String)capMap.get("sign"));
        map.put("SERVICE_ID",(String)capMap.get("serviceId"));
        map.put("PRODUCT_NAME",(String)capMap.get("productName"));
        map.put("PRODUCT_PRICE",(String)capMap.get("productPrice"));
        map.put("PRODUCT_TYPE",(String)capMap.get("productType"));
        map.put("VERSION","1.0.0");

        request = new HttpEntity<>(map, headers);
        response = restTemplate.postForEntity( url, request , Map.class );

        url = "https://cap.chinaunicom.cn/cap/statistics/sms";

        retMap = new HashMap();
        retMap.put("authNo",(String)capMap.get("authNo"));
        retMap.put("clientId",(String)capMap.get("clientId"));
        retMap.put("clientSecret",(String)capMap.get("sign"));
        retMap.put("orderId",(String)capMap.get("orderId"));
        retMap.put("sessionId",sessionId);
        retMap.put("accountId",accountId);
        retMap.put("productType",(String)capMap.get("productType"));
        retMap.put("productBase","0");
        return retMap;
    }

    public boolean pay(String sessionId, String authNo, String checkCode, String clientId, String clientSecret, String productBase, String productType, String accountId,String orderId) throws Exception {
        String url = "https://cap.chinaunicom.cn/cap/tokens/";
        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.COOKIE,new ArrayList<String>(){{
            add("PHPSESSID=" + sessionId);
        }});
        headers.setAccept(new ArrayList<MediaType>(){{
            add(MediaType.APPLICATION_JSON_UTF8);
        }});
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map map = new HashMap<>();
        map.put("CHECK_CODE",checkCode);
        map.put("CLIENT_ID",clientId);
        map.put("AUTH_NO",authNo);
        map.put("CLIENT_SECRET",clientSecret);
        map.put("SERVICE_ID",accountId);
        map.put("PRODUCT_BASE",productBase);
        map.put("PRODUCT_TYPE",productType);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity( url, request , Map.class );
        Map respMap = response.getBody();
        Map rspMap = (Map)respMap.get("RSP");
        String token = (String)((Map)((List)rspMap.get("DATA")).get(0)).get("TOKEN");

        url = "https://open.10155.com/confirm/updateToken?token=" + token + "&callNumber=" + accountId + "&orderId=" + orderId + "&rspCode=0000&rspDesc=success";
        headers = new HttpHeaders();
        headers.put(HttpHeaders.COOKIE,new ArrayList<String>(){{
            add("PHPSESSID=" + sessionId);
        }});
        headers.setAccept(new ArrayList<MediaType>(){{
            add(MediaType.APPLICATION_JSON_UTF8);
        }});
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        request = new HttpEntity<>( headers);
        response = restTemplate.exchange( url,HttpMethod.POST, request , Map.class );
        respMap = response.getBody();

        url = "https://m.xuelele.10155.com/api/V1/Order/payReturn?orderId=" + orderId;
        headers = new HttpHeaders();
        headers.put(HttpHeaders.COOKIE,new ArrayList<String>(){{
            add("PHPSESSID=" + sessionId);
        }});
        headers.setAccept(new ArrayList<MediaType>(){{
            add(MediaType.TEXT_HTML);
        }});

        HttpEntity entity = new HttpEntity(headers);

        ResponseEntity<String> strResp = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        String retStr = strResp.getBody();
        Document doc = Jsoup.parse(retStr);
        String capOrder = "";
        Elements e = doc.getElementsByTag("script").eq(1);
        for (Element element : e) {
            String[] els = element.data().toString().split("var");
            for (String variable : els) {
                if (variable.contains("=")) {
                    String[] kvp = variable.split("=");
                    String key = kvp[0].trim();
                    String val = kvp[1].split(";")[0].replaceAll("\"","");
                    if(StringUtils.equalsAnyIgnoreCase(key,"orderId")){
                        capOrder = val;
                        break;
                    }
                }

            }
        }

        if(StringUtils.isNotBlank(capOrder)){
            JobParameter param = new JobParameter();
            param.setJobName(capOrder);
            param.setJobGroup("JobGroup");
//            param.setCronExpression("0/5 * * * * ?");
            param.setCallBackUrl(callBackUrl);
            param.setDescription(orderId);
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
            return true;
        }else{
            return false;
        }

    }
}
