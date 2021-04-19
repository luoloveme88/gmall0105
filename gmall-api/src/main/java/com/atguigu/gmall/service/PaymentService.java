package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import javax.jms.JMSException;

public interface PaymentService {
    void savePayment(PaymentInfo paymentInfo);

    void updatePayment(PaymentInfo paymentInfo) throws Exception;
}
