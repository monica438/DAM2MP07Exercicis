package com.project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class ControllerDesktop implements Initializable {

    @FXML
    private ChoiceBox<String> choiceBox;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox detailVBox;

    private VBox contentVBox = new VBox(5);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        scrollPane.setContent(contentVBox);
        scrollPane.setFitToWidth(true);

        choiceBox.getItems().addAll("Games", "Consoles", "Characters");
        choiceBox.setValue("Games");

        loadAndShow("Games");

        choiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            loadAndShow(newVal);
        });
    }

    private void loadAndShow(String type) {
        contentVBox.getChildren().clear();
        detailVBox.getChildren().clear();

        try {
            JSONArray arr = loadJSONArray("/assets/" + type.toLowerCase() + ".json");

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);

                String name = obj.getString("name");
                String imagePath = "/assets/images/" + obj.getString("image");

                addListItem(name, imagePath, obj);
            }

            if (arr.length() > 0) {
                showDetail(arr.getJSONObject(0));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addListItem(String title, String imagePath, JSONObject jsonData) throws IOException {
        URL resource = getClass().getResource("/assets/listItem.fxml");
        FXMLLoader loader = new FXMLLoader(resource);
        Parent item = loader.load();

        ControllerListItem itemController = loader.getController();
        itemController.setTitle(title);
        itemController.setImatge(imagePath);

        item.setOnMouseClicked(e -> showDetail(jsonData));

        contentVBox.getChildren().add(item);
    }

    private void showDetail(JSONObject obj) {
        detailVBox.getChildren().clear();
        detailVBox.setAlignment(Pos.CENTER);
        detailVBox.setSpacing(10);

        // Título grande
        Label titleLabel = new Label(obj.optString("name", "No Name"));
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxWidth(350); // Para evitar que se expanda de más

        // Imagen grande
        String imageFile = obj.optString("image", null);
        Image image = null;
        if (imageFile != null) {
            image = new Image(getClass().getResourceAsStream("/assets/images/" + imageFile));
        }

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(200);
        imageView.setFitHeight(200);
        imageView.setSmooth(true);

        detailVBox.getChildren().addAll(titleLabel, imageView);

        // Mostrar los campos del JSON
        for (String key : obj.keySet()) {
            if (key.equals("name") || key.equals("image")) continue;

            String value = obj.optString(key, "N/A");

            // Formatear clave: primera letra mayúscula + reemplazar guiones bajos por espacios
            String formattedKey = capitalize(key.replace("_", " "));

            boolean isColorField = key.equalsIgnoreCase("color") || key.toLowerCase().endsWith("_color");

            if (isColorField && isColor(value)) {
                // Mostrar bolita de color
                Label label = new Label(formattedKey + ":");
                label.setStyle("-fx-font-weight: bold;");
                label.setAlignment(Pos.CENTER);

                Circle colorCircle = new Circle(10);
                try {
                    colorCircle.setFill(Color.web(value));
                } catch (Exception e) {
                    colorCircle.setFill(Color.GRAY);
                }

                VBox colorBox = new VBox(5, label, colorCircle);
                colorBox.setAlignment(Pos.CENTER);
                detailVBox.getChildren().add(colorBox);

            } else {
                // Mostrar texto normal
                Label label = new Label(formattedKey + ": " + value);
                label.setWrapText(true);
                label.setMaxWidth(350);
                label.setAlignment(Pos.CENTER);
                detailVBox.getChildren().add(label);
            }
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private boolean isColor(String value) {
        try {
            Color.web(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    private JSONArray loadJSONArray(String resourcePath) throws Exception {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            throw new IOException("No se encuentra el recurso: " + resourcePath);
        }
        Path path = Paths.get(url.toURI());
        String content = Files.readString(path, StandardCharsets.UTF_8);
        return new JSONArray(content);
    }

}
