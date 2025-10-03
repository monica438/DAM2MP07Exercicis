package com.project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.File;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONObject;

public class Controller implements Initializable {

    private static final String TEXT_MODEL = "gemma3:1b";

    @FXML private VBox history;
    @FXML private TextField message;
    @FXML private ImageView addIcon;
    @FXML private ImageView sendStopIcon;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private CompletableFuture<HttpResponse<InputStream>> streamRequest;
    private CompletableFuture<HttpResponse<String>> completeRequest;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private InputStream currentInputStream;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> streamReadingTask;
    private volatile boolean isFirstChunk = false;

    private Image imgPerezoso;
    private Image imgClip;
    private Image imgUpload;
    private Image imgStop;
    private Image imgUser;

    private enum State {
        IDLE,
        WRITING,
        WAITING_RESPONSE
    }
    private State currentState = State.IDLE;

    private ChatItemController lastBotMessageController = null;

    // Variable para guardar imagen adjuntada en base64
    private String attachedImageBase64 = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadImages();
        setupUI();
        setState(State.IDLE);
    }

    private void loadImages() {
        imgUser = new Image(getClass().getResourceAsStream("/assets/images/user.png"));
        imgPerezoso = new Image(getClass().getResourceAsStream("/assets/images/perezoso.png"));
        imgClip     = new Image(getClass().getResourceAsStream("/assets/images/clip.png"));
        imgUpload   = new Image(getClass().getResourceAsStream("/assets/images/upload.png"));
        imgStop     = new Image(getClass().getResourceAsStream("/assets/images/stop.png"));
    }

    private void setupUI() {
        history.widthProperty().addListener((obs, oldVal, newVal) -> {
            for (Node child : history.getChildren()) {
                if (child.getUserData() instanceof ChatItemController) {
                    ChatItemController c = (ChatItemController) child.getUserData();
                    c.bindWrappingWidth(newVal.doubleValue() - 100);
                }
            }
        });
        message.textProperty().addListener((obs, oldText, newText) -> {
            if (currentState != State.WAITING_RESPONSE) {
                if (newText.trim().isEmpty() && attachedImageBase64 == null) {
                    setState(State.IDLE);
                } else {
                    setState(State.WRITING);
                }
            }
        });

        sendStopIcon.setOnMouseClicked(event -> onSendClicked(new ActionEvent()));
        addIcon.setOnMouseClicked(event -> onAttachImageClicked());
    }

    private void setState(State newState) {
        currentState = newState;
        switch (newState) {
            case IDLE:
                addIcon.setImage(imgClip);
                sendStopIcon.setImage(imgUpload);
                break;
            case WRITING:
                addIcon.setImage(imgClip);
                sendStopIcon.setImage(imgUpload);
                break;
            case WAITING_RESPONSE:
                addIcon.setImage(imgClip);
                sendStopIcon.setImage(imgStop);
                break;
        }
    }

    private void onSendClicked(ActionEvent event) {
        if ((currentState == State.WRITING) || attachedImageBase64 != null) {
            String userText = message.getText().trim();
            if (!userText.isEmpty() || attachedImageBase64 != null) {
                addUserMessage(userText.isEmpty() ? "[Imatge adjuntada]" : userText);
                message.clear();
                setState(State.WAITING_RESPONSE);
                sendRequest(userText);
            }
        } else if (currentState == State.WAITING_RESPONSE) {
            cancelCurrentRequest();
        }
    }

    @FXML
    private void onAttachImageClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecciona una imatge");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Imatges", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        Window window = history.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(window);

        if (selectedFile != null) {
            try {
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                attachedImageBase64 = Base64.getEncoder().encodeToString(fileContent);
                addUserMessage("[Imatge adjuntada: " + selectedFile.getName() + "]");
                setState(State.WRITING);
            } catch (IOException e) {
                e.printStackTrace();
                addBotMessage("Error carregant la imatge.");
            }
        }
    }

    private void addMessageToHistory(String title, String messageText, Image logo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/chatItem.fxml"));
            Node chatItemNode = loader.load();

            ChatItemController chatItemController = loader.getController();
            chatItemController.setTitle(title);
            chatItemController.setMessage(messageText);
            chatItemController.setLogoImage(logo);

            chatItemController.bindWrappingWidth(history.getWidth() - 100);
            chatItemNode.setUserData(chatItemController);

            Platform.runLater(() -> history.getChildren().add(chatItemNode));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addUserMessage(String messageText) {
        addMessageToHistory("You", messageText, imgUser);
    }

    private void addBotMessage(String messageText) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/chatItem.fxml"));
            Node chatItemNode = loader.load();

            ChatItemController chatItemController = loader.getController();
            chatItemController.setTitle("Xat IETI");
            chatItemController.setMessage(messageText);
            chatItemController.setLogoImage(imgPerezoso);

            chatItemController.bindWrappingWidth(history.getWidth() - 100);
            chatItemNode.setUserData(chatItemController);

            lastBotMessageController = chatItemController;

            Platform.runLater(() -> history.getChildren().add(chatItemNode));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateLastBotMessage(String newText) {
        if (lastBotMessageController != null) {
            Platform.runLater(() -> lastBotMessageController.setMessage(newText));
        }
    }

    private void sendRequest(String prompt) {
        isCancelled.set(false);
        lastBotMessageController = null;

        ensureModelLoaded(TEXT_MODEL).whenComplete((v, err) -> {
            if (err != null) {
                Platform.runLater(() -> {
                    addBotMessage("Error carregant el model.");
                    setState(State.IDLE);
                });
                return;
            }
            executeImageTextRequest(TEXT_MODEL, prompt, attachedImageBase64);
        });
    }

    private void executeImageTextRequest(String model, String prompt, String imageBase64) {
        if (prompt == null || prompt.isEmpty()) {
            prompt = "Describe esta imagen";
        }

        JSONObject body = new JSONObject()
            .put("model", model)
            .put("prompt", prompt)
            .put("stream", false)
            .put("keep_alive", "10m");

        if (imageBase64 != null) {
            body.put("image_base64", imageBase64);
        }

        System.out.println("Request JSON: " + body.toString());  // debug para ver el JSON

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/generate"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(body.toString()))
            .build();

        Platform.runLater(() -> addBotMessage("thinking..."));

        completeRequest = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (isCancelled.get()) return null;
                String responseText = safeExtractTextResponse(response.body());
                Platform.runLater(() -> {
                    if (lastBotMessageController != null) {
                        lastBotMessageController.setMessage(responseText);
                    } else {
                        addBotMessage(responseText);
                    }
                    setState(State.IDLE);
                    attachedImageBase64 = null;
                });
                return response;
            })
            .exceptionally(e -> {
                if (!isCancelled.get()) e.printStackTrace();
                Platform.runLater(() -> {
                    addBotMessage("Error en la petició.");
                    setState(State.IDLE);
                });
                return null;
            });
    }

    private void cancelCurrentRequest() {
        isCancelled.set(true);
        if (streamRequest != null && !streamRequest.isDone()) {
            try { if (currentInputStream != null) currentInputStream.close(); } catch (Exception ignore) {}
            streamRequest.cancel(true);
        }
        if (completeRequest != null && !completeRequest.isDone()) {
            completeRequest.cancel(true);
        }
        Platform.runLater(() -> setState(State.IDLE));
    }

    private CompletableFuture<Void> ensureModelLoaded(String modelName) {
        return httpClient.sendAsync(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/ps"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )
            .thenCompose(resp -> {
                boolean loaded = false;
                try {
                    JSONObject o = new JSONObject(resp.body());
                    JSONArray models = o.optJSONArray("models");
                    if (models != null) {
                        for (int i = 0; i < models.length(); i++) {
                            if (modelName.equals(models.getString(i))) {
                                loaded = true;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (loaded) {
                    return CompletableFuture.completedFuture(null);
                } else {
                    return loadModel(modelName);
                }
            });
    }

    private CompletableFuture<Void> loadModel(String modelName) {
        JSONObject body = new JSONObject().put("model", modelName);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/load"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(body.toString()))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> null);
    }

    private String safeExtractTextResponse(String body) {
        try {
            JSONObject o = new JSONObject(body);
            return o.optString("response", "");
        } catch (Exception e) {
            return body;
        }
    }

}
