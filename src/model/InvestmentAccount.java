package model;

public class InvestmentAccount extends Account {

    public static final double MONTHLY_INTEREST_RATE = 0.015;

    public InvestmentAccount(String accountNumber, String customerId, String branch, double initialBalance) {
        super(accountNumber, customerId, "Investment Account", branch, initialBalance);
    }

    @Override
    public void withdraw(double amount) {
        throw new UnsupportedOperationException("Withdrawals are not permitted directly from an Investment Account.");
    }

    @Override
    public double calculateMonthlyInterest() {
        double interest = balance * MONTHLY_INTEREST_RATE;
        balance += interest;
        return interest;
    }
}