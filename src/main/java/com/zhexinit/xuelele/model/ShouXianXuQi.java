package com.zhexinit.xuelele.model;

import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Entity("shouxianxuqi")
public class ShouXianXuQi {
    @Id
    private String id;
    private String customerName;
    private String insuranceCode;
    private String returning;
    private String settleAccounts;
    private String castSlipCode;
    private String shouldPayMoney;
    private String slipYear;
    private String actualPay;
    private String receipt;
    private String endDateTime;
    private String effective;
    private String continuePayDateTime;
    private String withdraw;
    private String insuranceNameAttr;
    private String slipCode;
    private String insuranceMoney;
    private String receive;
    private String checkState;
    private String shouldPayDateTime;
    private String companyNameAttr;
    private String guaranteeFee;
    private String quit;
    private String slipCodeStop;
}
