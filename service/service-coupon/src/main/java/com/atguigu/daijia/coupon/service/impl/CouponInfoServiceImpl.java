package com.atguigu.daijia.coupon.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.coupon.mapper.CouponInfoMapper;
import com.atguigu.daijia.coupon.mapper.CustomerCouponMapper;
import com.atguigu.daijia.coupon.service.CouponInfoService;
import com.atguigu.daijia.model.entity.coupon.CouponInfo;
import com.atguigu.daijia.model.entity.coupon.CustomerCoupon;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {


	@Autowired
	private CouponInfoMapper couponInfoMapper;

	@Autowired
	private CustomerCouponMapper customerCouponMapper;

	@Autowired
	private RedissonClient redissonClient;

	//领取优惠卷
	@Override
	public Boolean receive(Long customerId, Long couponId) {
		//1 couponId查询优惠卷信息
		//判断如果优惠卷不存在
		CouponInfo couponInfo = couponInfoMapper.selectById(couponId);
		if(couponInfo == null) {
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}

		//2 判断优惠卷是否过期
		if(couponInfo.getExpireTime().before(new Date())) {
			throw new MyException(ResultCodeEnum.COUPON_EXPIRE);
		}

		//3 检查库存，发行数量 和 领取数量
		if(couponInfo.getPublishCount() != 0 &&
				couponInfo.getReceiveCount() == couponInfo.getPublishCount()) {
			throw new MyException(ResultCodeEnum.COUPON_LESS);
		}
		RLock lock = null;
		try {
			lock = redissonClient.getLock(RedisConstant.COUPON_LOCK + customerId);
			boolean flag = lock.tryLock(RedisConstant.COUPON_LOCK_WAIT_TIME,
					RedisConstant.COUPON_LOCK_LEASE_TIME, TimeUnit.SECONDS);
			if(flag) {
				//4 检查每个人限制领取数量
				if(couponInfo.getPerLimit() > 0) {
					//统计当前客户已经领取优惠卷数量
					LambdaQueryWrapper<CustomerCoupon> wrapper = new LambdaQueryWrapper<>();
					wrapper.eq(CustomerCoupon::getCouponId,couponId);
					wrapper.eq(CustomerCoupon::getCustomerId,customerId);
					Long count = customerCouponMapper.selectCount(wrapper);
					//判断
					if(count >= couponInfo.getPerLimit()) {
						throw new MyException(ResultCodeEnum.COUPON_USER_LIMIT);
					}
				}

				//5 领取优惠卷
				//5.1 更新领取数量
				int row = couponInfoMapper.updateReceiveCount(couponId);

				//5.2 添加领取记录
				this.saveCustomerCoupon(customerId,couponId,couponInfo.getExpireTime());

				return true;
			}
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(lock != null) {
				lock.unlock();
			}
		}
		return true;
	}

	private void saveCustomerCoupon(Long customerId, Long couponId, Date expireTime) {
		CustomerCoupon customerCoupon = new CustomerCoupon();
		customerCoupon.setCouponId(couponId);
		customerCoupon.setCustomerId(customerId);
		customerCoupon.setExpireTime(expireTime);
		customerCoupon.setReceiveTime(new Date());
		customerCoupon.setStatus(1);
		customerCouponMapper.insert(customerCoupon);
	}
}
