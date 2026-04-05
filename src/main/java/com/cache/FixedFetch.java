package main.java.com.cache;

public class FixedFetch implements Prefetch {
    private static FixedFetch instance;
    private int threshold;
    private Repository repository;

    private FixedFetch(int threshold) {
        this.threshold = threshold;
        this.repository = Repository.getInstance();
    }

    public static synchronized FixedFetch getInstance(int threshold) {
        if (instance == null) {
            instance = new FixedFetch(threshold);
        }
        return instance;
    }

    @Override
    public void prefetch(Node node, int key) {
        int start = key - threshold;
        int end = key + threshold;

        for (int i = start; i <= end; i++) {
            if (i != key && repository.containsKey(i)) {
                String value = repository.get(i);
                node.putIntoCache(i, value);
            }
        }
    }
}