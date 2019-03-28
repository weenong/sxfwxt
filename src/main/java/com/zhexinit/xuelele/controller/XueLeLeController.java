package com.zhexinit.xuelele.controller;

import com.zhexinit.xuelele.model.Error;
import com.zhexinit.xuelele.model.ShouQi;
import com.zhexinit.xuelele.utils.JackJson;
import lombok.Data;
import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
        String url = "http://test-zj.sxfwxt.com:8099/kpireportForm/getGuaranteeslipKpiList";
        try {
            System.out.println(1/0);
            HttpHeaders headers = new HttpHeaders();

            headers.setAccept(new ArrayList<MediaType>() {{
                add(MediaType.APPLICATION_JSON_UTF8);
            }});

            headers.set("X-Requested-With", "XMLHttpRequest");
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            paramMap.add("parmData", "{\"orgCode\":null,\"orgCode1\":null,\"orgCode2\":null,\"orgCode3\":null,\"companyCode\":null,\"type\":" + param.getType() + ",\"startDate\":\"" + param.getStartDate() + "\",\"endDate\":\"" + param.getEndDate() + "\",\"customerName\":\"\",\"userName\":\"\",\"dataType\":\"1\",\"proxyCompanyCode\":2,\"authOrgCode\":\"\"}");
            paramMap.add("page", page + "");
            paramMap.add("start", ((page - 1) * limit) + "");
            paramMap.add("limit", limit + "");
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(paramMap, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map respMap = response.getBody();
            List<Map> list = (List)respMap.get("data");
            for(Map m:list) {
                String str = JackJson.fromObjectToJson(m);
                datastore.save(JackJson.fromJsonToObject(str, ShouQi.class));
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

}
