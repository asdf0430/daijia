package com.atguigu.daijia.rules.service.impl;

import com.atguigu.daijia.model.entity.rule.RewardRule;
import com.atguigu.daijia.model.form.rules.RewardRuleRequest;
import com.atguigu.daijia.model.form.rules.RewardRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponse;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponseVo;
import com.atguigu.daijia.rules.config.DroolsHelper;
import com.atguigu.daijia.rules.mapper.RewardRuleMapper;
import com.atguigu.daijia.rules.service.RewardRuleService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class RewardRuleServiceImpl implements RewardRuleService {

	@Autowired
	private RewardRuleMapper rewardRuleMapper;


	@Override
	public RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm) {
		RewardRuleRequest rewardRuleRequest = new RewardRuleRequest();
		BeanUtils.copyProperties(rewardRuleRequestForm, rewardRuleRequest);
		KieSession kieSession = DroolsHelper.loadForRule(DroolsHelper.RULES_REWARD_RULE_DRL);
		kieSession.insert(rewardRuleRequest);

		RewardRuleResponse rewardRuleResponse = new RewardRuleResponse();
		kieSession.setGlobal("rewardRuleResponse", rewardRuleResponse);
		kieSession.fireAllRules();
		kieSession.dispose();

		return RewardRuleResponseVo.builder()
				.rewardAmount(rewardRuleResponse.getRewardAmount())
				.rewardRuleId(null)
				.build();
	}
}
