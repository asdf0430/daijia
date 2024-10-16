package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderInfoFeignClient orderInfoFeignClient;
	@Autowired
	private NewOrderFeignClient newOrderFeignClient;

	@Override
	public Integer getOrderStatus(Long orderId) {
		return orderInfoFeignClient.getOrderStatus(orderId).getData();
	}

	@Override
	public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
		return newOrderFeignClient.findNewOrderQueueData(driverId).getData();
	}
}