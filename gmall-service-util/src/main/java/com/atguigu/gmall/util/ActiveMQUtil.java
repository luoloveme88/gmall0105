package com.atguigu.gmall.util;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

import javax.jms.ConnectionFactory;

public class ActiveMQUtil {

    private PooledConnectionFactory pooledConnectionFactory;

    public void initPool(String brokerUrl){
        ActiveMQConnectionFactory  activeMQConnectionFactory= new ActiveMQConnectionFactory(brokerUrl);
        pooledConnectionFactory= new PooledConnectionFactory(activeMQConnectionFactory);
        pooledConnectionFactory.setReconnectOnException(true);
        //
        pooledConnectionFactory.setMaxConnections(5);
        pooledConnectionFactory.setExpiryTimeout(10000);
    }

    public ConnectionFactory getConectionFactory(){
        return pooledConnectionFactory;
    }
}
