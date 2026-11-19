package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Customer {

    private final String customerId;
    private String firstName;
    private String surname;
    private String address;
    private String pin;
    private final List<Account> accounts = new ArrayList<>();

    public Customer(String customerId, String firstName, String surname, String address, String pin) {
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null").trim();
        setFirstName(firstName);
        setSurname(surname);
        setAddress(address);
        setPin(pin);
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name is required.");
        }
        this.firstName = firstName.trim();
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        if (surname == null || surname.isBlank()) {
            throw new IllegalArgumentException("Surname is required.");
        }
        this.surname = surname.trim();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required.");
        }
        this.address = address.trim();
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        if (pin == null || !pin.trim().matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        }
        this.pin = pin.trim();
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public void addAccount(Account account) {
        if (account != null && !accounts.contains(account)) {
            accounts.add(account);
        }
    }

    public void clearAccounts() {
        accounts.clear();
    }

    @Override
    public String toString() {
        return customerId + " - " + firstName + " " + surname;
    }
}