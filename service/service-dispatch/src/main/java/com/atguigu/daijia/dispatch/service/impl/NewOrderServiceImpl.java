package com.atguigu.daijia.dispatch.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.dispatch.mapper.OrderJobMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.dispatch.xxl.client.XxlJobClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.dispatch.OrderJob;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {

	@Autowired
	private OrderJobMapper orderJobMapper;

	@Autowired
	private XxlJobClient xxlJobClient;

	@Autowired
	private LocationFeignClient locationFeignClient;
	@Autowired
	private OrderInfoFeignClient orderInfoFeignClient;
	@Autowired
	private RedisTemplate redisTemplate;

	//创建并启动任务调度方法
	@Override
	public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
		//1 判断当前订单是否启动任务调度
		//根据订单id查询
		LambdaQueryWrapper<OrderJob> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OrderJob::getOrderId,newOrderTaskVo.getOrderId());
		OrderJob orderJob = orderJobMapper.selectOne(wrapper);

		//2 没有启动，进行操作
		if(orderJob == null) {
			//创建并启动任务调度
			//String executorHandler 执行任务job方法
			// String param
			// String corn 执行cron表达式
			// String desc 描述信息
			Long jobId = xxlJobClient.addAndStart(
					"newOrderTask", //JobHandler中newOrderTask方法
					"",//参数
					"0 0/1 * * * ?",
					"新创建订单任务调度：" + newOrderTaskVo.getOrderId());

			//记录任务调度信息
			orderJob = new OrderJob();
			orderJob.setOrderId(newOrderTaskVo.getOrderId());
			orderJob.setJobId(jobId);
			orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));
			orderJobMapper.insert(orderJob);
		}
		return orderJob.getJobId();
	}

	@Override
	public void excuteTask(long jobId) {
		LambdaQueryWrapper<OrderJob> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OrderJob::getJobId,jobId);
		OrderJob orderJob = orderJobMapper.selectOne(wrapper);
		if(orderJob == null) {
			//不往下执行了
			return;
		}

		//2 查询订单状态，如果当前订单接单状态，继续执行。如果当前订单不是接单状态，停止任务调度
		//获取OrderJob里面对象
		String jsonString = orderJob.getParameter();
		NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(jsonString, NewOrderTaskVo.class);

		//获取orderId
		Long orderId = newOrderTaskVo.getOrderId();
		Integer status = orderInfoFeignClient.getOrderStatus(orderId).getData();
		if(status.intValue() != OrderStatus.WAITING_ACCEPT.getStatus().intValue()) {
			//停止任务调度
			xxlJobClient.stopJob(jobId);
			return;
		}

		//远程调用，搜索附近司机
		SearchNearByDriverForm searchNearByDriverForm=SearchNearByDriverForm.builder()
				.longitude(newOrderTaskVo.getStartPointLongitude())
				.latitude(newOrderTaskVo.getStartPointLatitude())
				.mileageDistance(newOrderTaskVo.getExpectDistance())
				.build();
		Result<List<NearByDriverVo>> listResult = locationFeignClient.searchNearByDriver(searchNearByDriverForm);

		//遍历司机信息，创建临时队列
		listResult.getData().forEach(nearByDriverVo -> {
			//根据订单id生成key
			String repeatKey =
					RedisConstant.DRIVER_ORDER_REPEAT_LIST+newOrderTaskVo.getOrderId();

			Boolean isMember = redisTemplate.opsForSet().isMember(repeatKey, nearByDriverVo.getDriverId());//判断redis是否存储过
			if(Boolean.FALSE.equals(isMember)) {
				//推送给满足条件的司机
				//过期时间，超过15分钟自动取消
				redisTemplate.opsForSet().add(repeatKey, nearByDriverVo.getDriverId());
				redisTemplate.expire(repeatKey,15, TimeUnit.MINUTES);

				//redis-list作为司机临时队列
				NewOrderDataVo newOrderDataVo=new NewOrderDataVo();
				BeanUtils.copyProperties(newOrderTaskVo,newOrderDataVo);
				newOrderDataVo.setDistance(nearByDriverVo.getDistance());
				String key = RedisConstant.DRIVER_ORDER_TEMP_LIST+nearByDriverVo.getDriverId();
				redisTemplate.opsForList().leftPush(key, JSONObject.toJSONString(newOrderDataVo));
				redisTemplate.expire(key,1, TimeUnit.MINUTES);
			}
		});
	}

	//获取最新订单
	@Override
	public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
		List<NewOrderDataVo> list = new ArrayList<>();
		String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
		Long size = redisTemplate.opsForList().size(key);
		if(size > 0) {
			for (int i = 0; i < size; i++) {
				String content = (String)redisTemplate.opsForList().leftPop(key);
				NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content,NewOrderDataVo.class);
				list.add(newOrderDataVo);
			}
		}
		return list;
	}

	//清空队列数据
	@Override
	public Boolean clearNewOrderQueueData(Long driverId) {
		String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
		redisTemplate.delete(key);
		return true;
	}

}