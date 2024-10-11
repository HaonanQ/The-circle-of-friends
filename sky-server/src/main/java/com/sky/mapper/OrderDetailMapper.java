package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    void batchInsert(List<OrderDetail> orderDetails);
    @Select("SELECT * FROM order_detail WHERE order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);
}
