package main.java.com.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class Node {

    private Map<Integer, String> cache; 
    private Integer id;
    private Repository repository;
    private Prefetch prefetch;
    private EvictionStrategy evictionStrategy;
    private static final int DEFAULT_THRESHOLD = 2;
    private static final int DEFAULT_CAPACITY = 100;

    public Node(Integer id) {
        this(id, DEFAULT_CAPACITY);
    }

    public Node(Integer id, int capacity) {
        this.id = id;
        this.cache = new LinkedHashMap<Integer, String>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                return size() > capacity;
            }
        };
        this.repository = Repository.getInstance();
        this.prefetch = FixedFetch.getInstance(DEFAULT_THRESHOLD);
        this.evictionStrategy = new LruEvictionStrategy(capacity);
    }

    public String getValue(int key) {
        if (cache.containsKey(key)) {
            evictionStrategy.onAccess(cache, key);
            return cache.get(key);
        }
        
        String value = repository.get(key);
        if (value != null) {
            evictionStrategy.onPut(cache, key, value);
            prefetch.prefetch(this, key);
        }
        return value;
    }

    public void putIntoCache(int key, String value) {
        evictionStrategy.onPut(cache, key, value);
    }

    public Integer getId() {
        return this.id; 
    }

    public Map<Integer, String> getCache() {
        return this.cache;
    }
}