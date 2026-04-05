package main.java.com.cache;

public class ModuloNodeSelectionStrategy implements INodeSelectionStrategy{

    @Override
    public int getNodeID(int key, int nodeCount) {
        return key%nodeCount; 
    }

}
