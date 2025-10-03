package com.project;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

public class ChatItemController {

    @FXML private Text textTitle;
    @FXML private Text textMessage;
    @FXML private ImageView logoImage;

    public void setTitle(String title) {
        textTitle.setText(title);
    }

    public void setMessage(String message) {
        textMessage.setText(message);
    }

    public void setLogoImage(javafx.scene.image.Image image) {
        logoImage.setImage(image);
    }

    // Método para ajustar el wrappingWidth del texto según el ancho disponible
    public void bindWrappingWidth(double width) {
        textMessage.wrappingWidthProperty().set(width);
    }
    
}
