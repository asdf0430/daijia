package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.model.entity.order.OrderMonitor;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import com.atguigu.daijia.order.mapper.OrderMonitorMapper;
import com.atguigu.daijia.order.service.OrderMonitorService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderMonitorServiceImpl extends ServiceImpl<OrderMonitorMapper, OrderMonitor> implements OrderMonitorService {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private OrderMonitorMapper orderMonitorMapper;

	@Override
	public Boolean saveOrderMonitorRecord(OrderMonitorRecord orderMonitorRecord) {
		mongoTemplate.save(orderMonitorRecord);
		return true;
	}

	@Override
	public OrderMonitor getOrderMonitor(Long orderId) {
		return orderMonitorMapper.selectById(orderId);
	}

	@Override
	public Boolean updateOrderMonitor(OrderMonitor orderMonitor) {
		orderMonitorMapper.updateById(orderMonitor);
		return null;
	}
}
