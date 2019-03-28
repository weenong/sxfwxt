package com.zhexinit.xuelele.model;

import com.zhexinit.xuelele.controller.XueLeLeController;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Data
@Entity("error")
public class Error {
    @Id
    private String id;
    private String url;
    private String param;
    private int page;
}
