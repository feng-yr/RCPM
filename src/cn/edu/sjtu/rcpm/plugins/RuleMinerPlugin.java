package cn.edu.sjtu.rcpm.plugins;

import javax.swing.JOptionPane;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.util.HTMLToString;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;

import cn.edu.sjtu.rcpm.algorithms.RuleMiner;
import cn.edu.sjtu.rcpm.plugins.dialogs.RuleMiningDialog;

public class RuleMinerPlugin {
	
	@Plugin(
			name = "Mine BPMN Model and Rules with Rule Miner", 
			level = PluginLevel.PeerReviewed, 
			parameterLabels = { "Log" }, 
			returnLabels = { "BPMNDiagram", "Process Rules" }, 
			returnTypes = { BPMNDiagram.class, HTMLToString.class }, 
			userAccessible = true,
			help = "Produces one BPMN model and some rules."
	)
	@UITopiaVariant(
			affiliation = UITopiaVariant.EHV, 
			author = "Yingrui Feng", 
			email = "feng-yr@sjtu.edu.cn",
			pack = "RCPM"
	)
	public Object[] mineGuiBPMNAndProcessRules(UIPluginContext context, XLog log) {
		
		RuleMiningDialog dialog = new RuleMiningDialog(log, true);
		InteractionResult result = context.showWizard("Mine using Rule Miner", true, true, dialog);
		context.log("Mining...");
		if (result != InteractionResult.FINISHED || !confirmLargeLogs(context, log, dialog)) {
			context.getFutureResult(0).cancel(false);
			context.getFutureResult(1).cancel(false);
			context.getFutureResult(2).cancel(false);
			return new Object[] { null, null };
		}
		
		return RuleMiner.mine(context, log, dialog.getMiningParameters());
	}
	
	public static boolean confirmLargeLogs(final UIPluginContext context, XLog log, RuleMiningDialog dialog) {
		if (dialog.getVariant().getWarningThreshold() > 0) {
			XEventClassifier classifier = dialog.getMiningParameters().getClassifier();
			XLogInfo xLogInfo = XLogInfoFactory.createLogInfo(log, classifier);
			int numberOfActivities = xLogInfo.getEventClasses().size();
			if (numberOfActivities > dialog.getVariant().getWarningThreshold()) {
				int cResult = JOptionPane.showConfirmDialog(null,
						dialog.getVariant().toString() + " might take a long time, as the event log contains "
								+ numberOfActivities
								+ " activities.\nThe chosen variant of Inductive Miner is exponential in the number of activities.\nAre you sure you want to continue?",
						"Inductive Miner might take a while", JOptionPane.YES_NO_OPTION);

				return cResult == JOptionPane.YES_OPTION;
			}
		}
		return true;
	}
}
