package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.annotation.LoginAno;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "位置API接口管理")
@RestController
@RequestMapping(value="/location")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationController {

	@Autowired
	private LocationService locationService;

	@Operation(summary = "开启接单服务：更新司机经纬度位置")
	@LoginAno
	@PostMapping("/updateDriverLocation")
	public Result<Boolean> updateDriverLocation(@RequestBody UpdateDriverLocationForm updateDriverLocationForm) {
		Long driverId = AuthContextHolder.getUserId();//司机id
		updateDriverLocationForm.setDriverId(driverId);
		return Result.ok(locationService.updateDriverLocation(updateDriverLocationForm));
	}

	@Operation(summary = "司机赶往代驾起始点：更新订单位置到Redis缓存")
	@LoginAno
	@PostMapping("/updateOrderLocationToCache")
	public Result<Boolean> updateOrderLocationToCache(@RequestBody UpdateOrderLocationForm updateOrderLocationForm) {
		return Result.ok(locationService.updateOrderLocationToCache(updateOrderLocationForm));
	}

}