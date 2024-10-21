package com.atguigu.daijia.customer.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.LocationUtil;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.map.client.WxPayFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderFeeForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.payment.CreateWxPaymentForm;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.atguigu.daijia.model.form.rules.RewardRuleRequestForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderBillVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderPayVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import com.atguigu.daijia.rules.client.ProfitsharingRuleFeignClient;
import com.atguigu.daijia.rules.client.RewardRuleFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
	@Autowired
	private RewardRuleFeignClient rewardRuleFeignClient;
	@Autowired
	private ProfitsharingRuleFeignClient profitsharingRuleFeignClient;
	@Autowired
	private WxPayFeignClient wxPayFeignClient;
	@Autowired
	private CustomerInfoFeignClient customerInfoFeignClient;

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

		//获取司机信息
		DriverInfoVo driverInfoVo = null;
		Long driverId = orderInfo.getDriverId();
		if(driverId != null) {
			driverInfoVo = driverInfoFeignClient.getDriverInfo(driverId).getData();
		}

		//获取账单信息
		OrderBillVo orderBillVo = null;
		if(orderInfo.getStatus() >= OrderStatus.UNPAID.getStatus()) {
			orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
		}

		OrderInfoVo orderInfoVo = new OrderInfoVo();
		orderInfoVo.setOrderId(orderId);
		BeanUtils.copyProperties(orderInfo,orderInfoVo);
		orderInfoVo.setOrderBillVo(orderBillVo);
		orderInfoVo.setDriverInfoVo(driverInfoVo);
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

	@Override
	public Boolean endDrive(OrderFeeForm orderFeeForm) {
		//1.获取订单信息
		OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderFeeForm.getOrderId()).getData();
		if(orderInfo.getDriverId().longValue() != orderFeeForm.getDriverId().longValue()) {
			throw new MyException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
		}

		//2.防止刷单，计算司机的经纬度与代驾的终点经纬度是否在2公里范围内
		OrderServiceLastLocationVo orderServiceLastLocationVo = locationFeignClient.getOrderServiceLastLocation(orderFeeForm.getOrderId()).getData();
		//司机的位置与代驾终点位置的距离
		double distance = LocationUtil.getDistance(orderInfo.getEndPointLatitude().doubleValue(), orderInfo.getEndPointLongitude().doubleValue(), orderServiceLastLocationVo.getLatitude().doubleValue(), orderServiceLastLocationVo.getLongitude().doubleValue());
		if(distance > SystemConstant.DRIVER_START_LOCATION_DISTION) {
			throw new MyException(ResultCodeEnum.DRIVER_END_LOCATION_DISTION_ERROR);
		}

		//3.计算订单实际里程
		BigDecimal realDistance = locationFeignClient.calculateOrderRealDistance(orderFeeForm.getOrderId()).getData();
		log.info("结束代驾，订单实际里程：{}", realDistance);

		//4.计算代驾实际费用
		FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
		feeRuleRequestForm.setDistance(realDistance);
		feeRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
		//等候时间
		Integer waitMinute = Math.abs((int) ((orderInfo.getArriveTime().getTime() - orderInfo.getAcceptTime().getTime()) / (1000 * 60)));
		feeRuleRequestForm.setWaitMinute(waitMinute);
		log.info("结束代驾，费用参数：{}", JSON.toJSONString(feeRuleRequestForm));
		FeeRuleResponseVo feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm).getData();
		log.info("费用明细：{}", JSON.toJSONString(feeRuleResponseVo));
		//订单总金额 需加上 路桥费、停车费、其他费用、乘客好处费
		BigDecimal totalAmount = feeRuleResponseVo.getTotalAmount().add(orderFeeForm.getTollFee()).add(orderFeeForm.getParkingFee()).add(orderFeeForm.getOtherFee()).add(orderInfo.getFavourFee());
		feeRuleResponseVo.setTotalAmount(totalAmount);

		//5.计算系统奖励
		//5.1.获取订单数
		String startTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 00:00:00";
		String endTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 24:00:00";
		Long orderNum = orderInfoFeignClient.getOrderNumByTime(startTime, endTime).getData();
		//5.2.封装参数
		RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
		rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
		rewardRuleRequestForm.setOrderNum(orderNum);
		//5.3.执行
		RewardRuleResponseVo rewardRuleResponseVo = rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm).getData();
		log.info("结束代驾，系统奖励：{}", JSON.toJSONString(rewardRuleResponseVo));

		//6.计算分账信息
		ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
		profitsharingRuleRequestForm.setOrderAmount(feeRuleResponseVo.getTotalAmount());
		profitsharingRuleRequestForm.setOrderNum(orderNum);
		ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitsharingRuleFeignClient.calculateOrderProfitsharingFee(profitsharingRuleRequestForm).getData();
		log.info("结束代驾，分账信息：{}", JSON.toJSONString(profitsharingRuleResponseVo));

		//7.封装更新订单账单相关实体对象
		UpdateOrderBillForm updateOrderBillForm = new UpdateOrderBillForm();
		updateOrderBillForm.setOrderId(orderFeeForm.getOrderId());
		updateOrderBillForm.setDriverId(orderFeeForm.getDriverId());
		//路桥费、停车费、其他费用
		updateOrderBillForm.setTollFee(orderFeeForm.getTollFee());
		updateOrderBillForm.setParkingFee(orderFeeForm.getParkingFee());
		updateOrderBillForm.setOtherFee(orderFeeForm.getOtherFee());
		//乘客好处费
		updateOrderBillForm.setFavourFee(orderInfo.getFavourFee());

		//实际里程
		updateOrderBillForm.setRealDistance(realDistance);
		//订单奖励信息
		BeanUtils.copyProperties(rewardRuleResponseVo, updateOrderBillForm);
		//代驾费用信息
		BeanUtils.copyProperties(feeRuleResponseVo, updateOrderBillForm);

		//分账相关信息
		BeanUtils.copyProperties(profitsharingRuleResponseVo, updateOrderBillForm);
		updateOrderBillForm.setProfitsharingRuleId(profitsharingRuleResponseVo.getProfitsharingRuleId());
		log.info("结束代驾，更新账单信息：{}", JSON.toJSONString(updateOrderBillForm));

		//8.结束代驾更新账单
		orderInfoFeignClient.endDrive(updateOrderBillForm);
		return true;
	}

	@Override
	public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
		//防止刷单，计算司机的经纬度与代驾的起始经纬度是否在1公里范围内
		OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
		OrderLocationVo orderLocationVo = locationFeignClient.getCacheOrderLocation(orderId).getData();
		//司机的位置与代驾起始点位置的距离
		double distance = LocationUtil.getDistance(orderInfo.getStartPointLatitude().doubleValue(), orderInfo.getStartPointLongitude().doubleValue(), orderLocationVo.getLatitude().doubleValue(), orderLocationVo.getLongitude().doubleValue());
		if(distance > SystemConstant.DRIVER_START_LOCATION_DISTION) {
			throw new MyException(ResultCodeEnum.DRIVER_START_LOCATION_DISTION_ERROR);
		}
		return orderInfoFeignClient.driverArriveStartLocation(orderId, driverId).getData();
	}

	@Override
	public PageVo findCustomerOrderPage(Long customerId, Long page, Long limit) {
		return orderInfoFeignClient.findCustomerOrderPage(customerId,page,limit).getData();
	}

	@Override
	public WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm) {
		//获取订单支付信息
		OrderPayVo orderPayVo = orderInfoFeignClient.getOrderPayVo(createWxPaymentForm.getOrderNo(),
				createWxPaymentForm.getCustomerId()).getData();
		//判断
		if(orderPayVo.getStatus() != OrderStatus.UNPAID.getStatus()) {
			throw new MyException(ResultCodeEnum.ILLEGAL_REQUEST);
		}

		//获取乘客和司机openid
		String customerOpenId = customerInfoFeignClient.getCustomerOpenId(orderPayVo.getCustomerId()).getData();

		String driverOpenId = driverInfoFeignClient.getDriverOpenId(orderPayVo.getDriverId()).getData();

		//封装需要数据到实体类，远程调用发起微信支付
		PaymentInfoForm paymentInfoForm = new PaymentInfoForm();
		paymentInfoForm.setCustomerOpenId(customerOpenId);
		paymentInfoForm.setDriverOpenId(driverOpenId);
		paymentInfoForm.setOrderNo(orderPayVo.getOrderNo());
		paymentInfoForm.setAmount(orderPayVo.getPayAmount());
		paymentInfoForm.setContent(orderPayVo.getContent());
		paymentInfoForm.setPayWay(1);

		WxPrepayVo wxPrepayVo = wxPayFeignClient.createWxPayment(paymentInfoForm).getData();
		return wxPrepayVo;
	}

	@Override
	public Boolean queryPayStatus(String orderNo) {
		return wxPayFeignClient.queryPayStatus(orderNo).getData();
	}

}
