package dao;

import model.Account;
import model.ChequeAccount;
import model.InvestmentAccount;
import model.SavingsAccount;
import model.TransactionRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO {

    public Account openAccount(String customerId, String accountType, String branch,
                              double initialDeposit, String companyName, String companyAddress) throws SQLException {
        if (initialDeposit < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative.");
        }

        String accountNumber = DatabaseManager.generateUniqueAccountNumber();
        Account account = switch (accountType) {
            case "Savings Account" -> new SavingsAccount(accountNumber, customerId, branch, initialDeposit);
            case "Investment Account" -> new InvestmentAccount(accountNumber, customerId, branch, initialDeposit);
            case "Cheque Account" -> new ChequeAccount(accountNumber, customerId, branch, initialDeposit, companyName, companyAddress);
            default -> throw new IllegalArgumentException("Unsupported account type: " + accountType);
        };

        String insertAccountSql = """
            INSERT INTO accounts (account_number, customer_id, account_type, branch, balance, company_name, company_address)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement(insertAccountSql)) {
                    pstmt.setString(1, account.getAccountNumber());
                    pstmt.setString(2, account.getCustomerId());
                    pstmt.setString(3, account.getAccountType());
                    pstmt.setString(4, account.getBranch());
                    pstmt.setDouble(5, account.getBalance());
                    if (account instanceof ChequeAccount cheque) {
                        pstmt.setString(6, cheque.getCompanyName());
                        pstmt.setString(7, cheque.getCompanyAddress());
                    } else {
                        pstmt.setNull(6, java.sql.Types.VARCHAR);
                        pstmt.setNull(7, java.sql.Types.VARCHAR);
                    }
                    pstmt.executeUpdate();
                }

                if (initialDeposit > 0) {
                    insertTransaction(conn, account.getAccountNumber(), "Opening Deposit", initialDeposit, account.getBalance());
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return account;
    }

    public List<Account> getAccountsByCustomer(String customerId) throws SQLException {
        String sql = """
            SELECT account_number, customer_id, account_type, branch, balance, company_name, company_address
            FROM accounts
            WHERE UPPER(customer_id) = UPPER(?)
            ORDER BY account_number ASC
            """;
        List<Account> accounts = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, customerId.trim());
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
                        default -> throw new IllegalStateException("Unknown account type: " + type);
                    };
                    accounts.add(account);
                }
            }
        }
        return accounts;
    }

    public void recordDeposit(Account account, double amount) throws SQLException {
        account.deposit(amount);
        persistBalanceAndTransaction(account, "Deposit", amount);
    }

    public void recordWithdrawal(Account account, double amount) throws SQLException {
        account.withdraw(amount);
        persistBalanceAndTransaction(account, "Withdrawal", amount);
    }

    public double recordMonthlyInterest(Account account) throws SQLException {
        double interest = account.calculateMonthlyInterest();
        if (interest > 0) {
            persistBalanceAndTransaction(account, "Monthly Interest", interest);
        }
        return interest;
    }

    public List<TransactionRecord> getTransactionsForAccount(String accountNumber) throws SQLException {
        String sql = """
            SELECT transaction_id, account_number, tx_type, amount, balance_after, tx_timestamp
            FROM transactions
            WHERE account_number = ?
            ORDER BY transaction_id DESC
            """;
        List<TransactionRecord> records = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new TransactionRecord(
                            rs.getInt("transaction_id"),
                            rs.getString("account_number"),
                            rs.getString("tx_type"),
                            rs.getDouble("amount"),
                            rs.getDouble("balance_after"),
                            rs.getString("tx_timestamp")
                    ));
                }
            }
        }
        return records;
    }

    private void persistBalanceAndTransaction(Account account, String txType, double amount) throws SQLException {
        String updateSql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setDouble(1, account.getBalance());
                    pstmt.setString(2, account.getAccountNumber());
                    pstmt.executeUpdate();
                }
                insertTransaction(conn, account.getAccountNumber(), txType, amount, account.getBalance());
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void insertTransaction(Connection conn, String accountNumber, String txType,
                                   double amount, double balanceAfter) throws SQLException {
        String insertTxSql = """
            INSERT INTO transactions (account_number, tx_type, amount, balance_after)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(insertTxSql)) {
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, txType);
            pstmt.setDouble(3, amount);
            pstmt.setDouble(4, balanceAfter);
            pstmt.executeUpdate();
        }
    }
}