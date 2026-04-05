package main.java.com.cache;

import java.util.HashMap;

public class Cluster {
    private HashMap<Integer, Node> nodesMap; 
    private DataDistribution dataDistribution; 
    private INodeSelectionStrategy nodeSelectionStrategy; 
    
    Cluster(INodeSelectionStrategy nodeSelectionStrategy){
        this.nodeSelectionStrategy = nodeSelectionStrategy;  
        this.nodesMap = new HashMap<>();
    }

    private DataDistribution geDataDistribution(){
        if(dataDistribution==null){
            dataDistribution = new CopyDataDistribution(nodeSelectionStrategy, nodesMap);
        }
        return dataDistribution; 
    }

    public Node getNode(int nodeId) {
        // What if given node id is not in the map? -> may be state of map and 
        // nodeSelectionStrategy must be in sync. 
        return nodesMap.get(nodeId); 
    }

    public void addNode(Node newNode){
        nodesMap.put(newNode.getId(), newNode); 
        geDataDistribution().additionRearrange(newNode.getId());  
    }

    public void removeNode(int nodeId){
        Node oldNode = nodesMap.remove(nodeId); 
        geDataDistribution().removalRearrange(oldNode);
    }

    public INodeSelectionStrategy getNodeSelectionStrategy() {
        return this.nodeSelectionStrategy; 
    }

    public int getNodeCount() {
        return this.nodesMap.size(); 
    }
}
