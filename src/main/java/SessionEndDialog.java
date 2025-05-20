import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Modality;

public class SessionEndDialog {
    private final Stage dialog;
    private final TextArea doneTextArea;
    private final TextArea todoTextArea;
    private String doneText = "";
    private String todoText = "";
    private boolean isCancelled = false;
    private HBox starsBox;
    private int note = 3; // valeur par défaut

    public SessionEndDialog(Stage parentStage, int duration) {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Fin de session");

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Durée de la session
        int hours = duration / 3600;
        int minutes = (duration % 3600) / 60;
        int seconds = duration % 60;
        Label durationLabel = new Label(
            String.format("Durée de la session: %02d:%02d:%02d", hours, minutes, seconds)
        );

        // Zone "Ce qui a été fait"
        Label doneLabel = new Label("Ce qui a été fait:");
        doneTextArea = new TextArea();
        doneTextArea.setPrefRowCount(3);

        // Zone "Ce qu'il reste à faire"
        Label todoLabel = new Label("Ce qu'il reste à faire:");
        todoTextArea = new TextArea();
        todoTextArea.setPrefRowCount(3);

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

        // Boutons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button saveButton = new Button("Sauvegarder");
        Button ignoreButton = new Button("Ignorer");
        
        saveButton.setOnAction(e -> {
            doneText = doneTextArea.getText();
            todoText = todoTextArea.getText();
            dialog.close();
        });

        ignoreButton.setOnAction(e -> {
            isCancelled = true;
            dialog.close();
        });

        buttonBox.getChildren().addAll(ignoreButton, saveButton);

        root.getChildren().addAll(
            durationLabel,
            doneLabel, doneTextArea,
            todoLabel, todoTextArea,
            noteLabel, starsBox,
            buttonBox
        );

        Scene scene = new Scene(root);
        dialog.setScene(scene);
    }

    public void showAndWait() {
        dialog.showAndWait();
    }

    public String getDoneText() {
        return doneText;
    }

    public String getTodoText() {
        return todoText;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public int getNote() {
        return note;
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
} 