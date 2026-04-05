package main.java.com.cache;

public class ApiServerGateway {

    //attributes
    private Cluster cluster; 
      
    
    //methods 
    ApiServerGateway(Cluster cluster){
        this.cluster = cluster; 
    }
    public String get(int key) {
        INodeSelectionStrategy nodeSelectionStrategy = cluster.getNodeSelectionStrategy();
        int nodeId = nodeSelectionStrategy.getNodeID(key, cluster.getNodeCount());
        Node node = cluster.getNode(nodeId); 
        return node.getValue(key); 
    }

}
