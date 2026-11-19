package controller;

import dao.AccountDAO;
import dao.CustomerDAO;
import dao.CustomerDAO.AdminCustomerSummary;
import dao.DatabaseManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import model.Account;
import model.Customer;
import model.TransactionRecord;

import java.sql.SQLException;
import java.util.List;

public class BankingFxmlController {

    private enum Role { NONE, ADMIN, CUSTOMER }

    @FXML private BorderPane rootPane;
    @FXML private Label roleBadgeLabel;
    @FXML private Button clearBtn;
    @FXML private Button logoutBtn;
    @FXML private Label statusLabel;

    // Login View
    @FXML private VBox loginPane;
    @FXML private TextField adminLoginUserField;
    @FXML private PasswordField adminLoginPinField;
    @FXML private TextField customerLoginIdField;
    @FXML private PasswordField customerLoginPinField;

    // Admin View
    @FXML private VBox adminPane;
    @FXML private TextField nextCustomerIdField;
    @FXML private TextField regFirstNameField;
    @FXML private TextField regSurnameField;
    @FXML private TextField regAddressField;
    @FXML private PasswordField regPinField;

    @FXML private TextField openAccCustomerIdField;
    @FXML private ComboBox<String> accountTypeCombo;
    @FXML private TextField branchField;
    @FXML private TextField initialDepositField;
    @FXML private TextField companyNameField;
    @FXML private TextField companyAddressField;

    @FXML private TextField newAdminUserField;
    @FXML private PasswordField newAdminPinField;

    @FXML private TableView<AdminCustomerSummary> adminCustomerTable;
    @FXML private TableColumn<AdminCustomerSummary, String> colCustomerId;
    @FXML private TableColumn<AdminCustomerSummary, String> colCustomerName;
    @FXML private TableColumn<AdminCustomerSummary, String> colCustomerAddress;
    @FXML private TableColumn<AdminCustomerSummary, String> colAccountTypes;
    @FXML private TableColumn<AdminCustomerSummary, String> colTotalBalance;

    // Customer View
    @FXML private VBox customerPane;
    @FXML private Label customerWelcomeLabel;
    @FXML private ListView<Account> customerAccountsListView;
    @FXML private Label selectedAccountBalanceLabel;
    @FXML private TextField txAmountField;
    @FXML private ListView<TransactionRecord> transactionHistoryListView;

    private final CustomerController customerController = new CustomerController(new CustomerDAO());
    private final AccountController accountController = new AccountController(new AccountDAO());

    private Role currentRole = Role.NONE;
    private Customer loggedInCustomer;

