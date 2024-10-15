package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderInfoFeignClient orderInfoFeignClient;

	@Override
	public Integer getOrderStatus(Long orderId) {
		return orderInfoFeignClient.getOrderStatus(orderId).getData();
	}
}