package com.project;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

public class UtilsViews {

    public static StackPane parentContainer = new StackPane();
    private static final List<Object> controllers = new ArrayList<>();

    // Afegeix una vista al StackPane (només una vegada)
    public static void addView(Class<?> cls, String viewId, String fxmlPath) throws Exception {
        FXMLLoader loader = new FXMLLoader(cls.getResource(fxmlPath));
        Pane view = loader.load();
        view.setId(viewId);

        // La primera vista es mostra, les altres s'oculten
        boolean primeraVista = parentContainer.getChildren().isEmpty();
        view.setVisible(primeraVista);
        view.setManaged(primeraVista);

        parentContainer.getChildren().add(view);
        controllers.add(loader.getController());
    }

    // Canvia la vista mostrant la que coincideix amb viewId
    public static void setView(String viewId) {
        for (Node node : parentContainer.getChildren()) {
            boolean esVistaActual = node.getId().equals(viewId);
            node.setVisible(esVistaActual);
            node.setManaged(esVistaActual);
        }

        // Treu el focus dels botons
        parentContainer.requestFocus();
    }

    // Retorna el controlador associat a una vista
    public static Object getController(String viewId) {
        int index = 0;
        for (Node node : parentContainer.getChildren()) {
            if (node.getId().equals(viewId)) {
                return controllers.get(index);
            }
            index++;
        }
        return null;
    }
}