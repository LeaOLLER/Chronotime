import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SessionManager {
    private List<Session> sessions;
    private final MongoDBManager mongoManager;

    public SessionManager() {
        this.sessions = new ArrayList<>();
        this.mongoManager = new MongoDBManager();
        loadSessions();
    }

    public void addSession(Session session) {
        sessions.add(session);
        mongoManager.saveSession(session);
    }

    public void removeSession(Session session) {
        mongoManager.deleteSession(session);
        sessions.remove(session);
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
        sessions = mongoManager.loadAllSessions();
    }

    public void close() {
        if (mongoManager != null) {
            mongoManager.close();
        }
    }
} 