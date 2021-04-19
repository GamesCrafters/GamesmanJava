package SQL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLDatabaseConnection {
    // Connect to your database.
    // Replace server name, username, and password with your credentials
    public static Connection getConnection() {
        String connectionUrl =
                "jdbc:postgresql://localhost:5432/connect4";

        try {
            Connection connection = DriverManager.getConnection(connectionUrl, "postgres", "password");
            System.out.println("Connected to the PostgreSQL server successfully.");
            // Code here.
            return connection;
        }
        // Handle any errors that may have occurred.
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}