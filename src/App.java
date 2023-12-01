import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;
import java.sql.DatabaseMetaData;

import java.util.Scanner;

public class App {
    public static void main(String[] args) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Trading System");

        while (true) {
            System.out.print("Are you an admin [1] or a normal user [2]? (Please enter corresponding number)");
            String userType = scanner.nextLine();

            if (userType.equalsIgnoreCase("1")) {
                // Handle admin operations
                boolean validInputAdmin = false;
                while (!validInputAdmin) {
                    System.out.println("Admin interface");
                    System.out.print("Enter Admin username: ");
                    String username = scanner.nextLine();
                    
                    System.out.print("Enter Admin password: ");
                    String password = scanner.nextLine();

                    LoginService loginService = new LoginService();

                    if (loginService.validateAdmin(username, password)) {
                        System.out.println("Login successful!.");
                        validInputAdmin = true;
                        // Proceed to the admin interface
                        boolean exit_admin = false;
                        while (!exit_admin) {
                            System.out.println("Choose an option:");
                            System.out.println("1. Add Interest");
                            System.out.println("2. Generate Monthly Statement");
                            System.out.println("3. List Active Customers");
                            System.out.println("4. Generate Government Drug & Tax Evasion Report (DTER)");
                            System.out.println("5. Customer Report");
                            System.out.println("6. Delete Transactions");
                            System.out.println("7. Open market");
                            System.out.println("8. Close market");
                            System.out.println("9. Change date");
                            System.out.println("10. Change stock price");
                            System.out.println("11. Quit");
            
                            System.out.print("Enter your choice: ");
                            String choice_admin = scanner.nextLine();
                            AdminService adminService = new AdminService();
            
                            switch (choice_admin) {
                                // Add Interest
                                case "1":
                                    adminService.addInterest();
                                    break;
                                // Generate Monthly Statement
                                case "2":
                                    System.out.print("Enter username for monthly statement: ");
                                    String monthly_report_username = scanner.nextLine();
                                    adminService.generateMonthlyStatement(monthly_report_username);
                                    break;
                                // List Active Customers
                                case "3":
                                    adminService.listActiveCustomers();
                                    break;
                                // Generate Government Drug & Tax Evasion Report (DTER)
                                case "4":
                                    adminService.generateDTER();
                                    break;
                                // Customer Report
                                case "5":
                                    adminService.generateCustomerReport();
                                    break;
                                // Delete Transactions
                                case "6":
                                    adminService.deleteTransactions();
                                    break;
                                // Open market
                                case "7":
                                    adminService.openMarket();
                                    break;
                                // Close market
                                case "8":
                                    adminService.closeMarket();
                                    break;
                                // Change date
                                case "9":
                                    adminService.changeCurrentDate();
                                    break;
                                // Change stock price
                                case "10":
                                    adminService.updateStockPrice();
                                    break;
                                // Quit
                                case "11":
                                    System.out.println("Exiting the application.");
                                    exit_admin = true;
                                    break;
                                default:
                                    System.out.println("Invalid choice. Please enter 1 to 10.");
                                    break;
                            }
                        }
                    } else {
                        System.out.println("Invalid username or password.");
                    }
                }
                break;
            } else if (userType.equalsIgnoreCase("2")) {
                AdminService adminService = new AdminService();
                if (adminService.getMarketStatus().equals("open")){
                    // Handle normal user operations
                    boolean validInput = false;
                    while (!validInput) {
                        System.out.print("Do you want to login [1] or register [2]? (Please enter corresponding number)");
                        String action = scanner.nextLine();

                        // Login action
                        if (action.equalsIgnoreCase("1")) {
                            System.out.print("Enter username: ");
                            String username = scanner.nextLine();
                            
                            System.out.print("Enter password: ");
                            String password = scanner.nextLine();

                            LoginService loginService = new LoginService();
                            if (loginService.validateUser(username, password)) {
                                System.out.println("Login successful!");
                                System.out.println("Welcome, " + username);
                                // Proceed to the trading interface
                                boolean exit = false;
                                while (!exit) {
                                    System.out.println("Choose an option:");
                                    System.out.println("1. Deposit Money");
                                    System.out.println("2. Withdraw");
                                    System.out.println("3. Buy");
                                    System.out.println("4. Sell");
                                    System.out.println("5. Cancel");
                                    System.out.println("6. Show Balance");
                                    System.out.println("7. List my transactions");
                                    System.out.println("8. List price of stock and actor profile");
                                    System.out.println("9. List movie information");
                                    System.out.println("10. Quit");
                    
                                    System.out.print("Enter your choice: ");
                                    String choice = scanner.nextLine();
                                    AccountService accountService = new AccountService();
                    
                                    switch (choice) {
                                        // Depoit
                                        case "1":
                                            accountService.deposit(username);
                                            break;
                                        // Withdraw
                                        case "2":
                                            accountService.withdraw(username);
                                            break;
                                        // Buy
                                        case "3":
                                            accountService.buy(username);
                                            break;
                                        // Sell
                                        case "4":
                                            accountService.sell(username);
                                            break;
                                        // Cancel
                                        case "5":
                                            accountService.cancel(username);
                                            break;
                                        // Show current balance
                                        case "6":
                                            accountService.checkBalance(username);
                                            break;
                                        // List user's transactions
                                        case "7":
                                            accountService.listTransactions(username);
                                            break;
                                        // List stock price and actor profile
                                        case "8":
                                            accountService.displayStockInfo();
                                            break;
                                        // List movie info
                                        case "9":
                                            System.out.print("Do you want to search for a specific movie [1] or find top movies [2]? (Please enter corresponding number)");
                                            String movie_action = scanner.nextLine();
                                            boolean movie_exit = false;
                                            while (!movie_exit) {
                                                if (movie_action.equalsIgnoreCase("1")) {
                                                    accountService.listMovieInformationAndReviews();
                                                    movie_exit = true;
                                                } else if (movie_action.equalsIgnoreCase("2")) {
                                                    accountService.listTopMoviesAndReviews();
                                                    movie_exit = true;
                                                } else {
                                                    System.out.print("Invalid choice. Please enter 1 or 2.");
                                                }
                                            }
                                            break;
                                        // Quit
                                        case "10":
                                            System.out.println("Exiting the application.");
                                            exit = true;
                                            break;
                                        default:
                                            System.out.println("Invalid choice. Please enter 1 to 10.");
                                            break;
                                    }
                                }
                            } else {
                                System.out.println("Invalid username or password.");
                            }
                            validInput = true; // Break the loop
                        } 
                        // Register action
                        else if (action.equalsIgnoreCase("2")){
                            RegistrationService registrationService = new RegistrationService();
                            registrationService.registerNewUser();
                            // Inform the user about successful registration
                            validInput = true; // Break the loop
                        }
                        // Handle case when user input something else
                        else {
                            System.out.println("Invalid option. Please try again.");
                        }
                    }
                } else {
                    System.out.println("Market is now closed. Please try again later.");
                }
                break;
            } else {
                System.out.println("Invalid option. Please try again.");
            }
        }
    }
}
