package com.atguigu.daijia.driver.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author zm
 */
@Configuration
@ConfigurationProperties(prefix="minio") //读取节点
@Data
public class MinioProperties {

	private String endpointUrl;
	private String accessKey;
	private String secreKey;
	private String bucketName;
}