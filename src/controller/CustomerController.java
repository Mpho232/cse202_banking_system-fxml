package controller;

import dao.CustomerDAO;
import model.Customer;

import java.sql.SQLException;
import java.util.List;

public class CustomerController {

    private final CustomerDAO customerDAO;

    public CustomerController(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    public boolean loginAdmin(String username, String pin) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Admin username is required.");
        }
        if (pin == null || !pin.trim().matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        }
        return customerDAO.authenticateAdmin(username, pin);
    }

    public boolean registerAdmin(String username, String pin) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Admin username is required.");
        }
        if (pin == null || !pin.trim().matches("\\d{4}")) {
            throw new IllegalArgumentException("Admin PIN must be exactly 4 digits.");
        }
        return customerDAO.registerAdmin(username, pin);
    }

    public Customer loginCustomer(String customerId, String pin) throws SQLException {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required to log in.");
        }
        if (pin == null || !pin.trim().matches("\\d{4}")) {
            throw new IllegalArgumentException("4-digit PIN is required to log in.");
        }
        Customer customer = customerDAO.authenticateCustomer(customerId, pin);
        if (customer == null) {
            throw new IllegalArgumentException("Invalid Customer ID or 4-digit PIN.");
        }
        return customer;
    }

    public Customer registerCustomer(String firstName, String surname, String address, String pin) throws SQLException {
        if (pin == null || !pin.trim().matches("\\d{4}")) {
            throw new IllegalArgumentException("Customer PIN must be exactly 4 digits.");
        }
        return customerDAO.registerCustomer(firstName, surname, address, pin);
    }

    public Customer findCustomer(String customerId) throws SQLException {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required.");
        }
        return customerDAO.findById(customerId);
    }

    public List<CustomerDAO.AdminCustomerSummary> getAdminCustomerSummaries() throws SQLException {
        return customerDAO.getAllCustomersWithAccounts();
    }
}