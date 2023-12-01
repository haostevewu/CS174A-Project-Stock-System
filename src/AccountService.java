import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.util.Scanner;
import java.util.Date;

public class AccountService {

    public void deposit(String username) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter deposit amount: ");
        BigDecimal amount;
        try {
            amount = scanner.nextBigDecimal();
        } catch (Exception e) {
            System.out.println("Invalid input. Please enter a valid number.");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Invalid deposit amount. Please enter a positive number.");
            return;
        }

        String sql = "UPDATE Accounts SET balance = balance + ? WHERE username = ?";
        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, amount);
            pstmt.setString(2, username);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Deposit successful. Amount deposited: $" + amount);

                // Record the transaction
                String transactionSql = "INSERT INTO Transactions (market_account_id, transaction_type, transaction_date, amount, commission) VALUES ((SELECT account_id FROM Accounts WHERE username = ?), 'Deposit', (SELECT currentdate FROM administrator WHERE username = 'admin'), ?, 0)";
                try (PreparedStatement transactionPstmt = conn.prepareStatement(transactionSql)) {
                    transactionPstmt.setString(1, username);
                    transactionPstmt.setBigDecimal(2, amount);
                    transactionPstmt.executeUpdate();
                }
            } else {
                System.out.println("Deposit failed. Account not found.");
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    public void withdraw(String username) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter withdrawal amount: ");
        BigDecimal amount;
        try {
            amount = scanner.nextBigDecimal();
        } catch (Exception e) {
            System.out.println("Invalid input. Please enter a valid number.");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Invalid withdrawal amount. Please enter a positive number.");
            return;
        }

        try (Connection conn = new DatabaseConnection().connect()) {
            // Check if the balance is sufficient for withdrawal
            BigDecimal balance = getBalance(conn, username);
            if (balance.compareTo(amount) < 0) {
                System.out.println("Insufficient funds for withdrawal.");
                return;
            }

            String sql = "UPDATE Accounts SET balance = balance - ? WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setString(2, username);
                int affectedRows = pstmt.executeUpdate();
        
                if (affectedRows > 0) {
                    System.out.println("Withdrawal successful. Amount withdrawn: $" + amount);
        
                    // Record the transaction
                    String transactionSql = "INSERT INTO Transactions (market_account_id, transaction_type, transaction_date, amount, commission) VALUES ((SELECT account_id FROM Accounts WHERE username = ?), 'Withdraw', (SELECT currentdate FROM administrator WHERE username = 'admin'), ?, 0)";
                    try (PreparedStatement transactionPstmt = conn.prepareStatement(transactionSql)) {
                        transactionPstmt.setString(1, username);
                        transactionPstmt.setBigDecimal(2, amount);
                        transactionPstmt.executeUpdate();
                    }
                } else {
                    System.out.println("Withdrawal failed. Account not found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    private BigDecimal getBalance(Connection conn, String username) throws SQLException {
        String sql = "SELECT balance FROM Accounts WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public void checkBalance(String username) {
        try (Connection conn = new DatabaseConnection().connect()) {
            BigDecimal balance = getBalance(conn, username);
            System.out.println("Current balance for " + username + ": $" + balance);
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    public void listTransactions(String username) {
        String sql = "SELECT transaction_type, transaction_date, stock_id, num_shares, amount FROM Transactions WHERE market_account_id = (SELECT account_id FROM Accounts WHERE username = ?) ORDER BY transaction_date DESC";

        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            System.out.println("Transactions for " + username + ":");
            System.out.println("Type       Date       Stock ID    Shares  Amount");
            System.out.println("--------------------------------------------------");
        
            while (rs.next()) {
                String type = rs.getString("transaction_type");
                String date = sdf.format(rs.getDate("transaction_date"));
                String stockId = rs.getString("stock_id") == null ? "N/A" : rs.getString("stock_id");
                int numShares = rs.getInt("num_shares");
                BigDecimal amount = rs.getBigDecimal("amount");
        
                // Format the output with fixed width for each column
                String formattedOutput = String.format("%-10s %-12s %-10s %-6d $%-10s", 
                                                       type, date, stockId, numShares, amount);
                System.out.println(formattedOutput);
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    public BigDecimal getStockPrice(String stockSymbol) {
        String sql = "SELECT current_price FROM Stocks WHERE stock_id = ?";
        BigDecimal price = BigDecimal.ZERO;

        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, stockSymbol);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                price = rs.getBigDecimal("current_price");
            } else {
                System.out.println("Stock symbol not found: " + stockSymbol);
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }

        return price;
    }

    public void getActorProfile(String stockSymbol) {
        String sql = "SELECT actor_name, dob FROM Actors WHERE stock_id = ?";
    
        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
    
            pstmt.setString(1, stockSymbol);
            ResultSet rs = pstmt.executeQuery();
    
            if (rs.next()) {
                System.out.println("Actor Name: " + rs.getString("actor_name"));
                System.out.println("Date of Birth: " + rs.getDate("dob"));
            } else {
                System.out.println("No actor profile found for stock symbol: " + stockSymbol);
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    public void displayStockInfo() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter stock symbol: ");
        String stockSymbol = scanner.nextLine();

        AccountService accountService = new AccountService();
        BigDecimal stockPrice = accountService.getStockPrice(stockSymbol);

        if (stockPrice.compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("Current Price for " + stockSymbol + ": $" + stockPrice);
            accountService.getActorProfile(stockSymbol);
        }
    }

    private boolean hasStockAccount(Connection conn, String username, String stockId, BigDecimal buyingPrice) throws SQLException {
        String sql = "SELECT COUNT(*) FROM StockAccounts WHERE username = ? AND stock_id = ? AND buying_price = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, stockId);
            pstmt.setBigDecimal(3, buyingPrice);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }    

    public void buy(String username) {
        Scanner scanner = new Scanner(System.in);
    
        System.out.print("Enter stock symbol: ");
        String stockSymbol = scanner.nextLine();
        System.out.print("Enter number of shares to buy: ");
        int numShares = scanner.nextInt();
        
        BigDecimal stockPrice = getStockPrice(stockSymbol);
        BigDecimal totalBuy = stockPrice.multiply(new BigDecimal(numShares));
        BigDecimal totalCost = totalBuy.add(new BigDecimal("20.00")); // Including $20 commission
    
        try (Connection conn = new DatabaseConnection().connect()) {
            boolean accountExists = hasStockAccount(conn, username, stockSymbol, stockPrice);

            String accountSql;
            if (accountExists) {
                // Update the existing stock account
                accountSql = "UPDATE StockAccounts SET shares = shares + ?, record_date = (SELECT currentdate FROM administrator WHERE username = 'admin') WHERE username = ? AND stock_id = ? AND buying_price = ?";
            } else {
                // Create a new stock account
                accountSql = "INSERT INTO StockAccounts (username, stock_id, shares, buying_price, record_date) VALUES (?, ?, ?, ?, (SELECT currentdate FROM administrator WHERE username = 'admin'))";
            }

            try (PreparedStatement accountPstmt = conn.prepareStatement(accountSql)) {
                if (accountExists) {
                    accountPstmt.setInt(1, numShares);
                    accountPstmt.setString(2, username);
                    accountPstmt.setString(3, stockSymbol);
                    accountPstmt.setBigDecimal(4, stockPrice);
                } else {
                    accountPstmt.setString(1, username);
                    accountPstmt.setString(2, stockSymbol);
                    accountPstmt.setInt(3, numShares);
                    accountPstmt.setBigDecimal(4, stockPrice);
                }
                accountPstmt.executeUpdate();
            }

            // Check the user's market account balance
            BigDecimal balance = getBalance(conn, username);
            if (balance.compareTo(totalCost) < 0) {
                System.out.println("Insufficient funds for the transaction.");
                return;
            }
    
            // Deduct the total cost from the market account
            String updateBalanceSql = "UPDATE Accounts SET balance = balance - ? WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateBalanceSql)) {
                pstmt.setBigDecimal(1, totalCost);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            }
    
            // Check if the user has a stock account for the specified stock, if not create one
            // Update or insert logic for the stock account
    
            // Record the transaction in the Transactions table
            String insertTransactionSql = "INSERT INTO Transactions (market_account_id, transaction_type, transaction_date, amount, stock_id, num_shares, stock_account_id, share_price, earning, commission) VALUES ((SELECT account_id FROM Accounts WHERE username = ?), 'Buy', (SELECT currentdate FROM administrator WHERE username = 'admin'), ?, ?, ?, (SELECT stock_account_id FROM StockAccounts WHERE username = ? AND stock_id = ? AND buying_price = ?), ?, ?, 20)";
            try (PreparedStatement transactionPstmt = conn.prepareStatement(insertTransactionSql)) {
                transactionPstmt.setString(1, username);
                transactionPstmt.setBigDecimal(2, totalBuy);
                transactionPstmt.setString(3, stockSymbol);
                transactionPstmt.setInt(4, numShares);
                transactionPstmt.setString(5, username);
                transactionPstmt.setString(6, stockSymbol);
                transactionPstmt.setBigDecimal(7, stockPrice);
                transactionPstmt.setBigDecimal(8, stockPrice);
                transactionPstmt.setBigDecimal(9, totalCost.negate());
                transactionPstmt.executeUpdate();
            }
    
            System.out.println("Transaction successful. Purchased " + numShares + " shares of " + stockSymbol);
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    public void listMovieInformationAndReviews() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter movie title: ");
        String title = scanner.nextLine();
        System.out.print("Enter production year: ");
        int year = scanner.nextInt();
        scanner.nextLine(); // Consume the newline

        // Fetch and display movie information
        String movieSql = "SELECT title, production_year, rating FROM Movies WHERE title = ? AND production_year = ?";
        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(movieSql)) {

            pstmt.setString(1, title);
            pstmt.setInt(2, year);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("Title: " + rs.getString("title"));
                System.out.println("Production Year: " + rs.getInt("production_year"));
                System.out.println("Rating: " + rs.getFloat("rating"));
            } else {
                System.out.println("Movie not found.");
                return; // Exit if movie is not found
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }

        System.out.print("Do you want to see reviews for this movie? (yes/no): ");
        String response = scanner.nextLine();

        if (response.equalsIgnoreCase("yes")) {
            // Fetch and display movie reviews
            String reviewsSql = "SELECT review_text FROM Reviews WHERE title = ? AND production_year = ?";
            try (Connection conn = new DatabaseConnection().connect();
                PreparedStatement pstmt = conn.prepareStatement(reviewsSql)) {

                pstmt.setString(1, title);
                pstmt.setInt(2, year);
                ResultSet rs = pstmt.executeQuery();

                System.out.println("Reviews for " + title + ":");
                while (rs.next()) {
                    System.out.println("- " + rs.getString("review_text"));
                }
            } catch (SQLException e) {
                System.out.println("SQL error occurred: " + e.getMessage());
            }
        }
    }

    public void listTopMoviesAndReviews() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter start year: ");
        int startYear = scanner.nextInt();
        System.out.print("Enter end year: ");
        int endYear = scanner.nextInt();
        scanner.nextLine(); // Consume the newline

        String moviesSql = "SELECT title, production_year FROM Movies WHERE rating = 10 AND production_year BETWEEN ? AND ?";
        
        try (Connection conn = new DatabaseConnection().connect();
             PreparedStatement pstmt = conn.prepareStatement(moviesSql)) {

            pstmt.setInt(1, startYear);
            pstmt.setInt(2, endYear);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String title = rs.getString("title");
                int year = rs.getInt("production_year");
                System.out.println("Title: " + title + ", Year: " + year);

                System.out.print("Do you want to see reviews for this movie? (yes/no): ");
                String response = scanner.nextLine();

                if (response.equalsIgnoreCase("yes")) {
                    displayMovieReviews(conn, title, year);
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    private void displayMovieReviews(Connection conn, String title, int year) throws SQLException {
        String reviewsSql = "SELECT review_text FROM Reviews WHERE title = ? AND production_year = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(reviewsSql)) {
            pstmt.setString(1, title);
            pstmt.setInt(2, year);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println("- " + rs.getString("review_text"));
            }
        }
    }

    public void sell(String username) {
        Scanner scanner = new Scanner(System.in);

        // User input for stock symbol, number of shares, and buying price
        System.out.print("Enter stock symbol: ");
        String stockSymbol = scanner.nextLine();

        BigDecimal stockPrice = getStockPrice(stockSymbol);

        System.out.print("Enter number of shares: ");
        int numShares;
        while (true) {
            try {
                numShares = Integer.parseInt(scanner.nextLine());
                break;
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid number for shares: ");
            }
        }

        System.out.print("Enter buying price: ");
        BigDecimal buyingPrice;
        while (true) {
            try {
                buyingPrice = new BigDecimal(scanner.nextLine());
                break;
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid price: ");
            }
        }

        try (Connection conn = new DatabaseConnection().connect()) {
            BigDecimal totalSell = stockPrice.multiply(new BigDecimal(numShares));
            BigDecimal netEarnings = totalSell.subtract(new BigDecimal("20.00")); // Including $20 commission

            // Check if user owns enough shares
            String checkSharesSql = "SELECT shares FROM StockAccounts WHERE username = ? AND stock_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkSharesSql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, stockSymbol);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("shares") >= numShares) {
                    // Update stock account (reduce number of shares)
                    String updateStockSql = "UPDATE StockAccounts SET shares = shares - ?, selling_price = ? WHERE username = ? AND stock_id = ? AND buying_price = ?";
                    try (PreparedStatement updateStockPstmt = conn.prepareStatement(updateStockSql)) {
                        updateStockPstmt.setInt(1, numShares);
                        updateStockPstmt.setBigDecimal(2, stockPrice); // Set the selling price
                        updateStockPstmt.setString(3, username);
                        updateStockPstmt.setString(4, stockSymbol);
                        updateStockPstmt.setBigDecimal(5, buyingPrice);
                        updateStockPstmt.executeUpdate();
                    }
    
                    // Update market account (add net earnings)
                    String updateMarketSql = "UPDATE Accounts SET balance = balance + ? WHERE username = ?";
                    try (PreparedStatement updateMarketPstmt = conn.prepareStatement(updateMarketSql)) {
                        updateMarketPstmt.setBigDecimal(1, netEarnings);
                        updateMarketPstmt.setString(2, username);
                        updateMarketPstmt.executeUpdate();
                    }
    
                    // Record the transaction
                    String insertTransactionSql = "INSERT INTO Transactions (market_account_id, transaction_type, transaction_date, amount, stock_id, num_shares, stock_account_id, share_price, earning, commission) VALUES ((SELECT account_id FROM Accounts WHERE username = ?), 'Sell', (SELECT currentdate FROM administrator WHERE username = 'admin'), ?, ?, ?, (SELECT stock_account_id FROM StockAccounts WHERE username = ? AND stock_id = ? AND buying_price = ?), ?, ?, 20)";
                    try (PreparedStatement insertTransactionPstmt = conn.prepareStatement(insertTransactionSql)) {
                        insertTransactionPstmt.setString(1, username);
                        insertTransactionPstmt.setBigDecimal(2, totalSell);
                        insertTransactionPstmt.setString(3, stockSymbol);
                        insertTransactionPstmt.setInt(4, numShares);
                        insertTransactionPstmt.setString(5, username);
                        insertTransactionPstmt.setString(6, stockSymbol);
                        insertTransactionPstmt.setBigDecimal(7, buyingPrice);
                        insertTransactionPstmt.setBigDecimal(8, buyingPrice);
                        insertTransactionPstmt.setBigDecimal(9, netEarnings);
                        insertTransactionPstmt.executeUpdate();
                    }
    
                    System.out.println("Sold " + numShares + " shares of " + stockSymbol + ". Earnings: " + netEarnings);
                } else {
                    System.out.println("Not enough shares to sell.");
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }

    public void cancel(String username) {
        try (Connection conn = new DatabaseConnection().connect()) {
            // Start a transaction
            conn.setAutoCommit(false);

            String findTransactionSql = "SELECT * FROM (SELECT * FROM Transactions WHERE market_account_id = (SELECT account_id FROM Accounts WHERE username = ?) ORDER BY transaction_id DESC) WHERE ROWNUM = 1";


            try (PreparedStatement findTransactionPstmt = conn.prepareStatement(findTransactionSql)) {
                findTransactionPstmt.setString(1, username);

                ResultSet rs = findTransactionPstmt.executeQuery();

                if (rs.next()) {
                    String transactionType = rs.getString("transaction_type");
                    BigDecimal amount = rs.getBigDecimal("amount");
                    String stockSymbol = rs.getString("stock_id");
                    int numShares = rs.getInt("num_shares");
                    BigDecimal sharePrice = rs.getBigDecimal("share_price");

                    if (!transactionType.equals("Buy") && !transactionType.equals("Sell")) {
                        System.out.println("No 'Buy' or 'Sell' transaction found to cancel.");
                        return; // Exit the method if it's not a 'Buy' or 'Sell' transaction
                    }

                    // 2. Reverse the stock/money movements
                    if (transactionType.equals("Buy")) {
                        // Reverse a buy transaction
                        // Increase balance in Accounts by amount (minus cancellation fee)
                        String increaseBalanceSql = "UPDATE Accounts SET balance = balance + ? - 20 WHERE username = ?";
                        try (PreparedStatement increaseBalancePstmt = conn.prepareStatement(increaseBalanceSql)) {
                            increaseBalancePstmt.setBigDecimal(1, amount);
                            increaseBalancePstmt.setString(2, username);
                            increaseBalancePstmt.executeUpdate();
                        }

                        // Decrease shares in StockAccounts
                        String decreaseSharesSql = "UPDATE StockAccounts SET shares = shares - ? WHERE username = ? AND stock_id = ?";
                        try (PreparedStatement decreaseSharesPstmt = conn.prepareStatement(decreaseSharesSql)) {
                            decreaseSharesPstmt.setInt(1, numShares);
                            decreaseSharesPstmt.setString(2, username);
                            decreaseSharesPstmt.setString(3, stockSymbol);
                            decreaseSharesPstmt.executeUpdate();
                        }
                    } else if (transactionType.equals("Sell")) {
                        // Reverse a sell transaction
                        // Decrease balance in Accounts by amount (plus cancellation fee)
                        String decreaseBalanceSql = "UPDATE Accounts SET balance = balance - ? + 20 WHERE username = ?";
                        try (PreparedStatement decreaseBalancePstmt = conn.prepareStatement(decreaseBalanceSql)) {
                            decreaseBalancePstmt.setBigDecimal(1, amount);
                            decreaseBalancePstmt.setString(2, username);
                            decreaseBalancePstmt.executeUpdate();
                        }

                        // Increase shares in StockAccounts
                        String increaseSharesSql = "UPDATE StockAccounts SET shares = shares + ? WHERE username = ? AND stock_id = ?";
                        try (PreparedStatement increaseSharesPstmt = conn.prepareStatement(increaseSharesSql)) {
                            increaseSharesPstmt.setInt(1, numShares);
                            increaseSharesPstmt.setString(2, username);
                            increaseSharesPstmt.setString(3, stockSymbol);
                            increaseSharesPstmt.executeUpdate();
                        }
                    }

                    // 3. Apply the $20 cancellation fee
                    // (already included in the balance update steps)

                    // 4. Record the cancel transaction
                    String recordCancelSql = "INSERT INTO Transactions (market_account_id, stock_account_id, transaction_type, transaction_date, amount, stock_id, num_shares, earning, share_price, commission) VALUES ((SELECT account_id FROM Accounts WHERE username = ?), (SELECT stock_account_id FROM StockAccounts WHERE username = ? AND stock_id = ? AND buying_price = ?), 'Cancel', (SELECT currentdate FROM administrator WHERE username = 'admin'), ?, ?, ?, -20, ?, 20)";
                    try (PreparedStatement recordCancelPstmt = conn.prepareStatement(recordCancelSql)) {
                        recordCancelPstmt.setString(1, username);
                        recordCancelPstmt.setString(2, username);
                        recordCancelPstmt.setString(3, stockSymbol);
                        recordCancelPstmt.setBigDecimal(4, sharePrice);
                        recordCancelPstmt.setBigDecimal(5, amount);
                        recordCancelPstmt.setString(6, stockSymbol);
                        recordCancelPstmt.setInt(7, numShares);
                        recordCancelPstmt.setBigDecimal(8, sharePrice);
                        recordCancelPstmt.executeUpdate();
                    }

                    // Commit the transaction
                    conn.commit();

                    System.out.println("Transaction cancelled. Cancellation fee of $20 applied.");
                } else {
                    System.out.println("No eligible transaction found to cancel.");
                }
            } catch (SQLException e) {
                // Rollback in case of error
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("SQL error occurred: " + e.getMessage());
        }
    }
}