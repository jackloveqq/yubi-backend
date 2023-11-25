package com.yupi.springbootinit.bizmq;


import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.utils.ExcelUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;
    @Resource
    private AiManager aiManager;


    //    下面代码等于消费者接受
//    channel.basicconsume(queue,true,xiaoyudeliver,connsumertag->)
//    注解简化异常化
    @SneakyThrows
//    指定监听队列名称,设置信息为手动
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
//     @Header(AmqpHeaders.DELIVERY_TAG) long deliverTag用于消息头获取投递标签
//     每一个消息都会分配唯一投递标签,用于标识消息在通道中的投递状态和顺序
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliverTag) {
        log.info("reciveMessage message={}", message);
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliverTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");

        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            channel.basicNack(deliverTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }
        //         修改图标任务状态
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
//            跟新失败就是数据库出问题
        if (!b) {
            channel.basicNack(deliverTag, false, false);
            handleChartUpdateError(chart.getId(), "更新状态失败");
            return;
        }
//            调用ai
        String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, buildUserInput(chart));
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            channel.basicNack(deliverTag, false, false);
            handleChartUpdateError(chart.getId(), "ai生成失败");
            return;
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        Chart updateChartResult = new Chart();
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setId(chart.getId());
        updateChartResult.setStatus("succeed");
        boolean b1 = chartService.updateById(updateChartResult);
        if (!b1) {
            channel.basicNack(deliverTag, false, false);
            handleChartUpdateError(chart.getId(), "图标更新失败");
        }
//       投机标记是一个数字标记,用于手动确认
        channel.basicAck(deliverTag, false);
    }

    private String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }




    private void handleChartUpdateError(long chartId,String execMessage){
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage("execMessage");
        boolean b = chartService.updateById(updateChartResult);
        if (!b){
            log.error("更新图标失败"+chartId+","+execMessage);
        }

    }
}
