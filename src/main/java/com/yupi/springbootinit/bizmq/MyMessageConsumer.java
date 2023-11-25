package com.yupi.springbootinit.bizmq;


import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MyMessageConsumer {
//    下面代码等于消费者接受
//    channel.basicconsume(queue,true,xiaoyudeliver,connsumertag->)
//    注解简化异常化
     @SneakyThrows
//    指定监听队列名称,设置信息为手动
    @RabbitListener(queues = {"code_queue"},ackMode = "MANUAL")
//     @Header(AmqpHeaders.DELIVERY_TAG) long deliverTag用于消息头获取投递标签
//     每一个消息都会分配唯一投递标签,用于标识消息在通道中的投递状态和顺序
    public void receiveMessage (String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliverTag){
         log.info("reciveMessage message={}",message);
//         投机标记是一个数字标记,用于手动确认
         channel.basicAck(deliverTag,false);
     }
}
