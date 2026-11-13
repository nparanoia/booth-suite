package com.booth.trade.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SeckillLimitConfig {

    @Value("${booth.seckill.hot-qps:20}")
    private double hotQps;

    @PostConstruct
    public void load() {
        ParamFlowRule rule = new ParamFlowRule("seckillBuy");
        rule.setParamIdx(0);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(hotQps);
        rule.setDurationInSec(1);
        ParamFlowRuleManager.loadRules(List.of(rule));
    }
}
