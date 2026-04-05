package main.java.com.cache;

import java.util.Map;

public interface EvictionStrategy {
    void onAccess(Map<Integer, String> cache, Integer key);
    void onPut(Map<Integer, String> cache, Integer key, String value);
}