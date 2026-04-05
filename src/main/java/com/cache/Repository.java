package main.java.com.cache;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Repository {
    private static Repository instance;
    private Map<Integer, String> database;

    private Repository() {
        database = new HashMap<>();
        loadDatabase();
    }

    public static synchronized Repository getInstance() {
        if (instance == null) {
            instance = new Repository();
        }
        return instance;
    }

    private void loadDatabase() {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/java/com/cache/fake-db.json")) {
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
            for (Object key : jsonObject.keySet()) {
                Integer intKey = Integer.parseInt((String) key);
                String value = (String) jsonObject.get(key);
                database.put(intKey, value);
            }
        } catch (IOException | ParseException e) {
            System.err.println("Error loading database: " + e.getMessage());
        }
    }

    public String get(Integer key) {
        return database.get(key);
    }

    public boolean containsKey(Integer key) {
        return database.containsKey(key);
    }
}