    private final ObservableList<AdminCustomerSummary> adminSummaryData = FXCollections.observableArrayList();
    private final ObservableList<Account> customerAccountData = FXCollections.observableArrayList();
    private final ObservableList<TransactionRecord> transactionData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        accountTypeCombo.setItems(FXCollections.observableArrayList(
                "Savings Account",
                "Investment Account",
                "Cheque Account"
        ));
        accountTypeCombo.getSelectionModel().selectFirst();
        updateChequeFieldsVisibility();
        accountTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateChequeFieldsVisibility());

        colCustomerId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().customerId()));
        colCustomerName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fullName()));
        colCustomerAddress.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().address()));
        colAccountTypes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().accountSummary()));
        colTotalBalance.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("BWP %.2f", data.getValue().totalBalance())));
        adminCustomerTable.setItems(adminSummaryData);

        customerAccountsListView.setItems(customerAccountData);
        transactionHistoryListView.setItems(transactionData);

        customerAccountsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldAcc, newAcc) -> {
            onAccountSelected(newAcc);
        });

        switchToLoginView();
    }

    private void updateChequeFieldsVisibility() {
        boolean isCheque = "Cheque Account".equals(accountTypeCombo.getValue());
        companyNameField.setDisable(!isCheque);
        companyAddressField.setDisable(!isCheque);
        if (!isCheque) {
            companyNameField.clear();
            companyAddressField.clear();
        }
    }

    @FXML
    private void handleAdminLogin() {
        try {
            boolean ok = customerController.loginAdmin(
                    adminLoginUserField.getText(),
                    adminLoginPinField.getText()
            );
            if (!ok) {
                showError("Invalid Admin username or 4-digit PIN.");
                return;
            }
            currentRole = Role.ADMIN;
            loggedInCustomer = null;
            showPane(adminPane);
            roleBadgeLabel.setText("ADMIN");
            logoutBtn.setVisible(true);
            logoutBtn.setManaged(true);
            refreshNextCustomerIdPreview();
            refreshAdminTable();
            setStatus("Logged in as Admin.");
        } catch (IllegalArgumentException | SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleCustomerLogin() {
        try {
            Customer customer = customerController.loginCustomer(
                    customerLoginIdField.getText(),
                    customerLoginPinField.getText()
            );
            currentRole = Role.CUSTOMER;
            loggedInCustomer = customer;
            showPane(customerPane);
            roleBadgeLabel.setText("CUSTOMER: " + customer.getCustomerId());
            logoutBtn.setVisible(true);
            logoutBtn.setManaged(true);
            customerWelcomeLabel.setText("Welcome, " + customer.getFirstName() + " " + customer.getSurname()
                    + " (" + customer.getCustomerId() + ")");
            refreshCustomerAccounts(null);
            setStatus("Logged in as " + customer.getCustomerId() + ".");
        } catch (IllegalArgumentException | SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleRegisterCustomer() {
        if (currentRole != Role.ADMIN) {
            showError("Only an authenticated Admin can register new customers.");
            return;
        }
        try {
            Customer created = customerController.registerCustomer(
                    regFirstNameField.getText(),
                    regSurnameField.getText(),
                    regAddressField.getText(),
                    regPinField.getText()
            );
            regFirstNameField.clear();
            regSurnameField.clear();
            regAddressField.clear();
            regPinField.clear();
            openAccCustomerIdField.setText(created.getCustomerId());
            refreshNextCustomerIdPreview();
            refreshAdminTable();
            showInfo("Customer Registered",
                    "New Customer ID: " + created.getCustomerId() + "\nName: " + created.getFirstName() + " " + created.getSurname());
            setStatus("Registered customer " + created.getCustomerId() + ".");
        } catch (IllegalArgumentException | SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleOpenAccount() {
        if (currentRole != Role.ADMIN) {
            showError("Only an authenticated Admin can open customer accounts.");
            return;
        }
        try {
            String cid = openAccCustomerIdField.getText().trim();
            Customer target = customerController.findCustomer(cid);
            if (target == null) {
                showError("Customer ID '" + cid + "' does not exist. Register the customer first.");
                return;
            }

            Account opened = accountController.openAccount(
                    target.getCustomerId(),
                    accountTypeCombo.getValue(),
                    branchField.getText(),
                    initialDepositField.getText(),
                    companyNameField.getText(),
                    companyAddressField.getText()
            );

            branchField.clear();
            initialDepositField.clear();
            companyNameField.clear();
            companyAddressField.clear();
            refreshAdminTable();
            showInfo("Account Opened",
                    "Account Number: " + opened.getAccountNumber()
                            + "\nCustomer: " + target.getCustomerId()
                            + "\nType: " + opened.getAccountType()
                            + "\nBalance: BWP " + String.format("%.2f", opened.getBalance()));
            setStatus("Opened " + opened.getAccountType() + " (" + opened.getAccountNumber() + ") for " + target.getCustomerId() + ".");
        } catch (IllegalArgumentException | SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleRegisterAdmin() {
        if (currentRole != Role.ADMIN) {
            showError("Only an authenticated Admin can register another Admin.");
            return;
        }
        try {
            boolean created = customerController.registerAdmin(
                    newAdminUserField.getText(),
                    newAdminPinField.getText()
            );
            if (!created) {
                showError("An admin with that username already exists.");
                return;
            }
            String createdUser = newAdminUserField.getText().trim();
            newAdminUserField.clear();
            newAdminPinField.clear();
            showInfo("Admin Created", "New admin '" + createdUser + "' has been registered.");
            setStatus("Registered new admin '" + createdUser + "'.");
        } catch (IllegalArgumentException | SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleRefreshAdminOverview() {
        try {
            refreshNextCustomerIdPreview();
            refreshAdminTable();
            setStatus("Refreshed customer and account balances overview.");
        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleDeposit() {
        Account selected = customerAccountsListView.getSelectionModel().getSelectedItem();
        try {
            accountController.deposit(selected, txAmountField.getText());
            txAmountField.clear();
            refreshCustomerAccounts(selected.getAccountNumber());
            setStatus("Deposit recorded on account " + selected.getAccountNumber() + ".");
        } catch (IllegalArgumentException | SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleWithdraw() {
        Account selected = customerAccountsListView.getSelectionModel().getSelectedItem();
        try {
            accountController.withdraw(selected, txAmountField.getText());
            txAmountField.clear();
            refreshCustomerAccounts(selected.getAccountNumber());
            setStatus("Withdrawal recorded on account " + selected.getAccountNumber() + ".");
        } catch (IllegalArgumentException | UnsupportedOperationException | SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleApplyInterest() {
        Account selected = customerAccountsListView.getSelectionModel().getSelectedItem();
        try {
            double interest = accountController.applyMonthlyInterest(selected);
            refreshCustomerAccounts(selected.getAccountNumber());
            if (interest > 0) {
                setStatus(String.format("Applied BWP %.2f monthly interest to %s.",
                        interest, selected.getAccountNumber()));
            } else {
                setStatus("No monthly interest applies to " + selected.getAccountType() + ".");
            }
        } catch (IllegalArgumentException | SQLException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Non-destructive Clear: clears all input fields on the active view without deleting saved DB data.
     */
    @FXML
    private void handleClear() {
        if (currentRole == Role.NONE) {
            adminLoginUserField.clear();
            adminLoginPinField.clear();
            customerLoginIdField.clear();
            customerLoginPinField.clear();
        } else if (currentRole == Role.ADMIN) {
            regFirstNameField.clear();
            regSurnameField.clear();
            regAddressField.clear();
            regPinField.clear();
            openAccCustomerIdField.clear();
            branchField.clear();
            initialDepositField.clear();
            companyNameField.clear();
            companyAddressField.clear();
            newAdminUserField.clear();
            newAdminPinField.clear();
        } else if (currentRole == Role.CUSTOMER) {
            txAmountField.clear();
        }
        setStatus("Input fields cleared (saved records remain intact).");
    }

    @FXML
    private void handleLogout() {
        switchToLoginView();
        setStatus("Logged out.");
    }

    private void switchToLoginView() {
        currentRole = Role.NONE;
        loggedInCustomer = null;
        adminLoginUserField.clear();
        adminLoginPinField.clear();
        customerLoginIdField.clear();
        customerLoginPinField.clear();
        customerAccountData.clear();
        transactionData.clear();
        roleBadgeLabel.setText("Not Logged In");
        logoutBtn.setVisible(false);
        logoutBtn.setManaged(false);
        showPane(loginPane);
    }

    private void showPane(VBox activePane) {
        loginPane.setVisible(activePane == loginPane);
        loginPane.setManaged(activePane == loginPane);

        adminPane.setVisible(activePane == adminPane);
        adminPane.setManaged(activePane == adminPane);

        customerPane.setVisible(activePane == customerPane);
        customerPane.setManaged(activePane == customerPane);
    }

    private void refreshNextCustomerIdPreview() throws SQLException {
        nextCustomerIdField.setText(DatabaseManager.generateNextCustomerId());
    }

    private void refreshAdminTable() throws SQLException {
        adminSummaryData.setAll(customerController.getAdminCustomerSummaries());
    }

    private void refreshCustomerAccounts(String preferredAccountNumber) throws SQLException {
        if (loggedInCustomer == null) return;
        List<Account> freshAccounts = accountController.getCustomerAccounts(loggedInCustomer.getCustomerId());
        customerAccountData.setAll(freshAccounts);

        Account toSelect = null;
        if (preferredAccountNumber != null) {
            for (Account acc : freshAccounts) {
                if (acc.getAccountNumber().equals(preferredAccountNumber)) {
                    toSelect = acc;
                    break;
                }
            }
        }
        if (toSelect == null && !freshAccounts.isEmpty()) {
            toSelect = freshAccounts.get(0);
        }

        if (toSelect != null) {
            customerAccountsListView.getSelectionModel().select(toSelect);
            onAccountSelected(toSelect);
        } else {
            selectedAccountBalanceLabel.setText("Selected Balance: BWP 0.00");
            transactionData.clear();
        }
    }

    private void onAccountSelected(Account account) {
        if (account == null) {
            selectedAccountBalanceLabel.setText("Selected Balance: BWP 0.00");
            transactionData.clear();
            return;
        }
        selectedAccountBalanceLabel.setText(String.format("Selected Balance (%s): BWP %.2f",
                account.getAccountNumber(), account.getBalance()));
        try {
            transactionData.setAll(accountController.getTransactionHistory(account));
        } catch (SQLException ex) {
            showError("Failed to load transaction history: " + ex.getMessage());
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        setStatus("Error: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Banking System");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}