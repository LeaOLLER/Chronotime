import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Session {
    private String category;
    private LocalDateTime dateTime;
    private int duration;
    private String doneText;
    private String todoText;
    private int note;
    private String tag;
    private Map<String, Integer> tabStats; // Statistiques des onglets pour cette session
    private Map<String, Integer> tabOpenCounts; // Nombre d'ouvertures des onglets
    private Map<String, LocalDateTime> tabLastUsed; // Dernière utilisation des onglets
    private String mongoId;

    public Session(String category, LocalDateTime dateTime, int duration, String doneText, String todoText) {
        this.category = category;
        this.dateTime = dateTime;
        this.duration = duration;
        this.doneText = doneText;
        this.todoText = todoText;
        this.note = 3;
        this.tag = "";
        this.tabStats = new HashMap<>();
        this.tabOpenCounts = new HashMap<>();
        this.tabLastUsed = new HashMap<>();
    }

    public void setTabStats(Map<String, Integer> tabStats) {
        this.tabStats = new HashMap<>(tabStats);
    }

    public void setTabOpenCounts(Map<String, Integer> tabOpenCounts) {
        this.tabOpenCounts = new HashMap<>(tabOpenCounts);
    }

    public void setTabLastUsed(Map<String, LocalDateTime> tabLastUsed) {
        this.tabLastUsed = new HashMap<>(tabLastUsed);
    }

    public Map<String, Integer> getTabStats() {
        return tabStats;
    }

    public Map<String, Integer> getTabOpenCounts() {
        return tabOpenCounts;
    }

    public Map<String, LocalDateTime> getTabLastUsed() {
        return tabLastUsed;
    }

    public String getCategory() { return category; }
    public LocalDateTime getDateTime() { return dateTime; }
    public int getDuration() { return duration; }
    public String getDoneText() { return doneText; }
    public String getTodoText() { return todoText; }
    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getMongoId() {
        return mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }
} 