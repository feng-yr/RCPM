package cn.edu.sjtu.rcpm.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.packages.PackageManager.Canceller;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.impl.ProcessTreeImpl;

import com.raffaeleconforti.conversion.petrinet.PetriNetToBPMNConverter;

import cn.edu.sjtu.rcpm.elements.ProcessRule;
import cn.edu.sjtu.rcpm.elements.RMLogInfo;
import cn.edu.sjtu.rcpm.elements.RuleAction;
import cn.edu.sjtu.rcpm.parameters.RuleMiningParameters;
import cn.edu.sjtu.rcpm.plugins.RuleHTMLToString;
import cn.edu.sjtu.rcpm.utils.ModelConvertor;

public class RuleMiner {
	
	private static XLog log;
	private static RMLogInfo logInfo;
	private static RuleMiningParameters parameters;
	private static Map<Node, Long> nodeWeightMap;
	private static List<ProcessRule> rules;
	
	@SuppressWarnings("unchecked")
	public static Object[] mine(final PluginContext context, XLog log, RuleMiningParameters parameters) {
		
		ProcessTree processTree = InductiveMiner.mineProcessTree(context, log, parameters);
		context.log("Mining Process Tree Ended.");
		Object[] result = reduce(context, log, parameters, processTree);
		context.log("Cutting Process Tree Ended.");
		
		Object[] petriNet = ModelConvertor.processTree2Petrinet((ProcessTree)result[0], new Canceller() {
			public boolean isCancelled() {
				return context.getProgress().isCancelled();
			}
		});
		context.log("Converting to Petri Net Ended.");
		
		BPMNDiagram bpmnDiagram = PetriNetToBPMNConverter.convert((Petrinet) petriNet[0], (Marking) petriNet[1], (Marking) petriNet[2], true);
		context.log("Converting to BPMN Ended.");
		
		return new Object[] { bpmnDiagram, new RuleHTMLToString((List<ProcessRule>) result[1]) };
	}
	
	public static Object[] reduce(final PluginContext context, XLog xlog, RuleMiningParameters parameters, ProcessTree srcProcessTree) {
		
		RuleMiner.parameters = parameters;
		nodeWeightMap = new HashMap<>();
		rules = new ArrayList<>();
		ProcessTree dstProcessTree = new ProcessTreeImpl(srcProcessTree);
		log = LogAligner.alignLog(xlog, dstProcessTree, parameters.getClassifier());
		logInfo = new RMLogInfo(log, parameters);
		
		Node root = dstProcessTree.getRoot();
		calculateFrequency(root, dstProcessTree);
		prune(root, dstProcessTree);
		
		return new Object[] { dstProcessTree, rules };
	}
	
