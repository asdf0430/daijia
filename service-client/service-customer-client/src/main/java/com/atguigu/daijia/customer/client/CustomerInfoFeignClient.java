package com.atguigu.daijia.customer.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author zm
 */
@FeignClient(value = "service-customer")
public interface CustomerInfoFeignClient {

	@GetMapping("/customer/info/login/{code}")
	Result<Long> login(@PathVariable(value = "code")String code);

	/**
	 * 获取客户登录信息
	 * @param customerId
	 * @return
	 */
	@GetMapping("/customer/info/getCustomerLoginInfo/{customerId}")
	Result<CustomerLoginVo> getCustomerLoginInfo(@PathVariable("customerId") Long customerId);

	@PostMapping("/customer/info/updateWxPhoneNumber")
	Result<Boolean> updateWxPhoneNumber(@RequestBody UpdateWxPhoneForm updateWxPhoneForm);


}