package cn.edu.sjtu.rcpm.plugins;

import java.util.List;

import org.processmining.framework.util.HTMLToString;

import cn.edu.sjtu.rcpm.elements.ProcessRule;
import cn.edu.sjtu.rcpm.elements.RuleAction;

public class RuleHTMLToString implements HTMLToString {
	
	private List<ProcessRule> rules;
	
	public RuleHTMLToString(List<ProcessRule> rules) {
		this.rules = rules;
	}

	@Override
	public String toHTMLString(boolean includeHTMLTags) {
		
		StringBuffer buffer = new StringBuffer();
		
		if (includeHTMLTags) {
			buffer.append("<html>");
		}
		
		buffer.append("<h1>Process Rules</h1>");
		
		buffer.append("<table border=\"1\">");
		buffer.append("<tr><th>Event</th><th>Condition</th><th>Operation</th><th>Type</th><th>Position</th><th>Content</th></tr>");
		
		for(ProcessRule rule : rules) {
			
			List<RuleAction> actions = rule.getActions();
			int actionSize = actions.size();
			if(actionSize < 1) {
				continue;
			}
			
			buffer.append("<tr>");
			if(actionSize == 1) {
				buffer.append("<td>");
				buffer.append(rule.getEvent() != null ? rule.getEvent() : "");
				buffer.append("</td>");
				buffer.append("<td>");
				buffer.append(rule.getCondition() != null ? rule.getCondition() : "");
				buffer.append("</td>");
			} else {
				buffer.append("<td rowspan=\"");
				buffer.append(actionSize);
				buffer.append("\">");
				buffer.append(rule.getEvent() != null ? rule.getEvent() : "");
				buffer.append("</td>");
				buffer.append("<td rowspan=\"");
				buffer.append(actionSize);
				buffer.append("\">");
				buffer.append(rule.getCondition() != null ? rule.getCondition() : "");
				buffer.append("</td>");
			}
			
			buffer.append("<td>");
			buffer.append(actions.get(0).getOperation() != null ? actions.get(0).getOperation() : "");
			buffer.append("</td>");
			buffer.append("<td>");
			buffer.append(actions.get(0).getType() != null ? actions.get(0).getType() : "");
			buffer.append("</td>");
			buffer.append("<td>");
			buffer.append(actions.get(0).getPosition() != null ? actions.get(0).getPosition() : "");
			buffer.append("</td>");
			buffer.append("<td>");
			buffer.append(actions.get(0).getContent() != null ? actions.get(0).getContent() : "");
			buffer.append("</td>");
			buffer.append("</tr>");
			
			for(int i = 1; i < actionSize; i++) {
				buffer.append("<tr>");
				buffer.append("<td>");
				buffer.append(actions.get(i).getOperation() != null ? actions.get(i).getOperation() : "");
				buffer.append("</td>");
				buffer.append("<td>");
				buffer.append(actions.get(i).getType() != null ? actions.get(i).getType() : "");
				buffer.append("</td>");
				buffer.append("<td>");
				buffer.append(actions.get(i).getPosition() != null ? actions.get(i).getPosition() : "");
				buffer.append("</td>");
				buffer.append("<td>");
				buffer.append(actions.get(i).getContent() != null ? actions.get(i).getContent() : "");
				buffer.append("</td>");
				buffer.append("</tr>");
			}
		}
		
		buffer.append("</table>");
		
		if (includeHTMLTags) {
			buffer.append("</html>");
		}
		return buffer.toString();
	}
}
