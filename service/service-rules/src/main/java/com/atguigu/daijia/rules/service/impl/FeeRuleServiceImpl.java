package com.atguigu.daijia.rules.service.impl;

import com.atguigu.daijia.model.form.rules.FeeRuleRequest;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponse;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.rules.mapper.FeeRuleMapper;
import com.atguigu.daijia.rules.service.FeeRuleService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class FeeRuleServiceImpl implements FeeRuleService {

	@Autowired
	private KieContainer kieContainer;

	@Override
	public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm calculateOrderFeeForm) {
		KieSession kieSession = kieContainer.newKieSession();
		//输入对象
		FeeRuleRequest feeRuleRequest=FeeRuleRequest.builder()
				.distance(calculateOrderFeeForm.getDistance())
				.startTime(new SimpleDateFormat("HH:mm:ss").format(calculateOrderFeeForm.getStartTime()))
				.waitMinute(calculateOrderFeeForm.getWaitMinute())
				.build();
		kieSession.insert(feeRuleRequest);

		//返回对象
		FeeRuleResponse feeRuleResponse=new FeeRuleResponse();
		kieSession.setGlobal("feeRuleResponse", feeRuleResponse);
		//触发规则
		kieSession.fireAllRules();
		//终止规则
		kieSession.dispose();
		FeeRuleResponseVo feeRuleResponseVo=new FeeRuleResponseVo();
		BeanUtils.copyProperties(feeRuleResponse,feeRuleResponseVo);
		return feeRuleResponseVo;
	}
}
