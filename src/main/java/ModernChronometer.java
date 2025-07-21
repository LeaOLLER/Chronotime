import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ModernChronometer extends Application {
    private static final int WEBSOCKET_PORT = 12345;
    
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
    private ConfigServer configServer;
    private DiscordReporter discordReporter;
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
    private TagManager tagManager;
    private ComboBox<String> tagBox;
    private Button addTagButton;

    private static void cleanupWebSocketPort() {
        try {
            // D'abord, vérifier si le port est utilisé
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", WEBSOCKET_PORT), 100);
                // Si on arrive ici, le port est occupé
                System.out.println("Port " + WEBSOCKET_PORT + " est occupé, tentative de libération...");
                
                // Utiliser lsof pour trouver le PID
                Process lsofProcess = Runtime.getRuntime().exec("lsof -t -i:" + WEBSOCKET_PORT);
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(lsofProcess.getInputStream()));
                String pid = reader.readLine();
                
                if (pid != null && !pid.trim().isEmpty()) {
                    // Tuer le processus avec SIGKILL
                    Runtime.getRuntime().exec("kill -9 " + pid.trim());
                    System.out.println("Processus " + pid.trim() + " tué.");
                }
                
                // Utiliser aussi fuser comme backup
                Runtime.getRuntime().exec("fuser -k -9 " + WEBSOCKET_PORT + "/tcp");
                
                // Attendre que le port soit libéré
                Thread.sleep(2000);
            }
        } catch (IOException e) {
            // Si on arrive ici avec une IOException, c'est probablement que le port est déjà libre
            System.out.println("Port " + WEBSOCKET_PORT + " est libre.");
        } catch (Exception e) {
            System.err.println("Erreur lors du nettoyage du port : " + e.getMessage());
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        sessionManager = new SessionManager();
        tabTracker = new ChromeTabTracker();
        tagManager = new TagManager();
        
        // Initialiser le reporter Discord
        discordReporter = new DiscordReporter(sessionManager);
        
        // Démarrer le serveur de configuration
        try {
            configServer = new ConfigServer();
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur de configuration : " + e.getMessage());
        }
        
        // Tentative de démarrage du serveur WebSocket
        try {
            tabInfoServer = new TabInfoServer();
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur WebSocket : " + e.getMessage());
            // Afficher une alerte à l'utilisateur
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de démarrage");
            alert.setHeaderText("Impossible de démarrer le serveur WebSocket");
            alert.setContentText("Le suivi des onglets Chrome ne sera pas disponible.\nErreur : " + e.getMessage());
            alert.showAndWait();
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
        categoryBox.getItems().addAll("Thales", "Études", "Perso", "ACADOMIA");
        categoryBox.setValue("Thales");
        categoryBox.setStyle("-fx-background-radius: 15; -fx-font-size: 14;");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        // Conteneur pour les tags
        HBox tagBoxContainer = new HBox(10);
        tagBoxContainer.setAlignment(Pos.CENTER);

        // Menu déroulant des tags
        tagBox = new ComboBox<>();
        tagBox.setPromptText("Sélectionner un tag");
        tagBox.setStyle("-fx-background-radius: 15; -fx-font-size: 14;");
        tagBox.setMaxWidth(Double.MAX_VALUE);

        // Bouton pour ajouter un nouveau tag
        addTagButton = new Button("+");
        addTagButton.setStyle("-fx-background-radius: 15; -fx-font-size: 14;");
        addTagButton.setOnAction(e -> showAddTagDialog());

        tagBoxContainer.getChildren().addAll(tagBox, addTagButton);

        // Mise à jour des tags quand la catégorie change
        categoryBox.setOnAction(e -> {
            updateTimeLabelColor();
            updateTags();
        });

        // Charger les tags initiaux
        updateTags();

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

        // Conteneur pour les boutons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        // Boutons
        Button playButton = createImageButton("play.png", "#4CAF50");
        Button resetButton = createImageButton("replay.png", "#FF9800");
        Button finishButton = createImageButton("check.png", "#2196F3");
        Button statsButton = createImageButton("stats.png", "#9C27B0");
        Button discordButton = createImageButton("report.png", "#5865F2");

        // Actions des boutons
        playButton.setOnAction(e -> toggleChronometer());
        resetButton.setOnAction(e -> resetChronometer());
        finishButton.setOnAction(e -> finishSession());
        statsButton.setOnAction(e -> showStats());
        discordButton.setOnAction(e -> sendDiscordReport());

        buttonBox.getChildren().addAll(playButton, resetButton, finishButton, statsButton, discordButton);

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
            tagBoxContainer,
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

        // Bouton de fermeture avec gestion agressive de l'arrêt
        Button closeButton = new Button("×");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16;");
        closeButton.setOnAction(e -> {
            // Arrêter le chronomètre
            if (timeline != null) {
                timeline.stop();
            }
            
            // Arrêter le serveur WebSocket de manière agressive
            if (tabInfoServer != null) {
                tabInfoServer.stop();
            }
            
            // Forcer la fermeture de tous les processus
            try {
                Runtime.getRuntime().exec("fuser -k -9 " + WEBSOCKET_PORT + "/tcp");
                Thread.sleep(200);
            } catch (Exception ex) {
                // Ignorer les erreurs
            }
            
            // Fermer la fenêtre et forcer l'arrêt
            primaryStage.close();
            System.exit(0);
        });
        
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

    private Button createImageButton(String imageName, String color) {
        Image img = new Image(getClass().getResourceAsStream("/images/" + imageName));
        ImageView view = new ImageView(img);
        view.setFitWidth(24);
        view.setFitHeight(24);
        Button button = new Button("", view);
        button.setStyle(String.format(
            "-fx-background-color: %s; -fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40;",
            color
        ));
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
                session.setTag(tagBox.getValue());
                
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
            case "Thales" -> "#2196F3";
            case "Études" -> "#4CAF50";
            case "Perso" -> "#FF9800";
            case "ACADOMIA" -> "#B388FF";
            default -> "#2196F3";
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

    private void updateTags() {
        String category = categoryBox.getValue();
        tagBox.getItems().clear();
        tagBox.getItems().addAll(tagManager.getTagsForCategory(category));
    }

    private void showAddTagDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        dialog.setTitle("Gérer les tags");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 15;");

        // Liste des tags existants dans un ScrollPane
        VBox tagsList = new VBox(5);
        tagsList.setPadding(new Insets(5));
        Label existingTagsLabel = new Label("Tags existants :");
        existingTagsLabel.setStyle("-fx-font-weight: bold;");
        tagsList.getChildren().add(existingTagsLabel);

        ScrollPane scrollPane = new ScrollPane(tagsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Zone pour ajouter un nouveau tag
        HBox addBox = new HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);
        TextField tagField = new TextField();
        tagField.setPromptText("Nouveau tag");
        tagField.setStyle("-fx-background-radius: 15; -fx-font-size: 14;");
        Button addButton = new Button("Ajouter");
        addButton.setStyle("-fx-background-radius: 15; -fx-font-size: 14; -fx-background-color: #4CAF50; -fx-text-fill: white;");

        // Fonction pour mettre à jour la liste des tags
        class TagListUpdater {
            // Palette de couleurs harmonieuses
            private final String[] COLORS = {
                "#2196F3", // Bleu primaire
                "#4CAF50", // Vert primaire
                "#FFC107", // Jaune
                "#FF9800", // Orange
                "#9C27B0", // Violet
                "#E91E63", // Rose
                "#00BCD4", // Cyan
                "#795548", // Marron
                "#607D8B", // Bleu gris
                "#3F51B5", // Indigo
                "#009688", // Turquoise
                "#FF5722", // Orange foncé
                "#673AB7", // Violet profond
                "#8BC34A", // Vert clair
                "#FFEB3B"  // Jaune clair
            };
            // Détecter si la couleur est claire
            private boolean isLightColor(String hexColor) {
                int r = Integer.valueOf(hexColor.substring(1, 3), 16);
                int g = Integer.valueOf(hexColor.substring(3, 5), 16);
                int b = Integer.valueOf(hexColor.substring(5, 7), 16);
                double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
                return luminance > 180;
            }
            void updateList() {
                tagsList.getChildren().clear();
                tagsList.getChildren().add(existingTagsLabel);
                for (String tag : tagManager.getTagsForCategory(categoryBox.getValue())) {
                    HBox tagBox = new HBox(10);
                    tagBox.setAlignment(Pos.CENTER_LEFT);
                    Label tagLabel = new Label(tag);
                    tagLabel.setWrapText(true);
                    int colorIndex = Math.abs(tag.hashCode()) % COLORS.length;
                    String color = COLORS[colorIndex];
                    String textColor = isLightColor(color) ? "#222" : "white";
                    tagLabel.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 10; -fx-padding: 5 10; -fx-text-fill: %s; -fx-font-weight: bold;", color, textColor));
                    Button deleteButton = new Button("×");
                    deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-background-radius: 10;");
                    deleteButton.setOnAction(e -> {
                        tagManager.removeTag(categoryBox.getValue(), tag);
                        updateList();
                        updateTags();
                    });
                    tagBox.getChildren().addAll(tagLabel, deleteButton);
                    tagsList.getChildren().add(tagBox);
                }
            }
        }

        TagListUpdater updater = new TagListUpdater();

        // Action pour ajouter un nouveau tag
        addButton.setOnAction(e -> {
            String newTag = tagField.getText().trim();
            if (!newTag.isEmpty()) {
                tagManager.addTag(categoryBox.getValue(), newTag);
                tagField.clear();
                updater.updateList();
                updateTags();
            }
        });

        addBox.getChildren().addAll(tagField, addButton);

        content.getChildren().addAll(scrollPane, addBox);

        // Afficher la liste initiale des tags
        updater.updateList();

        Scene dialogScene = new Scene(content);
        dialog.setScene(dialogScene);
        
        // Centrer la popup par rapport à la fenêtre principale
        dialog.setX(stage.getX() + (stage.getWidth() - dialog.getWidth()) / 2);
        dialog.setY(stage.getY() + (stage.getHeight() - dialog.getHeight()) / 2);
        
        dialog.showAndWait();
    }

    private void sendDiscordReport() {
        // Afficher une notification de confirmation
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Rapport Discord");
        confirmAlert.setHeaderText("Générer le rapport hebdomadaire");
        confirmAlert.setContentText("Voulez-vous envoyer le rapport hebdomadaire sur Discord maintenant ?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Exécuter l'envoi dans un thread séparé pour ne pas bloquer l'interface
            new Thread(() -> {
                boolean success = discordReporter.sendManualWeeklyReport();
                
                // Retourner sur le thread JavaFX pour afficher l'alerte de résultat
                Platform.runLater(() -> {
                    Alert resultAlert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                    resultAlert.setTitle("Rapport Discord");
                    resultAlert.setHeaderText(success ? "Succès !" : "Erreur");
                    resultAlert.setContentText(success ? 
                        "Le rapport hebdomadaire a été envoyé avec succès sur Discord ! 🎉" :
                        "Une erreur s'est produite lors de l'envoi du rapport. Vérifiez votre connexion internet et réessayez.");
                    resultAlert.showAndWait();
                });
            }).start();
        }
    }

    @Override
    public void stop() {
        // Arrêter d'abord les timelines
        if (timeline != null) {
            timeline.stop();
        }
        
        // Arrêter le serveur WebSocket
        if (tabInfoServer != null) {
            tabInfoServer.stop();
        }

        // Arrêter le serveur de configuration
        if (configServer != null) {
            configServer.stop();
        }

        // Arrêter le reporter Discord
        if (discordReporter != null) {
            discordReporter.stop();
        }

        // Fermer la connexion MongoDB
        if (sessionManager != null) {
            sessionManager.close();
        }
        
        // Appeler la méthode stop de la classe parente
        try {
            super.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Forcer l'arrêt de l'application
        System.exit(0);
    }

    public static void main(String[] args) {
        // Nettoyer le port avant de démarrer l'application
        cleanupWebSocketPort();
        launch(args);
    }
}