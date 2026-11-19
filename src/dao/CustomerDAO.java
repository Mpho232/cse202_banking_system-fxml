package dao;

import model.Account;
import model.ChequeAccount;
import model.Customer;
import model.InvestmentAccount;
import model.SavingsAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomerDAO {

    public record AdminCustomerSummary(
            String customerId,
            String fullName,
            String address,
            String accountSummary,
            double totalBalance
    ) {}

    public boolean authenticateAdmin(String username, String pin) throws SQLException {
        if (username == null || username.isBlank() || pin == null || !pin.trim().matches("\\d{4}")) {
            return false;
        }
        String sql = "SELECT 1 FROM admins WHERE username = ? AND pin = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            pstmt.setString(2, pin.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean registerAdmin(String username, String pin) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Admin username is required.");
        }
        if (pin == null || !pin.trim().matches("\\d{4}")) {
            throw new IllegalArgumentException("Admin PIN must be exactly 4 digits.");
        }
        String cleanUser = username.trim();
        String cleanPin = pin.trim();

        String checkSql = "SELECT 1 FROM admins WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, cleanUser);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    return false;
                }
            }

            String insertSql = "INSERT INTO admins (username, pin) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, cleanUser);
                insertStmt.setString(2, cleanPin);
                insertStmt.executeUpdate();
                return true;
            }
        }
    }

    public Customer registerCustomer(String firstName, String surname, String address, String pin) throws SQLException {
        String nextId = DatabaseManager.generateNextCustomerId();
        Customer customer = new Customer(nextId, firstName, surname, address, pin);

        String sql = "INSERT INTO customers (customer_id, first_name, surname, address, pin) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, customer.getCustomerId());
            pstmt.setString(2, customer.getFirstName());
            pstmt.setString(3, customer.getSurname());
            pstmt.setString(4, customer.getAddress());
            pstmt.setString(5, customer.getPin());
            pstmt.executeUpdate();
        }
        return customer;
    }

    public Customer authenticateCustomer(String customerId, String pin) throws SQLException {
        if (customerId == null || customerId.isBlank() || pin == null || !pin.trim().matches("\\d{4}")) {
            return null;
        }
        String sql = "SELECT customer_id, first_name, surname, address, pin FROM customers WHERE UPPER(customer_id) = UPPER(?) AND pin = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, customerId.trim());
            pstmt.setString(2, pin.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Customer customer = new Customer(
                            rs.getString("customer_id"),
                            rs.getString("first_name"),
                            rs.getString("surname"),
                            rs.getString("address"),
                            rs.getString("pin")
                    );
                    loadCustomerAccounts(conn, customer);
                    return customer;
                }
            }
        }
        return null;
    }

    public Customer findById(String customerId) throws SQLException {
        if (customerId == null || customerId.isBlank()) {
            return null;
        }
        String sql = "SELECT customer_id, first_name, surname, address, pin FROM customers WHERE UPPER(customer_id) = UPPER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, customerId.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Customer customer = new Customer(
                            rs.getString("customer_id"),
                            rs.getString("first_name"),
                            rs.getString("surname"),
                            rs.getString("address"),
                            rs.getString("pin")
                    );
                    loadCustomerAccounts(conn, customer);
                    return customer;
                }
            }
        }
        return null;
    }

    public List<AdminCustomerSummary> getAllCustomersWithAccounts() throws SQLException {
        String sql = """
            SELECT c.customer_id, c.first_name, c.surname, c.address,
                   a.account_number, a.account_type, a.balance
            FROM customers c
            LEFT JOIN accounts a ON c.customer_id = a.customer_id
            ORDER BY c.customer_id ASC, a.account_number ASC
            """;

        Map<String, List<String>> typeListMap = new LinkedHashMap<>();
        Map<String, Double> balanceSumMap = new LinkedHashMap<>();
        Map<String, String[]> metaMap = new LinkedHashMap<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String cid = rs.getString("customer_id");
                String fullName = rs.getString("first_name") + " " + rs.getString("surname");
                String address = rs.getString("address");

                metaMap.putIfAbsent(cid, new String[]{fullName, address});
                typeListMap.putIfAbsent(cid, new ArrayList<>());
                balanceSumMap.putIfAbsent(cid, 0.0);

                String accType = rs.getString("account_type");
                String accNum = rs.getString("account_number");
                if (accType != null && accNum != null) {
                    double bal = rs.getDouble("balance");
                    typeListMap.get(cid).add(accType + " (" + accNum + ")");
                    balanceSumMap.put(cid, balanceSumMap.get(cid) + bal);
                }
            }
        }

        List<AdminCustomerSummary> summaries = new ArrayList<>();
        for (String cid : metaMap.keySet()) {
            String[] info = metaMap.get(cid);
            List<String> types = typeListMap.get(cid);
            String summaryText = types.isEmpty() ? "No accounts yet" : String.join(", ", types);
            summaries.add(new AdminCustomerSummary(
                    cid,
                    info[0],
                    info[1],
                    summaryText,
                    balanceSumMap.get(cid)
            ));
        }
        return summaries;
    }

    private void loadCustomerAccounts(Connection conn, Customer customer) throws SQLException {
        String sql = """
            SELECT account_number, customer_id, account_type, branch, balance, company_name, company_address
            FROM accounts
            WHERE customer_id = ?
            ORDER BY account_number ASC
            """;
        customer.clearAccounts();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, customer.getCustomerId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String accNum = rs.getString("account_number");
                    String cid = rs.getString("customer_id");
                    String type = rs.getString("account_type");
                    String branch = rs.getString("branch");
                    double balance = rs.getDouble("balance");
                    String companyName = rs.getString("company_name");
                    String companyAddress = rs.getString("company_address");

                    Account account = switch (type) {
                        case "Savings Account" -> new SavingsAccount(accNum, cid, branch, balance);
                        case "Investment Account" -> new InvestmentAccount(accNum, cid, branch, balance);
                        case "Cheque Account" -> new ChequeAccount(accNum, cid, branch, balance,
                                companyName != null ? companyName : "N/A",
                                companyAddress != null ? companyAddress : "N/A");
                        default -> throw new IllegalStateException("Unknown account type in database: " + type);
                    };
                    customer.addAccount(account);
                }
            }
        }
    }
}