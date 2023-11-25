package com.yupi.springbootinit.bizmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MqInitMain {
//    用于创建测试成语用到的交换机和队列
    public static void main(String[] args) {
//1.创建建工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.203.128");
        factory.setUsername("root");
        factory.setPassword("123456");
// 2.创建连接
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String EXCHANGE_NAME = "code_exchange";
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            String queueName = "code_queue";
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, EXCHANGE_NAME, "my_routingKey");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
