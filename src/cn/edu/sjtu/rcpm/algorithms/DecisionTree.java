package cn.edu.sjtu.rcpm.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DecisionTree {

    public TreeNode createDT(List<ArrayList<String>> data, List<String> attributeList){
        
        TreeNode node = new TreeNode();
        String result = InfoGain.isPure(InfoGain.getTarget(data));
        if(result != null) {
            node.setNodeName("leafNode");
            node.setTargetFunValue(result);
            return node;
        }
        
        if(attributeList.size() == 0) {
        	node.setNodeName("leafNode");
        	if(InfoGain.hasExistence(InfoGain.getTarget(data))) {
                node.setTargetFunValue("1");
        	} else {
                node.setTargetFunValue("1");
        	}
        	return node;
        } else {
            InfoGain gain = new InfoGain(data, attributeList);
            double maxGain = 0.0;
            int attrIndex = -1;
            for(int i = 0; i < attributeList.size(); i++) {
                double tempGain = gain.getGainRatio(i);
                
                if(tempGain == 0) {
                	for (int j = 0; j < data.size(); j++) {
                        data.get(j).remove(i);
                    }
                	attributeList.remove(i);
                	i--;
                }
                
                if(maxGain < tempGain) {
                	maxGain = tempGain;
                    attrIndex = i;
                }
            }
            
            if(attrIndex == -1) {
            	node.setNodeName("leafNode");
            	if(InfoGain.hasExistence(InfoGain.getTarget(data))) {
                    node.setTargetFunValue("1");
            	} else {
                    node.setTargetFunValue("1");
            	}
            	return node;
            }
            
            node.setAttributeValue(attributeList.get(attrIndex));
            List<ArrayList<String>> resultData = null;
            Map<String, Long> attrvalueMap = gain.getAttributeValue(attrIndex);
            
            for(Map.Entry<String, Long> entry : attrvalueMap.entrySet()){
                resultData = gain.getData4Value(entry.getKey(), attrIndex);
                TreeNode leafNode = null;
                if(resultData.size() == 0){
                    leafNode = new TreeNode();
                    leafNode.setNodeName(attributeList.get(attrIndex));
                    leafNode.setTargetFunValue(result);
                    leafNode.setAttributeValue(entry.getKey());
                } else {
                    for (int j = 0; j < resultData.size(); j++) {
                        resultData.get(j).remove(attrIndex);
                    }
                    ArrayList<String> resultAttr = new ArrayList<String>(attributeList);
                    resultAttr.remove(attrIndex);
                    leafNode = createDT(resultData, resultAttr);
                }
                node.getChildTreeNode().add(leafNode);
                node.getPathName().add(entry.getKey());
            }
        }
        return node;
    }
    
    class TreeNode {
        
        private String attributeValue;
        private List<TreeNode> childTreeNode;
        private List<String> pathName;
        private String targetFunValue;
        private String nodeName;
        
        public TreeNode(String nodeName){
            
            this.nodeName = nodeName;
            this.childTreeNode = new ArrayList<TreeNode>();
            this.pathName = new ArrayList<String>();
        }
        
        public TreeNode(){
            this.childTreeNode = new ArrayList<TreeNode>();
            this.pathName = new ArrayList<String>();
        }

        public String getAttributeValue() {
            return attributeValue;
        }

        public void setAttributeValue(String attributeValue) {
            this.attributeValue = attributeValue;
        }

        public List<TreeNode> getChildTreeNode() {
            return childTreeNode;
        }

        public void setChildTreeNode(List<TreeNode> childTreeNode) {
            this.childTreeNode = childTreeNode;
        }

        public String getTargetFunValue() {
            return targetFunValue;
        }

        public void setTargetFunValue(String targetFunValue) {
            this.targetFunValue = targetFunValue;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public List<String> getPathName() {
            return pathName;
        }

        public void setPathName(List<String> pathName) {
            this.pathName = pathName;
        }   
    }
}