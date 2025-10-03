package com.project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class ControllerMobile implements Initializable {

    @FXML private Text headerTitle;
    @FXML private ImageView backArrow;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox mainContent;

    private ViewState currentState;
    private String currentCategory;
    private JSONArray currentData;

    private enum ViewState {
        MAIN_MENU,
        CATEGORY_LIST,
        ITEM_DETAIL
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        backArrow.setImage(new Image(getClass().getResourceAsStream("/assets/images/arrow-back.png")));
        backArrow.setOnMouseClicked(e -> goBack());

        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        loadMainMenu();
    }

    private void loadMainMenu() {
        currentState = ViewState.MAIN_MENU;
        headerTitle.setText("Nintendo DB");
        backArrow.setVisible(false);
        mainContent.getChildren().clear();

        String[] options = {"Games", "Consoles", "Characters"};

        for (String option : options) {
            Label optionLabel = new Label(option);
            optionLabel.setStyle("-fx-font-size: 18px; -fx-padding: 16px; -fx-background-color: #dddddd; -fx-background-radius: 8;");
            optionLabel.setMaxWidth(Double.MAX_VALUE);
            optionLabel.setAlignment(Pos.CENTER);
            optionLabel.setOnMouseClicked(e -> loadCategory(option));
            mainContent.getChildren().add(optionLabel);
        }
    }

    private void loadCategory(String category) {
        currentState = ViewState.CATEGORY_LIST;
        currentCategory = category;
        headerTitle.setText(category);
        backArrow.setVisible(true);
        mainContent.getChildren().clear();

        try {
            JSONArray arr = loadJSONArray("/assets/" + category.toLowerCase() + ".json");
            currentData = arr;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.optString("name", "No Name");
                String imagePath = "/assets/images/" + obj.optString("image", "");

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/listItem.fxml"));
                Parent listItem = loader.load();

                ControllerListItem itemController = loader.getController();
                itemController.setTitle(name);
                itemController.setImatge(imagePath);

                int index = i; // necesario para usarlo dentro del lambda
                listItem.setOnMouseClicked(e -> loadDetail(currentData.getJSONObject(index)));

                mainContent.getChildren().add(listItem);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDetail(JSONObject obj) {
        currentState = ViewState.ITEM_DETAIL;
        String name = obj.optString("name", "No Name");
        headerTitle.setText(name);
        mainContent.getChildren().clear();

        VBox detailBox = new VBox(10);
        detailBox.setAlignment(Pos.CENTER);

        // Imagen
        String imageFile = obj.optString("image", null);
        if (imageFile != null) {
            Image image = new Image(getClass().getResourceAsStream("/assets/images/" + imageFile));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(150);
            imageView.setFitHeight(150);
            imageView.setPreserveRatio(true);
            detailBox.getChildren().add(imageView);
        }

        // Resto de campos
        for (String key : obj.keySet()) {
            if (key.equals("name") || key.equals("image")) continue;
            String value = obj.optString(key);

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
                mainContent.getChildren().add(colorBox);

            } else {
                // Mostrar texto normal
                Label label = new Label(formattedKey + ": " + value);
                label.setWrapText(true);
                label.setMaxWidth(350);
                label.setAlignment(Pos.CENTER);
                mainContent.getChildren().add(label);
            }
        }

        mainContent.getChildren().add(detailBox);
    }

    private void goBack() {
        switch (currentState) {
            case CATEGORY_LIST -> loadMainMenu();
            case ITEM_DETAIL -> loadCategory(currentCategory);
        }
    }

    private JSONArray loadJSONArray(String resourcePath) throws Exception {
        URL url = getClass().getResource(resourcePath);
        if (url == null) throw new IOException("No se encuentra el recurso: " + resourcePath);
        Path path = Paths.get(url.toURI());
        String content = Files.readString(path, StandardCharsets.UTF_8);
        return new JSONArray(content);
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
}
