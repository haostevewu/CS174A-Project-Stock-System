import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

public class RegistrationService {

    public void registerNewUser() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter full name: ");
        String name = scanner.nextLine();

        System.out.print("Enter state: ");
        String state = scanner.nextLine();

        System.out.print("Enter phone number (without parenthesis): ");
        String phone_number = scanner.nextLine();

        System.out.print("Enter email address: ");
        String email_adress = scanner.nextLine();

        System.out.print("Enter tax ID: ");
        String tax_ID = scanner.nextLine();
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine(); // In real-world applications, hash this password

        try (Connection conn = new DatabaseConnection().connect()) {
            // Insert into Customers table
            String sql = "INSERT INTO Customers (username, name, state, phone_number, email_address, tax_id, password) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, name);
                pstmt.setString(3, state);
                pstmt.setString(4, phone_number);
                pstmt.setString(5, email_adress);
                pstmt.setString(6, tax_ID);
                pstmt.setString(7, password);
                pstmt.executeUpdate();
            }

            // Insert into Accounts table with default balance and creation date
            sql = "INSERT INTO Accounts (username, balance, creation_date) VALUES (?, 0, CURRENT_DATE)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}