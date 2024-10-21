package com.atguigu.daijia.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.mapper.CustomerInfoMapper;
import com.atguigu.daijia.customer.mapper.CustomerLoginLogMapper;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.customer.CustomerLoginLog;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

	private final static String DEFAULT_IMAGE ="https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg";

	@Resource
	private WxMaService wxMaService;
	@Resource
	private CustomerInfoMapper customerInfoMapper;
	@Resource
	private CustomerLoginLogMapper customerLoginLogMapper;

	@Override
	public Long login(String code)  {
		String openId=null;
		try {
			//获取openid
			WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
			openId = sessionInfo.getOpenid();

		}catch (WxErrorException e){
			e.printStackTrace();
		}
		//根据openid查表,唯一标识
		LambdaQueryWrapper<CustomerInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(CustomerInfo::getWxOpenId,openId);
		CustomerInfo customerInfo = customerInfoMapper.selectOne(queryWrapper);

		//数据库没有信息，则新增数据
		if(Objects.isNull(customerInfo)){
			customerInfo = CustomerInfo.builder()
					.wxOpenId(openId)
					.nickname(RandomStringUtils.random(8, true, true).toUpperCase())
					.avatarUrl(DEFAULT_IMAGE)
					.build();
			customerInfoMapper.insert(customerInfo);
		}

		//登录日志添加
		CustomerLoginLog customerLoginLog = CustomerLoginLog.builder()
				.customerId(customerInfo.getId())
				.msg("小程序登录")
				.build();
		customerLoginLogMapper.insert(customerLoginLog);

		return customerInfo.getId();
	}


	@Override
	public CustomerLoginVo getCustomerLoginInfo(Long customerId) {
		CustomerInfo customerInfo = this.getById(customerId);
		CustomerLoginVo customerInfoVo = new CustomerLoginVo();
		BeanUtils.copyProperties(customerInfo, customerInfoVo);
		//判断是否绑定手机号码，如果未绑定，小程序端发起绑定事件
		Boolean isBindPhone = StringUtils.hasText(customerInfo.getPhone());
		customerInfoVo.setIsBindPhone(isBindPhone);
		return customerInfoVo;
	}

	////更新客户微信手机号码
	@Override
	public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
		//1 根据code值获取微信绑定手机号码
		try {
			WxMaPhoneNumberInfo phoneNoInfo =
					wxMaService.getUserService().getPhoneNoInfo(updateWxPhoneForm.getCode());
			String phoneNumber = phoneNoInfo.getPhoneNumber();

			//更新用户信息
			Long customerId = updateWxPhoneForm.getCustomerId();
			CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);
			customerInfo.setPhone(phoneNumber);
			customerInfoMapper.updateById(customerInfo);

			return true;
		} catch (WxErrorException e) {
			throw new MyException(ResultCodeEnum.DATA_ERROR);
		}
	}

	@Override
	public String getCustomerOpenId(Long customerId) {
		LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CustomerInfo::getId,customerId);
		CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);
		return customerInfo.getWxOpenId();
	}

}
