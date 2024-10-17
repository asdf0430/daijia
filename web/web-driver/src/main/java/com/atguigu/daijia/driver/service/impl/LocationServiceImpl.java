package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

	@Autowired
	private LocationFeignClient locationFeignClient;
	@Autowired
	private DriverInfoFeignClient driverInfoFeignClient;

	//更新司机位置
	@Override
	public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
		//根据司机id获取司机个性化设置信息
		Long driverId = updateDriverLocationForm.getDriverId();
		Result<DriverSet> driverSetResult = driverInfoFeignClient.getDriverSet(driverId);
		DriverSet driverSet = driverSetResult.getData();

		//判断：如果司机开始接单，更新位置信息
		if(driverSet.getServiceStatus() == 1) {
			Result<Boolean> booleanResult = locationFeignClient.updateDriverLocation(updateDriverLocationForm);
			return booleanResult.getData();
		} else {
			//没有接单
			throw new MyException(ResultCodeEnum.NO_START_SERVICE);
		}
	}

	@Override
	public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
		return locationFeignClient.updateOrderLocationToCache(updateOrderLocationForm).getData();
	}

}