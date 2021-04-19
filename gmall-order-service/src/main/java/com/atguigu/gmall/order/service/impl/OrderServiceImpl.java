package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Reference
    CartService cartService;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public String checkTradeCode(String memberId, String tradeCode) {
        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:" + memberId + ":tradeCode";


            //String tradeCodeFromCache = jedis.get(tradeKey);// 使用lua脚本在发现key的同时将key删除，防止并发订单攻击
            //对比防重删令牌
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));

            if (eval != null && eval != 0) {
                jedis.del(tradeKey);
                return "success";
            } else {
                return "fail";
            }
        } finally {
            jedis.close();
        }

    }

    @Override
    public String genTradeCode(String memberId) {

        Jedis jedis = redisUtil.getJedis();

        String tradeKey = "user:" + memberId + ":tradeCode";

        String tradeCode = UUID.randomUUID().toString();

        jedis.setex(tradeKey, 60 * 15, tradeCode);

        jedis.close();

        return tradeCode;
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {

        // 保存订单表
        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();
        // 保存订单详情
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
            // 删除购物车数据
            // cartService.delCart();
        }
    }

    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {
        Example e = new Example(OmsOrder.class);
        e.createCriteria().andEqualTo("orderSn", outTradeNo);
        OmsOrder omsOrder = omsOrderMapper.selectOneByExample(e);
        return omsOrder;
    }

    @Override
    public void updateOrderByNo(String outTradeNo) {
        Example e = new Example(OmsOrder.class);
        e.createCriteria().andEqualTo("orderSn", outTradeNo);
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setStatus("1");
        omsOrderMapper.updateByExampleSelective(omsOrder, e);

        ConnectionFactory conectionFactory = activeMQUtil.getConectionFactory();
        Connection connection = null;
        Session session = null;
        try {
            connection = conectionFactory.createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(queue);
            MapMessage message = new ActiveMQMapMessage();
            producer.send(message);

            session.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            try {

                session.rollback();


            } catch (JMSException exc) {
                exc.printStackTrace();
            } finally {
                try {
                    connection.close();
                } catch (JMSException exc) {
                    exc.printStackTrace();
                }
            }

        }

    }


}