	public static Boolean prune(Node node, ProcessTree tree) {
		
		if(node instanceof AbstractBlock.Xor || node instanceof AbstractBlock.Def || node instanceof AbstractBlock.Or) {
			AbstractBlock block = (AbstractBlock) node;
			List<Boolean> canDeletes = new ArrayList<>();
			List<Node> children = block.getChildren();
			for(Node child : children) {
				Boolean canDelete = prune(child, tree);
				canDeletes.add(canDelete);
			}
			
			boolean[] isDelete = new boolean[children.size()];
			int deleteNum = 0;
			Node lastLeftNode = null;
			for(int i = 0; i < children.size(); i++) {
				if(canDeletes.get(i) && 1.0 * children.size() * nodeWeightMap.get(children.get(i)) / nodeWeightMap.get(node) < parameters.getRuleThreshold()) {
					System.out.println(children.get(i).toString());
					System.out.println("Xor: " + nodeWeightMap.get(children.get(i)) + " " + nodeWeightMap.get(node));
					isDelete[i] = true;
					deleteNum++;
				} else {
					isDelete[i] = false;
					lastLeftNode = children.get(i);
				}
			}
			
			boolean oneLeft = children.size() - deleteNum == 1;
			for(int i = 0; i < children.size(); i++) {
				
				if(isDelete[i]) {
					
					String event = findRuleEvent(children.get(i), tree);
					String condition = findRuleCondition(children.get(i), tree);
					if(oneLeft) {
						if(lastLeftNode instanceof AbstractTask.Automatic) {
							addProcessRule("EndOf(" + event + ")", condition, "Activity", "Add", "After", findNextNode(node, tree).toString(), children.get(i).toString(), rules);
						} else {
							addProcessRule("EndOf(" + event + ")", condition, "Activity", "Add", "Xor", lastLeftNode.toString(), children.get(i).toString(), rules);
						}
					} else {
						addProcessRule("EndOf(" + event + ")", condition, "Activity", "Add", "In", node.toString(), children.get(i).toString(), rules);
					}
					
					removeTreeNode(children.get(i), tree);
				}
			}
			
			return false;
			
		} else if(node instanceof AbstractBlock.XorLoop || node instanceof AbstractBlock.DefLoop) {
			AbstractBlock loop = (AbstractBlock) node;
			List<Boolean> canDeletes = new ArrayList<>();
			List<Node> children = loop.getChildren();
			for(Node child : children) {
				Boolean canDelete = prune(child, tree);
				canDeletes.add(canDelete);
			}
			
			if(children.size() >= 2 && canDeletes.get(1) && nodeWeightMap.get(children.get(1)) / (nodeWeightMap.get(children.get(0)) + 0.001) < parameters.getRuleThreshold()) {
				
				System.out.println("Loop: " + nodeWeightMap.get(children.get(1)) + " " + nodeWeightMap.get(children.get(0)));
				for(int i = 2; i < children.size(); i++) {
					removeTreeNode(children.get(i), tree);
				}
				
				Node leftNode = children.get(0);
				while(leftNode instanceof AbstractBlock.Seq) {
					AbstractBlock.Seq tmpBlock = (AbstractBlock.Seq) leftNode;
					leftNode = tmpBlock.getChildren().get(0);
				}
				
				Node rightNode = children.get(0);
				while(rightNode instanceof AbstractBlock.Seq) {
					AbstractBlock.Seq tmpBlock = (AbstractBlock.Seq) rightNode;
					rightNode = tmpBlock.getChildren().get(tmpBlock.getChildren().size() - 1);
				}
				
				String event = findRuleEvent(children.get(1), tree);
				String condition = findRuleCondition(children.get(1), tree);
				if(children.get(0) instanceof AbstractTask.Automatic) {
					addProcessRule("EndOf(" + event + ")", condition, "Activity", "Add", "After", findNextNode(node, tree).toString(), children.get(1).toString(), rules);
				} else {
					addProcessRule("EndOf(" + event + ")", condition, "Activity", "Add", "Loop", children.get(0).toString(), children.get(1).toString(), rules);
				}
				removeTreeNode(children.get(1), tree);
			}
			
			return false;
		} else if(node instanceof AbstractBlock) {
			AbstractBlock block = (AbstractBlock) node;
			Boolean canDelete = true;
			for(Node child : block.getChildren()) {
				canDelete &= prune(child, tree);
			}
			if(node instanceof AbstractBlock.Seq) {
				return canDelete;
			} else {
				return false;
			}
		} else if(node instanceof AbstractTask) {
			return true;
		}
		return false;
	}
	
	public static Long calculateFrequency(Node node, ProcessTree tree) {
		
		if(nodeWeightMap.containsKey(node)) {
			return nodeWeightMap.get(node);
		}
	
		if(node instanceof AbstractTask.Manual) {
			AbstractTask task = (AbstractTask) node;
			Set<Node> preTasks = findPreTasks(node, tree);
			if(preTasks.isEmpty()) {
				long weight = logInfo.getVerticeWeight(task.getName());
				nodeWeightMap.put(node, weight);
				return weight;
			} else {
				long weight = 0;
				for(Node preTask : preTasks) {
					weight += logInfo.getEdgeWeight(preTask.getName(), task.getName());
				}
				nodeWeightMap.put(node, weight);
				return weight;
			}
		} else if(node instanceof AbstractTask.Automatic) {
			Set<Node> preTasks = findPreTasks(node, tree);
			Set<Node> nextTasks = findNextTasks(node, tree);
			if(preTasks == null || preTasks.isEmpty()) {
				long weight = 0;
				for(Node nextTask : nextTasks) {
					weight += logInfo.getVerticeWeight(nextTask.getName());
				}
				nodeWeightMap.put(node, weight);
				return weight;
			} else if(nextTasks == null || nextTasks.isEmpty()) {
				long weight = 0;
				for(Node preTask : preTasks) {
					weight += logInfo.getVerticeWeight(preTask.getName());
				}
				nodeWeightMap.put(node, weight);
				return weight;
			} else {
				long weight = 0;
				for(Node preTask : preTasks) {
					for(Node nextTask : nextTasks) {
						weight += logInfo.getEdgeWeight(preTask.getName(), nextTask.getName());
					}
				}
				nodeWeightMap.put(node, weight);
				return weight;
			}
		} else if(node instanceof AbstractBlock.Seq || node instanceof AbstractBlock.And
				|| node instanceof AbstractBlock.XorLoop || node instanceof AbstractBlock.DefLoop) {
			AbstractBlock block = (AbstractBlock) node;
			long firstWeight = 0;
			boolean first = true;
			for(Node child : block.getChildren()) {
				long weight = calculateFrequency(child, tree);
				if(first) {
					firstWeight = weight;
					first = false;
				}
			}
			nodeWeightMap.put(node, firstWeight);
			return firstWeight;
		} else if(node instanceof AbstractBlock.Xor || node instanceof AbstractBlock.Def || node instanceof AbstractBlock.Or) {
			AbstractBlock block = (AbstractBlock) node;
			long sumWeight = 0;
			List<Node> children = block.getChildren();
			for(Node child : children) {
				long weight = calculateFrequency(child, tree);
				sumWeight += weight;
			}
			nodeWeightMap.put(node, sumWeight);
			return sumWeight;
		}
		return (long) 0;
	}
	
