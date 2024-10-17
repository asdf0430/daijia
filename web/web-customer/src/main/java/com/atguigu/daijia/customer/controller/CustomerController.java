package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.annotation.LoginAno;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.customer.service.impl.CustomerServiceImpl;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @author admin
 */
@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerController {
	@Autowired
	private CustomerServiceImpl customerService;
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	@Autowired
	private OrderService orderService;

	@Operation(summary = "小程序授权登录")
	@GetMapping("/login/{code}")
	public Result<String> wxLogin(@PathVariable String code) {
		return Result.ok(customerService.login(code));
	}


	@Operation(summary = "获取客户登录信息")
	@GetMapping("/getCustomerLoginInfo")
	@LoginAno
	public Result<CustomerLoginVo> getCustomerLoginInfo(@RequestHeader(value = "token") String token) {
		Long userId = AuthContextHolder.getUserId();
		if (userId == null) {
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}
		return Result.ok(customerService.getCustomerLoginInfo(userId));
	}

	@Operation(summary = "更新用户微信手机号")
	@LoginAno
	@PostMapping("/updateWxPhone")
	public Result updateWxPhone(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
		updateWxPhoneForm.setCustomerId(AuthContextHolder.getUserId());
		return Result.ok(customerService.updateWxPhoneNumber(updateWxPhoneForm));
	}

	@Operation(summary = "根据订单id获取司机基本信息")
	@LoginAno
	@GetMapping("/getDriverInfo/{orderId}")
	public Result<DriverInfoVo> getDriverInfo(@PathVariable Long orderId) {
		Long customerId = AuthContextHolder.getUserId();
		return Result.ok(orderService.getDriverInfo(orderId, customerId));
	}


}

