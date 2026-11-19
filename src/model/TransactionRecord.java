package model;

public record TransactionRecord(
        int transactionId,
        String accountNumber,
        String txType,
        double amount,
        double balanceAfter,
        String txTimestamp
) {
    @Override
    public String toString() {
        return String.format("[%s] %s: BWP %.2f | Bal: BWP %.2f",
                txTimestamp, txType, amount, balanceAfter);
    }
}