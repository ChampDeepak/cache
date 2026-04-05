package main.java.com.cache;

import java.util.HashMap;

public abstract class DataDistribution {

    INodeSelectionStrategy nodeSelectionStrategy; 
    HashMap<Integer, Node> nodesMap; 
    DataDistribution(INodeSelectionStrategy nodeSelectionStrategy, HashMap<Integer, Node> nodesMap){
        this.nodeSelectionStrategy = nodeSelectionStrategy; 
        this.nodesMap = nodesMap;
    }
     
    //rearrange data across all nodes when new node is added
    abstract void additionRearrange(Integer integer);


    //rearrange data across all nodes when node is removed
    abstract void removalRearrange(Node oldNode);

}
