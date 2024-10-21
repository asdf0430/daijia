package com.atguigu.daijia.coupon.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.coupon.service.CouponInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "优惠券活动接口管理")
@RestController
@RequestMapping(value="/coupon/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponInfoController {

	@Autowired
	private CouponInfoService couponInfoService;

	@Operation(summary = "领取优惠券")
	@GetMapping("/receive/{customerId}/{couponId}")
	public Result<Boolean> receive(@PathVariable Long customerId, @PathVariable Long couponId) {
		return Result.ok(couponInfoService.receive(customerId, couponId));
	}
}

