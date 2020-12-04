package cn.edu.sjtu.rcpm.elements;

import java.util.List;

public class ProcessRule {
	
	private String event;
	private String condition;
	private List<RuleAction> actions;
	
	public String getEvent() {
		return event;
	}
	public void setEvent(String event) {
		this.event = event;
	}
	public String getCondition() {
		return condition;
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	public List<RuleAction> getActions() {
		return actions;
	}
	public void setActions(List<RuleAction> actions) {
		this.actions = actions;
	}
}
