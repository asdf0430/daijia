package com.atguigu.daijia.dispatch.xxl.job;/*
 *@title JobHandler
 *@description
 *@author zm
 *@version 1.0
 *@create 2024/10/15 上午10:53
 */

import com.atguigu.daijia.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.model.entity.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JobHandler {

	@Autowired
	private XxlJobLogMapper xxlJobLogMapper;

	@Autowired
	private NewOrderService newOrderService;


	@XxlJob("newOrderTask")
	public void newOrderTask(){
		XxlJobLog xxlJobLog = new XxlJobLog();
		long jobId = XxlJobHelper.getJobId();
		xxlJobLog.setJobId(jobId);
		long start = System.currentTimeMillis();
		try{
			newOrderService.excuteTask(jobId);
		} catch (Exception e) {
			//失败状态
			xxlJobLog.setStatus(0);
			xxlJobLog.setError(e.getMessage());
			e.printStackTrace();
		} finally {
			long times = System.currentTimeMillis()- start;
			xxlJobLog.setTimes((int) times);
			xxlJobLogMapper.insert(xxlJobLog);
		}
	}
}
