package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.order.*;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.order.mapper.OrderBillMapper;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderProfitsharingMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.atguigu.daijia.order.service.OrderMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author zm
 */
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

	@Autowired
	private OrderInfoMapper orderInfoMapper;

	@Autowired
	private OrderStatusLogMapper orderStatusLogMapper;

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private RedissonClient redissonClient;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private OrderMonitorService orderMonitorService;

	@Autowired
	private OrderBillMapper orderBillMapper;

	@Autowired
	private OrderProfitsharingMapper orderProfitsharingMapper;

	// 乘客下单
	@Override
	public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
		// order_info添加订单数据
		OrderInfo orderInfo = new OrderInfo();
		BeanUtils.copyProperties(orderInfoForm, orderInfo);
		// 订单号
		String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
		orderInfo.setOrderNo(orderNo);
		// 订单状态
		orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
		orderInfoMapper.insert(orderInfo);

		this.sendDelayMessage(orderInfo.getId());
		// 记录日志
		this.log(orderInfo.getId(), orderInfo.getStatus());

		// 向redis添加标识
		// 接单标识，标识不存在了说明不在等待接单状态了
		redisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK,
				"0", RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);
		return orderInfo.getId();
	}

	// 根据订单id获取订单状态
	@Override
	public Integer getOrderStatus(Long orderId) {
		// sql语句： select status from order_info where id=?
		LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OrderInfo::getId, orderId);
		wrapper.select(OrderInfo::getStatus);
		// 调用mapper方法
		OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
		// 订单不存在
		if (orderInfo == null) {
			return OrderStatus.NULL_ORDER.getStatus();
		}
		return orderInfo.getStatus();
	}

	// 司机端查找当前订单
	@Override
	public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
		// 封装条件
		LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OrderInfo::getDriverId, driverId);
		Integer[] statusArray = {
				OrderStatus.ACCEPTED.getStatus(),
				OrderStatus.DRIVER_ARRIVED.getStatus(),
				OrderStatus.UPDATE_CART_INFO.getStatus(),
				OrderStatus.START_SERVICE.getStatus(),
				OrderStatus.END_SERVICE.getStatus()
		};
		wrapper.in(OrderInfo::getStatus, statusArray);
		wrapper.orderByDesc(OrderInfo::getId);
		wrapper.last(" limit 1");
		OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
		// 封装到vo
		CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
		if (null != orderInfo) {
			currentOrderInfoVo.setStatus(orderInfo.getStatus());
			currentOrderInfoVo.setOrderId(orderInfo.getId());
			currentOrderInfoVo.setIsHasCurrentOrder(true);
		} else {
			currentOrderInfoVo.setIsHasCurrentOrder(false);
		}
		return currentOrderInfoVo;
	}

	// 司机抢单
	@Transactional(rollbackFor = Exception.class)
	@Override
	public Boolean robNewOrder(Long driverId, Long orderId) {
		//抢单成功或取消订单，都会删除该key，redis判断，减少数据库压力
		if(Boolean.FALSE.equals(redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK))) {
			//抢单失败
			throw new MyException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
		}

		// 初始化分布式锁，创建一个RLock实例
		RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);
		try {
			/**
			 * TryLock是一种非阻塞式的分布式锁，实现原理：Redis的SETNX命令
			 * 参数：
			 *     waitTime：等待获取锁的时间
			 *     leaseTime：加锁的时间
			 */
			boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME,RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
			//获取到锁
			if (flag){
				//二次判断，防止重复抢单
				if(Boolean.FALSE.equals(redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK))) {
					//抢单失败
					throw new MyException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
				}

				//修改订单状态
				//update order_info set status = 2, driver_id = #{driverId} where id = #{id}
				//修改字段
				OrderInfo orderInfo = new OrderInfo();
				orderInfo.setId(orderId);
				orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
				orderInfo.setAcceptTime(new Date());
				orderInfo.setDriverId(driverId);
				int rows = orderInfoMapper.updateById(orderInfo);
				if(rows != 1) {
					//抢单失败
					throw new MyException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
				}

				//记录日志
				this.log(orderId, orderInfo.getStatus());

				//删除redis订单标识
				redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
			}
		} catch (InterruptedException e) {
			//抢单失败
			throw new MyException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
		} finally {
			if(lock.isLocked()) {
				lock.unlock();
			}
		}
		return true;
	}

	//乘客端查找当前订单
	@Override
	public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
		//封装条件
		//乘客id
		LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OrderInfo::getCustomerId,customerId);

		//各种状态
		Integer[] statusArray = {
				OrderStatus.ACCEPTED.getStatus(),
				OrderStatus.DRIVER_ARRIVED.getStatus(),
				OrderStatus.UPDATE_CART_INFO.getStatus(),
				OrderStatus.START_SERVICE.getStatus(),
				OrderStatus.END_SERVICE.getStatus(),
				OrderStatus.UNPAID.getStatus()
		};
		wrapper.in(OrderInfo::getStatus,statusArray);

		//获取最新一条记录
		wrapper.orderByDesc(OrderInfo::getId);
		wrapper.last(" limit 1");

		//调用方法
		OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);

		//封装到CurrentOrderInfoVo
		CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
		if(orderInfo != null) {
			currentOrderInfoVo.setOrderId(orderInfo.getId());
			currentOrderInfoVo.setStatus(orderInfo.getStatus());
			currentOrderInfoVo.setIsHasCurrentOrder(true);
		} else {
			currentOrderInfoVo.setIsHasCurrentOrder(false);
		}
		return currentOrderInfoVo;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
		LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(OrderInfo::getId, orderId);
		queryWrapper.eq(OrderInfo::getDriverId, driverId);

		OrderInfo updateOrderInfo = new OrderInfo();
		updateOrderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
		updateOrderInfo.setArriveTime(new Date());
		//只能更新自己的订单
		int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
		if(row == 1) {
			//记录日志
			this.log(orderId, OrderStatus.DRIVER_ARRIVED.getStatus());
		} else {
			throw new MyException(ResultCodeEnum.UPDATE_ERROR);
		}
		return true;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
		LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(OrderInfo::getId, updateOrderCartForm.getOrderId());
		queryWrapper.eq(OrderInfo::getDriverId, updateOrderCartForm.getDriverId());

		OrderInfo updateOrderInfo = new OrderInfo();
		BeanUtils.copyProperties(updateOrderCartForm, updateOrderInfo);
		updateOrderInfo.setStatus(OrderStatus.UPDATE_CART_INFO.getStatus());
		//只能更新自己的订单
		int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
		if(row == 1) {
			//记录日志
			this.log(updateOrderCartForm.getOrderId(), OrderStatus.UPDATE_CART_INFO.getStatus());
		} else {
			throw new MyException(ResultCodeEnum.UPDATE_ERROR);
		}
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean startDrive(StartDriveForm startDriveForm) {
		LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(OrderInfo::getOrderNo, startDriveForm.getOrderId());
		queryWrapper.eq(OrderInfo::getDriverId, startDriveForm.getDriverId());
		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
		orderInfo.setStartServiceTime(new Date());
		int row = orderInfoMapper.update(orderInfo, queryWrapper);
		if(row == 1) {
			//记录日志
			this.log(startDriveForm.getOrderId(), OrderStatus.START_SERVICE.getStatus());
		} else {
			throw new MyException(ResultCodeEnum.UPDATE_ERROR);
		}
		return true;
	}

	@Override
	public Long getOrderNumByTime(String startTime, String endTime) {
		LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.ge(OrderInfo::getStartServiceTime, startTime);
		queryWrapper.lt(OrderInfo::getStartServiceTime, endTime);
		Long count = orderInfoMapper.selectCount(queryWrapper);
		return count;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public Boolean endDrive(UpdateOrderBillForm updateOrderBillForm) {
		//更新订单信息
		LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(OrderInfo::getId, updateOrderBillForm.getOrderId());
		queryWrapper.eq(OrderInfo::getDriverId, updateOrderBillForm.getDriverId());
		//更新字段
		OrderInfo updateOrderInfo = new OrderInfo();
		updateOrderInfo.setStatus(OrderStatus.END_SERVICE.getStatus());
		updateOrderInfo.setRealAmount(updateOrderBillForm.getTotalAmount());
		updateOrderInfo.setFavourFee(updateOrderBillForm.getFavourFee());
		updateOrderInfo.setEndServiceTime(new Date());
		updateOrderInfo.setRealDistance(updateOrderBillForm.getRealDistance());
		//只能更新自己的订单
		int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
		if(row == 1) {
			//记录日志
			this.log(updateOrderBillForm.getOrderId(), OrderStatus.END_SERVICE.getStatus());

			//插入实际账单数据
			OrderBill orderBill = new OrderBill();
			BeanUtils.copyProperties(updateOrderBillForm, orderBill);
			orderBill.setOrderId(updateOrderBillForm.getOrderId());
			orderBill.setPayAmount(orderBill.getTotalAmount());
			orderBillMapper.insert(orderBill);

			//插入分账信息数据
			OrderProfitsharing orderProfitsharing = new OrderProfitsharing();
			BeanUtils.copyProperties(updateOrderBillForm, orderProfitsharing);
			orderProfitsharing.setOrderId(updateOrderBillForm.getOrderId());
			orderProfitsharing.setRuleId(updateOrderBillForm.getProfitsharingRuleId());
			orderProfitsharing.setStatus(1);
			orderProfitsharingMapper.insert(orderProfitsharing);
		} else {
			throw new MyException(ResultCodeEnum.UPDATE_ERROR);
		}
		return true;
	}

	//获取乘客订单分页列表
	@Override
	public PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
		IPage<OrderListVo> pageInfo =  orderInfoMapper.selectCustomerOrderPage(pageParam,customerId);
		return new PageVo<>(pageInfo.getRecords(),pageInfo.getPages(),pageInfo.getTotal());
	}

	@Override
	public PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
		IPage<OrderListVo> pageInfo =  orderInfoMapper.selectDriverOrderPage(pageParam,driverId);
		return new PageVo<>(pageInfo.getRecords(),pageInfo.getPages(),pageInfo.getTotal());
	}

	@Override
	public OrderBillVo getOrderBillInfo(Long orderId) {
		LambdaQueryWrapper<OrderBill> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OrderBill::getOrderId,orderId);
		OrderBill orderBill = orderBillMapper.selectOne(wrapper);

		OrderBillVo orderBillVo = new OrderBillVo();
		BeanUtils.copyProperties(orderBill,orderBillVo);
		return orderBillVo;
	}

	@Override
	public OrderProfitsharingVo getOrderProfitsharing(Long orderId) {
		LambdaQueryWrapper<OrderProfitsharing> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OrderProfitsharing::getOrderId,orderId);
		OrderProfitsharing orderProfitsharing = orderProfitsharingMapper.selectOne(wrapper);

		OrderProfitsharingVo orderProfitsharingVo = new OrderProfitsharingVo();
		BeanUtils.copyProperties(orderProfitsharing,orderProfitsharingVo);
		return orderProfitsharingVo;
	}

	@Override
	public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
		//更新订单信息
		LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(OrderInfo::getId, orderId);
		queryWrapper.eq(OrderInfo::getDriverId, driverId);
		//更新字段
		OrderInfo updateOrderInfo = new OrderInfo();
		updateOrderInfo.setStatus(OrderStatus.UNPAID.getStatus());
		//只能更新自己的订单
		int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
		if(row == 1) {
			return true;
		} else {
			throw new MyException(ResultCodeEnum.UPDATE_ERROR);
		}
	}

	@Override
	public OrderPayVo getOrderPayVo(String orderNo, Long customerId) {
		OrderPayVo orderPayVo = orderInfoMapper.selectOrderPayVo(orderNo,customerId);
		if(orderPayVo != null) {
			String content = orderPayVo.getStartLocation() + " 到 "+orderPayVo.getEndLocation();
			orderPayVo.setContent(content);
		}
		return orderPayVo;
	}

	@Override
	public Boolean updateOrderPayStatus(String orderNo) {
		//1 根据订单编号查询，判断订单状态
		LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OrderInfo::getOrderNo,orderNo);
		OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
		if(orderInfo == null || orderInfo.getStatus() == OrderStatus.PAID.getStatus()) {
			return true;
		}

		//2 更新状态
		LambdaQueryWrapper<OrderInfo> updateWrapper = new LambdaQueryWrapper<>();
		updateWrapper.eq(OrderInfo::getOrderNo,orderNo);

		OrderInfo updateOrderInfo = new OrderInfo();
		updateOrderInfo.setStatus(OrderStatus.PAID.getStatus());
		updateOrderInfo.setPayTime(new Date());

		int rows = orderInfoMapper.update(updateOrderInfo, updateWrapper);

		if(rows == 1) {
			return true;
		} else {
			throw new MyException(ResultCodeEnum.UPDATE_ERROR);
		}
	}

	@Override
	public OrderRewardVo getOrderRewardFee(String orderNo) {
		//根据订单编号查询订单表
		OrderInfo orderInfo =
				orderInfoMapper.selectOne(
						new LambdaQueryWrapper<OrderInfo>()
								.eq(OrderInfo::getOrderNo, orderNo)
								.select(OrderInfo::getId,OrderInfo::getDriverId));

		//根据订单id查询系统奖励表
		OrderBill orderBill =
				orderBillMapper.selectOne(new LambdaQueryWrapper<OrderBill>()
						.eq(OrderBill::getOrderId, orderInfo.getId())
						.select(OrderBill::getRewardFee));

		//封装到vo里面
		OrderRewardVo orderRewardVo = new OrderRewardVo();
		orderRewardVo.setOrderId(orderInfo.getId());
		orderRewardVo.setDriverId(orderInfo.getDriverId());
		orderRewardVo.setRewardFee(orderBill.getRewardFee());
		return orderRewardVo;
	}

	@Override
	//生成订单之后，发送延迟消息
	public void sendDelayMessage(Long orderId) {
		try{
			//1 创建队列
			RBlockingQueue<Object> blockingDeque = redissonClient.getBlockingQueue("queue_cancel");

			//2 把创建队列放到延迟队列里面
			RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);

			//3 发送消息到延迟队列里面
			//设置过期时间
			delayedQueue.offer(orderId.toString(),15,TimeUnit.MINUTES);

		}catch (Exception e) {
			e.printStackTrace();
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}
	}

	//调用方法取消订单
	@Override
	public void orderCancel(long orderId) {
		//orderId查询订单信息
		OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
		//判断
		if(orderInfo.getStatus()==OrderStatus.WAITING_ACCEPT.getStatus()) {
			//修改订单状态：取消状态
			orderInfo.setStatus(OrderStatus.CANCEL_ORDER.getStatus());
			int rows = orderInfoMapper.updateById(orderInfo);
			if(rows == 1) {
				//删除接单标识

				redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
			}
		}
	}

	public void log(Long orderId, Integer status) {
		OrderStatusLog orderStatusLog = new OrderStatusLog();
		orderStatusLog.setOrderId(orderId);
		orderStatusLog.setOrderStatus(status);
		orderStatusLog.setOperateTime(new Date());
		orderStatusLogMapper.insert(orderStatusLog);
	}
}