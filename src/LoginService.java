import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginService {

    public boolean validateUser(String username, String password) {
        String sql = "SELECT COUNT(*) FROM CUSTOMERS WHERE username = ? AND password = ?";
        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public boolean validateAdmin(String username, String password) {
        String sql_admin = "SELECT COUNT(*) FROM ADMINISTRATOR WHERE username = ? AND password = ?";
        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(sql_admin)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
}