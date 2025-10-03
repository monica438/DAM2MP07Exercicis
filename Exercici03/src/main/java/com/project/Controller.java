package com.project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.Files;
import java.io.IOException;
import java.io.File;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONObject;

public class Controller implements Initializable {

    private static final String TEXT_MODEL = "gemma3:1b";
    private static final String IMAGE_TEXT_MODEL = "llava-phi3";

    @FXML private VBox history;
    @FXML private TextField message;
    @FXML private ImageView addIcon;
    @FXML private ImageView sendStopIcon;
    @FXML private ScrollPane scrollPane;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private CompletableFuture<HttpResponse<java.util.stream.Stream<String>>> streamRequest;
    private CompletableFuture<HttpResponse<String>> completeRequest;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

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

        history.heightProperty().addListener((obs, oldVal, newVal) -> {
            // Solo hacer scroll si el contenido excede el viewport
            if (history.getHeight() > scrollPane.getViewportBounds().getHeight()) {
                scrollPane.setVvalue(1.0);
            }
        });

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
                String finalMessage = userText.isEmpty() ? "Describe this image" : userText;
                addUserMessage(finalMessage);
                message.clear();
                setState(State.WAITING_RESPONSE);
                sendRequest(finalMessage);
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

            Platform.runLater(() -> {
                history.getChildren().add(chatItemNode);
            });

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

            Platform.runLater(() -> {
                history.getChildren().add(chatItemNode);
            });
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

        // Elegir el modelo adecuado según si hay imagen o no
        String model = (attachedImageBase64 != null) ? IMAGE_TEXT_MODEL : TEXT_MODEL;

        ensureModelLoaded(model).whenComplete((v, err) -> {
            if (err != null) {
                Platform.runLater(() -> {
                    addBotMessage("Error carregant el model.");
                    setState(State.IDLE);
                });
                return;
            }
            executeImageTextRequest(model, prompt, attachedImageBase64);
        });
    }

    private void executeImageTextRequest(String model, String prompt, String imageBase64) {
        if (prompt == null || prompt.isEmpty()) {
            prompt = "Describe esta imagen";
        }

        boolean isImageRequest = (imageBase64 != null);


        JSONObject body = new JSONObject()
            .put("model", model)
            .put("prompt", prompt)
            .put("stream", !isImageRequest)
            .put("keep_alive", "10m");

        if (isImageRequest) {
            body.put("images", new JSONArray().put(imageBase64));
        }


        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/generate"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(body.toString()))
            .build();

        if (isImageRequest) {
            // Imagen: no usamos streaming
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

        } else {
            // Texto: usamos streaming línea a línea
            Platform.runLater(() -> addBotMessage("")); // Vacío al inicio

            streamRequest = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines());

            streamRequest.thenAcceptAsync(response -> {
                StringBuilder fullResponse = new StringBuilder();
                response.body().forEach(line -> {
                    if (isCancelled.get()) return;
                    try {
                        JSONObject json = new JSONObject(line);
                        String chunk = json.optString("response", "");
                        fullResponse.append(chunk);
                        updateLastBotMessage(fullResponse.toString());
                    } catch (Exception e) {
                        System.err.println("Error procesando línea JSON: " + e.getMessage());
                    }
                });

                Platform.runLater(() -> {
                    setState(State.IDLE);
                    attachedImageBase64 = null;
                });

            }, executorService)
            .exceptionally(e -> {
                if (!isCancelled.get()) e.printStackTrace();
                Platform.runLater(() -> {
                    addBotMessage("Error en la petició.");
                    setState(State.IDLE);
                });
                return null;
            });
        }
    }



    private void cancelCurrentRequest() {
        isCancelled.set(true);
        if (streamRequest != null && !streamRequest.isDone()) {
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
                            JSONObject modelObj = models.getJSONObject(i);
                            String name = modelObj.optString("name", "");
                            if (modelName.equals(name)) {
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
