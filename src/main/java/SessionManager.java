import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

public class SessionManager {
    private List<Session> sessions;
    private static final String SAVE_FILE = "sessions.json";
    private final Gson gson;

    public SessionManager() {
        this.sessions = new ArrayList<>();
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
        loadSessions();
    }

    public void addSession(Session session) {
        sessions.add(session);
        saveSessions();
    }

    public void removeSession(Session session) {
        sessions.remove(session);
        saveSessions();
    }

    public List<Session> getSessionsByCategory(String category) {
        if (category.equals("Tout")) {
            return new ArrayList<>(sessions);
        }
        return sessions.stream()
            .filter(s -> s.getCategory().equals(category))
            .toList();
    }

    public Map<LocalDate, Integer> getDailyDurations(String category) {
        Map<LocalDate, Integer> dailyDurations = new TreeMap<>();
        
        for (Session session : sessions) {
            if (category.equals("Tout") || session.getCategory().equals(category)) {
                LocalDate date = session.getDateTime().toLocalDate();
                dailyDurations.merge(date, session.getDuration(), Integer::sum);
            }
        }
        
        return dailyDurations;
    }

    public List<Session> getSessions() {
        return new ArrayList<>(sessions);
    }

    public Map<String, Integer> getCategoryStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (Session session : sessions) {
            stats.merge(session.getCategory(), session.getDuration(), Integer::sum);
        }
        return stats;
    }

    private void loadSessions() {
        try {
            if (Files.exists(Paths.get(SAVE_FILE))) {
                String json = Files.readString(Paths.get(SAVE_FILE));
                Session[] loadedSessions = gson.fromJson(json, Session[].class);
                sessions = new ArrayList<>(Arrays.asList(loadedSessions));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSessions() {
        try {
            String json = gson.toJson(sessions);
            Files.writeString(Paths.get(SAVE_FILE), json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 