package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.annotation.LoginAno;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

	@Operation(summary = "查询司机新订单数据")
	@LoginAno
	@GetMapping("/findNewOrderQueueData")
	public Result<List<NewOrderDataVo>> findNewOrderQueueData() {
		Long driverId = AuthContextHolder.getUserId();
		return Result.ok(orderService.findNewOrderQueueData(driverId));
	}

	@Operation(summary = "司机端查找当前订单")
	@LoginAno
	@GetMapping("/searchDriverCurrentOrder")
	public Result<CurrentOrderInfoVo> searchDriverCurrentOrder() {
		Long driverId = AuthContextHolder.getUserId();
		return Result.ok(orderService.searchDriverCurrentOrder(driverId));
	}

	@Operation(summary = "司机抢单")
	@LoginAno
	@GetMapping("/robNewOrder/{orderId}")
	public Result<Boolean> robNewOrder(@PathVariable Long orderId) {
		Long driverId = AuthContextHolder.getUserId();
		return Result.ok(orderService.robNewOrder(driverId, orderId));
	}

	@Operation(summary = "获取订单账单详细信息")
	@LoginAno
	@GetMapping("/getOrderInfo/{orderId}")
	public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId) {
		Long driverId = AuthContextHolder.getUserId();
		return Result.ok(orderService.getOrderInfo(orderId, driverId));
	}

	@Operation(summary = "计算最佳驾驶线路")
	@LoginAno
	@PostMapping("/calculateDrivingLine")
	public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
		return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
	}

	@Operation(summary = "司机到达代驾起始地点")
	@LoginAno
	@GetMapping("/driverArriveStartLocation/{orderId}")
	public Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId) {
		Long driverId = AuthContextHolder.getUserId();
		return Result.ok(orderService.driverArriveStartLocation(orderId, driverId));
	}

	@Operation(summary = "更新代驾车辆信息")
	@LoginAno
	@PostMapping("/updateOrderCart")
	public Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm) {
		Long driverId = AuthContextHolder.getUserId();
		updateOrderCartForm.setDriverId(driverId);
		return Result.ok(orderService.updateOrderCart(updateOrderCartForm));
	}
}