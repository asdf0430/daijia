package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

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

		//TODO 查询附近司机

		return orderInfoResult.getData();
	}

	//查询订单状态
	@Override
	public Integer getOrderStatus(Long orderId) {
		Result<Integer> integerResult = orderInfoFeignClient.getOrderStatus(orderId);
		return integerResult.getData();
	}
}
