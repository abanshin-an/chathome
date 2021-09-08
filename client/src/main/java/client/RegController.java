package client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegController {
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField nicknameField;
    @FXML
    private TextArea textArea;
    @FXML
    private Button regButton;
    @FXML
    private Button closeButton;
    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @FXML
    public void tryToReg() {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = nicknameField.getText().trim();

        if (login.equals("") || password.equals("") || nickname.equals("")) {
            textArea.appendText("Поля должны быть не пустые\n");
            return;
        }

        if (login.contains(" ") || password.contains(" ") || nickname.contains(" ")) {
            textArea.appendText("Логин пароль и никнейм не должны содержать пробелы\n");
            return;
        }
        if (controller.isAuthenticated()) {
            if (!nicknameField.getText().equals(controller.getNickname()))
                controller.registration("/ren", login, password, nickname);
        } else
            controller.registration("/reg", login, password, nickname);
    }

    public void regResult(String msg) {
        textArea.appendText(msg + "\n");
    }

    public void close() {
        // get a handle to the stage
        Stage stage = (Stage) closeButton.getScene().getWindow();
        // do what you have to do
        stage.close();
    }

    public void setupWindow() {
        regButton.setText(!controller.isAuthenticated() ? "Регистрация" : "Смена ника");
        textArea.clear();
        passwordField.clear();
        nicknameField.setText(controller.getNickname());
    }
}
