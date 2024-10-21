package com.atguigu.daijia.coupon.client;

import com.atguigu.daijia.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(value = "service-coupon")
public interface CouponFeignClient {

	/**
	 * 领取优惠券
	 * @param customerId
	 * @param couponId
	 * @return
	 */
	@GetMapping("/coupon/info/receive/{customerId}/{couponId}")
	Result<Boolean> receive(@PathVariable("customerId") Long customerId, @PathVariable("couponId") Long couponId);
}