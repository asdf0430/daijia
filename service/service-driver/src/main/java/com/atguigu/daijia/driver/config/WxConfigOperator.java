package com.atguigu.daijia.driver.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WxConfigOperator {

	@Resource
	private WxConfigProperty wxConfigProperty;

	@Bean
	public WxMaService wxMaService() {
		WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
		config.setAppid(wxConfigProperty.getAppId());
		config.setSecret(wxConfigProperty.getSecret());
		WxMaService wxMaService = new WxMaServiceImpl();
		wxMaService.setWxMaConfig(config);
		return wxMaService;
	}
}
