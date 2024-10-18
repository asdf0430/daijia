package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.client.CiFeignClient;
import com.atguigu.daijia.driver.service.FileService;
import com.atguigu.daijia.driver.service.MonitorService;
import com.atguigu.daijia.model.entity.order.OrderMonitor;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import com.atguigu.daijia.model.vo.order.TextAuditingVo;
import com.atguigu.daijia.order.client.OrderMonitorFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MonitorServiceImpl implements MonitorService {

	@Autowired
	private OrderMonitorFeignClient orderMonitorFeignClient;

	@Autowired
	private FileService fileService;

	@Autowired
	private CiFeignClient ciFeignClient;

	@Override
	public Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm) {
		//上传文件
		String url = fileService.upload(file);
		TextAuditingVo data = ciFeignClient.textAuditing(orderMonitorForm.getContent()).getData();
		OrderMonitorRecord orderMonitorRecord = OrderMonitorRecord.builder()
				.orderId(orderMonitorForm.getOrderId())
				.fileUrl(url)
				.content(orderMonitorForm.getContent())
				.result(data.getResult())
				.keywords(data.getKeywords())
				.build();
		OrderMonitor orderMonitor = orderMonitorFeignClient.getOrderMonitor(orderMonitorForm.getOrderId()).getData();
		int fileNum = orderMonitor.getFileNum() + 1;
		orderMonitor.setFileNum(fileNum);
		//审核结果: 0（审核正常），1 （判定为违规敏感文件），2（疑似敏感，建议人工复核）。
		if("3".equals(orderMonitorRecord.getResult())) {
			int auditNum = orderMonitor.getAuditNum() + 1;
			orderMonitor.setAuditNum(auditNum);
		}
		orderMonitorFeignClient.updateOrderMonitor(orderMonitor);
		//保存到mongodb中
		return orderMonitorFeignClient.saveMonitorRecord(orderMonitorRecord).getData();
	}
}
