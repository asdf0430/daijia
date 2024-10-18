package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.service.CiService;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

	@Resource
	private TencentCloudProperties tencentCloudProperties;

	@Autowired
	private COSClient cosClient;

	@Autowired
	private CiService ciService;

	@Override
	public CosUploadVo upLoad(MultipartFile file, String path) {

		//文件上传
		//元数据信息
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(file.getSize());
		meta.setContentEncoding("UTF-8");
		meta.setContentType(file.getContentType());

		//向存储桶中保存文件
		String fileType = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")); //文件后缀名
		String uploadPath = "/driver/" + path + "/" + UUID.randomUUID().toString().replaceAll("-", "") + fileType;
		// 01.jpg
		// /driver/auth/0o98754.jpg
		PutObjectRequest putObjectRequest = null;
		try {
			//1 bucket名称
			//2
			putObjectRequest = new PutObjectRequest(tencentCloudProperties.getBucketPrivate(),
					uploadPath,
					file.getInputStream(),
					meta);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		putObjectRequest.setStorageClass(StorageClass.Standard);
		PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest); //上传文件
		cosClient.shutdown();
		Boolean aBoolean = ciService.imageAuditing(uploadPath);
		if (Boolean.FALSE.equals(aBoolean)) {
			cosClient.deleteObject(tencentCloudProperties.getBucketPrivate(), uploadPath);
			throw new MyException(ResultCodeEnum.IMAGE_AUDITION_FAIL);
		}
		//返回vo对象
		CosUploadVo cosUploadVo = new CosUploadVo();
		cosUploadVo.setUrl(uploadPath);
		//图片临时访问url，回显使用
		cosUploadVo.setShowUrl(getImageUrl(uploadPath));
		return cosUploadVo;

	}


	//获取临时签名URL
	@Override
	public String getImageUrl(String path) {
		if(!StringUtils.hasText(path)) {
			return "";
		}
		//GeneratePresignedUrlRequest
		GeneratePresignedUrlRequest request =
				new GeneratePresignedUrlRequest(tencentCloudProperties.getBucketPrivate(),
						path, HttpMethodName.GET);
		//设置临时URL有效期为15分钟
		Date date = new DateTime().plusMinutes(15).toDate();
		request.setExpiration(date);
		//调用方法获取
		URL url = cosClient.generatePresignedUrl(request);
		cosClient.shutdown();
		return url.toString();
	}
}
