package com.atguigu.daijia.common.aop;/*
 *@title LoginAspect
 *@description
 *@author admin
 *@version 1.0
 *@create 2024/10/8 下午4:50
 */

import com.atguigu.daijia.common.annotation.LoginAno;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Component
@Aspect
public class LoginAspect {
	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Around("execution(* com.atguigu.daijia.*.controller.*.*(..)) && @annotation(loginAno)")
	public Object login(ProceedingJoinPoint joinPoint, LoginAno loginAno) throws Throwable {
		//获取request对象
		RequestAttributes attribute= RequestContextHolder.getRequestAttributes();
		ServletRequestAttributes attributes= (ServletRequestAttributes) attribute;
		HttpServletRequest request= Objects.requireNonNull(attributes).getRequest();

		//获取token
		String token = request.getHeader("token");

		//判空
		if(StringUtils.isBlank(token)){
			throw new MyException(ResultCodeEnum.FAIL_AUTH);
		}
		//查询redis
		String userId = stringRedisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);

		//查询对应useId，放入ThreadLocal
		if(StringUtils.isBlank(userId)){
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}
		AuthContextHolder.setUserId(Long.valueOf(userId));
		//执行业务方法
		return joinPoint.proceed();
	}
}
