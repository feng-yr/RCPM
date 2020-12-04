package cn.edu.sjtu.rcpm.parameters;

import org.processmining.plugins.InductiveMiner.mining.MiningParameters;

public class RuleMiningParameters extends MiningParameters {
	
	private float ruleThreshold;

	public float getRuleThreshold() {
		return ruleThreshold;
	}

	public void setRuleThreshold(float ruleThreshold) {
		this.ruleThreshold = ruleThreshold;
	}
}