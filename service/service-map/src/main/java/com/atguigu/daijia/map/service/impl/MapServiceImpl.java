package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.atguigu.daijia.common.execption.MyException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {

	private final RestTemplate restTemplate;

	MapServiceImpl(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Value("${cos.key}")
	private String key;

	// 计算驾驶路线
	@Override
	public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
		// 定义调用地址https://apis.map.qq.com/ws/direction/v1/driving/?from=39.915285,116.403857&to=39.915285,116.803857&waypoints=39.111,116.112;39.112,116.113&output=json&callback=cb&key=[你的key]
		String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";
		Map<String, String> map = new HashMap<>();
		// 获取开始结束的经纬度
		String from = calculateDrivingLineForm.getStartPointLatitude() + "," + calculateDrivingLineForm.getStartPointLongitude();
		String end = calculateDrivingLineForm.getEndPointLatitude() + "," + calculateDrivingLineForm.getEndPointLongitude();
		map.put("from", from);
		map.put("to", end);
		map.put("key", key);
		// 通过restTemplate调用get请求
		// 参数：请求地址，返回类型，map传参
		JSONObject jsonObject = restTemplate.getForObject(url, JSONObject.class, map);
		int status = jsonObject.getIntValue("status");
		if (status != 0) {
			throw new MyException(ResultCodeEnum.MAP_FAIL);
		}
		JSONObject route = jsonObject.getJSONObject("result").getJSONArray("routes").getJSONObject(0);
		return DrivingLineVo.builder()
				.distance(route.getBigDecimal("distance")
						.divide(new BigDecimal(1000))
						.setScale(2, RoundingMode.HALF_DOWN))//距离
				.duration(route.getBigDecimal("duration"))//预估时间
				.polyline(route.getJSONArray("polyline")) //路线
				.build();
	}
}
