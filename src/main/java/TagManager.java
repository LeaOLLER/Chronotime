import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class TagManager {
    private static final String TAGS_FILE = "tags.json";
    private Map<String, List<String>> categoryTags;
    private final Gson gson;

    public TagManager() {
        this.categoryTags = new HashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadTags();
    }

    public void addTag(String category, String tag) {
        categoryTags.computeIfAbsent(category, k -> new ArrayList<>());
        if (!categoryTags.get(category).contains(tag)) {
            categoryTags.get(category).add(tag);
            saveTags();
        }
    }

    public List<String> getTagsForCategory(String category) {
        return categoryTags.getOrDefault(category, new ArrayList<>());
    }

    public void removeTag(String category, String tag) {
        if (categoryTags.containsKey(category)) {
            categoryTags.get(category).remove(tag);
            saveTags();
        }
    }

    private void saveTags() {
        try {
            String json = gson.toJson(categoryTags);
            Files.writeString(Paths.get(TAGS_FILE), json);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde des tags : " + e.getMessage());
        }
    }

    private void loadTags() {
        try {
            if (Files.exists(Paths.get(TAGS_FILE))) {
                String json = Files.readString(Paths.get(TAGS_FILE));
                categoryTags = gson.fromJson(json, new TypeToken<Map<String, List<String>>>(){}.getType());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des tags : " + e.getMessage());
        }
    }
} 