package model;

import java.util.Objects;

public abstract class Account {

    private final String accountNumber;
    private final String customerId;
    private final String accountType;
    private String branch;
    protected double balance;

    protected Account(String accountNumber, String customerId, String accountType, String branch, double initialBalance) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null").trim();
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null").trim();
        this.accountType = Objects.requireNonNull(accountType, "Account type cannot be null").trim();
        setBranch(branch);
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        this.balance = initialBalance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("Branch is required.");
        }
        this.branch = branch.trim();
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }
        this.balance = balance;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        }
        this.balance += amount;
    }

    public abstract void withdraw(double amount);

    public abstract double calculateMonthlyInterest();

    @Override
    public String toString() {
        return String.format("%s | %s (%s) — BWP %.2f", accountNumber, accountType, branch, balance);
    }
}