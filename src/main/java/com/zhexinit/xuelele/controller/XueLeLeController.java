package com.zhexinit.xuelele.controller;

import com.zhexinit.xuelele.model.Error;
import com.zhexinit.xuelele.model.GuaranteeslipByCode;
import com.zhexinit.xuelele.model.ShouQi;
import com.zhexinit.xuelele.model.ShouXianShouQi;
import com.zhexinit.xuelele.utils.JackJson;
import lombok.Data;
import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping
public class XueLeLeController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    Datastore datastore;

    @Autowired
    private RestTemplate restTemplate;
    int limit =50;


    @GetMapping("/test")
    public Object test(String type,String startDate,String endDate) throws Exception{
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Param param = new Param();
        param.setEndDate(endDate);
        param.setStartDate(startDate);
        param.setType(type);
        List<Map> list = Collections.synchronizedList(new ArrayList());

        Map map = fetch(param,1);
        list.add(map);
        int cnt = (int)map.get("totalCount");
        int pageTotal = cnt/limit;
        pageTotal = cnt%limit == 0?pageTotal:(pageTotal+1);
        System.out.println(pageTotal);
        if(pageTotal>1){
            for(int i=2;i<=pageTotal;i++){
                FetchTask fetchTask = new FetchTask(param,i,list);
                executorService.execute(fetchTask);
            }
            executorService.shutdown();
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for(Map restMap:list){
            if(null != restMap)
                System.out.println(((List)restMap.get("data")).size());
        }
        return null;
    }

    private Map fetch(Param param,int page){
        MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
        System.out.println("startdate:" + param.getStartDate() + ",enddate:" + param.getEndDate() + ",page:" + page);
        String url = "http://test-zj.sxfwxt.com:8099/lifeInsurance/getGuaranteeslipList";
        try {
            HttpHeaders headers = new HttpHeaders();

            headers.setAccept(new ArrayList<MediaType>() {{
                add(MediaType.APPLICATION_JSON_UTF8);
            }});

            headers.set("X-Requested-With", "XMLHttpRequest");
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            paramMap.add("parmData", "{\"companyCode\":null,\"slipOrCastSlipCode\":\"\",\"customerName\":\"\",\"userName\":\"\",\"flowState\":\"0\",\"startDate\":\""+param.getStartDate()+"\",\"endDate\":\""+param.getEndDate()+"\",\"proxyCompanyCode\":2,\"authOrgCode\":\"\",\"pagesize\":1}");
            paramMap.add("page", page + "");
            paramMap.add("start", ((page - 1) * limit) + "");
            paramMap.add("limit", limit + "");
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(paramMap, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map respMap = response.getBody();
            List<Map> list = (List)respMap.get("data");
            for(Map m:list) {
                String str = JackJson.fromObjectToJson(m);
                ShouXianShouQi shouXianShouQi = JackJson.fromJsonToObject(str, ShouXianShouQi.class);
                detail(shouXianShouQi);
//                datastore.save(shouXianShouQi);
            }
            return respMap;
        }catch (Exception ex){
            Error error = new Error();
            error.setParam(JackJson.fromObjectToJson(param) );
            error.setPage(page);
            error.setUrl(url);
            datastore.save(error);
            System.out.println("startdate:" + param.getStartDate() + ",enddate:" + param.getEndDate() + ",page:" + page + " 出错");
        }
        return null;
    }



    private Map detail(ShouXianShouQi shouXianShouQi){
        MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
        System.out.println("CastSlipCode:" + shouXianShouQi.getCastSlipCode() );
        String url = "http://test-zj.sxfwxt.com:8099/lifeInsurance/getGuaranteeslipByCode/2/" + shouXianShouQi.getCastSlipCode();
        try {
            HttpHeaders headers = new HttpHeaders();

            headers.setAccept(new ArrayList<MediaType>() {{
                add(MediaType.APPLICATION_JSON_UTF8);
            }});

            headers.set("X-Requested-With", "XMLHttpRequest");
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(paramMap, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET,request,Map.class);
            Map respMap = response.getBody();
            Map m = (Map)respMap.get("data");
            String str = JackJson.fromObjectToJson(m);
            GuaranteeslipByCode guaranteeslipByCode = JackJson.fromJsonToObject(str, GuaranteeslipByCode.class);
            shouXianShouQi.setGuaranteeslipByCode(guaranteeslipByCode);
            datastore.save(shouXianShouQi);
            return respMap;
        }catch (Exception ex){
            ex.printStackTrace();
            Error error = new Error();
            error.setParam("CastSlipCode:" + shouXianShouQi.getCastSlipCode() );
            error.setUrl(url);
            datastore.save(error);
            System.out.println("CastSlipCode:" + shouXianShouQi.getCastSlipCode() + " 出错");
        }
        return null;
    }
    @Data
    class Param{
        private String type;
        private String startDate;
        private String endDate;
    }

    class FetchTask implements Runnable{

        private Param param;
        private int page;
        private List<Map> dataList = null;

        public FetchTask(Param param,int page,List<Map> dataList){
            this.param = param;
            this.page = page;
            this.dataList = dataList;
        }
        @Override
        public void run() {
            dataList.add(fetch(param,page));
        }
    }

    public static void main(String[] args) {
        String str = "proxyCompanyCode: null,orgType: 0,authOrgCode: null,createTime: 2019-03-20 09:00:04.0,updateTime: 2019-03-20 09:00:04.0,flag: null,page: 1,start: 0,limit: 15,castSlipCode: SX151829581,slipCode: ,companyCode: null,insuranceName: 工银安盛人寿安康e生医疗保险,insuranceCode: MMD,insuranceMoney: 2000000.0000,timeLimit: 一年,paymentTimeLimit: 1年交,isSingleRow: 0,slipYear: 1,formalitiesFee: null,commissionFee: null,insurancePremium: 418.0000,otherPremium: 0.0000,guaranteeFee: 41.8000,feeProportion: 0.0800,fee: 33.4400,commissionProportion: 0.0500,commission: 20.9000,ageIndex: 0,settleAccounts: 0,subtotal: 418.0000";
        String[] tmp1 = str.split(",");
        for(String t:tmp1){
            String s = t.split(":")[0];
            System.out.println("private String " + s + ";");
        }
    }
}
