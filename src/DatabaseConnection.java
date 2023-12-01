import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private final String url = "jdbc:oracle:thin:@cs174aproject_tp?TNS_ADMIN=C:/Users/Hao/Downloads/Wallet_CS174Aproject";
    private final String user = "ADMIN";
    private final String password = "Countingstars123$";

    public Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            //System.out.println("Connected to the database successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }
}