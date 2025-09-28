package com.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

public class Controller1 {

    @FXML
    private Label labelSalutacio;
    @FXML
    private Button botoTornar;


    @FXML
    public void setData() {
        String nom = Main.nom;
        String edat = Main.edat;
        labelSalutacio.setText("Hola " + nom + ", tens " + edat + " anys!");
    }
    
    @FXML
    private void tornarAVista0(ActionEvent event) {
        UtilsViews.setView("View0");
    }
}