	public static Node findPreNode(Node node, ProcessTree tree) {
		
		Node curNode = node;
		if(curNode.getParents().iterator().hasNext()) {
			AbstractBlock parent = (AbstractBlock) curNode.getParents().iterator().next();
			while(!(parent instanceof AbstractBlock.Seq || parent instanceof AbstractBlock.DefLoop || parent instanceof AbstractBlock.XorLoop)
					|| parent.getChildren().get(0) == curNode) {
				curNode = parent;
				if(curNode.getParents().iterator().hasNext()) {
					parent = (AbstractBlock) curNode.getParents().iterator().next();
				} else {
					return null;
				}
			}
			
			List<Node> children = parent.getChildren();
			for(int i = 0; i < children.size(); i++) {
				if(children.get(i + 1) == curNode) {
					return children.get(i);
				}
			}
		}
		
		return null;
	}
	
	public static Node findNextNode(Node node, ProcessTree tree) {
		
		Node curNode = node;
		if(curNode.getParents().iterator().hasNext()) {
			AbstractBlock parent = (AbstractBlock) curNode.getParents().iterator().next();
			while(!(parent instanceof AbstractBlock.Seq || parent instanceof AbstractBlock.DefLoop || parent instanceof AbstractBlock.XorLoop)
					|| parent.getChildren().get(parent.getChildren().size() - 1) == curNode) {
				curNode = parent;
				if(curNode.getParents().iterator().hasNext()) {
					parent = (AbstractBlock) curNode.getParents().iterator().next();
				} else {
					return null;
				}
			}
			
			List<Node> children = parent.getChildren();
			for(int i = 0; i < children.size() - 1; i++) {
				if(children.get(i) == curNode) {
					return children.get(i + 1);
				}
			}
		}
		
		return null;
	}
	
	public static Set<Node> findPreTasks(Node node, ProcessTree tree) {
		
		Node preNode = findPreNode(node, tree);
		if(preNode == null) {
			return new HashSet<>();
		}
		
		return findLastTasks(preNode, tree);
	}
	
	public static Set<Node> findNextTasks(Node node, ProcessTree tree) {
		
		Node nextNode = findNextNode(node, tree);
		if(nextNode == null) {
			return new HashSet<>();
		}
		
		Set<Node> nextTasks = new HashSet<>();
		nextTasks.addAll(findFirstTasks(nextNode, tree));
		Node parent = nextNode.getParents().iterator().next();
		if(parent instanceof AbstractBlock.DefLoop || parent instanceof AbstractBlock.XorLoop) {
			nextTasks.addAll(findNextTasks(parent, tree));
		}
		return nextTasks;
	}
	
	public static Set<Node> findFirstTasks(Node node, ProcessTree tree) {
		
		if(node instanceof AbstractTask.Manual) {
			Set<Node> lastTasks = new HashSet<>();
			lastTasks.add(node);
			return lastTasks;
		} else if(node instanceof AbstractTask.Automatic) {
			return findNextTasks(node, tree);
		} else if(node instanceof AbstractBlock.Seq || node instanceof AbstractBlock.And
				|| node instanceof AbstractBlock.DefLoop || node instanceof AbstractBlock.XorLoop) {
			AbstractBlock block = (AbstractBlock) node;
			List<Node> children = block.getChildren();
			return findFirstTasks(children.get(0), tree);
		} else if(node instanceof AbstractBlock) {
			AbstractBlock block = (AbstractBlock) node;
			Set<Node> lastTasks = new HashSet<>();
			for(Node child : block.getChildren()) {
				lastTasks.addAll(findFirstTasks(child, tree));
			}
			return lastTasks;
		}
		return new HashSet<>();
	}
	
	
	public static Set<Node> findLastTasks(Node node, ProcessTree tree) {
		
		if(node instanceof AbstractTask.Manual) {
			Set<Node> lastTasks = new HashSet<>();
			lastTasks.add(node);
			return lastTasks;
		} else if(node instanceof AbstractTask.Automatic) {
			return findPreTasks(node, tree);
		} else if(node instanceof AbstractBlock.Seq || node instanceof AbstractBlock.And) {
			AbstractBlock block = (AbstractBlock) node;
			List<Node> children = block.getChildren();
			return findLastTasks(children.get(children.size() - 1), tree);
		} else if(node instanceof AbstractBlock) {
			AbstractBlock block = (AbstractBlock) node;
			Set<Node> lastTasks = new HashSet<>();
			for(Node child : block.getChildren()) {
				lastTasks.addAll(findLastTasks(child, tree));
			}
			return lastTasks;
		}
		return new HashSet<>();
	}
	
