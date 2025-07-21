import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.time.Duration;
import java.util.stream.Collectors;
import org.json.JSONObject;
import java.util.concurrent.atomic.AtomicInteger;

public class DiscordReporter {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1374457931289985204/JJdFU9cPpxgZnql5wHzZ8nZTwR_JcswKYVxROwsRpbSguNE2IEwMKNIdo3PGB2_mJx3m";
    private final SessionManager sessionManager;
    private Timer timer;

    public DiscordReporter(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.timer = new Timer(true);
        scheduleWeeklyReport();
    }

    private void scheduleWeeklyReport() {
        // Calculer le prochain dimanche à 20h00
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                                 .withHour(20)
                                 .withMinute(0)
                                 .withSecond(0);
        
        // Si on est dimanche après 20h, prendre le dimanche suivant
        if (now.getDayOfWeek() == DayOfWeek.SUNDAY && now.getHour() >= 20) {
            nextRun = nextRun.plusWeeks(1);
        }
        
        // Convertir en milliseconds
        long delay = Duration.between(now, nextRun).toMillis();
        long weekInMillis = 7 * 24 * 60 * 60 * 1000; // Une semaine en millisecondes

        System.out.println("Prochain rapport prévu le " + nextRun.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")));

        // Planifier l'exécution hebdomadaire
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    sendWeeklyReport();
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'envoi du rapport Discord : " + e.getMessage());
                }
            }
        }, delay, weekInMillis);
    }

    /**
     * Génère et envoie immédiatement un rapport hebdomadaire sur Discord
     * @return true si le rapport a été envoyé avec succès, false sinon
     */
    public boolean sendManualWeeklyReport() {
        try {
            sendWeeklyReport();
            System.out.println("Rapport hebdomadaire envoyé manuellement avec succès !");
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi manuel du rapport Discord : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void sendWeeklyReport() throws IOException, InterruptedException {
        // Récupérer les sessions de la semaine
        LocalDateTime weekStart = LocalDateTime.now().with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        List<Session> weekSessions = sessionManager.getSessions().stream()
            .filter(s -> s.getDateTime().isAfter(weekStart))
            .collect(Collectors.toList());

        if (weekSessions.isEmpty()) {
            // Envoyer un message simple si aucune activité
            JSONObject json = new JSONObject();
            json.put("content", "**Rapport hebdomadaire**\nAucune activité enregistrée cette semaine.");
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEBHOOK_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
            
            client.send(request, HttpResponse.BodyHandlers.ofString());
            return;
        }

        // Calculer les statistiques par catégorie
        Map<String, Integer> categoryDurations = new HashMap<>();
        Map<String, Integer> tagDurations = new HashMap<>();
        Map<String, Integer> urlCounts = new HashMap<>();
        
        // Statistiques de productivité
        Map<DayOfWeek, List<Integer>> notesByDay = new HashMap<>();
        Map<Integer, List<Integer>> notesByHour = new HashMap<>();
        Map<Integer, Integer> noteDistribution = new HashMap<>();
        
        for (Session session : weekSessions) {
            categoryDurations.merge(session.getCategory(), session.getDuration(), Integer::sum);
            
            // Ajouter les statistiques par tag
            String tag = session.getTag();
            if (tag != null && !tag.trim().isEmpty()) {
                tagDurations.merge(tag.trim(), session.getDuration(), Integer::sum);
            }
            
            if (session.getTabStats() != null) {
                session.getTabStats().forEach((url, duration) -> 
                    urlCounts.merge(url, duration, Integer::sum));
            }
            
            // Analyser la productivité
            DayOfWeek dayOfWeek = session.getDateTime().getDayOfWeek();
            int hour = session.getDateTime().getHour();
            int note = session.getNote();
            
            notesByDay.computeIfAbsent(dayOfWeek, k -> new ArrayList<>()).add(note);
            notesByHour.computeIfAbsent(hour, k -> new ArrayList<>()).add(note);
            noteDistribution.merge(note, 1, Integer::sum);
        }

        // Créer le message avec un format amélioré
        StringBuilder message = new StringBuilder();
        
        // En-tête avec date et indication de rapport manuel
        message.append("**RAPPORT HEBDOMADAIRE**");
        
        // Ajouter une indication si c'est un rapport manuel
        Thread currentThread = Thread.currentThread();
        if (!currentThread.getName().startsWith("Timer-")) {
            message.append(" (Généré manuellement)");
        }
        
        message.append("\nSemaine du ").append(weekStart.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");

        // Section temps par catégorie
        message.append("**TEMPS PAR CATÉGORIE**\n");
        categoryDurations.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> {
                int hours = entry.getValue() / 3600;
                int minutes = (entry.getValue() % 3600) / 60;
                message.append(String.format("**%s** : %dh%02dm\n", 
                    entry.getKey(), hours, minutes));
            });

        // Section temps par tag
        if (!tagDurations.isEmpty()) {
            message.append("\n**TEMPS PAR TAG**\n");
            tagDurations.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5) // Limiter aux 5 premiers
                .forEach(entry -> {
                    int hours = entry.getValue() / 3600;
                    int minutes = (entry.getValue() % 3600) / 60;
                    message.append(String.format("**%s** : %dh%02dm\n", 
                        entry.getKey(), hours, minutes));
                });
        }

        // Section PRODUCTIVITÉ
        message.append("\n**ANALYSE DE PRODUCTIVITÉ**\n");
        
        // Note moyenne globale
        double averageNote = weekSessions.stream()
            .mapToInt(Session::getNote)
            .average()
            .orElse(0.0);
        message.append(String.format("Note moyenne : %.1f/5\n", averageNote));
        
        // Répartition des notes
        message.append("Répartition des notes : ");
        for (int i = 1; i <= 5; i++) {
            int count = noteDistribution.getOrDefault(i, 0);
            if (count > 0) {
                message.append(String.format("%d★(%d) ", i, count));
            }
        }
        message.append("\n");
        
        // Jour le plus productif
        Map.Entry<DayOfWeek, Double> bestDay = notesByDay.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0)
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);
            
        if (bestDay != null) {
            String dayName = getDayName(bestDay.getKey());
            message.append(String.format("Jour le plus productif : %s (%.1f/5)\n", 
                dayName, bestDay.getValue()));
        }
        
        // Créneau horaire le plus productif
        Map.Entry<Integer, Double> bestHour = notesByHour.entrySet().stream()
            .filter(entry -> entry.getValue().size() >= 2) // Au moins 2 sessions pour être significatif
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0)
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);
            
        if (bestHour != null) {
            message.append(String.format("Créneau le plus productif : %02dh-%02dh (%.1f/5)\n", 
                bestHour.getKey(), bestHour.getKey() + 1, bestHour.getValue()));
        }

        // Section sites les plus visités (top 3 seulement)
        if (!urlCounts.isEmpty()) {
            message.append("\n**TOP SITES VISITÉS**\n");
            AtomicInteger rank = new AtomicInteger(1);
            urlCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    int hours = entry.getValue() / 3600;
                    int minutes = (entry.getValue() % 3600) / 60;
                    message.append(String.format("%d. %s : %dh%02dm\n", 
                        rank.getAndIncrement(), formatUrl(entry.getKey()), hours, minutes));
                });
        }

        // Statistiques globales
        int totalDuration = categoryDurations.values().stream().mapToInt(Integer::intValue).sum();
        int totalHours = totalDuration / 3600;
        int totalMinutes = (totalDuration % 3600) / 60;
        
        message.append("\n**STATISTIQUES GLOBALES**\n");
        message.append(String.format("Temps total : %dh%02dm\n", totalHours, totalMinutes));
        message.append(String.format("Sessions : %d\n", weekSessions.size()));
        if (!tagDurations.isEmpty()) {
            message.append(String.format("Tags utilisés : %d\n", tagDurations.size()));
        }
        
        // Temps moyen par session
        double avgSessionDuration = (double) totalDuration / weekSessions.size() / 3600.0;
        message.append(String.format("Durée moyenne/session : %.1fh\n", avgSessionDuration));

        // Envoyer le message à Discord
        JSONObject json = new JSONObject();
        json.put("content", message.toString());

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(WEBHOOK_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 204) {
            throw new IOException("Erreur lors de l'envoi du message Discord : " + response.statusCode());
        }
    }
    
    private String getDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Lundi";
            case TUESDAY -> "Mardi";
            case WEDNESDAY -> "Mercredi";
            case THURSDAY -> "Jeudi";
            case FRIDAY -> "Vendredi";
            case SATURDAY -> "Samedi";
            case SUNDAY -> "Dimanche";
        };
    }

    private String formatUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        // Nettoyer l'URL pour l'affichage
        String domain = url.toLowerCase();
        if (domain.startsWith("http://")) {
            domain = domain.substring(7);
        } else if (domain.startsWith("https://")) {
            domain = domain.substring(8);
        }
        
        if (domain.startsWith("www.")) {
            domain = domain.substring(4);
        }
        
        int slashIndex = domain.indexOf('/');
        if (slashIndex != -1) {
            domain = domain.substring(0, slashIndex);
        }
        
        return domain;
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
} 