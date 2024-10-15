package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {

	@Autowired
	private DriverInfoFeignClient driverInfoFeignClient;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	//登录
	@Override
	public String login(String code) {
		//远程调用，得到司机id
		Result<Long> longResult = driverInfoFeignClient.login(code);
		//判断
		Integer resultCode = longResult.getCode();
		if (resultCode != 200) {
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}

		// 获取用户id
		Long driverId = longResult.getData();
		if (driverId == null) {
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}

		//token字符串
		String token = UUID.randomUUID().toString().replaceAll("-","");
		//放到redis，设置过期时间
		stringRedisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
				driverId.toString(),
				RedisConstant.USER_LOGIN_KEY_TIMEOUT,
				TimeUnit.SECONDS);
		return token;
	}

	//司机认证信息
	@Override
	public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
		Result<DriverAuthInfoVo> authInfoVoResult = driverInfoFeignClient.getDriverAuthInfo(driverId);
		return authInfoVoResult.getData();
	}

	//更新司机认证信息
	@Override
	public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
		Result<Boolean> booleanResult = driverInfoFeignClient.UpdateDriverAuthInfo(updateDriverAuthInfoForm);
		return booleanResult.getData();
	}

	//创建司机人脸模型
	@Override
	public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
		Result<Boolean> booleanResult = driverInfoFeignClient.creatDriverFaceModel(driverFaceModelForm);
		return booleanResult.getData();
	}

}