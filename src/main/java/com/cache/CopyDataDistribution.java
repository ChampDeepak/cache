package main.java.com.cache;

import java.util.HashMap;
import java.util.Map;

public class CopyDataDistribution extends DataDistribution {

    CopyDataDistribution(INodeSelectionStrategy nodeSelectionStrategy, HashMap<Integer, Node> nodesMap) {
        super(nodeSelectionStrategy, nodesMap);
    }

    @Override
    void additionRearrange(Integer newNodeId) {
        // Iterate through all nodes and their keys
        for (Node node : nodesMap.values()) {
            Map<Integer, String> cache = node.getCache();
            if (cache != null) {
                // Check each key in this node's cache
                for (Integer key : cache.keySet()) {
                    // Determine which node this key should belong to based on strategy
                    int targetNodeID = nodeSelectionStrategy.getNodeID(key, nodesMap.size());
                    
                    // If the key should belong to the new node, copy it
                    if (targetNodeID == newNodeId) {
                        Node targetNode = nodesMap.get(newNodeId);
                        if (targetNode != null && targetNode.getCache() != null) {
                            targetNode.putIntoCache(key, cache.get(key));
                        }
                    }
                }
            }
        }
    }

    @Override
    void removalRearrange(Node oldNode) {
        if (nodesMap.isEmpty() || oldNode == null || oldNode.getCache() == null) {
            return;
        }
        
        Map<Integer, String> removedNodeCache = oldNode.getCache();
        
        // Iterate through keys in the removed node and redistribute to remaining nodes
        for (Integer key : removedNodeCache.keySet()) {
            int targetNodeID = nodeSelectionStrategy.getNodeID(key, nodesMap.size());
            Node targetNode = nodesMap.get(targetNodeID);
            
            if (targetNode != null && targetNode.getCache() != null) {
                targetNode.putIntoCache(key, removedNodeCache.get(key));
            }
        }
    }

}
