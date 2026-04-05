package main.java.com.cache;

public class client {
    public static void main(String[] args) {
        INodeSelectionStrategy nodeSelectionStrategy = new ModuloNodeSelectionStrategy(); 
        Cluster cluster = new Cluster(nodeSelectionStrategy); 
        cluster.addNode(new Node(0));
        cluster.addNode(new Node(1));
        ApiServerGateway apiServerGateway = new ApiServerGateway(cluster);
        System.out.println("Value for key 4: " + apiServerGateway.get(4));
        System.out.println("Value for key 3: " + apiServerGateway.get(3));
    }
}
