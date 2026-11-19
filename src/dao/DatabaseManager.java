package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:banking.db";
    private static final Random RANDOM = new Random();

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public static void initializeDatabase() {
        String createAdminsTable = """
            CREATE TABLE IF NOT EXISTS admins (
                username TEXT PRIMARY KEY,
                pin TEXT NOT NULL CHECK (length(pin) = 4)
            );
            """;

        String createCustomersTable = """
            CREATE TABLE IF NOT EXISTS customers (
                customer_id TEXT PRIMARY KEY,
                first_name TEXT NOT NULL,
                surname TEXT NOT NULL,
                address TEXT NOT NULL,
                pin TEXT NOT NULL CHECK (length(pin) = 4)
            );
            """;

        String createAccountsTable = """
            CREATE TABLE IF NOT EXISTS accounts (
                account_number TEXT PRIMARY KEY,
                customer_id TEXT NOT NULL,
                account_type TEXT NOT NULL,
                branch TEXT NOT NULL,
                balance REAL NOT NULL DEFAULT 0.0,
                company_name TEXT,
                company_address TEXT,
                FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
            );
            """;

        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_number TEXT NOT NULL,
                tx_type TEXT NOT NULL,
                amount REAL NOT NULL,
                balance_after REAL NOT NULL,
                tx_timestamp TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                FOREIGN KEY (account_number) REFERENCES accounts(account_number)
            );
            """;

        String seedAdmin = "INSERT OR IGNORE INTO admins (username, pin) VALUES ('admin', '1234');";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createAdminsTable);
            stmt.execute(createCustomersTable);
            stmt.execute(createAccountsTable);
            stmt.execute(createTransactionsTable);
            stmt.execute(seedAdmin);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to initialize SQLite database: " + ex.getMessage(), ex);
        }
    }

    public static String generateNextCustomerId() throws SQLException {
        String sql = "SELECT customer_id FROM customers WHERE customer_id GLOB 'C[0-9]*'";
        int maxNumber = 0;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("customer_id");
                if (id != null && id.length() > 1) {
                    try {
                        int num = Integer.parseInt(id.substring(1).trim());
                        if (num > maxNumber) {
                            maxNumber = num;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return String.format("C%04d", maxNumber + 1);
    }

    public static String generateUniqueAccountNumber() throws SQLException {
        String checkSql = "SELECT 1 FROM accounts WHERE account_number = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            while (true) {
                int suffix = RANDOM.nextInt(1_000_000);
                String candidate = String.format("267%06d", suffix);
                pstmt.setString(1, candidate);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        return candidate;
                    }
                }
            }
        }
    }
}