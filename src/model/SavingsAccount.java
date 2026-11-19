package model;

public class SavingsAccount extends Account {

    public static final double MONTHLY_INTEREST_RATE = 0.005;

    public SavingsAccount(String accountNumber, String customerId, String branch, double initialBalance) {
        super(accountNumber, customerId, "Savings Account", branch, initialBalance);
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient funds in Savings Account.");
        }
        balance -= amount;
    }

    @Override
    public double calculateMonthlyInterest() {
        double interest = balance * MONTHLY_INTEREST_RATE;
        balance += interest;
        return interest;
    }
}