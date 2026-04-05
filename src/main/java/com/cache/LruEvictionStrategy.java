package main.java.com.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruEvictionStrategy implements EvictionStrategy {
    private int capacity;

    public LruEvictionStrategy(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void onAccess(Map<Integer, String> cache, Integer key) {
        // Move accessed key to end (most recently used)
        // LinkedHashMap with access order handles this automatically
        if (cache instanceof LinkedHashMap) {
            String value = cache.get(key);
            cache.remove(key);
            cache.put(key, value);
        }
    }

    @Override
    public void onPut(Map<Integer, String> cache, Integer key, String value) {
        if (cache.containsKey(key)) {
            // Key exists - update and move to end (most recently used)
            cache.remove(key);
            cache.put(key, value);
        } else if (cache.size() >= capacity) {
            // Cache full - remove least recently used (first element)
            Integer eldestKey = cache.keySet().iterator().next();
            cache.remove(eldestKey);
            cache.put(key, value);
        } else {
            cache.put(key, value);
        }
    }

    public int getCapacity() {
        return capacity;
    }
}