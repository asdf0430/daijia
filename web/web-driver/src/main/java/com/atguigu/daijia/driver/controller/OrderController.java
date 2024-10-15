package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.annotation.LoginAno;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {

	@Autowired
	private OrderService orderService;

	@Operation(summary = "查询订单状态")
	@LoginAno
	@GetMapping("/getOrderStatus/{orderId}")
	public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
		return Result.ok(orderService.getOrderStatus(orderId));
	}
}