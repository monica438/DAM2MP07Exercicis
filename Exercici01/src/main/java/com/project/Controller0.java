package com.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

public class Controller0 {

    @FXML
    private TextField inputNom;
    @FXML
    private TextField inputEdat;
    @FXML
    private Button botoContinuar;

    @FXML
    public void initialize() {
        // Cada vegada que canvia el contingut, es comprova si s'han omplert els dos camps
        inputNom.textProperty().addListener((obs, oldText, newText) -> actualitzaEstatBoto());
        inputEdat.textProperty().addListener((obs, oldText, newText) -> actualitzaEstatBoto());

        actualitzaEstatBoto(); // Per desactivar-lo al principi
    }

    private void actualitzaEstatBoto() {
        String nom = inputNom.getText();
        String edat = inputEdat.getText();
        botoContinuar.setDisable(nom.isEmpty() || edat.isEmpty());
    }


    @FXML
    private void anarAVista1(ActionEvent event) {
        String nom = inputNom.getText();
        String edat = inputEdat.getText();

        Main.nom = nom;
        Main.edat = edat;


        Controller1 ctrl1 = (Controller1) UtilsViews.getController("View1");
        ctrl1.setData();
        UtilsViews.setView("View1");
    }
}