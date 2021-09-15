package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DbAuthService implements AuthService {
    /*mvn findbugs:findbugs
     * mvn org.liquibase:liquibase-maven-plugin:update
     * mvn liquibase:update
     */

    private static final String SELECT_NICKNAME_FROM_USERS_WHERE_LOGIN_AND_PASSWORD = "select nickname from users where login=? and password = ?";
    private static final String COUNT_FROM_USERS_WHERE_LOGIN_OR_NICKNAME = "select count(*) from users where login=? or nickname=?";
    private static final String COUNT_FROM_USERS_WHERE_NICKNAME = "select count(*) from users where nickname=?";
    private static final String INSERT_INTO_USERS_LOGIN_PASSWORD_NICKNAME_VALUES = "insert into users (login,password,nickname) values (?,?,?)";
    private static final String UPDATE_USERS_SET_NICKNAME_WHERE_LOGIN_AND_PASSWORD = "update users set nickname = ? where login = ? and password=?";
    private final Connection connection;

    public DbAuthService(Connection connection) {
        this.connection = connection;
    }

    private static void initDb() {
        List<UserData> newUsers = new ArrayList<>();
        newUsers.add(new UserData("qwe", "qwe", "qwe"));
        newUsers.add(new UserData("asd", "asd", "asd"));
        newUsers.add(new UserData("zxc", "zxc", "zxc"));
        try (Connection connection = StartDbServer.connect();
             Statement statement = connection.createStatement()) {
            statement.execute("drop table if exists users");
            statement.execute("create table users (login varchar(32) primary key, password varchar(32), nickname varchar(32))");
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(INSERT_INTO_USERS_LOGIN_PASSWORD_NICKNAME_VALUES)) {
                for (UserData user : newUsers) {
                    ps.setString(1, user.getLogin());
                    ps.setString(2, user.getPassword());
                    ps.setString(3, user.getNickname());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        initDb();
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {

        try (PreparedStatement ps = connection.prepareStatement(SELECT_NICKNAME_FROM_USERS_WHERE_LOGIN_AND_PASSWORD)) {
            ps.setString(1, login);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                } else {
                    return rs.getString("nickname");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {

        try {
            try (PreparedStatement ps = connection.prepareStatement(COUNT_FROM_USERS_WHERE_LOGIN_OR_NICKNAME)) {
                ps.setString(1, login);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) > 0)
                        return false;
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(INSERT_INTO_USERS_LOGIN_PASSWORD_NICKNAME_VALUES)) {
                ps.setString(1, login);
                ps.setString(2, password);
                ps.setString(3, nickname);
                ps.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean changeNick(String login, String password, String nickname) {

        try {
            try (PreparedStatement ps = connection.prepareStatement(COUNT_FROM_USERS_WHERE_NICKNAME)) {
                ps.setString(1, nickname);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || (rs.getInt(1) > 0))
                        return false;
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(UPDATE_USERS_SET_NICKNAME_WHERE_LOGIN_AND_PASSWORD)) {
                ps.setString(1, nickname);
                ps.setString(2, login);
                ps.setString(3, password);
                int res = ps.executeUpdate();
                return res == 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
