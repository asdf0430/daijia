package com.atguigu.daijia.driver.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.LocationUtil;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderFeeForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.atguigu.daijia.model.form.rules.RewardRuleRequestForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import com.atguigu.daijia.rules.client.ProfitsharingRuleFeignClient;
import com.atguigu.daijia.rules.client.RewardRuleFeignClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
@Slf4j
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderInfoFeignClient orderInfoFeignClient;
	@Autowired
	private NewOrderFeignClient newOrderFeignClient;
	@Autowired
	private MapFeignClient mapFeignClient;
	@Autowired
	private ThreadPoolExecutor threadPoolExecutor;
	@Autowired
	private LocationFeignClient locationFeignClient;
	@Autowired
	private RewardRuleFeignClient rewardRuleFeignClient;
	@Autowired
	private ProfitsharingRuleFeignClient profitsharingRuleFeignClient;
	@Autowired
	private FeeRuleFeignClient feeRuleFeignClient;

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

		//获取账单和分账数据，封装到vo里面
		OrderBillVo orderBillVo = null;
		OrderProfitsharingVo orderProfitsharingVo = null;
		//判断
		if(orderInfo.getStatus() >= OrderStatus.END_SERVICE.getStatus()) {
			//账单信息
			orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();

			//分账信息
			orderProfitsharingVo = orderInfoFeignClient.getOrderProfitsharing(orderId).getData();
		}

		OrderInfoVo orderInfoVo = new OrderInfoVo();
		orderInfoVo.setOrderId(orderId);
		BeanUtils.copyProperties(orderInfo,orderInfoVo);
		orderInfoVo.setOrderBillVo(orderBillVo);
		orderInfoVo.setOrderProfitsharingVo(orderProfitsharingVo);
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

	@Override
	public Boolean startDrive(StartDriveForm startDriveForm) {

		return orderInfoFeignClient.startDrive(startDriveForm).getData();
	}

	@Override
	public PageVo findDriverOrderPage(Long driverId, Long page, Long limit) {
		return orderInfoFeignClient.findDriverOrderPage(driverId,page,limit).getData();
	}

	@Override
	@SneakyThrows
	public Boolean endDrive(OrderFeeForm orderFeeForm) {
		//1.获取订单信息
		CompletableFuture<OrderInfo> orderInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
			OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderFeeForm.getOrderId()).getData();
			return orderInfo;
		}, threadPoolExecutor);

		//2.防止刷单，计算司机的经纬度与代驾的终点经纬度是否在2公里范围内
		CompletableFuture<OrderServiceLastLocationVo> orderServiceLastLocationVoCompletableFuture = CompletableFuture.supplyAsync((() -> {
			OrderServiceLastLocationVo orderServiceLastLocationVo = locationFeignClient.getOrderServiceLastLocation(orderFeeForm.getOrderId()).getData();
			return orderServiceLastLocationVo;
		}), threadPoolExecutor);

		//合并
		CompletableFuture.allOf(orderInfoCompletableFuture,
				orderServiceLastLocationVoCompletableFuture
		).join();

		//获取数据
		OrderInfo orderInfo = orderInfoCompletableFuture.get();
		//2.1.判断刷单
		OrderServiceLastLocationVo orderServiceLastLocationVo = orderServiceLastLocationVoCompletableFuture.get();
		//司机的位置与代驾终点位置的距离
		double distance = LocationUtil.getDistance(orderInfo.getEndPointLatitude().doubleValue(), orderInfo.getEndPointLongitude().doubleValue(), orderServiceLastLocationVo.getLatitude().doubleValue(), orderServiceLastLocationVo.getLongitude().doubleValue());
		if(distance > SystemConstant.DRIVER_START_LOCATION_DISTION) {
			throw new MyException(ResultCodeEnum.DRIVER_END_LOCATION_DISTION_ERROR);
		}

		//3.计算订单实际里程
		CompletableFuture<BigDecimal> realDistanceCompletableFuture = CompletableFuture.supplyAsync(() -> {
			BigDecimal realDistance = locationFeignClient.calculateOrderRealDistance(orderFeeForm.getOrderId()).getData();
			log.info("结束代驾，订单实际里程：{}", realDistance);
			return realDistance;
		}, threadPoolExecutor);


		//4.计算代驾实际费用
		CompletableFuture<FeeRuleResponseVo> feeRuleResponseVoCompletableFuture = realDistanceCompletableFuture
				.thenApplyAsync((realDistance)->{ //realDistanceCompletableFuture的结果进行下一步操作
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
			return feeRuleResponseVo;
		});

		//5.计算系统奖励
		//5.1.获取订单数
		CompletableFuture<Long> orderNumCompletableFuture = CompletableFuture.supplyAsync(() -> {
			String startTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 00:00:00";
			String endTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 24:00:00";
			Long orderNum = orderInfoFeignClient.getOrderNumByTime(startTime, endTime).getData();
			return orderNum;
		}, threadPoolExecutor);
		//5.2.封装参数
		CompletableFuture<RewardRuleResponseVo> rewardRuleResponseVoCompletableFuture = orderNumCompletableFuture.thenApplyAsync((orderNum)->{
			RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
			rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
			rewardRuleRequestForm.setOrderNum(orderNum);
			//5.3.执行
			RewardRuleResponseVo rewardRuleResponseVo = rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm).getData();
			log.info("结束代驾，系统奖励：{}", JSON.toJSONString(rewardRuleResponseVo));
			return rewardRuleResponseVo;
		});

		//6.计算分账信息
		CompletableFuture<ProfitsharingRuleResponseVo> profitsharingRuleResponseVoCompletableFuture = feeRuleResponseVoCompletableFuture.thenCombineAsync(orderNumCompletableFuture, (feeRuleResponseVo, orderNum)->{
			ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
			profitsharingRuleRequestForm.setOrderAmount(feeRuleResponseVo.getTotalAmount());
			profitsharingRuleRequestForm.setOrderNum(orderNum);
			ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitsharingRuleFeignClient.calculateOrderProfitsharingFee(profitsharingRuleRequestForm).getData();
			log.info("结束代驾，分账信息：{}", JSON.toJSONString(profitsharingRuleResponseVo));
			return profitsharingRuleResponseVo;
		});
		CompletableFuture.allOf(orderInfoCompletableFuture,
				realDistanceCompletableFuture,
				feeRuleResponseVoCompletableFuture,
				orderNumCompletableFuture,
				rewardRuleResponseVoCompletableFuture,
				profitsharingRuleResponseVoCompletableFuture
		).join();

		//获取执行结果
		BigDecimal realDistance = realDistanceCompletableFuture.get();
		FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoCompletableFuture.get();
		RewardRuleResponseVo rewardRuleResponseVo = rewardRuleResponseVoCompletableFuture.get();
		ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitsharingRuleResponseVoCompletableFuture.get();

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
	public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
		return orderInfoFeignClient.sendOrderBillInfo(orderId, driverId).getData();
	}

}