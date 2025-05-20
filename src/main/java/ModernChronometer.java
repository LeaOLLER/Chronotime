import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Modality;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.io.IOException;
import javafx.collections.FXCollections;

public class ModernChronometer extends Application {
    private Timeline timeline;
    private Label timeLabel;
    private Label tabLabel;
    private Button startButton;
    private ComboBox<String> categoryBox;
    private int seconds = 0;
    private boolean running = false;
    private SessionManager sessionManager;
    private ChromeTabTracker tabTracker;
    private double xOffset = 0;
    private double yOffset = 0;
    private Stage stage;
    private TabInfoServer tabInfoServer;
    private int extensionSeconds = 0;
    private String lastTabType = "";
    private Map<String, Integer> tabTypeSeconds = new HashMap<>();
    private String lastTabTypeKey = "";
    private Map<String, Integer> tabTitleSeconds = new HashMap<>();
    private Map<String, Integer> tabOpenCounts = new HashMap<>();
    private Map<String, LocalDateTime> tabLastUsed = new HashMap<>();
    private ComboBox<Integer> noteCombo;
    private int note = 3; // valeur par défaut
    private HBox starsBox;
    private Label durationLabel;
    private Label doneLabel;
    private TextArea doneTextArea;
    private Label todoLabel;
    private TextArea todoTextArea;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        sessionManager = new SessionManager();
        tabTracker = new ChromeTabTracker();
        try {
            tabInfoServer = new TabInfoServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Fenêtre sans bordures et toujours au premier plan
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setAlwaysOnTop(true);

        // Conteneur principal
        VBox root = new VBox(10);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 15;");
        root.setPadding(new Insets(15));
        root.setEffect(new DropShadow(10, Color.GRAY));

        // Barre de titre personnalisée
        HBox titleBar = createTitleBar(primaryStage);
        
        // Menu déroulant des catégories
        categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Études", "Thales", "Lecture", "Autre");
        categoryBox.setValue("Études");
        categoryBox.setStyle("-fx-background-radius: 15; -fx-font-size: 14;");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        // Label du temps
        timeLabel = new Label("00:00:00");
        timeLabel.setStyle("-fx-font-size: 40; -fx-font-family: 'Segoe UI'; -fx-text-fill: #4CAF50;");
        timeLabel.setAlignment(Pos.CENTER);
        timeLabel.setMaxWidth(Double.MAX_VALUE);

        // Label pour l'onglet actif
        tabLabel = new Label("Aucun onglet détecté");
        tabLabel.setStyle("-fx-font-size: 12; -fx-font-family: 'Segoe UI'; -fx-text-fill: #666666;");
        tabLabel.setAlignment(Pos.CENTER);
        tabLabel.setMaxWidth(Double.MAX_VALUE);
        tabLabel.setWrapText(true);

        // Mise à jour de la couleur en fonction de la catégorie
        categoryBox.setOnAction(e -> updateTimeLabelColor());

        // Conteneur pour les boutons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        // Boutons
        startButton = createButton("▶", "#4CAF50");
        Button resetButton = createButton("⟲", "#FF9800");
        Button finishButton = createButton("✓", "#2196F3");
        Button statsButton = createButton("📊", "#9C27B0");

        // Actions des boutons
        startButton.setOnAction(e -> toggleChronometer());
        resetButton.setOnAction(e -> resetChronometer());
        finishButton.setOnAction(e -> finishSession());
        statsButton.setOnAction(e -> showStats());

        buttonBox.getChildren().addAll(startButton, resetButton, finishButton, statsButton);

        // Configuration du chronomètre
        setupChronometer();

        // Initialisation des composants pour la fin de session (non affichés initialement)
        Label extensionStat = new Label("Temps passé sur des extensions Chrome : " +
            String.format("%02d:%02d:%02d", extensionSeconds / 3600, (extensionSeconds % 3600) / 60, extensionSeconds % 60));

        // Note de la session
        Label noteLabel = new Label("Note de la session :");
        starsBox = new HBox(2);
        starsBox.setAlignment(Pos.CENTER_LEFT);

        for (int i = 1; i <= 5; i++) {
            Label star = new Label("☆");
            star.setStyle("-fx-font-size: 22; -fx-cursor: hand;");
            final int rating = i;
            star.setOnMouseClicked(e -> {
                note = rating;
                updateStars();
            });
            starsBox.getChildren().add(star);
        }
        updateStars();

        // Initialisation des composants pour la durée, les tâches faites et à faire
        durationLabel = new Label("Durée de la session");
        durationLabel.setStyle("-fx-font-size: 14;");

        doneLabel = new Label("Tâches accomplies :");
        doneTextArea = new TextArea();
        doneTextArea.setPrefRowCount(3);
        doneTextArea.setWrapText(true);

        todoLabel = new Label("Tâches à faire :");
        todoTextArea = new TextArea();
        todoTextArea.setPrefRowCount(3);
        todoTextArea.setWrapText(true);

        // Ajout uniquement des composants essentiels
        root.getChildren().addAll(
            titleBar,
            categoryBox,
            timeLabel,
            tabLabel,
            buttonBox
        );

        // Configuration de la scène
        Scene scene = new Scene(root);
        scene.setFill(null);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Met à jour le label de l'onglet actif toutes les secondes
        Timeline tabUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (tabInfoServer != null) {
                String currentTab = tabInfoServer.getCurrentTabTitle();
                String currentUrl = tabInfoServer.getCurrentTabUrl();
                
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    // Formatage de l'URL pour l'affichage
                    String displayUrl = formatUrl(currentUrl);
                    tabLabel.setText(displayUrl);
                    
                    // Mise à jour des statistiques avec l'URL
                    tabTitleSeconds.merge(currentUrl, 1, Integer::sum);
                    tabOpenCounts.merge(currentUrl, 1, Integer::sum);
                    tabLastUsed.put(currentUrl, LocalDateTime.now());
                    
                    // Vérification si c'est une extension
                    if (currentUrl.contains("extension") || currentUrl.contains("Extension")) {
                        extensionSeconds++;
                    }
                } else if (currentTab != null && !currentTab.isEmpty()) {
                    tabLabel.setText(currentTab);
                }
            }
        }));
        tabUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        tabUpdateTimeline.play();
    }

    private HBox createTitleBar(Stage primaryStage) {
        HBox titleBar = new HBox();
        titleBar.setStyle("-fx-background-color: #2c2c2c; -fx-background-radius: 15 15 0 0;");
        titleBar.setPadding(new Insets(5));
        
        // Permet de déplacer la fenêtre
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        
        titleBar.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        });

        // Bouton de fermeture
        Button closeButton = new Button("×");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16;");
        closeButton.setOnAction(e -> primaryStage.close());
        
        titleBar.getChildren().add(closeButton);
        HBox.setMargin(closeButton, new Insets(0, 0, 0, 10));
        
        return titleBar;
    }

    private Button createButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-text-fill: white;
            -fx-font-size: 16;
            -fx-background-radius: 20;
            -fx-min-width: 40;
            -fx-min-height: 40;
            """, color));

        button.setOnMouseEntered(e -> button.setStyle(button.getStyle() + "-fx-opacity: 0.8;"));
        button.setOnMouseExited(e -> button.setStyle(button.getStyle().replace("-fx-opacity: 0.8;", "")));

        return button;
    }

    private void setupChronometer() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (running) {
                seconds++;
                updateTimeLabel();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void toggleChronometer() {
        running = !running;
        if (running) {
            startButton.setText("⏸");
            timeline.play();
        } else {
            startButton.setText("▶");
            timeline.pause();
        }
    }

    private void resetChronometer() {
        running = false;
        seconds = 0;
        startButton.setText("▶");
        timeline.stop();
        updateTimeLabel();
    }

    private void finishSession() {
        if (seconds > 0) {
            // Créer une nouvelle fenêtre pour la fin de session
            Stage endDialog = new Stage();
            endDialog.initOwner(stage);
            endDialog.initModality(Modality.APPLICATION_MODAL);
            endDialog.setTitle("Fin de session");

            VBox dialogRoot = new VBox(10);
            dialogRoot.setPadding(new Insets(15));
            dialogRoot.setStyle("-fx-background-color: white;");

            // Durée de la session
            Label sessionDurationLabel = new Label(String.format("Durée de la session: %02d:%02d:%02d", 
                seconds / 3600, (seconds % 3600) / 60, seconds % 60));
            sessionDurationLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

            // Extension stats
            Label extensionStat = new Label("Temps passé sur des extensions Chrome : " +
                String.format("%02d:%02d:%02d", extensionSeconds / 3600, (extensionSeconds % 3600) / 60, extensionSeconds % 60));

            // Note de la session
            Label noteLabel = new Label("Note de la session :");
            HBox starsBox = new HBox(2);
            starsBox.setAlignment(Pos.CENTER_LEFT);
            int[] currentNote = {3}; // Pour stocker la note actuelle

            for (int i = 1; i <= 5; i++) {
                Label star = new Label("☆");
                star.setStyle("-fx-font-size: 22; -fx-cursor: hand;");
                final int rating = i;
                star.setOnMouseClicked(e -> {
                    currentNote[0] = rating;
                    updateStarsInDialog(starsBox, rating);
                });
                starsBox.getChildren().add(star);
            }
            updateStarsInDialog(starsBox, currentNote[0]);

            // Tâches accomplies
            Label doneLabel = new Label("Tâches accomplies :");
            TextArea doneTextArea = new TextArea();
            doneTextArea.setPrefRowCount(3);
            doneTextArea.setWrapText(true);

            // Tâches à faire
            Label todoLabel = new Label("Tâches à faire :");
            TextArea todoTextArea = new TextArea();
            todoTextArea.setPrefRowCount(3);
            todoTextArea.setWrapText(true);

            // Boutons
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);
            Button saveButton = new Button("Enregistrer");
            Button cancelButton = new Button("Annuler");

            saveButton.setOnAction(e -> {
                Session session = new Session(
                    categoryBox.getValue(),
                    LocalDateTime.now(),
                    seconds,
                    doneTextArea.getText(),
                    todoTextArea.getText()
                );
                session.setNote(currentNote[0]);
                
                // Sauvegarder les statistiques Chrome
                session.setTabStats(new HashMap<>(tabTitleSeconds));
                session.setTabOpenCounts(new HashMap<>(tabOpenCounts));
                session.setTabLastUsed(new HashMap<>(tabLastUsed));
                
                sessionManager.addSession(session);
                endDialog.close();
                resetChronometer();
                
                // Réinitialiser les statistiques Chrome pour la nouvelle session
                tabTitleSeconds.clear();
                tabOpenCounts.clear();
                tabLastUsed.clear();
                extensionSeconds = 0;
            });

            cancelButton.setOnAction(e -> {
                endDialog.close();
            });

            buttonBox.getChildren().addAll(saveButton, cancelButton);

            // Ajouter tous les composants à la boîte de dialogue
            dialogRoot.getChildren().addAll(
                sessionDurationLabel,
                extensionStat,
                noteLabel,
                starsBox,
                doneLabel,
                doneTextArea,
                todoLabel,
                todoTextArea,
                buttonBox
            );

            Scene dialogScene = new Scene(dialogRoot);
            endDialog.setScene(dialogScene);
            endDialog.showAndWait();
        }
    }

    private void updateStarsInDialog(HBox starsBox, int note) {
        for (int i = 0; i < 5; i++) {
            Label star = (Label) starsBox.getChildren().get(i);
            if (i < note) {
                star.setText("★");
                star.setStyle("-fx-font-size: 22; -fx-text-fill: #FFD700; -fx-cursor: hand;");
            } else {
                star.setText("☆");
                star.setStyle("-fx-font-size: 22; -fx-text-fill: #bbb; -fx-cursor: hand;");
            }
        }
    }

    private void updateTimeLabel() {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        timeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, secs));
    }

    private void updateTimeLabelColor() {
        String category = categoryBox.getValue();
        String color = switch (category) {
            case "Études" -> "#4CAF50";
            case "Thales" -> "#2196F3";
            case "Lecture" -> "#FF9800";
            default -> "#9C27B0";
        };
        timeLabel.setStyle("-fx-font-size: 40; -fx-font-family: 'Segoe UI'; -fx-text-fill: " + color + ";");
    }

    private void showStats() {
        StatsWindow statsWindow = new StatsWindow(sessionManager, tabTracker, tabTitleSeconds, tabOpenCounts, tabLastUsed);
        statsWindow.display();
    }

    private void updateStars() {
        for (int i = 0; i < 5; i++) {
            Label star = (Label) starsBox.getChildren().get(i);
            if (i < note) {
                star.setText("★");
                star.setStyle("-fx-font-size: 22; -fx-text-fill: #FFD700; -fx-cursor: hand;");
            } else {
                star.setText("☆");
                star.setStyle("-fx-font-size: 22; -fx-text-fill: #bbb; -fx-cursor: hand;");
            }
        }
    }

    private String formatUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        // Si l'URL commence par http:// ou https://, on l'enlève
        String domain = url.toLowerCase();
        if (domain.startsWith("http://")) {
            domain = domain.substring(7);
        } else if (domain.startsWith("https://")) {
            domain = domain.substring(8);
        }
        
        // Pour les URLs chrome://, on retourne juste "chrome"
        if (domain.startsWith("chrome://")) {
            return "chrome";
        }
        
        // Enlever tout ce qui vient après le premier /
        int slashIndex = domain.indexOf('/', domain.indexOf("//") + 2);
        if (slashIndex != -1) {
            domain = domain.substring(0, slashIndex);
        }
        
        // Enlever www. si présent
        if (domain.startsWith("www.")) {
            domain = domain.substring(4);
        }
        
        return domain;
    }

    @Override
    public void stop() {
        if (tabTracker != null) {
            tabTracker.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}