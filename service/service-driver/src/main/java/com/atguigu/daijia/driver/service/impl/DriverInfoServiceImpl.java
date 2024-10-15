package com.atguigu.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.mapper.DriverAccountMapper;
import com.atguigu.daijia.driver.mapper.DriverInfoMapper;
import com.atguigu.daijia.driver.mapper.DriverLoginLogMapper;
import com.atguigu.daijia.driver.mapper.DriverSetMapper;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.DriverAccount;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.entity.driver.DriverLoginLog;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20180301.IaiClient;
import com.tencentcloudapi.iai.v20180301.models.CreatePersonRequest;
import com.tencentcloudapi.iai.v20180301.models.CreatePersonResponse;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverInfoService {

	@Autowired
	private WxMaService wxMaService;

	@Autowired
	private DriverInfoMapper driverInfoMapper;

	@Autowired
	private DriverSetMapper driverSetMapper;

	@Autowired
	private DriverAccountMapper driverAccountMapper;

	@Autowired
	private DriverLoginLogMapper driverLoginLogMapper;

	@Autowired
	private CosServiceImpl cosService;

	@Autowired
	private TencentCloudProperties tencentCloudProperties;

	//小程序授权登录
	@Override
	public Long login(String code) {
		try {
			//根据code + 小程序id + 秘钥请求微信接口，返回openid
			WxMaJscode2SessionResult sessionInfo =
					wxMaService.getUserService().getSessionInfo(code);
			String openid = sessionInfo.getOpenid();

			//根据openid查询是否第一次登录
			LambdaQueryWrapper<DriverInfo> wrapper = new LambdaQueryWrapper<>();
			wrapper.eq(DriverInfo::getWxOpenId,openid);
			DriverInfo driverInfo = driverInfoMapper.selectOne(wrapper);

			if(driverInfo == null) {
				//添加司机基本信息
				driverInfo = new DriverInfo();
				driverInfo.setNickname(String.valueOf(System.currentTimeMillis()));
				driverInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
				driverInfo.setWxOpenId(openid);
				driverInfoMapper.insert(driverInfo);

				//初始化司机设置
				DriverSet driverSet = new DriverSet();
				driverSet.setDriverId(driverInfo.getId());
				driverSet.setOrderDistance(new BigDecimal(0));//0：无限制
				driverSet.setAcceptDistance(new BigDecimal(SystemConstant.ACCEPT_DISTANCE));//默认接单范围：5公里
				driverSet.setIsAutoAccept(0);//0：否 1：是
				driverSetMapper.insert(driverSet);

				//初始化司机账户信息
				DriverAccount driverAccount = new DriverAccount();
				driverAccount.setDriverId(driverInfo.getId());
				driverAccountMapper.insert(driverAccount);
			}

			//记录司机登录信息
			DriverLoginLog driverLoginLog = new DriverLoginLog();
			driverLoginLog.setDriverId(driverInfo.getId());
			driverLoginLog.setMsg("小程序登录");
			driverLoginLogMapper.insert(driverLoginLog);

			//返回司机id
			return driverInfo.getId();
		} catch (WxErrorException e) {
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}
	}

	//获取司机登录信息
	@Override
	public DriverLoginVo getDriverInfo(Long driverId) {
		//根据司机id获取司机信息
		DriverInfo driverInfo = driverInfoMapper.selectById(driverId);

		//driverInfo -- DriverLoginVo
		DriverLoginVo driverLoginVo = new DriverLoginVo();
		BeanUtils.copyProperties(driverInfo,driverLoginVo);

		//是否建档人脸识别
		String faceModelId = driverInfo.getFaceModelId();
		boolean isArchiveFace = StringUtils.hasText(faceModelId);
		driverLoginVo.setIsArchiveFace(isArchiveFace);
		return driverLoginVo;
	}

	//获取司机认证信息
	@Override
	public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
		DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
		DriverAuthInfoVo driverAuthInfoVo = new DriverAuthInfoVo();
		BeanUtils.copyProperties(driverInfo,driverAuthInfoVo);
		driverAuthInfoVo.setIdcardBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardBackUrl()));
		driverAuthInfoVo.setIdcardFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardFrontUrl()));
		driverAuthInfoVo.setIdcardHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardHandUrl()));
		driverAuthInfoVo.setDriverLicenseFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseFrontUrl()));
		driverAuthInfoVo.setDriverLicenseBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseBackUrl()));
		driverAuthInfoVo.setDriverLicenseHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseHandUrl()));
		return driverAuthInfoVo;
	}

	//更新司机认证信息
	@Override
	public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
		//获取司机id
		Long driverId = updateDriverAuthInfoForm.getDriverId();

		//修改操作
		DriverInfo driverInfo = new DriverInfo();
		driverInfo.setId(driverId);
		BeanUtils.copyProperties(updateDriverAuthInfoForm,driverInfo);
		boolean update = this.updateById(driverInfo);
		return update;
	}

	//创建司机人脸模型
	@Override
	public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
		//根据司机id获取司机信息
		DriverInfo driverInfo =
				driverInfoMapper.selectById(driverFaceModelForm.getDriverId());
		try{

			// 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
			// 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
			// 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
			Credential cred = new Credential(tencentCloudProperties.getSecretId(),
					tencentCloudProperties.getSecretKey());
			// 实例化一个http选项，可选的，没有特殊需求可以跳过
			HttpProfile httpProfile = new HttpProfile();
			httpProfile.setEndpoint("iai.tencentcloudapi.com");
			// 实例化一个client选项，可选的，没有特殊需求可以跳过
			ClientProfile clientProfile = new ClientProfile();
			clientProfile.setHttpProfile(httpProfile);
			// 实例化要请求产品的client对象,clientProfile是可选的
			IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(),
					clientProfile);
			// 实例化一个请求对象,每个接口都会对应一个request对象
			CreatePersonRequest req = new CreatePersonRequest();
			//设置相关值
			req.setGroupId(tencentCloudProperties.getPersonGroupId());
			//基本信息
			req.setPersonId(String.valueOf(driverInfo.getId()));
			req.setGender(Long.parseLong(driverInfo.getGender()));
			req.setQualityControl(4L);
			req.setUniquePersonControl(4L);
			req.setPersonName(driverInfo.getName());
			req.setImage(driverFaceModelForm.getImageBase64());

			// 返回的resp是一个CreatePersonResponse的实例，与请求对象对应
			CreatePersonResponse resp = client.CreatePerson(req);
			// 输出json格式的字符串回包
			System.out.println(AbstractModel.toJsonString(resp));
			String faceId = resp.getFaceId();

			if(!StringUtils.hasText(faceId)) {
				throw new MyException(504,"人脸识别异常");
			}
			driverInfo.setFaceModelId(faceId);
			driverInfoMapper.updateById(driverInfo);
			return true;
		} catch (TencentCloudSDKException e) {
			e.printStackTrace();
			return false;
		}
	}

	//获取司机设置信息
	@Override
	public DriverSet getDriverSet(Long driverId) {
		LambdaQueryWrapper<DriverSet> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(DriverSet::getDriverId,driverId);
		DriverSet driverSet = driverSetMapper.selectOne(wrapper);
		return driverSet;
	}

}