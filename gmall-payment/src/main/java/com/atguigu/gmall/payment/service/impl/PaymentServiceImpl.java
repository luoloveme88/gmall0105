package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public void savePayment(PaymentInfo paymentInfo) {
        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void  updatePayment(PaymentInfo paymentInfo) throws JMSException {
        //throw new Exception("ss");
        Example e = new Example(PaymentInfo.class);
        e.createCriteria().andEqualTo("orderSn", paymentInfo.getOrderSn());
        // paymentInfoMapper.updateByExampleSelective(paymentInfo, e);
        paymentInfoMapper.updateByExampleSelective(paymentInfo, e);

        ConnectionFactory conectionFactory = activeMQUtil.getConectionFactory();
        Connection connection = conectionFactory.createConnection();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue queue = session.createQueue("PAYHMENT_SUCCESS_QUEUE");
        MessageProducer producer = session.createProducer(queue);
        MapMessage message = new ActiveMQMapMessage();
        message.setString("out_trade_no", paymentInfo.getOrderSn());
        producer.send(message);
        session.commit();
        connection.close();



    }
}
