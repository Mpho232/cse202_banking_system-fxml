package model;

public class ChequeAccount extends Account {

    private String companyName;
    private String companyAddress;

    public ChequeAccount(String accountNumber, String customerId, String branch,
                         double initialBalance, String companyName, String companyAddress) {
        super(accountNumber, customerId, "Cheque Account", branch, initialBalance);
        setCompanyName(companyName);
        setCompanyAddress(companyAddress);
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Employer / Company name is required for a Cheque Account.");
        }
        this.companyName = companyName.trim();
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        if (companyAddress == null || companyAddress.isBlank()) {
            throw new IllegalArgumentException("Employer / Company address is required for a Cheque Account.");
        }
        this.companyAddress = companyAddress.trim();
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient funds in Cheque Account.");
        }
        balance -= amount;
    }

    @Override
    public double calculateMonthlyInterest() {
        return 0.0;
    }
}