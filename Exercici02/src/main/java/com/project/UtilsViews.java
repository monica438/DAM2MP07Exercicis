package com.project;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.ArrayList;

public class UtilsViews {

    public static StackPane parentContainer = new StackPane();  // Conté totes les vistes
    public static ArrayList<Object> controllers = new ArrayList<>();  // Guarda els controladors de cada vista

    // Afegeix una vista nova
    public static void addView(Class<?> cls, String name, String path) throws IOException {
        boolean defaultView = false;

        FXMLLoader loader = new FXMLLoader(cls.getResource(path));
        Pane view = loader.load();

        ObservableList<Node> children = parentContainer.getChildren();

        // La primera vista que s'afegeix és la visible per defecte
        if (children.isEmpty()) {
            defaultView = true;
        }

        view.setId(name);
        view.setVisible(defaultView);
        view.setManaged(defaultView);

        children.add(view);
        controllers.add(loader.getController());
    }

    // Canvia la vista visible segons l'ID (nom)
    public static void setView(String viewId) {
        ObservableList<Node> children = parentContainer.getChildren();

        int i = 0;
        for (Node n : children) {
            boolean isVisible = n.getId().equals(viewId);
            n.setVisible(isVisible);
            n.setManaged(isVisible);
            i++;
        }

        // Elimina el focus (per estètica)
        parentContainer.requestFocus();
    }

    // Obté el controlador associat a una vista (pel seu nom)
    public static Object getController(String viewId) {
        ObservableList<Node> children = parentContainer.getChildren();

        int i = 0;
        for (Node n : children) {
            if (n.getId().equals(viewId)) {
                return controllers.get(i);
            }
            i++;
        }
        return null;
    }

    // Retorna el nom de la vista activa (visible)
    public static String getActiveView() {
        for (Node n : parentContainer.getChildren()) {
            if (n.isVisible()) {
                return n.getId();
            }
        }
        return null;
    }
}
