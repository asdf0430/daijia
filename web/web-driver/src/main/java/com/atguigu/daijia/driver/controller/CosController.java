package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "腾讯云cos上传接口管理")
@RestController
@RequestMapping(value="/cos")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosController {

	@Autowired
	private CosService cosService;

	//文件上传接口
	@Operation(summary = "上传")
	@PostMapping("/upload")
	public Result<CosUploadVo> upload(@RequestPart("file") MultipartFile file,
	                                  @RequestParam(name = "path",defaultValue = "auth") String path) {
		CosUploadVo cosUploadVo = cosService.uploadFile(file,path);
		return Result.ok(cosUploadVo);
	}
}