	public static boolean removeTreeNode(Node node, ProcessTree tree) {
		
		if(node instanceof AbstractBlock.Seq) {
			AbstractBlock.Seq seq = (AbstractBlock.Seq) node;
			for(Node child : seq.getChildren()) {
				removeTreeNode(child, tree);
			}
		}
		
		if(node instanceof AbstractTask || node instanceof AbstractBlock.Seq) {
			List<Edge> incomingEdges = node.getIncomingEdges();
			Block parent = null;
			for(int i = 0; i < incomingEdges.size(); i++) {
				Edge edge = incomingEdges.get(i);
				parent = edge.getSource();
				parent.removeOutgoingEdge(edge);
				node.removeIncomingEdge(edge);
				tree.removeEdge(edge);
			}
			tree.removeNode(node);
			
			if(parent instanceof AbstractBlock) {
				Edge parentIncomingEdge = parent.getIncomingEdges().get(0);
				if(parent.getOutgoingEdges().size() == 1) {
					Edge parentOutgoingEdge = parent.getOutgoingEdges().get(0);
					Node target = parentOutgoingEdge.getTarget();
					parentIncomingEdge.setTarget(target);
					target.removeIncomingEdge(target.getIncomingEdges().get(0));
					target.addIncomingEdge(parentIncomingEdge);
					tree.removeEdge(parentOutgoingEdge);
					tree.removeNode(parent);
				}
			}
			return true;
		}
		return false;
	}
	
	public static String findRuleEvent(Node node, ProcessTree tree) {
		
		List<Node> preTaskList = new ArrayList<>();
		Set<Node> allPreTasks = new HashSet<>();
		Set<Node> preTasks = findPreTasks(node, tree);
		Queue<Node> queue = new LinkedList<>();
		for(Node preTask : preTasks) {
			queue.offer(preTask);
		}
		while(!queue.isEmpty()) {
			Node preTask = queue.poll();
			if(!allPreTasks.contains(preTask)) {
				preTaskList.add(preTask);
				allPreTasks.add(preTask);
				preTasks = findPreTasks(preTask, tree);
				for(Node preTask1 : preTasks) {
					queue.offer(preTask1);
				}
			}
		}
		
		Set<String> eventSet = new HashSet<>();
		for(Node preTask : preTaskList) {
			eventSet.add(preTask.toString());
		}
		for(XTrace trace : log) {
			
			boolean containEvent = containEvent(trace, node, tree);
			if(containEvent) {
				Set<String> curSet = new HashSet<>();
				for(XEvent event : trace) {
					String taskName = parameters.getClassifier().getClassIdentity(event);
					if(eventSet.contains(taskName)) {
						curSet.add(taskName);
					}
				}
				eventSet = curSet;
			}
		}
		
		String event = "Start";
		for(Node task : preTaskList) {
			if(eventSet.contains(task.toString())) {
				event = task.toString();
				break;
			}
		}
		return event;
	}
	
public static String findRuleCondition(Node node, ProcessTree tree) {
		
		List<Node> preTaskList = new ArrayList<>();
		Set<Node> allPreTasks = new HashSet<>();
		Set<Node> preTasks = findPreTasks(node, tree);
		Queue<Node> queue = new LinkedList<>();
		for(Node preTask : preTasks) {
			queue.offer(preTask);
		}
		while(!queue.isEmpty()) {
			Node preTask = queue.poll();
			if(!allPreTasks.contains(preTask)) {
				preTaskList.add(preTask);
				allPreTasks.add(preTask);
				preTasks = findPreTasks(preTask, tree);
				for(Node preTask1 : preTasks) {
					queue.offer(preTask1);
				}
			}
		}
		
		List<ArrayList<String>> dataList = new ArrayList<ArrayList<String>>();
		List<String> attributeList = new ArrayList<String>();
		
		for(Node preTask : preTaskList) {
			attributeList.add(preTask.toString());
		}
		
		Set<String> eventSet = new HashSet<>();
		for(Node preTask : preTaskList) {
			eventSet.add(preTask.toString());
		}
		
		for(XTrace trace : log) {
			Set<String> curSet = new HashSet<>();
			boolean containEvent = containEvent(trace, node, tree);
			for(XEvent event : trace) {
				String taskName = parameters.getClassifier().getClassIdentity(event);
				if(eventSet.contains(taskName)) {
					curSet.add(taskName);
				}
			}
			ArrayList<String> record = new ArrayList<>();
			for(String preTaskName : attributeList) {
				if(curSet.contains(preTaskName)) {
					record.add("1");
				} else {
					record.add("0");
				}
			}
			if(containEvent) {
				record.add("1");
			} else {
				record.add("0");
			}
			dataList.add(record);
		}
		
		DecisionTree dt = new DecisionTree();
        DecisionTree.TreeNode dtNode = dt.createDT(dataList, attributeList);
        String condition = parseConditionFromTree(dtNode);
		
		return condition;
	}

