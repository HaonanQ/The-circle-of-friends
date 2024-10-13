package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;
    @Scheduled(cron = "0 * * * * ?")
    public void setStatusorderTimeout(){
        log.info("开始执行定时任务，设置订单超时状态");
        List<Orders> orders = orderMapper.updateStatusByTimeout(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));
        if(orders!= null && orders.size() > 0){
            for(Orders order : orders){
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时未支付");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void setStatusPaisong(){
        List<Orders> orders = orderMapper.updateStatusByTimeout(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));
        if(orders!= null && orders.size() > 0){
            for(Orders order : orders){
                order.setStatus(Orders.COMPLETED);
                order.setCancelReason("派送完成");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }
}
