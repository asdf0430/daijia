package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author admin
 */
@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {

	@Resource
	private CustomerInfoFeignClient client;
	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
	public String login(String code) {
		// 远程调用
		Result<Long> result = client.login(code);
		// 状态码
		Integer resultCode = result.getCode();
		if (resultCode != 200) {
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}

		// 获取用户id
		Long customerId = result.getData();
		if (customerId == null) {
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}
		// 生成token
		String token = UUID.randomUUID().toString().replace("-", "");
		// 放入redis
		stringRedisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
				customerId.toString(),
				RedisConstant.USER_LOGIN_KEY_TIMEOUT,
				TimeUnit.SECONDS);
		return token;
	}

	@Override
	public CustomerLoginVo getCustomerLoginInfo(Long customerId) {
		Result<CustomerLoginVo> result = client.getCustomerLoginInfo(customerId);
		if(result.getCode() != 200) {
			throw new MyException(result.getCode(), result.getMessage());
		}
		CustomerLoginVo customerLoginVo = result.getData();
		if(null == customerLoginVo) {
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}
		return customerLoginVo;
	}

	//更新用户微信手机号
	@Override
	public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
		Result<Boolean> booleanResult = client.updateWxPhoneNumber(updateWxPhoneForm);
		return true;
	}
}
