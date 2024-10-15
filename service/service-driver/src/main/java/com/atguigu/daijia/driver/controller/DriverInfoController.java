package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value="/driver/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoController {
	@Autowired
	private DriverInfoService driverInfoService;

	@Operation(summary = "小程序授权登录")
	@GetMapping("/login/{code}")
	public Result<Long> login(@PathVariable String code) {
		return Result.ok(driverInfoService.login(code));
	}

	@Operation(summary = "获取司机登录信息")
	@GetMapping("/getDriverLoginInfo/{driverId}")
	public Result<DriverLoginVo> getDriverInfo(@PathVariable Long driverId) {
		DriverLoginVo driverLoginVo = driverInfoService.getDriverInfo(driverId);
		return Result.ok(driverLoginVo);
	}

	@Operation(summary = "获取司机认证信息")
	@GetMapping("/getDriverAuthInfo/{driverId}")
	public Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable Long driverId) {
		DriverAuthInfoVo driverAuthInfoVo = driverInfoService.getDriverAuthInfo(driverId);
		return Result.ok(driverAuthInfoVo);
	}

	@Operation(summary = "更新司机认证操作")
	@PostMapping("/updateDriverAuthInfo")
	public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm driverAuthInfoForm) {
		return  Result.ok(driverInfoService.updateDriverAuthInfo(driverAuthInfoForm));
	}

	@Operation(summary = "创建司机人脸模型")
	@PostMapping("/creatDriverFaceModel")
	public Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm driverAuthInfoForm) {
		return  Result.ok(driverInfoService.creatDriverFaceModel(driverAuthInfoForm));
	}

	@Operation(summary = "获取司机设置信息")
	@GetMapping("/getDriverSet/{driverId}")
	public Result<DriverSet> getDriverSet(@PathVariable Long driverId) {
		return Result.ok(driverInfoService.getDriverSet(driverId));
	}
}
