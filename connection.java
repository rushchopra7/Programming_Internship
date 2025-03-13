import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class connection {
    public static void main(String[] args) {
        String host = "mysql2-ext.bio.ifi.lmu.de";
        String port = "3306";
        String database = "bioprakt2";
        String user = "bioprakt2";
        String password = "$1$xyWsttEl$sAmFI1NOY5sVGpgmzf1ga1";
        String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";

        try {
            // Explicitly load the driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            Connection connection = DriverManager.getConnection(dbUrl, user, password);
            System.out.println("Connection successful!");
            connection.close();
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found!");
        }
    }
}

