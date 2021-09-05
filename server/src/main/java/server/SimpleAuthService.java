package server;

import java.util.ArrayList;
import java.util.List;

public class SimpleAuthService implements AuthService {

    private final List<UserData> users;

    public SimpleAuthService() {
        this.users = new ArrayList<>();
        users.add(new UserData("qwe", "qwe", "qwe"));
        users.add(new UserData("asd", "asd", "asd"));
        users.add(new UserData("zxc", "zxc", "zxc"));
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        for (UserData u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.nickname;
            }
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        for (UserData u : users) {
            if (u.login.equals(login) || u.nickname.equals(nickname)) {
                return false;
            }
        }
        users.add(new UserData(login, password, nickname));
        return true;
    }

    @Override
    public boolean changeNick(String login, String password, String nickname) {
        for (UserData u : users) {
            if ( u.nickname.equals(nickname)) {
                return false;
            }
        }
        for (UserData u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                u.setNickname(nickname);
                return true;
            }
        }
        return false;
    }
}
