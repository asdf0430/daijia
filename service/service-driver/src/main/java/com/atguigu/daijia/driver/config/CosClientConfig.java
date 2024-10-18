package com.atguigu.daijia.driver.config;


import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class CosClientConfig {

	@Autowired
	private TencentCloudProperties tencentCloudProperties;

	@Bean
	public COSClient getCosClient() {
		// 1 初始化用户身份信息（secretId, secretKey）。
		String secretId = tencentCloudProperties.getSecretId();
		String secretKey = tencentCloudProperties.getSecretKey();
		COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
		// 2 设置 bucket 的地域, COS 地域
		Region region = new Region(tencentCloudProperties.getRegion());
		ClientConfig clientConfig = new ClientConfig(region);
		// 这里建议设置使用 https 协议
		clientConfig.setHttpProtocol(HttpProtocol.https);
		// 3 生成 cos 客户端。
		return new COSClient(cred, clientConfig);
	}
}
