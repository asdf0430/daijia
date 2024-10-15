package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.annotation.LoginAno;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
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

	//TODO 后续完善，目前假设乘客当前没有订单
	@Operation(summary = "查找乘客端当前订单")
	@LoginAno
	@GetMapping("/searchCustomerCurrentOrder")
	public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder() {
		CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
		currentOrderInfoVo.setIsHasCurrentOrder(false);
		return Result.ok(currentOrderInfoVo);
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


}