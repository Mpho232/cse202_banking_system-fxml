package controller;

import dao.AccountDAO;
import model.Account;
import model.TransactionRecord;

import java.sql.SQLException;
import java.util.List;

public class AccountController {

    private final AccountDAO accountDAO;

    public AccountController(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    public Account openAccount(String customerId, String accountType, String branch,
                              String initialDepositText, String companyName, String companyAddress) throws SQLException {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Select or load a customer before opening an account.");
        }
        if (accountType == null || accountType.isBlank()) {
            throw new IllegalArgumentException("Account type is required.");
        }
        double initialDeposit = parsePositiveOrZeroAmount(initialDepositText, "Initial deposit");
        return accountDAO.openAccount(customerId.trim(), accountType, branch, initialDeposit, companyName, companyAddress);
    }

    public List<Account> getCustomerAccounts(String customerId) throws SQLException {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required.");
        }
        return accountDAO.getAccountsByCustomer(customerId);
    }

    public void deposit(Account account, String amountText) throws SQLException {
        requireAccount(account);
        double amount = parsePositiveAmount(amountText, "Deposit amount");
        accountDAO.recordDeposit(account, amount);
    }

    public void withdraw(Account account, String amountText) throws SQLException {
        requireAccount(account);
        double amount = parsePositiveAmount(amountText, "Withdrawal amount");
        accountDAO.recordWithdrawal(account, amount);
    }

    public double applyMonthlyInterest(Account account) throws SQLException {
        requireAccount(account);
        return accountDAO.recordMonthlyInterest(account);
    }

    public List<TransactionRecord> getTransactionHistory(Account account) throws SQLException {
        requireAccount(account);
        return accountDAO.getTransactionsForAccount(account.getAccountNumber());
    }

    private void requireAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Please select an account first.");
        }
    }

    private double parsePositiveAmount(String text, String label) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        try {
            double value = Double.parseDouble(text.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(label + " must be greater than zero.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a valid number.");
        }
    }

    private double parsePositiveOrZeroAmount(String text, String label) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        try {
            double value = Double.parseDouble(text.trim());
            if (value < 0) {
                throw new IllegalArgumentException(label + " cannot be negative.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a valid number.");
        }
    }
}