package com.project;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

public class Controller {

    @FXML
    private Button btn1;
    
    @FXML
    private Button btn2;
    
    @FXML
    private Button btn3;

    @FXML
    private Button btn4;

    @FXML
    private Button btn5;

    @FXML
    private Button btn6;

    @FXML
    private Button btn7;

    @FXML
    private Button btn8;

    @FXML
    private Button btn9;

    @FXML
    private Button btn0;

    @FXML
    private Button btnplus;

    @FXML
    private Button btnminus;

    @FXML
    private Button btnmult;

    @FXML
    private Button btndiv;

    @FXML
    private Button btneq;

    @FXML
    private Button btnclear;

    @FXML
    private TextField resultbox;

    private double result = 0;
    private boolean startOperation = true;

    // Quan cliques un número: 
    // Si es el principi d'una operació, neteja la caixa de text i posa el número
    // Si no, afegeix el número a la caixa de text
    @FXML
    private void pressnum(ActionEvent event) {
        Button pressedButton = (Button) event.getSource();
        String buttonText = pressedButton.getText();
        resultbox.appendText(buttonText);
    }

    // Quan cliques un operador:
    // Si es el principi d'una operació, mostra un error
    // Si no, afegeix l'operador a la caixa de text amb espais davant i darrere
    @FXML
    private void pressother(ActionEvent event) {
        Button pressedButton = (Button) event.getSource();
        String buttonText = pressedButton.getText();
        resultbox.appendText(" " + buttonText + " ");
    }

    @FXML
    private void pressclear(ActionEvent event) {
        resultbox.clear();
        result = 0;
    }

    // Quan cliques el botó d'igual:
    // Si la caixa de text conté 2 números i 1 operador al mig, realitza l'operació i mostra el resultat
    // Si no, mostra un missatge d'error
    // Si per exemple escrius 3 + - = , donarà error perquè tots els operadors van envoltats d'espais i al fer l'split quedaria [3, +, , -] i genera error perquè la longitud és 4
    // Al final, marca que s'ha acabat l'operació i que el següent número començarà una nova operació
    @FXML
    private void presseq(ActionEvent event) {
    String expression = resultbox.getText();
    String[] tokens = expression.split(" ");
    if (tokens.length < 3 || tokens.length % 2 == 0) {
        resultbox.setText("Error: Invalid expression");
        return;
    }
    try {
        double result = Double.parseDouble(tokens[0]);
        for (int i = 1; i < tokens.length; i += 2) {
            String operator = tokens[i];
            double num = Double.parseDouble(tokens[i + 1]);
            switch (operator) {
                case "+":
                    result += num;
                    break;
                case "-":
                    result -= num;
                    break;
                case "*":
                    result *= num;
                    break;
                case "/":
                    if (num != 0) {
                        result /= num;
                    } else {
                        resultbox.setText("Error: Div by 0");
                        return;
                    }
                    break;
                default:
                    resultbox.setText("Error: Unknown operator");
                    return;
            }
        }
        resultbox.setText(String.valueOf(result));
    } catch (Exception e) {
        resultbox.setText("Error: Invalid input");
    }
}
}