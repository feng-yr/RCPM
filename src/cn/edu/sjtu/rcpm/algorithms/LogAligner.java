package cn.edu.sjtu.rcpm.algorithms;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;

public class LogAligner {
	
	public static XLog alignLog(XLog xlog, ProcessTree tree, XEventClassifier activityClassifier) {

		Map<String, Integer> nodeIndexMap = new HashMap<>();
		preorder(tree.getRoot(), 0, nodeIndexMap);
		
		XLog log = sortLog(xlog, nodeIndexMap, activityClassifier);
		return log;
	}
	
	public static Integer preorder(Node root, Integer curIndex, Map<String, Integer> nodeIndexMap) {
		
		if(root instanceof AbstractTask.Manual) {
			nodeIndexMap.put(root.toString(), curIndex);
		} else if(root instanceof AbstractBlock.And) {
			AbstractBlock block = (AbstractBlock) root;
			for(Node child : block.getChildren()) {
				curIndex = preorder(child, curIndex + 1, nodeIndexMap);
			}
		} else if(root instanceof AbstractBlock) {
			AbstractBlock block = (AbstractBlock) root;
			for(Node child : block.getChildren()) {
				curIndex = preorder(child, curIndex, nodeIndexMap);
			}
		}
		return curIndex;
	}
	
	public static XLog sortLog(XLog xlog, Map<String, Integer> nodeIndexMap, XEventClassifier activityClassifier) {
		
		XLog log = (XLog) xlog.clone();
		for (XTrace trace : log) {
			Collections.sort(trace, new Comparator<XEvent>() {

				@Override
				public int compare(XEvent o1, XEvent o2) {
					String activity1 = activityClassifier.getClassIdentity(o1);
					String activity2 = activityClassifier.getClassIdentity(o2);
					Integer index1 = nodeIndexMap.get(activity1);
					Integer index2 = nodeIndexMap.get(activity2);
					
					if(index1 == null) {
						index1 = Integer.MAX_VALUE;
					}
					
					if(index2 == null) {
						index2 = Integer.MAX_VALUE;
					}
					
					return index1 - index2;
				}
			});
		}
		return log;
	}
}
