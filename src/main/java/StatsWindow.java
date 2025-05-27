import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class StatsWindow extends Stage {
    private final SessionManager sessionManager;
    private final ChromeTabTracker tabTracker;
    private final Map<String, Integer> tabTitleSeconds;
    private final TabPane tabPane;
    private final VBox root;
    private final ComboBox<String> categoryFilter;
    private final ComboBox<String> periodFilter;
    private VBox detailsBox;
    private final Map<String, Integer> tabOpenCounts;
    private final Map<String, LocalDateTime> tabLastUsed;
    private Session selectedSession;
    private Tab chromeTab;
    private final TagManager tagManager;
    private ComboBox<String> tagFilter;

    public StatsWindow(SessionManager sessionManager, ChromeTabTracker tabTracker, Map<String, Integer> tabTitleSeconds, Map<String, Integer> tabOpenCounts, Map<String, LocalDateTime> tabLastUsed) {
        this.sessionManager = sessionManager;
        this.tabTracker = tabTracker;
        this.tabTitleSeconds = tabTitleSeconds;
        this.tabOpenCounts = tabOpenCounts;
        this.tabLastUsed = tabLastUsed;
        this.tagManager = new TagManager();
        this.root = new VBox(10);
        this.root.setPadding(new Insets(10));

        // Conteneur pour les filtres
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        // Filtre par catégorie
        categoryFilter = new ComboBox<>();
        categoryFilter.getItems().addAll("Thales", "Études", "Perso", "ACADOMIA");
        categoryFilter.setValue("Thales");
        categoryFilter.setOnAction(e -> {
            updateStats();
            updateTagFilter();
        });

        // Filtre par tag
        tagFilter = new ComboBox<>();
        tagFilter.setPromptText("Tous les tags");
        tagFilter.setOnAction(e -> updateStats());

        // Filtre par période
        periodFilter = new ComboBox<>();
        periodFilter.getItems().addAll("Jour", "Semaine", "Mois", "Année");
        periodFilter.setValue("Jour");
        periodFilter.setOnAction(e -> updateStats());

        filterBox.getChildren().addAll(
            new Label("Catégorie: "), categoryFilter,
            new Label("Tag: "), tagFilter,
            new Label("Période: "), periodFilter
        );

        // Zone pour les détails
        detailsBox = new VBox(5);
        detailsBox.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(detailsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);

        root.getChildren().addAll(filterBox, scrollPane);

        tabPane = new TabPane();
        
        // Onglet des statistiques générales
        Tab generalTab = new Tab("Statistiques générales");
        generalTab.setContent(createGeneralStatsContent());
        generalTab.setClosable(false);
        
        // Onglet Statistiques Chrome
        chromeTab = new Tab("Statistiques Chrome");
        chromeTab.setContent(createChromeStatsContent());
        chromeTab.setClosable(false);
        
        tabPane.getTabs().addAll(generalTab, chromeTab);
        
        Button exportExcelButton = new Button("Exporter Excel");
        exportExcelButton.setStyle("-fx-background-radius: 10; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        exportExcelButton.setOnAction(e -> exportExcel());
        HBox exportBox = new HBox(exportExcelButton);
        exportBox.setAlignment(Pos.TOP_RIGHT);
        HBox.setMargin(exportExcelButton, new Insets(2, 10, 2, 0));
        VBox mainLayout = new VBox(10);
        mainLayout.getChildren().addAll(exportBox, tabPane);
        Scene scene = new Scene(mainLayout);
        setScene(scene);

        setTitle("Statistiques");
        setWidth(800);
        setHeight(600);
    }

    private VBox createGeneralStatsContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Filtres
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> categoryFilter = new ComboBox<>();
        categoryFilter.getItems().addAll("Thales", "Études", "Perso", "ACADOMIA");
        categoryFilter.setValue("Thales");

        ComboBox<String> tagFilter = new ComboBox<>();
        tagFilter.setPromptText("Tous les tags");
        tagFilter.getItems().add("Tous les tags");
        tagFilter.getItems().addAll(tagManager.getTagsForCategory(categoryFilter.getValue()));

        ComboBox<String> periodFilter = new ComboBox<>();
        periodFilter.getItems().addAll("Jour", "Semaine", "Mois", "Année");
        periodFilter.setValue("Jour");

        filterBox.getChildren().addAll(
            new Label("Catégorie: "), categoryFilter,
            new Label("Tag: "), tagFilter,
            new Label("Période: "), periodFilter
        );

        content.getChildren().add(filterBox);

        // Liste des sessions
        List<Session> sessions = sessionManager.getSessionsByCategory(categoryFilter.getValue());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        TableView<Session> table = new TableView<>();
        
        // Colonne de la date
        TableColumn<Session, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateTime().format(formatter)));
        dateCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(Pos.CENTER);
            }
        });

        // Colonne de la durée
        TableColumn<Session, String> durationCol = new TableColumn<>("Durée");
        durationCol.setCellValueFactory(data -> {
            int seconds = data.getValue().getDuration();
            return new SimpleStringProperty(String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60));
        });
        durationCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(Pos.CENTER);
            }
        });

        // Colonne du tag
        TableColumn<Session, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTag()));
        tagCol.setCellFactory(column -> new TableCell<>() {
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
                "#FFEB3B", // Jaune clair
                "#B388FF"  // ACADOMIA (violet clair)
            };

            // Méthode utilitaire pour déterminer si une couleur est claire
            private boolean isLightColor(String hexColor) {
                int r = Integer.valueOf(hexColor.substring(1, 3), 16);
                int g = Integer.valueOf(hexColor.substring(3, 5), 16);
                int b = Integer.valueOf(hexColor.substring(5, 7), 16);
                // Formule de luminosité perçue
                double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
                return luminance > 180;
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    int colorIndex = Math.abs(item.hashCode()) % COLORS.length;
                    String color = COLORS[colorIndex];
                    String textColor = isLightColor(color) ? "#222" : "white";
                    setStyle(String.format("""
                        -fx-background-color: %s;
                        -fx-background-radius: 10;
                        -fx-padding: 5 10;
                        -fx-text-fill: %s;
                        -fx-font-weight: bold;
                        -fx-alignment: center;
                        """, color, textColor));
                }
            }
        });

        // Colonne des notes
        TableColumn<Session, Void> noteCol = new TableColumn<>("Note");
        noteCol.setCellFactory(col -> new TableCell<>() {
            private final HBox starsBox = new HBox(2);

            {
                starsBox.setAlignment(Pos.CENTER);
                for (int i = 1; i <= 5; i++) {
                    Label star = new Label("☆");
                    star.setStyle("-fx-font-size: 18;");
                    starsBox.getChildren().add(star);
                }
            }

            private void updateStars(int note) {
                for (int i = 0; i < 5; i++) {
                    Label star = (Label) starsBox.getChildren().get(i);
                    if (i < note) {
                        star.setText("★");
                        star.setStyle("-fx-font-size: 18; -fx-text-fill: #FFD700;");
                    } else {
                        star.setText("☆");
                        star.setStyle("-fx-font-size: 18; -fx-text-fill: #bbb;");
                    }
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Session session = getTableView().getItems().get(getIndex());
                    updateStars(session.getNote());
                    setGraphic(starsBox);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Colonne des détails
        TableColumn<Session, Void> detailsCol = new TableColumn<>("Détails");
        detailsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Détails");
            {
                btn.setOnAction(e -> {
                    Session session = getTableView().getItems().get(getIndex());
                    showSessionDetails(session);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Nouvelle colonne pour sélectionner la session pour les stats Chrome
        TableColumn<Session, Void> selectForChromeCol = new TableColumn<>("Stats Chrome");
        selectForChromeCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Voir");
            {
                btn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                btn.setOnAction(e -> {
                    Session session = getTableView().getItems().get(getIndex());
                    selectedSession = session;
                    updateChromeStats();
                    tabPane.getSelectionModel().select(chromeTab);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Colonne pour supprimer
        TableColumn<Session, Void> deleteCol = new TableColumn<>("Supprimer");
        deleteCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.setOnAction(e -> {
                    Session session = getTableView().getItems().get(getIndex());
                    sessionManager.removeSession(session);
                    getTableView().getItems().remove(session);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(dateCol, durationCol, tagCol, noteCol, detailsCol, selectForChromeCol, deleteCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Ajuster les largeurs des colonnes
        dateCol.prefWidthProperty().bind(table.widthProperty().multiply(0.20));
        durationCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        tagCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        noteCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        detailsCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        selectForChromeCol.prefWidthProperty().bind(table.widthProperty().multiply(0.10));
        deleteCol.prefWidthProperty().bind(table.widthProperty().multiply(0.10));

        // Mise à jour du tableau quand les filtres changent
        categoryFilter.setOnAction(e -> {
            String category = categoryFilter.getValue();
            table.setItems(FXCollections.observableArrayList(sessionManager.getSessionsByCategory(category)));
            tagFilter.getItems().clear();
            tagFilter.getItems().add("Tous les tags");
            tagFilter.getItems().addAll(tagManager.getTagsForCategory(category));
            tagFilter.setValue("Tous les tags");
        });

        tagFilter.setOnAction(e -> {
            String category = categoryFilter.getValue();
            String tag = tagFilter.getValue();
            List<Session> filteredSessions = sessionManager.getSessionsByCategory(category);
            if (tag != null && !tag.equals("Tous les tags")) {
                filteredSessions = filteredSessions.stream()
                    .filter(s -> tag.equals(s.getTag()))
                    .toList();
            }
            table.setItems(FXCollections.observableArrayList(filteredSessions));
        });

        table.setItems(FXCollections.observableArrayList(sessions));

        content.getChildren().add(table);

        // Graphique dynamique selon le filtre
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);

        String period = periodFilter.getValue();
        barChart.setTitle("Temps par " + period.toLowerCase());
        xAxis.setLabel("Période");
        yAxis.setLabel("Heures");

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // Récupère les données par jour/semaine/mois/année
        Map<LocalDate, Integer> dailyData = sessionManager.getDailyDurations(categoryFilter.getValue());
        Map<String, Double> aggregatedData = switch (period) {
            case "Jour" -> aggregateByDay(dailyData);
            case "Semaine" -> aggregateByWeek(dailyData);
            case "Mois" -> aggregateByMonth(dailyData);
            case "Année" -> aggregateByYear(dailyData);
            default -> aggregateByDay(dailyData);
        };

        for (Map.Entry<String, Double> entry : aggregatedData.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barChart.getData().add(series);
        barChart.setLegendVisible(false);

        content.getChildren().add(barChart);

        // À la fin, ajoute un listener pour mettre à jour le graphique et le tableau dynamiquement :
        categoryFilter.setOnAction(e -> {
            updateStatsWithFilters(content, categoryFilter, periodFilter);
            // Mettre à jour le tableau avec les sessions filtrées
            table.setItems(FXCollections.observableArrayList(sessionManager.getSessionsByCategory(categoryFilter.getValue())));
        });
        periodFilter.setOnAction(e -> updateStatsWithFilters(content, categoryFilter, periodFilter));

        // Affiche la première fois
        updateStatsWithFilters(content, categoryFilter, periodFilter);

        return content;
    }

    private String extractDomain(String url) {
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
        
        // Enlever tout ce qui vient après le premier /
        int slashIndex = domain.indexOf('/');
        if (slashIndex != -1) {
            domain = domain.substring(0, slashIndex);
        }
        
        // Enlever www. si présent
        if (domain.startsWith("www.")) {
            domain = domain.substring(4);
        }
        
        return domain;
    }

    private VBox createChromeStatsContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        if (selectedSession == null) {
            Label noSessionLabel = new Label("Veuillez sélectionner une session dans l'onglet Statistiques générales");
            noSessionLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #666666;");
            content.getChildren().add(noSessionLabel);
            return content;
        }

        // Titre avec la date de la session
        Label title = new Label("Statistiques Chrome - Session du " + 
            selectedSession.getDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #111;");

        // Durée de la session
        Label duration = new Label(String.format("Durée: %02d:%02d:%02d", 
            selectedSession.getDuration() / 3600, 
            (selectedSession.getDuration() % 3600) / 60, 
            selectedSession.getDuration() % 60));
        duration.setStyle("-fx-font-size: 14; -fx-text-fill: #666666;");

        content.getChildren().addAll(title, duration);

        // Récupérer les statistiques de la session
        Map<String, Integer> sessionTabStats = selectedSession.getTabStats();
        Map<String, Integer> sessionTabOpenCounts = selectedSession.getTabOpenCounts();

        // Vérifier si les statistiques sont disponibles
        if (sessionTabStats == null || sessionTabStats.isEmpty()) {
            Label noStatsLabel = new Label("Aucune statistique Chrome disponible pour cette session");
            noStatsLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #666666;");
            content.getChildren().add(noStatsLabel);
            return content;
        }

        // Trie les onglets par temps décroissant
        List<Map.Entry<String, Integer>> sortedTabs = new ArrayList<>(sessionTabStats.entrySet());
        sortedTabs.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Top 3 des onglets les plus utilisés
        HBox top3Box = new HBox(20);
        top3Box.setAlignment(Pos.CENTER);
        top3Box.setPadding(new Insets(10, 0, 10, 0));
        String[] colors = {"#FFD700", "#C0C0C0", "#CD7F32"}; // Or, argent, bronze

        for (int i = 0; i < Math.min(3, sortedTabs.size()); i++) {
            Map.Entry<String, Integer> entry = sortedTabs.get(i);
            int seconds = entry.getValue();
            VBox card = new VBox(5);
            card.setAlignment(Pos.CENTER);
            card.setStyle("-fx-background-color: " + colors[i] + "; -fx-background-radius: 10; -fx-padding: 10;");
            
            Label rank = new Label(String.valueOf(i + 1));
            rank.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #222;");
            Label name = new Label(extractDomain(entry.getKey()));
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
            Label time = new Label(String.format("%02d:%02d:%02d", seconds/3600, (seconds%3600)/60, seconds%60));
            time.setStyle("-fx-font-size: 13;");
            
            card.getChildren().addAll(rank, name, time);
            top3Box.getChildren().add(card);
        }

        // Tableau détaillé
        TableView<Map.Entry<String, Integer>> table = new TableView<>();
        
        // Colonne du nom de l'onglet
        TableColumn<Map.Entry<String, Integer>, String> nameCol = new TableColumn<>("Site");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(extractDomain(data.getValue().getKey())));
        nameCol.prefWidthProperty().bind(table.widthProperty().multiply(0.7));
        nameCol.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setAlignment(Pos.CENTER);
                }
        });

        // Colonne du temps
        TableColumn<Map.Entry<String, Integer>, String> timeCol = new TableColumn<>("Temps");
        timeCol.setCellValueFactory(data -> {
            int seconds = data.getValue().getValue();
            return new SimpleStringProperty(String.format("%02d:%02d:%02d", 
                seconds / 3600, (seconds % 3600) / 60, seconds % 60));
        });
        timeCol.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
        timeCol.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setAlignment(Pos.CENTER);
                }
        });

        // Ajuster les largeurs des colonnes
        nameCol.prefWidthProperty().bind(table.widthProperty().multiply(0.7));
        timeCol.prefWidthProperty().bind(table.widthProperty().multiply(0.3));

        table.getColumns().addAll(nameCol, timeCol);
        table.setItems(FXCollections.observableArrayList(sortedTabs));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Graphique
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Temps passé par site");
        xAxis.setLabel("Site");
        yAxis.setLabel("Minutes");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entry : sortedTabs) {
            double minutes = entry.getValue() / 60.0;
            series.getData().add(new XYChart.Data<>(extractDomain(entry.getKey()), minutes));
        }
        barChart.getData().add(series);
        barChart.setLegendVisible(false);

        content.getChildren().addAll(top3Box, table, barChart);
        return content;
    }

    private VBox createExtensionStatsContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        int extSeconds = tabTitleSeconds.getOrDefault("Extension Chrome", 0);

        Label extensionStat = new Label("Temps passé sur des extensions Chrome : " +
            String.format("%02d:%02d:%02d", extSeconds / 3600, (extSeconds % 3600) / 60, extSeconds % 60));
        extensionStat.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        // Graphique à barres
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Temps passé sur les extensions Chrome");
        xAxis.setLabel("Type");
        yAxis.setLabel("Minutes");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Extensions Chrome");

        double minutes = extSeconds / 60.0;
        series.getData().add(new XYChart.Data<>("Extensions Chrome", minutes));

        barChart.getData().add(series);

        content.getChildren().addAll(extensionStat, barChart);
        return content;
    }

    private VBox createTabTitlesStatsContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label title = new Label("Temps passé par nom d'onglet (titre)");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Temps par nom d'onglet");
        xAxis.setLabel("Titre d'onglet");
        yAxis.setLabel("Minutes");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        tabTitleSeconds.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .forEach(entry -> {
                double minutes = entry.getValue() / 60.0;
                series.getData().add(new XYChart.Data<>(entry.getKey(), minutes));
            });
        barChart.getData().add(series);

        content.getChildren().addAll(title, barChart);
        return content;
    }

    public void display() {
        super.show();
    }

    private void updateStats() {
        String selectedCategory = categoryFilter.getValue();
        String selectedTag = tagFilter.getValue();
        List<Session> filteredSessions = sessionManager.getSessionsByCategory(selectedCategory);
        
        if (selectedTag != null && !selectedTag.equals("Tous les tags")) {
            filteredSessions = filteredSessions.stream()
                .filter(s -> selectedTag.equals(s.getTag()))
                .toList();
        }

        detailsBox.getChildren().clear();
        for (Session session : filteredSessions) {
            HBox sessionBox = new HBox(10);
            sessionBox.setAlignment(Pos.CENTER_LEFT);
            Label dateLabel = new Label(session.getDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            Label durationLabel = new Label(String.format("Durée: %02d:%02d:%02d", 
                session.getDuration() / 3600, 
                (session.getDuration() % 3600) / 60, 
                session.getDuration() % 60));
            Label tagLabel = new Label(session.getTag().isEmpty() ? "" : "[" + session.getTag() + "]");
            tagLabel.setStyle("-fx-text-fill: #666666;");
            Button detailsButton = new Button("Détails");
            detailsButton.setOnAction(e -> {
                selectedSession = session;
                updateChromeStats();
                tabPane.getSelectionModel().select(chromeTab);
            });
            Button deleteButton = new Button("Supprimer");
            deleteButton.setOnAction(e -> {
                sessionManager.removeSession(session);
                updateStats();
            });
            sessionBox.getChildren().addAll(dateLabel, durationLabel, tagLabel, detailsButton, deleteButton);
            detailsBox.getChildren().add(sessionBox);
        }
        updateChart();
    }

    private void updateChart() {
        root.getChildren().removeIf(node -> node instanceof BarChart);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        
        barChart.setTitle("Temps par " + periodFilter.getValue().toLowerCase());
        xAxis.setLabel("Période");
        yAxis.setLabel("Heures");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<LocalDate, Integer> dailyData = sessionManager.getDailyDurations(categoryFilter.getValue());
        
        Map<String, Double> aggregatedData = switch (periodFilter.getValue()) {
            case "Jour" -> aggregateByDay(dailyData);
            case "Semaine" -> aggregateByWeek(dailyData);
            case "Mois" -> aggregateByMonth(dailyData);
            case "Année" -> aggregateByYear(dailyData);
            default -> aggregateByDay(dailyData);
        };

        for (Map.Entry<String, Double> entry : aggregatedData.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barChart.getData().add(series);
        barChart.setLegendVisible(false);

        String color = switch (categoryFilter.getValue()) {
            case "Thales" -> "#2196F3";
            case "Études" -> "#4CAF50";
            case "Perso" -> "#FF9800";
            default -> "#2196F3";
        };

        series.getData().forEach(data -> 
            data.getNode().setStyle(String.format("""
                -fx-bar-fill: %s;
                -fx-bar-width: 2px;
                """, color))
        );

        barChart.setCategoryGap(20);
        barChart.setBarGap(0);

        root.getChildren().add(barChart);
    }

    private Map<String, Double> aggregateByDay(Map<LocalDate, Integer> dailyData) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        return dailyData.entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().format(formatter),
                e -> e.getValue() / 3600.0,
                Double::sum,
                TreeMap::new
            ));
    }

    private Map<String, Double> aggregateByWeek(Map<LocalDate, Integer> dailyData) {
        return dailyData.entrySet().stream()
            .collect(Collectors.groupingBy(
                e -> "S" + e.getKey().get(WeekFields.ISO.weekOfWeekBasedYear()),
                TreeMap::new,
                Collectors.summingDouble(e -> e.getValue() / 3600.0)
            ));
    }

    private Map<String, Double> aggregateByMonth(Map<LocalDate, Integer> dailyData) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
        return dailyData.entrySet().stream()
            .collect(Collectors.groupingBy(
                e -> e.getKey().format(formatter),
                TreeMap::new,
                Collectors.summingDouble(e -> e.getValue() / 3600.0)
            ));
    }

    private Map<String, Double> aggregateByYear(Map<LocalDate, Integer> dailyData) {
        return dailyData.entrySet().stream()
            .collect(Collectors.groupingBy(
                e -> String.valueOf(e.getKey().getYear()),
                TreeMap::new,
                Collectors.summingDouble(e -> e.getValue() / 3600.0)
            ));
    }

    private void showSessionDetails(Session session) {
        Stage detailsStage = new Stage();
        detailsStage.setTitle("Détails de la session");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        TextArea doneArea = new TextArea(session.getDoneText());
        doneArea.setEditable(false);
        doneArea.setPrefRowCount(3);
        
        TextArea todoArea = new TextArea(session.getTodoText());
        todoArea.setEditable(false);
        todoArea.setPrefRowCount(3);

        content.getChildren().addAll(
            new Label("Ce qui a été fait :"),
            doneArea,
            new Label("Ce qu'il reste à faire :"),
            todoArea
        );

        detailsStage.setScene(new Scene(content));
        detailsStage.showAndWait();
    }

    private void updateStatsWithFilters(VBox content, ComboBox<String> categoryFilter, ComboBox<String> periodFilter) {
        // Supprime l'ancien graphique s'il existe
        content.getChildren().removeIf(node -> node instanceof BarChart);

        // Graphique dynamique selon le filtre
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);

        String period = periodFilter.getValue();
        barChart.setTitle("Temps par " + period.toLowerCase());
        xAxis.setLabel("Période");
        yAxis.setLabel("Heures");

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // Récupère les données par jour/semaine/mois/année
        Map<LocalDate, Integer> dailyData = sessionManager.getDailyDurations(categoryFilter.getValue());
        Map<String, Double> aggregatedData = switch (period) {
            case "Jour" -> aggregateByDay(dailyData);
            case "Semaine" -> aggregateByWeek(dailyData);
            case "Mois" -> aggregateByMonth(dailyData);
            case "Année" -> aggregateByYear(dailyData);
            default -> aggregateByDay(dailyData);
        };

        for (Map.Entry<String, Double> entry : aggregatedData.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barChart.getData().add(series);
        barChart.setLegendVisible(false);

        content.getChildren().add(barChart);
    }

    private String getStars(int note) {
        return "★".repeat(note) + "☆".repeat(5 - note);
    }

    private void updateChromeStats() {
        if (selectedSession != null) {
            chromeTab.setContent(createChromeStatsContent());
        }
    }

    private void updateTagFilter() {
        String category = categoryFilter.getValue();
        tagFilter.getItems().clear();
        tagFilter.getItems().add("Tous les tags");
        tagFilter.getItems().addAll(tagManager.getTagsForCategory(category));
        tagFilter.setValue("Tous les tags");
    }

    private void exportExcel() {
        String selectedCategory = categoryFilter.getValue();
        String selectedTag = tagFilter != null ? tagFilter.getValue() : null;
        List<Session> filteredSessions = sessionManager.getSessionsByCategory(selectedCategory);
        if (selectedTag != null && !selectedTag.equals("Tous les tags")) {
            filteredSessions = filteredSessions.stream()
                .filter(s -> selectedTag.equals(s.getTag()))
                .toList();
        }
        if (filteredSessions.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les sessions (Excel)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fileChooser.setInitialFileName("export_sessions.xlsx");
        java.io.File file = fileChooser.showSaveDialog(this);
        if (file == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            // Feuille 1 : Sessions
            Sheet sheet = workbook.createSheet("Sessions");
            Row header = sheet.createRow(0);
            String[] headers = {"Date", "Durée (hh:mm:ss)", "Catégorie", "Tag", "Note", "Fait", "À faire"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            int rowIdx = 1;
            for (Session s : filteredSessions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                row.createCell(1).setCellValue(formatSecondsToHMS(s.getDuration()));
                row.createCell(2).setCellValue(s.getCategory());
                row.createCell(3).setCellValue(s.getTag());
                row.createCell(4).setCellValue(s.getNote());
                row.createCell(5).setCellValue(s.getDoneText());
                row.createCell(6).setCellValue(s.getTodoText());
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            // Feuille 2 : Totaux
            Sheet totaux = workbook.createSheet("Totaux");
            int rowT = 0;
            // Totaux par catégorie
            totaux.createRow(rowT++).createCell(0).setCellValue("Totaux par catégorie");
            Map<String, Integer> catTotals = filteredSessions.stream().collect(Collectors.groupingBy(Session::getCategory, Collectors.summingInt(Session::getDuration)));
            for (var entry : catTotals.entrySet()) {
                Row r = totaux.createRow(rowT++);
                r.createCell(0).setCellValue(entry.getKey());
                r.createCell(1).setCellValue(formatSecondsToHMS(entry.getValue()));
            }
            rowT++;
            // Totaux par jour
            totaux.createRow(rowT++).createCell(0).setCellValue("Totaux par jour");
            Map<String, Integer> dayTotals = filteredSessions.stream().collect(Collectors.groupingBy(s -> s.getDateTime().toLocalDate().toString(), Collectors.summingInt(Session::getDuration)));
            for (var entry : dayTotals.entrySet()) {
                Row r = totaux.createRow(rowT++);
                r.createCell(0).setCellValue(entry.getKey());
                r.createCell(1).setCellValue(formatSecondsToHMS(entry.getValue()));
            }
            rowT++;
            // Totaux par semaine
            totaux.createRow(rowT++).createCell(0).setCellValue("Totaux par semaine");
            Map<String, Integer> weekTotals = filteredSessions.stream().collect(Collectors.groupingBy(s -> {
                LocalDate d = s.getDateTime().toLocalDate();
                int week = d.get(WeekFields.ISO.weekOfWeekBasedYear());
                int year = d.getYear();
                return year + "-S" + week;
            }, Collectors.summingInt(Session::getDuration)));
            for (var entry : weekTotals.entrySet()) {
                Row r = totaux.createRow(rowT++);
                r.createCell(0).setCellValue(entry.getKey());
                r.createCell(1).setCellValue(formatSecondsToHMS(entry.getValue()));
            }
            for (int i = 0; i < 2; i++) totaux.autoSizeColumn(i);

            // Sauvegarde du fichier
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
                workbook.write(out);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Utilitaire pour formater les secondes en hh:mm:ss
    private String formatSecondsToHMS(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
} 