package com.zhexinit.xuelele.config;


import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * 返回RestTemplate
     * @param factory
     * @return
     */
    @Bean
    @Scope(value="prototype", proxyMode= ScopedProxyMode.TARGET_CLASS)
    public RestTemplate restTemplate(ClientHttpRequestFactory factory){
        //消息转换器，Spring Boot环境可省略，只需要添加相关依赖即可
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        messageConverters.add(new StringHttpMessageConverter(Charset.forName("UTF-8")));
        messageConverters.add(new FormHttpMessageConverter());
        messageConverters.add(new MappingJackson2XmlHttpMessageConverter());
        messageConverters.add(new MappingJackson2HttpMessageConverter());

        RestTemplate restTemplate = new RestTemplate(factory);

        restTemplate.setMessageConverters(messageConverters);
        return restTemplate;
    }

    /**
     * ClientHttpRequestFactory接口的另一种实现方式（推荐使用），即：
     * HttpComponentsClientHttpRequestFactory：底层使用Httpclient连接池的方式创建Http连接请求
     * @return
     */
    @Bean
    @Scope(value="prototype", proxyMode= ScopedProxyMode.TARGET_CLASS)
    public HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory(){
        //Httpclient连接池，长连接保持90秒
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(90, TimeUnit.SECONDS);

        //设置总连接数
        connectionManager.setMaxTotal(1000);
        //设置同路由的并发数
        connectionManager.setDefaultMaxPerRoute(1000);

        //设置header
        List<Header> headers = new ArrayList<Header>();
        headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.04"));
        headers.add(new BasicHeader("Accept-Encoding", "gzip, deflate"));
        headers.add(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3"));
        headers.add(new BasicHeader("Connection", "keep-alive"));

        //创建HttpClient
        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultHeaders(headers)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(6, true)) //设置重试次数
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy()) //设置保持长连接
                .build();

        //创建HttpComponentsClientHttpRequestFactory实例
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        //设置客户端和服务端建立连接的超时时间
        requestFactory.setConnectTimeout(300000);
        requestFactory.setReadTimeout(300000);
        //设置从连接池获取连接的超时时间，不宜过长
        requestFactory.setConnectionRequestTimeout(300000);
        //缓冲请求数据，默认为true。通过POST或者PUT大量发送数据时，建议将此更改为false，以免耗尽内存
        requestFactory.setBufferRequestBody(true);

        return requestFactory;
    }

}
