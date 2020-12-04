package cn.edu.sjtu.rcpm.algorithms;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.packages.PackageManager.Canceller;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.processtree.ProcessTree;

public class InductiveMiner {

	public static ProcessTree mineProcessTree(final PluginContext context, XLog log, MiningParameters parameters) {
		
		return IMProcessTree.mineProcessTree(log, parameters, new Canceller() {
			public boolean isCancelled() {
				return context.getProgress().isCancelled();
			}
		});
	}
}
