package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//mvn findbugs:findbugs
//mvn org.liquibase:liquibase-maven-plugin:update
//mvn liquibase:update
public class StartDbServer {

    public static void main(String[] args) {
        try (Connection connection=connect()) {
            new Server(new DbAuthService(connection));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:chathome.db");
    }

}
