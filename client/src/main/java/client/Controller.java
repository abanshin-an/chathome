package client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller implements Initializable {
    @FXML
    private ListView<String> clientList;
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox authPanel;
    @FXML
    private HBox msgPanel;

    private Socket socket;
    private static final int PORT = 8189;
    private static final String IP_ADDRESS = "localhost";
    private DataOutputStream out;

    private final History history= new History();

    public boolean isAuthenticated() {
        return authenticated;
    }

    private boolean authenticated;

    public String getNickname() {
        return nickname;
    }

    private String nickname;
    private String login;
    private Stage stage;
    private Stage regStage;
    private RegController regController;

    private static final int  MAX_LINES = 100 ;

    private void setupTextArea() {

        textArea.setWrapText(false);
        textArea.setEditable(false);

        Pattern newline = Pattern.compile("\n");
        textArea.setTextFormatter(new TextFormatter<>(change ->  {

            String newText = change.getControlNewText();

            // count lines in proposed new text:
            Matcher matcher = newline.matcher(newText);
            int lines = 1 ;
            while (matcher.find()) lines++;

            // if there aren't too many lines just return the changed unmodified:
            if (lines <= MAX_LINES) return change ;

            // drop first (lines - 100) lines and replace all text
            // (there's no other way AFAIK to drop text at the beginning
            // and replace it at the end):
            int linesToDrop = lines - MAX_LINES ;
            int index = 0 ;
            for (int i = 0 ; i < linesToDrop ; i++) {
                index = newText.indexOf('\n', index) ;
            }
            change.setRange(0, change.getControlText().length());
            change.setText(newText.substring(index+1));
            return change  ;
        }));
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);

        if (!authenticated) {
            nickname = "";
            login = "";
        }
        setTitle(nickname);
        textArea.clear();
        if (!login.equals("")) {
            textArea.setText(history.load(login));
            if (textArea.getText().length()>0)
                textArea.appendText("\n");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textField.getScene().getWindow();
            setupTextArea();
            stage.setOnCloseRequest(event -> {
                System.out.println("bye");
                if (socket != null && !socket.isClosed()) {
                    try {
                        out.writeUTF("/end");
                   } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        setAuthenticated(false);
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            System.out.println("client open socket");
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    // цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            }
                            if (str.startsWith("/authok")) {
                                nickname = str.split("\\s")[1];
                                setAuthenticated(true);
                                break;
                            }

                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }
                    // цикл работы
                    while (authenticated) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            }
                            if (str.startsWith("/renok")) {
                                nickname = str.split("\\s+")[1];
                                setTitle(nickname);
                                regController.regResult(str.substring(6));
                            }
                            if (str.startsWith("/renno")) {
                                regController.regResult(str.substring(6));
                            }
                            if (str.startsWith("/clientlist ")) {
                                String[] token = str.split("\\s+");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    history.save(textArea.getText());
                    System.out.println("disconnected");
                    setAuthenticated(false);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (ConnectException e1) {
            textArea.setText("Ошибка подключения к серверу");
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String msg = String.format("/auth %s %s", login, password);

        try {
            out.writeUTF(msg);
            passwordField.clear();
        }catch (java.net.ConnectException e1){
            textArea.appendText( "Ошибка подключения к серверу\n");
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String nickname) {
        Platform.runLater(() -> {
            if (!nickname.equals("")) {
                stage.setTitle(String.format("Home Chat[ %s ]", nickname));
            } else {
                stage.setTitle("Home Chat");
            }
        });
    }

    public void clientListClick() {
        String receiver = clientList.getSelectionModel().getSelectedItem();
        textField.setText(String.format("/w %s ", receiver));
    }

    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("Home Chat registration");
            regStage.setScene(new Scene(root, 600, 400));
            regController = fxmlLoader.getController();
            regController.setController(this);
            regStage.initStyle(StageStyle.UTILITY);
            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.setOnShown(event -> regController.setupWindow());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showRegWindow() {
        if (regStage == null) {
            createRegWindow();
        }
        regStage.show();
    }

    public void registration(String action, String login, String password, String nickname){
        String msg = String.format("%s %s %s %s",action, login, password, nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
