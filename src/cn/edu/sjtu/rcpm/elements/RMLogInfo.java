package cn.edu.sjtu.rcpm.elements;

import java.util.HashMap;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.InductiveMiner.graphs.Graph;
import org.processmining.plugins.InductiveMiner.mining.IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLogImpl;

import cn.edu.sjtu.rcpm.parameters.RuleMiningParameters;

public class RMLogInfo {
	
	private Map<String, Long> verticeCountMap;
	private Map<String, XEventClass> nameVerticeMap;
	private Graph<XEventClass> dfg;
	
	@SuppressWarnings("deprecation")
	public RMLogInfo(XLog xlog, RuleMiningParameters parameters) {
		
		IMLog imlog = new IMLogImpl(xlog, parameters.getClassifier(), parameters.getLifeCycleClassifier());
		IMLogInfo logInfo = parameters.getLog2LogInfo().createLogInfo(imlog);
		
		MultiSet<XEventClass> verticeSet = logInfo.getActivities();
		verticeCountMap = new HashMap<>();
		for(XEventClass vertice : verticeSet) {
			verticeCountMap.put(vertice.getId(), verticeSet.getCardinalityOf(vertice));
		}
		
		dfg = logInfo.getDfg().getDirectlyFollowsGraph();
		nameVerticeMap = new HashMap<>();
		for(XEventClass vertice : dfg.getVertices()) {
			nameVerticeMap.put(vertice.getId(), vertice);
		}
	}
	
	public long getVerticeWeight(String name) {
		
		if(verticeCountMap.containsKey(name)) {
			return verticeCountMap.get(name);
		} else {
			return 0;
		}
	}
	
	public long getEdgeWeight(String source, String target) {
		
		XEventClass sourceVertice = nameVerticeMap.get(source);
		XEventClass targetVertice = nameVerticeMap.get(target);
		return dfg.getEdgeWeight(sourceVertice, targetVertice);
	}
}
