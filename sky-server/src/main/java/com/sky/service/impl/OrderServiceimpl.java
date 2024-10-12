package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceimpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetail;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //处理业务异常
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);
        if(shoppingCarts == null || shoppingCarts.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //插入1条订单记录
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        order.setUserId(userId);
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()+BaseContext.getCurrentId()));
        orderMapper.insert(order);
        //插入1条订单详情记录
        List<OrderDetail> orderDetails = new ArrayList<>();
        for(ShoppingCart sc : shoppingCarts){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(sc,orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetails.add(orderDetail);
        }
        //批量插入订单详情记录
        orderDetail.batchInsert(orderDetails);
        //清空购物车
        shoppingCartMapper.deleteByuserid(userId);
        //返回订单提交信息
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderTime(order.getOrderTime())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .build();
        return orderSubmitVO;
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if ((jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID"))) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    @Override
    public PageResult pagequery(int pagenum, int pagesize, Integer status) {
        PageHelper.startPage(pagenum, pagesize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        Page<Orders> page = orderMapper.pagequery(ordersPageQueryDTO);
        List<OrderVO> orderVOS = new ArrayList<>();
        if(page!= null && page.size() > 0){
            for(Orders orders : page){
                Long orderId = orders.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetails);
                orderVOS.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(),orderVOS);
    }

    @Override
    public OrderVO orderDetails(Long id) {
        Orders order = orderMapper.getById(id);
        List<OrderDetail> orderDetails = orderDetail.getByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order,orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    @Override
    public void cancel(Long id) throws Exception{
        // 根据订单id查询订单
        Orders orderd = orderMapper.getById(id);
        // 校验订单是否存在
        if (orderd == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if(orderd.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 订单状态修改为 6已取消
        Orders orders = new Orders();
        orders.setId(id);
        // 订单处于待接单状态下取消，需要进行退款
        if (orderd.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    orderd.getNumber(), //商户订单号
//                    orderd.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消订单");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        // 根据订单id查询订单
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCarts = orderDetails.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart,"id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());
        // 插入购物车
        shoppingCartMapper.insertBatch(shoppingCarts);

    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page  = orderMapper.pagequery(ordersPageQueryDTO);
        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> list  = getOrderVOList(page);
        return new PageResult(page.getTotal(),list);
    }

    @Override
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
               .status(Orders.CONFIRMED)
               .build();
        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders orderdb = orderMapper.getById(ordersRejectionDTO.getId());
        // 订单只有存在且状态为2（待接单）才可以拒单
        if(orderdb == null && !orderdb.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Integer payStatus = orderdb.getPayStatus();
        if(payStatus == Orders.PAID){
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    orderdb.getNumber(),
//                    orderdb.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));

        }
        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders order = new Orders();
        order.setId(ordersRejectionDTO.getId());
        order.setStatus(Orders.CANCELLED);
        order.setCancelReason(ordersRejectionDTO.getRejectionReason());
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    @Override
    public void shopcancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders orderdb = orderMapper.getById(ordersCancelDTO.getId());
        Integer payStatus = orderdb.getPayStatus();
        if(payStatus == Orders.PAID){
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    orderdb.getNumber(),
//                    orderdb.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));

        }
        Orders order = new Orders();
        order.setId(ordersCancelDTO.getId());
        order.setStatus(Orders.CANCELLED);
        order.setCancelReason(ordersCancelDTO.getCancelReason());
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    @Override
    public void delivery(Long id) {
        Orders orderdb = orderMapper.getById(id);
        // 订单只有存在且状态为3（待派送）才可以派送
        if(orderdb == null && !(orderdb.getStatus() == Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 订单状态修改为 4派送中
        Orders order = new Orders();
        order.setId(orderdb.getId());
        order.setStatus(Orders.DELIVERY_IN_PROGRESS);
//        order.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(order);

    }

    @Override
    public void complete(Long id) {
        Orders orderdb = orderMapper.getById(id);
        // 订单只有存在且状态为4（派送中）才可以完成
        if(orderdb == null && !(orderdb.getStatus() == Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 订单状态修改为 5已完成
        Orders order = new Orders();
        order.setId(orderdb.getId());
        order.setStatus(Orders.COMPLETED);
        order.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    public List<OrderVO> getOrderVOList(Page<Orders> page){
        List<OrderVO> list = new ArrayList<>();
        List<Orders> ordersList = page.getResult();
        if(!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderdish = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderdish);
                list.add(orderVO);
            }

        }
        return list;
    }
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }
}