	public static String parseConditionFromTree(DecisionTree.TreeNode dtNode) {
		
		if(dtNode == null) return null;
		
		if("leafNode".equals(dtNode.getNodeName()) && "1".equals(dtNode.getTargetFunValue())) {
			return "";
		}
		
		List<String> pathNames = dtNode.getPathName();
		List<String> pathConditions = new ArrayList<>();
		int cnt = 0;
		for(DecisionTree.TreeNode child : dtNode.getChildTreeNode()) {
			String condition = parseConditionFromTree(child);
			pathConditions.add(condition);
			if(condition != null) {
				cnt++;
			}
	    }
		if(cnt == 0) return null;
		if(cnt == 2 && "".equals(pathConditions.get(0)) && "".equals(pathConditions.get(1))) return "";
		String condition = "";
		boolean first = true;
		for(int i = 0; i < cnt; i++) {
			if(pathConditions.get(i) != null) {
				if(!first) {
					condition = "(" + condition + ") OR (";
				}
				
				if("1".equals(pathNames.get(i))) {
					condition += "Exists(" + dtNode.getAttributeValue();
				} else {
					condition += "NotExists(" + dtNode.getAttributeValue() + ")";
				}
				if(pathConditions.get(i).length() > 0) {
					condition = "(" + condition + ") AND (" + pathConditions.get(i) + ")";
				}
				
				if(!first) {
					condition += ")";
				}
				
				first = false;
			}
		}
		return condition;
	}
	
	public static Boolean containEvent(XTrace trace, Node node, ProcessTree tree) {
		boolean containEvent = false;
		if(node instanceof AbstractTask.Manual) {
			for(XEvent event : trace) {
				String taskName = parameters.getClassifier().getClassIdentity(event);
				if(taskName.equals(node.toString())) {
					containEvent = true;
					break;
				}
			}
		} else if(node instanceof AbstractTask.Automatic) {
			Set<Node> preTasks = findPreTasks(node, tree);
			Set<Node> nextTasks = findNextTasks(node, tree);
			Set<String> preTaskNames = new HashSet<>();
			Set<String> nextTaskNames = new HashSet<>();
			
			for(Node preTask : preTasks) {
				preTaskNames.add(preTask.toString());
			}
			
			for(Node nextTask : nextTasks) {
				nextTaskNames.add(nextTask.toString());
			}
			
			for(int i = 0; i < trace.size() - 1; i++) {
				if(preTaskNames.contains(trace.get(i).toString()) && nextTaskNames.contains(trace.get(i + 1).toString())) {
					containEvent = true;
					break;
				}
			}
		}
		return containEvent;
	}
	
	public static void addProcessRule(String event, String condition, String actionType, String operation, String posType, String posNode, String content, List<ProcessRule> rules) {

		ProcessRule rule = new ProcessRule();
		rule.setEvent(event);
		rule.setCondition(condition);
		
		List<RuleAction> actions = new ArrayList<>();
		RuleAction action = new RuleAction();
		action.setType(actionType);
		action.setOperation(operation);
		String position = posType + "(" + posNode + ")";
		action.setPosition(position);
		action.setContent(content);
		actions.add(action);
		rule.setActions(actions);
		rules.add(rule);
	}
}
