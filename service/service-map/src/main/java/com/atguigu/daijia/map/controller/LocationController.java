package com.atguigu.daijia.map.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "位置API接口管理")
@RestController
@RequestMapping("/map/location")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationController {

	@Autowired
	private LocationService locationService;

	//司机开启接单，更新司机位置信息
	@Operation(summary = "开启接单服务：更新司机经纬度位置")
	@PostMapping("/updateDriverLocation")
	public Result<Boolean> updateDriverLocation(@RequestBody
	                                            UpdateDriverLocationForm updateDriverLocationForm) {
		Boolean flag = locationService.updateDriverLocation(updateDriverLocationForm);
		return Result.ok(flag);
	}

	//司机关闭接单，删除司机位置信息
	@Operation(summary = "关闭接单服务：删除司机经纬度位置")
	@DeleteMapping("/removeDriverLocation/{driverId}")
	public Result<Boolean> removeDriverLocation(@PathVariable Long driverId) {
		return Result.ok(locationService.removeDriverLocation(driverId));
	}

	@Operation(summary = "搜索附近满足条件的司机")
	@PostMapping("/searchNearByDriver")
	public Result<List<NearByDriverVo>> searchNearByDriver(@RequestBody
	                                                       SearchNearByDriverForm searchNearByDriverForm) {
		return Result.ok(locationService.searchNearByDriver(searchNearByDriverForm));
	}
}