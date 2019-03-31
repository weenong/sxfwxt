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
public class ShouXianShouQiController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    Datastore datastore;

    @Autowired
    private RestTemplate restTemplate;
    int limit =600;

    @GetMapping("/shouqibypage")
    public Object byPage(Param param,int page) throws Exception{
        Map map = fetch(param,page);
        return map;
    }

    @GetMapping("/shouqi")
    public Object test(String startDate,String endDate) throws Exception{
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Param param = new Param();
        param.setEndDate(endDate);
        param.setStartDate(startDate);
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
//            ExecutorService executorService = Executors.newFixedThreadPool(1);
            List<ShouXianShouQi> shouXianShouQiList = new ArrayList<>();
            for(Map m:list) {
                String str = JackJson.fromObjectToJson(m);
                ShouXianShouQi shouXianShouQi = JackJson.fromJsonToObject(str, ShouXianShouQi.class);
                detail(shouXianShouQi);
                shouXianShouQiList.add(shouXianShouQi);
//                DetailTask detailTask = new DetailTask(shouXianShouQi);
//                executorService.execute(detailTask);
//                datastore.save(shouXianShouQi);
            }
            datastore.save(shouXianShouQiList);
//            executorService.shutdown();
//            try {
//                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            return respMap;
        }catch (Exception ex){
            ex.printStackTrace();
            Error error = new Error();
            error.setParam(JackJson.fromObjectToJson(paramMap) );
            error.setPage(page);
            error.setUrl(url);
            datastore.save(error);
            System.out.println("startdate:" + param.getStartDate() + ",enddate:" + param.getEndDate() + ",page:" + page + " 出错");
        }
        return null;
    }



    private Map detail(ShouXianShouQi shouXianShouQi) throws Exception{
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
//            datastore.save(shouXianShouQi);
            return respMap;
        }catch (Exception ex){
            ex.printStackTrace();
            throw ex;
        }
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

    class DetailTask implements Runnable{

        private ShouXianShouQi shouXianShouQi;

        public DetailTask(ShouXianShouQi shouXianShouQi){
            this.shouXianShouQi = shouXianShouQi;
        }
        @Override
        public void run() {
            try {
                detail(shouXianShouQi);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String str = "customerName: 王岭,insuranceCode: 3151,returning: 1,settleAccounts: 1,castSlipCode: 1001011206431708,shouldPayMoney: 6000.00,slipYear: 3,actualPay: 6000.00,receipt: 1,endDateTime: 2019-03-08,effective: 1,continuePayDateTime: 2019-01-07,insuranceNameAttr: 盛世年年,slipCode: 1402001710000028,insuranceMoney: 1850.46,receive: 1,checkState: 2,shouldPayDateTime: 2019-01-07,companyNameAttr: 国华人寿,guaranteeFee: 0.0000";
        String[] tmp1 = str.split(",");
        for(String t:tmp1){
            String s = t.split(":")[0];
            System.out.println("private String " + s + ";");
        }
    }
}
