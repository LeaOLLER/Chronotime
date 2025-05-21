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

    private void sendWeeklyReport() throws IOException, InterruptedException {
        // Récupérer les sessions de la semaine
        LocalDateTime weekStart = LocalDateTime.now().with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        List<Session> weekSessions = sessionManager.getSessions().stream()
            .filter(s -> s.getDateTime().isAfter(weekStart))
            .collect(Collectors.toList());

        // Calculer les statistiques par catégorie
        Map<String, Integer> categoryDurations = new HashMap<>();
        Map<String, Integer> urlCounts = new HashMap<>();

        for (Session session : weekSessions) {
            categoryDurations.merge(session.getCategory(), session.getDuration(), Integer::sum);
            if (session.getTabStats() != null) {
                session.getTabStats().forEach((url, duration) -> 
                    urlCounts.merge(url, duration, Integer::sum));
            }
        }

        // Créer le message avec un format amélioré
        StringBuilder message = new StringBuilder();
        
        // En-tête avec date
        message.append("📊 **Rapport hebdomadaire**\n");
        message.append("*Semaine du ").append(weekStart.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("*\n\n");

        // Section temps par catégorie
        message.append("⏱️ **Temps par catégorie**\n");
        if (categoryDurations.isEmpty()) {
            message.append("*Aucune activité cette semaine*\n");
        } else {
            categoryDurations.forEach((category, duration) -> {
                String emoji = switch (category) {
                    case "Études" -> "📚";
                    case "Thales" -> "💼";
                    case "Lecture" -> "📖";
                    default -> "🎯";
                };
                int hours = duration / 3600;
                int minutes = (duration % 3600) / 60;
                message.append(String.format("%s **%s** : `%dh%02dm`\n", 
                    emoji, category, hours, minutes));
            });
        }

        // Section sites les plus visités
        message.append("\n🌐 **Top sites visités**\n");
        if (urlCounts.isEmpty()) {
            message.append("*Aucune navigation enregistrée*\n");
        } else {
            AtomicInteger rank = new AtomicInteger(1);
            urlCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    int hours = entry.getValue() / 3600;
                    int minutes = (entry.getValue() % 3600) / 60;
                    String medal = switch (rank.get()) {
                        case 1 -> "🥇";
                        case 2 -> "🥈";
                        case 3 -> "🥉";
                        default -> "•";
                    };
                    message.append(String.format("%s `%s` : **%dh%02dm**\n", 
                        medal, formatUrl(entry.getKey()), hours, minutes));
                    rank.incrementAndGet();
                });
        }

        // Statistiques globales
        int totalDuration = categoryDurations.values().stream().mapToInt(Integer::intValue).sum();
        int totalHours = totalDuration / 3600;
        int totalMinutes = (totalDuration % 3600) / 60;
        
        message.append("\n📈 **Statistiques globales**\n");
        message.append(String.format("⏰ Temps total : **%dh%02dm**\n", totalHours, totalMinutes));
        message.append(String.format("📝 Sessions : **%d**\n", weekSessions.size()));

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