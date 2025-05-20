import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChromeTabTracker {
    private Map<String, Duration> tabTimes;
    private String currentTab;
    private ScheduledExecutorService scheduler;
    private static final Pattern URL_PATTERN = Pattern.compile("https?://(?:www\\.)?([^/]+)");

    public ChromeTabTracker() {
        this.tabTimes = new HashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        startTracking();
    }

    private void startTracking() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                String activeTab = getActiveChromeTab();
                if (activeTab != null) {
                    if (!activeTab.equals(currentTab)) {
                        currentTab = activeTab;
                    }
                    tabTimes.merge(currentTab, Duration.ofSeconds(1), Duration::plus);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private String getActiveChromeTab() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", "ps aux | grep chrome | grep -v grep");
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("--type=renderer")) {
                    Matcher matcher = URL_PATTERN.matcher(line);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Duration> getTabTimes() {
        return new HashMap<>(tabTimes);
    }

    public String getCurrentTab() {
        return currentTab;
    }

    public void stop() {
        scheduler.shutdown();
    }
} 