package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.annotation.LoginAno;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;

/**
 * @author admin
 */
@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

	@Autowired
	private MapFeignClient mapFeignClient;
	@Autowired
	private FeeRuleFeignClient feeRuleFeignClient;
	@Autowired
	private OrderInfoFeignClient orderInfoFeignClient;
	@Autowired
	private NewOrderFeignClient newOrderFeignClient;
	@Autowired
	private DriverInfoFeignClient driverInfoFeignClient;
	@Autowired
	private LocationFeignClient locationFeignClient;

	@Override
	public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {
		//获取驾驶线路
		CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
		BeanUtils.copyProperties(expectOrderForm,calculateDrivingLineForm);
		Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
		DrivingLineVo drivingLineVo = drivingLineVoResult.getData();
		//获取订单费用
		FeeRuleRequestForm calculateOrderFeeForm = new FeeRuleRequestForm();
		calculateOrderFeeForm.setDistance(drivingLineVo.getDistance());
		calculateOrderFeeForm.setStartTime(new Date());
		calculateOrderFeeForm.setWaitMinute(0);
		Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(calculateOrderFeeForm);
		FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

		return new ExpectOrderVo(drivingLineVo,feeRuleResponseVo);
	}

	@Override
	public Long submitOrder(SubmitOrderForm submitOrderForm) {
		CalculateDrivingLineForm calculateDrivingLineForm=new CalculateDrivingLineForm();
		BeanUtils.copyProperties(submitOrderForm,calculateDrivingLineForm);
		Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
		DrivingLineVo drivingLineVo = drivingLineVoResult.getData();

		//获取订单费用
		FeeRuleRequestForm calculateOrderFeeForm = new FeeRuleRequestForm();
		calculateOrderFeeForm.setDistance(drivingLineVo.getDistance());
		calculateOrderFeeForm.setStartTime(new Date());
		calculateOrderFeeForm.setWaitMinute(0);
		Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(calculateOrderFeeForm);
		FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

		OrderInfoForm orderInfoForm = new OrderInfoForm();
		BeanUtils.copyProperties(submitOrderForm,orderInfoForm);
		orderInfoForm.setExpectDistance(drivingLineVo.getDistance());
		orderInfoForm.setExpectAmount(feeRuleResponseVo.getTotalAmount());
		Result<Long> orderInfoResult = orderInfoFeignClient.saveOrderInfo(orderInfoForm);
		Long orderId = orderInfoResult.getData();
		//任务调度：查询附近可以接单司机
		NewOrderTaskVo newOrderDispatchVo = new NewOrderTaskVo();
		newOrderDispatchVo.setOrderId(orderId);
		newOrderDispatchVo.setStartLocation(orderInfoForm.getStartLocation());
		newOrderDispatchVo.setStartPointLongitude(orderInfoForm.getStartPointLongitude());
		newOrderDispatchVo.setStartPointLatitude(orderInfoForm.getStartPointLatitude());
		newOrderDispatchVo.setEndLocation(orderInfoForm.getEndLocation());
		newOrderDispatchVo.setEndPointLongitude(orderInfoForm.getEndPointLongitude());
		newOrderDispatchVo.setEndPointLatitude(orderInfoForm.getEndPointLatitude());
		newOrderDispatchVo.setExpectAmount(orderInfoForm.getExpectAmount());
		newOrderDispatchVo.setExpectDistance(orderInfoForm.getExpectDistance());
		newOrderDispatchVo.setExpectTime(drivingLineVo.getDuration());
		newOrderDispatchVo.setFavourFee(orderInfoForm.getFavourFee());
		newOrderDispatchVo.setCreateTime(new Date());
		//远程调用
		Long jobId = newOrderFeignClient.addAndStartTask(newOrderDispatchVo).getData();
		//返回订单id
		return orderId;
	}

	//查询订单状态
	@Override
	public Integer getOrderStatus(Long orderId) {
		Result<Integer> integerResult = orderInfoFeignClient.getOrderStatus(orderId);
		return integerResult.getData();
	}

	//乘客查找当前订单
	@Override
	public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
		return orderInfoFeignClient.searchCustomerCurrentOrder(customerId).getData();
	}

	@Override
	public OrderInfoVo getOrderInfo(Long orderId, Long customerId) {
		OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
		//判断
		if(orderInfo.getCustomerId() != customerId) {
			throw new MyException(ResultCodeEnum.ILLEGAL_REQUEST);
		}

		OrderInfoVo orderInfoVo = new OrderInfoVo();
		orderInfoVo.setOrderId(orderId);
		BeanUtils.copyProperties(orderInfo,orderInfoVo);
		return orderInfoVo;
	}

	@Override
	public DriverInfoVo getDriverInfo(Long orderId, Long customerId) {
		//根据订单id获取订单信息
		OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
		if(orderInfo.getCustomerId() != customerId) {
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}
		return driverInfoFeignClient.getDriverInfo(orderInfo.getDriverId()).getData();
	}

	@Override
	public OrderLocationVo getCacheOrderLocation(Long orderId) {
		return locationFeignClient.getCacheOrderLocation(orderId).getData();
	}

	@Override
	public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
		return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
	}

	@Override
	public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
		return locationFeignClient.getOrderServiceLastLocation(orderId).getData();
	}


}
