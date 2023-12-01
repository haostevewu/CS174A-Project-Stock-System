import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.text.ParseException;
import java.sql.*;

public class AdminService {

    public void listActiveCustomers() {
        String sql = "SELECT a.username, SUM(t.num_shares) as total_shares " +
                     "FROM Transactions t " +
                     "JOIN Accounts a ON t.market_account_id = a.account_id " +
                     "WHERE t.transaction_type IN ('Buy', 'Sell') " +
                     "AND EXTRACT(MONTH FROM t.transaction_date) = EXTRACT(MONTH FROM (SELECT currentdate FROM administrator WHERE username = 'admin')) " +
                     "AND EXTRACT(YEAR FROM t.transaction_date) = EXTRACT(YEAR FROM (SELECT currentdate FROM administrator WHERE username = 'admin')) " +
                     "GROUP BY a.username " +
                     "HAVING SUM(t.num_shares) >= 1000";

        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();

            System.out.println("Active Customers (Traded 1000+ Shares in Current Month):");
            while (rs.next()) {
                String username = rs.getString("username");
                int totalShares = rs.getInt("total_shares");
                System.out.println("Username: " + username + ", Total Shares Traded: " + totalShares);
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    public void deleteTransactions() {
        String sql = "DELETE FROM Transactions";

        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("All transactions have been successfully deleted.");
            } else {
                System.out.println("No transactions were deleted (table was already empty or another issue occurred).");
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred while deleting transactions: " + e.getMessage());
        }
    }

    public void generateCustomerReport() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the customer's username: ");
        String username = scanner.nextLine();

        // Fetch regular account information
        String accountSql = "SELECT account_id, balance FROM Accounts WHERE username = ?";
        // Fetch stock account information
        String stockAccountSql = "SELECT stock_account_id, stock_id, shares FROM StockAccounts WHERE username = ?";

        try (Connection conn = new DatabaseConnection().connect();
            PreparedStatement pstmtAccount = conn.prepareStatement(accountSql);
            PreparedStatement pstmtStockAccount = conn.prepareStatement(stockAccountSql)) {

            pstmtAccount.setString(1, username);
            ResultSet rsAccount = pstmtAccount.executeQuery();

            System.out.println("Regular Account Report for Customer: " + username);
            boolean hasAccounts = false;
            while (rsAccount.next()) {
                hasAccounts = true;
                String accountId = rsAccount.getString("account_id");
                BigDecimal balance = rsAccount.getBigDecimal("balance");
                System.out.println("Account ID: " + accountId + ", Balance: " + balance);
            }

            if (!hasAccounts) {
                System.out.println("No regular accounts found for the specified customer.");
            }

            pstmtStockAccount.setString(1, username);
            ResultSet rsStockAccount = pstmtStockAccount.executeQuery();

            System.out.println("Stock Account Report for Customer: " + username);
            boolean hasStockAccounts = false;
            while (rsStockAccount.next()) {
                hasStockAccounts = true;
                String stockAccountId = rsStockAccount.getString("stock_account_id");
                String stockId = rsStockAccount.getString("stock_id");
                int shares = rsStockAccount.getInt("shares");
                System.out.println("Stock Account ID: " + stockAccountId + ", Stock ID: " + stockId + ", Shares: " + shares);
            }

            if (!hasStockAccounts) {
                System.out.println("No stock accounts found for the specified customer.");
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred while fetching customer report: " + e.getMessage());
        }
    }

    public void changeCurrentDate() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the new current date (DD-MM-YYYY): ");
        String inputDate = scanner.nextLine();

        // Validate the date format
        if (!isValidDateFormat(inputDate, "dd-MM-yyyy")) {
            System.out.println("Invalid date format. Please enter the date in DD-MM-YYYY format.");
            return;
        }

        // SQL to update the current date
        String sql = "UPDATE administrator SET currentdate = TO_DATE(?, 'DD-MM-YYYY') WHERE username = 'admin'";

        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, inputDate);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Current date updated successfully.");
            } else {
                System.out.println("Update failed. Admin user not found.");
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    private boolean isValidDateFormat(String dateStr, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            sdf.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public void openMarket() {
        if (getMarketStatus().equals("open")) {
            System.out.println("The market is already open.");
        } else {
            incrementCurrentDate();
            updateMarketStatus("open");
        }
    }
    
    public void recordDailyBalances() {
        String accountsSql = "SELECT account_id, balance FROM Accounts";
        String insertSql = "INSERT INTO DailyAccountBalances (account_id, balance_date, daily_balance) VALUES (?, (SELECT currentdate FROM Administrator WHERE username = 'admin'), ?)";

        try (Connection conn = new DatabaseConnection().connect();
             Statement stmt = conn.createStatement();) {
                
                try (ResultSet rsAccounts = stmt.executeQuery(accountsSql);
                     PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

                    while (rsAccounts.next()) {
                        int accountId = rsAccounts.getInt("account_id");
                        BigDecimal balance = rsAccounts.getBigDecimal("balance");

                        pstmt.setInt(1, accountId);
                        pstmt.setBigDecimal(2, balance);
                        pstmt.executeUpdate();
                    }
                }
            System.out.println("Accounts daily balances updated.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void closeMarket() {
        if (getMarketStatus().equals("close")) {
            System.out.println("The market is already closed.");
        } else {
            updateMarketStatus("close");
            updateDailyClosingPrices();
            recordDailyBalances();
        }
    }

    private void updateDailyClosingPrices() {
        String sql = "UPDATE stocks SET daily_closing_price = current_price";

        try (Connection conn = new DatabaseConnection().connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Daily closing prices updated successfully.");
            } else {
                System.out.println("No stocks were updated.");
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred while updating daily closing prices: " + e.getMessage());
        }
    }

    public String getMarketStatus() {
        String sql = "SELECT market_open FROM administrator WHERE username = ?";
    
        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "admin");
    
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("market_open");
                } else {
                    System.out.println("Admin user not found in the administrator table.");
                    return "unknown"; // or handle this case as needed
                }
            }
    
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
            return "unknown"; // or handle this case as needed
        }
    }

    private void updateMarketStatus(String status) {
        String sql = "UPDATE administrator SET market_open = ? WHERE username = 'admin'";

        try (Connection conn = new DatabaseConnection().connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Market is now " + status + ".");
            } else {
                System.out.println("Failed to update market status. Admin user not found.");
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    private void incrementCurrentDate() {
        String sql = "UPDATE administrator SET currentdate = currentdate + 1 WHERE username = 'admin'";

        try (Connection conn = new DatabaseConnection().connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Market date incremented to the next day.");
            } else {
                System.out.println("Failed to increment market date.");
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    public void updateStockPrice() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the stock symbol: ");
        String stockSymbol = scanner.nextLine();

        System.out.print("Enter the new price for the stock: ");
        BigDecimal newPrice;
        try {
            newPrice = scanner.nextBigDecimal();
        } catch (Exception e) {
            System.out.println("Invalid input. Please enter a valid number.");
            return;
        }

        String sql = "UPDATE Stocks SET current_price = ? WHERE stock_id = ?";

        try (Connection conn = new DatabaseConnection().connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, newPrice);
            pstmt.setString(2, stockSymbol);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Stock price updated successfully.");
            } else {
                System.out.println("Stock update failed. Please check if the symbol is correct.");
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    public void addInterest() {
        String sqlGetCurrentDate = "SELECT currentdate FROM Administrator WHERE username = 'admin'";
        String sqlAverageBalance = "SELECT account_id, SUM(daily_balance) / COUNT(*) as average_balance FROM DailyAccountBalances WHERE balance_date BETWEEN ? AND ? GROUP BY account_id";
        String sqlUpdateBalance = "UPDATE Accounts SET balance = balance + ? WHERE account_id = ?";

        try (Connection conn = new DatabaseConnection().connect();
            PreparedStatement pstmtDate = conn.prepareStatement(sqlGetCurrentDate);
            PreparedStatement pstmtAverage = conn.prepareStatement(sqlAverageBalance);
            PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateBalance)) {

            // Fetch the current date
            ResultSet rsDate = pstmtDate.executeQuery();
            if (!rsDate.next()) {
                throw new SQLException("Current date not found in Administrator table");
            }
            Date currentDate = rsDate.getDate("currentdate");
            LocalDate localCurrentDate = currentDate.toLocalDate();

            // Check if it's the last day of the month
            if (localCurrentDate.equals(localCurrentDate.withDayOfMonth(localCurrentDate.lengthOfMonth()))) {
                // Set dates for the average balance calculation
                pstmtAverage.setDate(1, Date.valueOf(localCurrentDate.withDayOfMonth(1)));
                pstmtAverage.setDate(2, currentDate);

                ResultSet rs = pstmtAverage.executeQuery();

                while (rs.next()) {
                    int accountId = rs.getInt("account_id");
                    BigDecimal averageBalance = rs.getBigDecimal("average_balance");
                    BigDecimal interest = averageBalance.multiply(new BigDecimal("0.00166667"));

                    // Update account balance with interest
                    pstmtUpdate.setBigDecimal(1, interest);
                    pstmtUpdate.setInt(2, accountId);
                    pstmtUpdate.executeUpdate();
                }
                System.out.println("Interest added to all accounts.");
            } else {
                System.out.println("Interest can only be added on the last business day of the month.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void generateMonthlyStatement(String username) {
        String sqlGetCurrentDate = "SELECT currentdate FROM Administrator WHERE username = 'admin'";
        String customerSql = "SELECT name, email_address FROM Customers WHERE username = ?";
        String accountSql = "SELECT account_id FROM Accounts WHERE username = ?";
        String transactionSql = "SELECT SUM(earning), SUM(commission) FROM Transactions WHERE market_account_id = ? AND EXTRACT(MONTH FROM transaction_date) = ? AND EXTRACT(YEAR FROM transaction_date) = ?";

        String initialBalanceSql = "SELECT daily_balance FROM (SELECT daily_balance, ROWNUM as rn FROM DailyAccountBalances WHERE account_id = ? AND EXTRACT(MONTH FROM balance_date) = ? AND EXTRACT(YEAR FROM balance_date) = ? ORDER BY balance_date ASC) WHERE rn = 1";
        String finalBalanceSql = "SELECT daily_balance FROM (SELECT daily_balance, ROWNUM as rn FROM DailyAccountBalances WHERE account_id = ? AND EXTRACT(MONTH FROM balance_date) = ? AND EXTRACT(YEAR FROM balance_date) = ? ORDER BY balance_date DESC) WHERE rn = 1";

        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmtDate = conn.prepareStatement(sqlGetCurrentDate);
             PreparedStatement pstmtCustomer = conn.prepareStatement(customerSql);
             PreparedStatement pstmtAccount = conn.prepareStatement(accountSql);
             PreparedStatement pstmtTransaction = conn.prepareStatement(transactionSql);
             PreparedStatement pstmtInitialBalance = conn.prepareStatement(initialBalanceSql);
             PreparedStatement pstmtFinalBalance = conn.prepareStatement(finalBalanceSql)) {

            // Fetch the current date
            ResultSet rsDate = pstmtDate.executeQuery();
            if (!rsDate.next()) {
                System.out.println("Current date not found in Administrator table");
                return;
            }
            Date currentDateSql = rsDate.getDate("currentdate");
            LocalDate currentDate = currentDateSql.toLocalDate();
            int currentMonth = currentDate.getMonthValue();
            int currentYear = currentDate.getYear();

            // Fetch customer details
            pstmtCustomer.setString(1, username);
            ResultSet rsCustomer = pstmtCustomer.executeQuery();
            if (!rsCustomer.next()) {
                System.out.println("Customer not found.");
                return;
            }
            String customerName = rsCustomer.getString("name");
            String customerEmail = rsCustomer.getString("email_address");

            // Fetch accounts
            pstmtAccount.setString(1, username);
            ResultSet rsAccount = pstmtAccount.executeQuery();
            List<Integer> accountIds = new ArrayList<>();
            while (rsAccount.next()) {
                accountIds.add(rsAccount.getInt("account_id"));
            }

            // For each account
            for (int accountId : accountIds) {
                // Fetch initial balance
                pstmtInitialBalance.setInt(1, accountId);
                pstmtInitialBalance.setInt(2, currentMonth);
                pstmtInitialBalance.setInt(3, currentYear);
                ResultSet rsInitialBalance = pstmtInitialBalance.executeQuery();
                BigDecimal initialBalance = rsInitialBalance.next() ? rsInitialBalance.getBigDecimal("daily_balance") : BigDecimal.ZERO;

                // Fetch final balance
                pstmtFinalBalance.setInt(1, accountId);
                pstmtFinalBalance.setInt(2, currentMonth);
                pstmtFinalBalance.setInt(3, currentYear);
                ResultSet rsFinalBalance = pstmtFinalBalance.executeQuery();
                BigDecimal finalBalance = rsFinalBalance.next() ? rsFinalBalance.getBigDecimal("daily_balance") : BigDecimal.ZERO;

                // Calculate total earnings/losses and commissions
                pstmtTransaction.setInt(1, accountId);
                pstmtTransaction.setInt(2, currentMonth);
                pstmtTransaction.setInt(3, currentYear);
                ResultSet rsTransaction = pstmtTransaction.executeQuery();
                BigDecimal totalEarnings = BigDecimal.ZERO;
                BigDecimal totalCommissions = BigDecimal.ZERO;
                if (rsTransaction.next()) {
                    totalEarnings = rsTransaction.getBigDecimal(1) != null ? rsTransaction.getBigDecimal(1) : BigDecimal.ZERO;
                    totalCommissions = rsTransaction.getBigDecimal(2) != null ? rsTransaction.getBigDecimal(2) : BigDecimal.ZERO;
                }

                // Compile and display the statement
                System.out.println("Monthly Statement for Account ID: " + accountId);
                System.out.println("Customer Name: " + customerName);
                System.out.println("Customer Email: " + customerEmail);
                System.out.println("Initial Balance: " + initialBalance);
                System.out.println("Final Balance: " + finalBalance);
                System.out.println("Total Earnings/Losses: " + totalEarnings);
                System.out.println("Total Commissions: " + totalCommissions);
                AccountService accountService = new AccountService();
                accountService.listTransactions(username);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    public void generateDTER() {
        // SQL to get the current date
        String sqlGetCurrentDate = "SELECT currentdate FROM Administrator WHERE username = 'admin'";

        // SQL to calculate interest for each account
        String sqlInterest = "SELECT a.username, SUM(d.daily_balance) / COUNT(*) * 0.00166667 as interest " +
        "FROM DailyAccountBalances d JOIN Accounts a ON d.account_id = a.account_id " +
        "WHERE d.balance_date BETWEEN ? AND ? " +
        "GROUP BY a.username";

        // SQL to aggregate transaction earnings
        String sqlEarnings = "SELECT c.username, c.state, SUM(t.earning) as transaction_earnings " +
        "FROM Customers c JOIN Accounts a ON c.username = a.username " +
        "JOIN Transactions t ON a.account_id = t.market_account_id " +
        "WHERE t.transaction_date BETWEEN ? AND ? " +
        "GROUP BY c.username, c.state";

        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmtDate = conn.prepareStatement(sqlGetCurrentDate);
             PreparedStatement pstmtInterest = conn.prepareStatement(sqlInterest);
             PreparedStatement pstmtEarnings = conn.prepareStatement(sqlEarnings)) {

            // Fetch the current date from the Administrator table
            ResultSet rsDate = pstmtDate.executeQuery();
            if (!rsDate.next()) {
                System.out.println("Current date not found in Administrator table");
                return;
            }
            Date currentDate = rsDate.getDate("currentdate");
            LocalDate currentLocalDate = currentDate.toLocalDate();
            LocalDate firstOfLastMonth = currentLocalDate.minusMonths(1).withDayOfMonth(1);
            LocalDate lastOfLastMonth = firstOfLastMonth.withDayOfMonth(firstOfLastMonth.lengthOfMonth());

            // Calculate interest for each account
            pstmtInterest.setDate(1, Date.valueOf(firstOfLastMonth));
            pstmtInterest.setDate(2, Date.valueOf(lastOfLastMonth));
            ResultSet rsInterest = pstmtInterest.executeQuery();
            
            Map<String, BigDecimal> interestMap = new HashMap<>();
            while (rsInterest.next()) {
                String username = rsInterest.getString("username");
                BigDecimal interest = rsInterest.getBigDecimal("interest");
                interestMap.put(username, interest);
            }

            // Aggregate transaction earnings for each customer
            pstmtEarnings.setDate(1, Date.valueOf(firstOfLastMonth));
            pstmtEarnings.setDate(2, Date.valueOf(lastOfLastMonth));
            ResultSet rsEarnings = pstmtEarnings.executeQuery();

            // System.out.println("Username\tState\tTotal Earnings");
            System.out.println("Username\tState\tTotal Earnings");
            System.out.println("-----------------------------------------");
            while (rsEarnings.next()) {
                String username = rsEarnings.getString("username");
                String state = rsEarnings.getString("state");
                BigDecimal transactionEarnings = rsEarnings.getBigDecimal("transaction_earnings");
                BigDecimal totalEarnings = transactionEarnings.add(interestMap.getOrDefault(username, BigDecimal.ZERO));

                if (totalEarnings.compareTo(BigDecimal.valueOf(10000)) > 0) {
                    // System.out.printf("%s\t%s\t$%,.2f\n", username, state, totalEarnings);
                    String formattedOutput = String.format("%-12s %-12s $%,.2f", username, state, totalEarnings);
                    System.out.println(formattedOutput);
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }
}

