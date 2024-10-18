package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.annotation.LoginAno;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {

	@Autowired
	private OrderService orderService;

	@Operation(summary = "乘客端查找当前订单")
	@LoginAno
	@GetMapping("/searchCustomerCurrentOrder")
	public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder() {
		Long customerId = AuthContextHolder.getUserId();
		return Result.ok(orderService.searchCustomerCurrentOrder(customerId));
	}

	@Operation(summary = "预估订单数据")
	@LoginAno
	@PostMapping("/expectOrder")
	public Result<ExpectOrderVo> expectOrder(@RequestBody ExpectOrderForm expectOrderForm) {
		return Result.ok(orderService.expectOrder(expectOrderForm));
	}

	@Operation(summary = "乘客下单")
	@LoginAno
	@PostMapping("/submitOrder")
	public Result<Long> submitOrder(@RequestBody SubmitOrderForm submitOrderForm) {
		submitOrderForm.setCustomerId(AuthContextHolder.getUserId());
		return Result.ok(orderService.submitOrder(submitOrderForm));
	}

	@Operation(summary = "查询订单状态")
	@LoginAno
	@GetMapping("/getOrderStatus/{orderId}")
	public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
		return Result.ok(orderService.getOrderStatus(orderId));
	}

	@Operation(summary = "获取订单信息")
	@LoginAno
	@GetMapping("/getOrderInfo/{orderId}")
	public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId) {
		Long customerId = AuthContextHolder.getUserId();
		return Result.ok(orderService.getOrderInfo(orderId, customerId));
	}

	@Operation(summary = "司机赶往代驾起始点：获取订单经纬度位置")
	@LoginAno
	@GetMapping("/getCacheOrderLocation/{orderId}")
	public Result<OrderLocationVo> getOrderLocation(@PathVariable Long orderId) {
		return Result.ok(orderService.getCacheOrderLocation(orderId));
	}

	@Operation(summary = "计算最佳驾驶线路")
	@LoginAno
	@PostMapping("/calculateDrivingLine")
	public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
		return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
	}

	@Operation(summary = "代驾服务：获取订单服务最后一个位置信息")
	@LoginAno
	@GetMapping("/getOrderServiceLastLocation/{orderId}")
	public Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId) {
		return Result.ok(orderService.getOrderServiceLastLocation(orderId));
	}
}
