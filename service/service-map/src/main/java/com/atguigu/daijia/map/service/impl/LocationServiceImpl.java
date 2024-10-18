package com.atguigu.daijia.map.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.repository.OrderServiceLocationRepository;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.entity.map.OrderServiceLocation;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoLocation;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private DriverInfoFeignClient driverInfoFeignClient;

	@Autowired
	private OrderServiceLocationRepository orderServiceLocationRepository;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
		Point point = new Point(
				updateDriverLocationForm.getLongitude().doubleValue(),
				updateDriverLocationForm.getLatitude().doubleValue());
		redisTemplate.opsForGeo().add(
				RedisConstant.DRIVER_GEO_LOCATION,
				point,
				updateDriverLocationForm.getDriverId().toString());

		return true;
	}

	@Override
	public Boolean removeDriverLocation(Long driverId) {
		redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
		return true;
	}

	@Override
	public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
		// 定义point
		Point point = new Point(searchNearByDriverForm.getLongitude().doubleValue(),
				searchNearByDriverForm.getLatitude().doubleValue());
		// 定义距离：5km
		Distance distance = new Distance(SystemConstant.NEARBY_DRIVER_RADIUS,
				RedisGeoCommands.DistanceUnit.KILOMETERS);
		Circle circle = new Circle(point, distance);

		// 定义geo参数，设置返回结果包含内容
		RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
				.includeDistance() // 包含距离
				.includeCoordinates() // 包含坐标
				.sortAscending(); // 排列：升序
		// 查询附近信息
		GeoResults<RedisGeoCommands.GeoLocation<String>> radius = redisTemplate.opsForGeo()
				.radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);
		// 获取list集合
		List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = radius.getContent();

		List<NearByDriverVo> nearByDriverVoList = new ArrayList<>();
		if (!CollectionUtils.isEmpty(content)) {
			Iterator<GeoResult<RedisGeoCommands.GeoLocation<String>>> iterator = content.iterator();
			while (iterator.hasNext()) {
				GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult = iterator.next();
				Long id = Long.parseLong(geoResult.getContent().getName());// 获取司机id
				Result<DriverSet> driverSet = driverInfoFeignClient.getDriverSet(id);
				DriverSet data = driverSet.getData();

				BigDecimal orderDistance = data.getOrderDistance();// 获取订单里程:0-没有限制，30-三十公里之内
				// 判断接单距离
				if (orderDistance.doubleValue() != 0
						&& orderDistance.subtract(searchNearByDriverForm.getMileageDistance()).doubleValue() < 0) {
					continue;
				}
				//当前接单距离
				BigDecimal currentDistance=new BigDecimal(geoResult.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP);
				// 判断接单里程
				BigDecimal acceptDistance = data.getAcceptDistance();
				if (acceptDistance.doubleValue() != 0
						&& acceptDistance.subtract(currentDistance).doubleValue() < 0) {
					continue;
				}
				//封装数据
				NearByDriverVo nearByDriverVo = NearByDriverVo.builder()
						.driverId(id)
						.distance(currentDistance)
						.build();
				nearByDriverVoList.add(nearByDriverVo);
			}
		}
		return nearByDriverVoList;
	}

	//司机赶往代驾起始点：更新订单地址到缓存
	@Override
	public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {

		OrderLocationVo orderLocationVo = new OrderLocationVo();
		orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());
		orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());

		String key = RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId();
		redisTemplate.opsForValue().set(key,orderLocationVo);
		return true;
	}

	@Override
	public OrderLocationVo getCacheOrderLocation(Long orderId) {
		String key = RedisConstant.UPDATE_ORDER_LOCATION + orderId;
		OrderLocationVo orderLocationVo = (OrderLocationVo)redisTemplate.opsForValue().get(key);
		return orderLocationVo;
	}

	@Override
	public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {
		List<OrderServiceLocation> locations=new ArrayList<>();
		orderLocationServiceFormList.forEach(orderServiceLocationForm -> {
			OrderServiceLocation orderServiceLocation = new OrderServiceLocation();
			BeanUtils.copyProperties(orderServiceLocationForm, orderServiceLocation);
			orderServiceLocation.setId(ObjectId.get().toString());
			orderServiceLocation.setCreateTime(new Date());
			locations.add(orderServiceLocation);
		});
		orderServiceLocationRepository.saveAll(locations);
		return true;
	}

	@Override
	public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
		Query query=new Query();
		Criteria criteria=Criteria.where("orderId").is(orderId);
		query.addCriteria(criteria);
		query.with(Sort.by(Sort.Order.desc("createTime")));
		query.limit(1);
		OrderServiceLocation orderServiceLocation = mongoTemplate.findOne(query, OrderServiceLocation.class);
		OrderServiceLastLocationVo orderServiceLastLocationVo = new OrderServiceLastLocationVo();
		BeanUtils.copyProperties(orderServiceLocation, orderServiceLastLocationVo);
		return orderServiceLastLocationVo;
	}
}
