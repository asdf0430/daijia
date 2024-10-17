package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderInfoFeignClient orderInfoFeignClient;
	@Autowired
	private NewOrderFeignClient newOrderFeignClient;
	@Autowired
	private MapFeignClient mapFeignClient;

	@Override
	public Integer getOrderStatus(Long orderId) {
		return orderInfoFeignClient.getOrderStatus(orderId).getData();
	}

	@Override
	public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
		return newOrderFeignClient.findNewOrderQueueData(driverId).getData();
	}

	@Override
	public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
		return orderInfoFeignClient.searchDriverCurrentOrder(driverId).getData();
	}

	@Override
	public Boolean robNewOrder(Long driverId, Long orderId) {
		return orderInfoFeignClient.robNewOrder(driverId,orderId).getData();
	}

	@Override
	public OrderInfoVo getOrderInfo(Long orderId, Long driverId) {
		OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
		if(!Objects.equals(orderInfo.getDriverId(), driverId)) {
			throw new MyException(ResultCodeEnum.ILLEGAL_REQUEST);
		}
		OrderInfoVo orderInfoVo = new OrderInfoVo();
		orderInfoVo.setOrderId(orderId);
		BeanUtils.copyProperties(orderInfo,orderInfoVo);
		return orderInfoVo;
	}

	//计算最佳驾驶线路
	@Override
	public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
		return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
	}

	@Override
	public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
		return orderInfoFeignClient.driverArriveStartLocation(orderId, driverId).getData();
	}

	@Override
	public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
		return orderInfoFeignClient.updateOrderCart(updateOrderCartForm).getData();
	}


}