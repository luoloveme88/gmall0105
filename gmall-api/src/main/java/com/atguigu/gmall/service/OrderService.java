package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsOrder;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;

public interface OrderService {

    String checkTradeCode(String memberId, String tradeCode);

    String genTradeCode(String memberId);

    void saveOrder(OmsOrder omsOrder);

    OmsOrder getOrderByOutTradeNo(String outTradeNo);

    void updateOrderByNo(String outTradeNo);
}
