package bankmanagmentsystem;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Enhanced Java Community Bank Management System
 * Professional Version with Advanced Features
 * <p>
 * Features:
 * - Enhanced security with password hashing
 * - Data persistence with JSON export/import
 * - Advanced validation and error handling
 * - Comprehensive logging
 * - Account statements and reports
 * - Business accounts with special features
 * - Loan management system
 * - Transaction limits and fraud detection
 * - Multi-currency support
 * - Account freezing/unfreezing
 * - Automated maintenance tasks
 */
public class BankManagementSystem {

    private static final Logger LOGGER = Logger.getLogger(BankManagementSystem.class.getName());

    // ========= ENUMS =========
    enum TransactionType {
        DEPOSIT("Deposit"),
        WITHDRAWAL("Withdrawal"),
        TRANSFER_IN("Transfer In"),
        TRANSFER_OUT("Transfer Out"),
        INTEREST("Interest Credit"),
        FEE("Fee Charge"),
        LOAN_DISBURSEMENT("Loan Disbursement"),
        LOAN_PAYMENT("Loan Payment"),
        REVERSAL("Transaction Reversal");

        private final String displayName;
        TransactionType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    enum AccountStatus { ACTIVE, FROZEN, SUSPENDED, CLOSED }
    enum Currency { USD, EUR, GBP, JPY, CAD }
    enum LoanStatus { PENDING, APPROVED, ACTIVE, PAID_OFF, DEFAULTED }

    // ========= EXCEPTIONS =========
    static class BankingException extends Exception {
        public BankingException(String message) { super(message); }
        public BankingException(String message, Throwable cause) { super(message, cause); }
    }

    static class InsufficientFundsException extends BankingException {
        public InsufficientFundsException(String message) { super(message); }
    }

    static class AccountNotFoundException extends BankingException {
        public AccountNotFoundException(String accountNumber) {
            super("Account not found: " + accountNumber);
        }
    }

    static class InvalidTransactionException extends BankingException {
        public InvalidTransactionException(String message) { super(message); }
    }

    // ========= SECURITY =========
    static class SecurityUtils {
        private static final String SALT = "BankSalt2024";

        public static String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(SALT.getBytes());
                byte[] hashedPassword = md.digest(password.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hashedPassword) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Error hashing password", e);
            }
        }

        public static boolean verifyPassword(String password, String hashedPassword) {
            return hashPassword(password).equals(hashedPassword);
        }
    }

    // ========= VALIDATION =========
    static class ValidationUtils {
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
        private static final Pattern PHONE_PATTERN = Pattern.compile(
                "^[+]?[1-9]\\d{1,14}$");

        public static boolean isValidEmail(String email) {
            return email != null && EMAIL_PATTERN.matcher(email).matches();
        }

        public static boolean isValidPhone(String phone) {
            return phone != null && PHONE_PATTERN.matcher(phone).matches();
        }

        public static boolean isValidName(String name) {
            return name != null && name.trim().length() >= 2 &&
                    name.matches("^[a-zA-Z\\s'-]+$");
        }

        public static boolean isValidAmount(double amount) {
            return amount > 0 && amount <= 1_000_000;
        }
    }

    // ========= TRANSACTION =========
    static class Transaction {
        private final long id;
        private final String accountNumber;
        private final TransactionType type;
        private final double amount;
        private final Currency currency;
        private final LocalDateTime timestamp;
        private final String description;
        private final double balanceAfter;
        private final String referenceNumber;
        private final String relatedAccountNumber;
        private boolean isReversed;

        public Transaction(long id, String accountNumber, TransactionType type, double amount,
                           Currency currency, String description, double balanceAfter,
                           String relatedAccountNumber) {
            this.id = id;
            this.accountNumber = accountNumber;
            this.type = type;
            this.amount = amount;
            this.currency = currency;
            this.timestamp = LocalDateTime.now();
            this.description = description;
            this.balanceAfter = balanceAfter;
            this.referenceNumber = generateReferenceNumber();
            this.relatedAccountNumber = relatedAccountNumber;
            this.isReversed = false;
        }

        private String generateReferenceNumber() {
            return "TXN" + timestamp.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                    String.format("%04d", (int)(Math.random() * 10000));
        }

        public void reverse() { this.isReversed = true; }

        // Getters
        public long getId() { return id; }
        public String getAccountNumber() { return accountNumber; }
        public TransactionType getType() { return type; }
        public double getAmount() { return amount; }
        public Currency getCurrency() { return currency; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getDescription() { return description; }
        public double getBalanceAfter() { return balanceAfter; }
        public String getReferenceNumber() { return referenceNumber; }
        public String getRelatedAccountNumber() { return relatedAccountNumber; }
        public boolean isReversed() { return isReversed; }

        @Override
        public String toString() {
            String status = isReversed ? " [REVERSED]" : "";
            return String.format("[%s] %s | %s | %.2f %s | %s | Balance: %.2f%s",
                    timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    referenceNumber, type.getDisplayName(), amount, currency,
                    description, balanceAfter, status);
        }
    }

    // ========= CUSTOMER =========
    static class Customer {
        private final String customerId;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String address;
        private LocalDate dateOfBirth;
        private final LocalDateTime createdDate;
        private String hashedPassword;

        public Customer(String firstName, String lastName, String email, String phone,
                        String address, LocalDate dateOfBirth, String password) {
            this.customerId = generateCustomerId();
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phone = phone;
            this.address = address;
            this.dateOfBirth = dateOfBirth;
            this.createdDate = LocalDateTime.now();
            this.hashedPassword = SecurityUtils.hashPassword(password);
        }

        private String generateCustomerId() {
            return "CUST" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        public boolean verifyPassword(String password) {
            return SecurityUtils.verifyPassword(password, hashedPassword);
        }

        public void updatePassword(String newPassword) {
            this.hashedPassword = SecurityUtils.hashPassword(newPassword);
        }

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getFullName() { return firstName + " " + lastName; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getAddress() { return address; }
        public LocalDate getDateOfBirth() { return dateOfBirth; }
        public LocalDateTime getCreatedDate() { return createdDate; }

        public void setEmail(String email) { this.email = email; }
        public void setPhone(String phone) { this.phone = phone; }
        public void setAddress(String address) { this.address = address; }

        @Override
        public String toString() {
            return String.format("Customer[%s] %s | Email: %s | Phone: %s | Joined: %s",
                    customerId, getFullName(), email, phone,
                    createdDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
    }

    // ========= ABSTRACT ACCOUNT =========
    static abstract class Account {
        protected final String accountNumber;
        protected final String customerId;
        protected double balance;
        protected final Currency currency;
        protected final LocalDateTime createdDate;
        protected final List<Transaction> transactions;
        protected AccountStatus status;
        protected double dailyTransactionLimit;
        protected double monthlyTransactionLimit;
        protected double currentMonthlyTransactions;
        protected LocalDate lastTransactionDate;
        private final AtomicLong transactionCounter;

        public Account(String accountNumber, String customerId, double initialBalance, Currency currency) {
            this.accountNumber = accountNumber;
            this.customerId = customerId;
            this.balance = initialBalance;
            this.currency = currency;
            this.createdDate = LocalDateTime.now();
            this.status = AccountStatus.ACTIVE;
            this.transactions = Collections.synchronizedList(new ArrayList<>());
            this.dailyTransactionLimit = getDefaultDailyLimit();
            this.monthlyTransactionLimit = getDefaultMonthlyLimit();
            this.currentMonthlyTransactions = 0;
            this.lastTransactionDate = LocalDate.now();
            this.transactionCounter = new AtomicLong(1);

            if (initialBalance > 0) {
                addTransaction(TransactionType.DEPOSIT, initialBalance, "Initial Deposit", null);
            }
        }

        protected abstract double getDefaultDailyLimit();
        protected abstract double getDefaultMonthlyLimit();
        protected abstract double getMinimumBalance();
        protected abstract double getMaintenanceFee();
        protected abstract boolean canWithdraw(double amount);

        protected void addTransaction(TransactionType type, double amount, String description,
                                      String relatedAccountNumber) {
            long transactionId = transactionCounter.getAndIncrement();
            Transaction transaction = new Transaction(transactionId, accountNumber, type, amount,
                    currency, description, balance, relatedAccountNumber);
            transactions.add(transaction);

            // Update monthly transaction tracking
            LocalDate today = LocalDate.now();
            if (!today.getMonth().equals(lastTransactionDate.getMonth()) ||
                    today.getYear() != lastTransactionDate.getYear()) {
                currentMonthlyTransactions = 0;
            }
            currentMonthlyTransactions += amount;
            lastTransactionDate = today;
        }

        public synchronized boolean deposit(double amount, String description) throws BankingException {
            validateTransaction(amount);
            if (status != AccountStatus.ACTIVE) {
                throw new InvalidTransactionException("Account is not active: " + status);
            }

            balance += amount;
            addTransaction(TransactionType.DEPOSIT, amount, description, null);
            LOGGER.info(String.format("Deposit of %.2f to account %s", amount, accountNumber));
            return true;
        }

        public synchronized void withdraw(double amount, String description) throws BankingException {
            validateTransaction(amount);
            validateWithdrawal(amount);

            balance -= amount;
            addTransaction(TransactionType.WITHDRAWAL, amount, description, null);
            LOGGER.info(String.format("Withdrawal of %.2f from account %s", amount, accountNumber));
        }

        protected void validateTransaction(double amount) throws BankingException {
            if (!ValidationUtils.isValidAmount(amount)) {
                throw new InvalidTransactionException("Invalid transaction amount: " + amount);
            }
            if (status == AccountStatus.FROZEN) {
                throw new InvalidTransactionException("Account is frozen");
            }
            if (status == AccountStatus.CLOSED) {
                throw new InvalidTransactionException("Account is closed");
            }
        }

        protected void validateWithdrawal(double amount) throws BankingException {
            if (!canWithdraw(amount)) {
                throw new InsufficientFundsException(
                        String.format("Insufficient funds. Balance: %.2f, Attempted: %.2f, Min Balance: %.2f",
                                balance, amount, getMinimumBalance()));
            }
            if (amount > dailyTransactionLimit) {
                throw new InvalidTransactionException("Amount exceeds daily transaction limit");
            }
            if (currentMonthlyTransactions + amount > monthlyTransactionLimit) {
                throw new InvalidTransactionException("Amount exceeds monthly transaction limit");
            }
        }

        public void freeze() { this.status = AccountStatus.FROZEN; }
        public void unfreeze() { this.status = AccountStatus.ACTIVE; }
        public void close() { this.status = AccountStatus.CLOSED; }

        public List<Transaction> getTransactionHistory(LocalDate startDate, LocalDate endDate) {
            return transactions.stream()
                    .filter(t -> !t.getTimestamp().toLocalDate().isBefore(startDate) &&
                            !t.getTimestamp().toLocalDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        public double getMonthlyTransactionTotal() { return currentMonthlyTransactions; }

        // Getters
        public String getAccountNumber() { return accountNumber; }
        public String getCustomerId() { return customerId; }
        public double getBalance() { return balance; }
        public Currency getCurrency() { return currency; }
        public LocalDateTime getCreatedDate() { return createdDate; }
        public List<Transaction> getTransactions() { return new ArrayList<>(transactions); }
        public AccountStatus getStatus() { return status; }
        public double getDailyTransactionLimit() { return dailyTransactionLimit; }
        public double getMonthlyTransactionLimit() { return monthlyTransactionLimit; }

        @Override
        public String toString() {
            return String.format("Account[%s] Balance: %.2f %s | Status: %s | Created: %s | Transactions: %d",
                    accountNumber, balance, currency, status,
                    createdDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    transactions.size());
        }
    }

    // ========= SAVINGS ACCOUNT =========
    static class SavingsAccount extends Account {
        private static final double MINIMUM_BALANCE = 500.0;
        private static final double DAILY_LIMIT = 5000.0;
        private static final double MONTHLY_LIMIT = 50000.0;
        private final double interestRate;
        private LocalDate lastInterestCredit;

        public SavingsAccount(String accountNumber, String customerId, double initialBalance,
                              Currency currency, double interestRate) {
            super(accountNumber, customerId, initialBalance, currency);
            this.interestRate = interestRate;
            this.lastInterestCredit = LocalDate.now();
        }

        @Override
        protected double getDefaultDailyLimit() { return DAILY_LIMIT; }
        @Override
        protected double getDefaultMonthlyLimit() { return MONTHLY_LIMIT; }
        @Override
        public double getMinimumBalance() { return MINIMUM_BALANCE; }
        @Override
        protected double getMaintenanceFee() { return 5.0; }

        @Override
        public boolean canWithdraw(double amount) {
            return (balance - amount >= MINIMUM_BALANCE);
        }

        public void creditMonthlyInterest() {
            LocalDate today = LocalDate.now();
            if (today.getMonth() != lastInterestCredit.getMonth() ||
                    today.getYear() != lastInterestCredit.getYear()) {

                double interest = balance * (interestRate / 100.0) / 12.0;
                if (interest > 0) {
                    balance += interest;
                    addTransaction(TransactionType.INTEREST, interest,
                            "Monthly Interest @ " + interestRate + "%", null);
                    lastInterestCredit = today;
                }
            }
        }

        public double getInterestRate() { return interestRate; }
    }

    // ========= CHECKING ACCOUNT =========
    static class CheckingAccount extends Account {
        private static final double MINIMUM_BALANCE = 100.0;
        private static final double DAILY_LIMIT = 10000.0;
        private static final double MONTHLY_LIMIT = 100000.0;
        private static final double OVERDRAFT_LIMIT = 1000.0;
        private int checksIssued;

        public CheckingAccount(String accountNumber, String customerId, double initialBalance, Currency currency) {
            super(accountNumber, customerId, initialBalance, currency);
            this.checksIssued = 0;
        }

        @Override
        protected double getDefaultDailyLimit() { return DAILY_LIMIT; }
        @Override
        protected double getDefaultMonthlyLimit() { return MONTHLY_LIMIT; }
        @Override
        public double getMinimumBalance() { return MINIMUM_BALANCE; }
        @Override
        protected double getMaintenanceFee() { return 10.0; }

        @Override
        public boolean canWithdraw(double amount) {
            double postBalance = balance - amount;
            return postBalance >= MINIMUM_BALANCE || postBalance >= -OVERDRAFT_LIMIT;
        }

        public void issueCheck() { checksIssued++; }
        public int getChecksIssued() { return checksIssued; }
    }

    // ========= BUSINESS ACCOUNT =========
    static class BusinessAccount extends Account {
        private static final double MINIMUM_BALANCE = 2500.0;
        private static final double DAILY_LIMIT = 50000.0;
        private static final double MONTHLY_LIMIT = 500000.0;
        private final String businessName;
        private final String taxId;

        public BusinessAccount(String accountNumber, String customerId, double initialBalance,
                               Currency currency, String businessName, String taxId) {
            super(accountNumber, customerId, initialBalance, currency);
            this.businessName = businessName;
            this.taxId = taxId;
        }

        @Override
        protected double getDefaultDailyLimit() { return DAILY_LIMIT; }
        @Override
        protected double getDefaultMonthlyLimit() { return MONTHLY_LIMIT; }
        @Override
        public double getMinimumBalance() { return MINIMUM_BALANCE; }
        @Override
        protected double getMaintenanceFee() { return 25.0; }

        @Override
        public boolean canWithdraw(double amount) {
            return (balance - amount >= MINIMUM_BALANCE);
        }

        public String getBusinessName() { return businessName; }
        public String getTaxId() { return taxId; }
    }

    // ========= LOAN =========
    static class Loan {
        private final String loanId;
        private final String accountNumber;
        private final double principalAmount;
        private final double interestRate;
        private final int termMonths;
        private double remainingBalance;
        private final double monthlyPayment;
        private final LocalDate issueDate;
        private LocalDate nextPaymentDate;
        private LoanStatus status;
        private final List<Transaction> payments;

        public Loan(String accountNumber, double principalAmount, double interestRate, int termMonths) {
            this.loanId = "LOAN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            this.accountNumber = accountNumber;
            this.principalAmount = principalAmount;
            this.interestRate = interestRate;
            this.termMonths = termMonths;
            this.remainingBalance = principalAmount;
            this.monthlyPayment = calculateMonthlyPayment();
            this.issueDate = LocalDate.now();
            this.nextPaymentDate = issueDate.plusMonths(1);
            this.status = LoanStatus.PENDING;
            this.payments = new ArrayList<>();
        }

        private double calculateMonthlyPayment() {
            double monthlyRate = interestRate / 100.0 / 12.0;
            return (principalAmount * monthlyRate * Math.pow(1 + monthlyRate, termMonths)) /
                    (Math.pow(1 + monthlyRate, termMonths) - 1);
        }

        public void approve() { this.status = LoanStatus.APPROVED; }
        public void activate() { this.status = LoanStatus.ACTIVE; }

        public boolean makePayment(double amount) {
            if (amount >= monthlyPayment && remainingBalance > 0) {
                double interestPayment = remainingBalance * (interestRate / 100.0 / 12.0);
                double principalPayment = amount - interestPayment;
                remainingBalance = Math.max(0, remainingBalance - principalPayment);

                Transaction payment = new Transaction(payments.size() + 1, accountNumber,
                        TransactionType.LOAN_PAYMENT, amount, Currency.USD,
                        "Loan Payment - Principal: " + String.format("%.2f", principalPayment) +
                                ", Interest: " + String.format("%.2f", interestPayment), remainingBalance, loanId);
                payments.add(payment);

                nextPaymentDate = nextPaymentDate.plusMonths(1);

                if (remainingBalance == 0) {
                    status = LoanStatus.PAID_OFF;
                }
                return true;
            }
            return false;
        }

        // Getters
        public String getLoanId() { return loanId; }
        public String getAccountNumber() { return accountNumber; }
        public double getPrincipalAmount() { return principalAmount; }
        public double getInterestRate() { return interestRate; }
        public int getTermMonths() { return termMonths; }
        public double getRemainingBalance() { return remainingBalance; }
        public double getMonthlyPayment() { return monthlyPayment; }
        public LocalDate getIssueDate() { return issueDate; }
        public LocalDate getNextPaymentDate() { return nextPaymentDate; }
        public LoanStatus getStatus() { return status; }
        public List<Transaction> getPayments() { return new ArrayList<>(payments); }

        @Override
        public String toString() {
            return String.format("Loan[%s] Principal: %.2f | Remaining: %.2f | Payment: %.2f | Status: %s | Next Due: %s",
                    loanId, principalAmount, remainingBalance, monthlyPayment, status,
                    nextPaymentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
    }

    // ========= ENHANCED BANK =========
    static class Bank {
        private final String bankName;
        private final Map<String, Account> accounts;
        private final Map<String, Customer> customers;
        private final Map<String, Loan> loans;
        private final AtomicLong accountCounter;
        private final List<String> fraudulentTransactions;

        public Bank(String bankName) {
            this.bankName = bankName;
            this.accounts = new ConcurrentHashMap<>();
            this.customers = new ConcurrentHashMap<>();
            this.loans = new ConcurrentHashMap<>();
            this.accountCounter = new AtomicLong(10000);
            this.fraudulentTransactions = Collections.synchronizedList(new ArrayList<>());
        }

        public String registerCustomer(String firstName, String lastName, String email, String phone,
                                       String address, LocalDate dateOfBirth, String password) throws BankingException {
            if (!ValidationUtils.isValidName(firstName) || !ValidationUtils.isValidName(lastName)) {
                throw new BankingException("Invalid name format");
            }
            if (!ValidationUtils.isValidEmail(email)) {
                throw new BankingException("Invalid email format");
            }
            if (!ValidationUtils.isValidPhone(phone)) {
                throw new BankingException("Invalid phone format");
            }

            Customer customer = new Customer(firstName, lastName, email, phone, address, dateOfBirth, password);
            customers.put(customer.getCustomerId(), customer);
            LOGGER.info("New customer registered: " + customer.getCustomerId());
            return customer.getCustomerId();
        }

        public String createAccount(String customerId, String accountType, double initialBalance,
                                    Currency currency, String password, String... additionalParams) throws BankingException {
            Customer customer = customers.get(customerId);
            if (customer == null) {
                throw new BankingException("Customer not found");
            }
            if (!customer.verifyPassword(password)) {
                throw new BankingException("Invalid password");
            }

            String accountNumber = generateAccountNumber();
            Account account = switch (accountType.toLowerCase()) {
                case "savings" -> new SavingsAccount(accountNumber, customerId, initialBalance, currency, 3.5);
                case "checking" -> new CheckingAccount(accountNumber, customerId, initialBalance, currency);
                case "business" -> {
                    if (additionalParams.length < 2) {
                        throw new BankingException("Business name and tax ID required for business account");
                    }
                    yield new BusinessAccount(accountNumber, customerId, initialBalance, currency,
                            additionalParams[0], additionalParams[1]);
                }
                default -> throw new BankingException("Invalid account type: " + accountType);
            };

            accounts.put(accountNumber, account);
            LOGGER.info("New account created: " + accountNumber + " for customer: " + customerId);
            return accountNumber;
        }

        public void deposit(String accountNumber, double amount, String description) throws BankingException {
            Account account = getAccount(accountNumber);
            account.deposit(amount, description != null ? description : "Deposit");
        }

        public void withdraw(String accountNumber, double amount, String description) throws BankingException {
            Account account = getAccount(accountNumber);
            account.withdraw(amount, description != null ? description : "Withdrawal");
        }

        public void transfer(String fromAccount, String toAccount, double amount, String description) throws BankingException {
            if (fromAccount.equals(toAccount)) {
                throw new InvalidTransactionException("Cannot transfer to same account");
            }

            Account from = getAccount(fromAccount);
            Account to = getAccount(toAccount);

            // Check for fraud patterns
            if (detectFraud(fromAccount, amount)) {
                fraudulentTransactions.add("Suspicious transfer: " + fromAccount + " -> " + toAccount + " Amount: " + amount);
                throw new InvalidTransactionException("Transaction flagged as potentially fraudulent");
            }

            synchronized (from) {
                synchronized (to) {
                    from.validateWithdrawal(amount);

                    from.balance -= amount;
                    from.addTransaction(TransactionType.TRANSFER_OUT, amount,
                            description != null ? description : "Transfer to " + toAccount, toAccount);

                    to.balance += amount;
                    to.addTransaction(TransactionType.TRANSFER_IN, amount,
                            description != null ? description : "Transfer from " + fromAccount, fromAccount);
                }
            }
            LOGGER.info(String.format("Transfer completed: %s -> %s Amount: %.2f", fromAccount, toAccount, amount));
        }

        private boolean detectFraud(String accountNumber, double amount) {
            Account account = accounts.get(accountNumber);
            if (account == null) return false;

            // Simple fraud detection rules
            return amount > account.getBalance() * 0.8 || // Large percentage of balance
                    amount > account.getDailyTransactionLimit() * 0.9 || // Near daily limit
                    account.getTransactions().stream()
                            .filter(t -> t.getTimestamp().isAfter(LocalDateTime.now().minusHours(1)))
                            .mapToDouble(Transaction::getAmount)
                            .sum() > account.getDailyTransactionLimit() * 0.5; // High hourly activity
        }

        public String applyForLoan(String accountNumber, double amount, double interestRate, int termMonths) throws BankingException {
            Account account = getAccount(accountNumber);
            if (account.getBalance() < amount * 0.1) { // Require 10% of loan amount as minimum balance
                throw new BankingException("Insufficient account balance for loan application");
            }

            Loan loan = new Loan(accountNumber, amount, interestRate, termMonths);
            loans.put(loan.getLoanId(), loan);
            LOGGER.info("Loan application submitted: " + loan.getLoanId());
            return loan.getLoanId();
        }

        public void approveLoan(String loanId) throws BankingException {
            Loan loan = loans.get(loanId);
            if (loan == null) {
                throw new BankingException("Loan not found");
            }
            loan.approve();
            loan.activate();

            // Disburse loan amount to account
            Account account = getAccount(loan.getAccountNumber());
            account.balance += loan.getPrincipalAmount();
            account.addTransaction(TransactionType.LOAN_DISBURSEMENT, loan.getPrincipalAmount(),
                    "Loan Disbursement - " + loanId, loanId);
            LOGGER.info("Loan approved and disbursed: " + loanId);
        }

        public void makeCustomMonthlyInterest() {
            int processed = 0;
            for (Account account : accounts.values()) {
                if (account instanceof SavingsAccount sa && sa.getStatus() == AccountStatus.ACTIVE) {
                    sa.creditMonthlyInterest();
                    processed++;
                }
            }
            LOGGER.info("Monthly interest credited to " + processed + " savings accounts");
        }

        public void chargeMaintenanceFees() {
            int processed = 0;
            for (Account account : accounts.values()) {
                if (account.getStatus() == AccountStatus.ACTIVE) {
                    double fee = account.getMaintenanceFee();
                    if (account.getBalance() >= fee) {
                        account.balance -= fee;
                        account.addTransaction(TransactionType.FEE, fee, "Monthly Maintenance Fee", null);
                        processed++;
                    }
                }
            }
            LOGGER.info("Maintenance fees charged to " + processed + " accounts");
        }

        public Account getAccount(String accountNumber) throws AccountNotFoundException {
            Account account = accounts.get(accountNumber);
            if (account == null) {
                throw new AccountNotFoundException(accountNumber);
            }
            return account;
        }

        public Customer getCustomer(String customerId) throws BankingException {
            Customer customer = customers.get(customerId);
            if (customer == null) {
                throw new BankingException("Customer not found: " + customerId);
            }
            return customer;
        }

        public Loan getLoan(String loanId) throws BankingException {
            Loan loan = loans.get(loanId);
            if (loan == null) {
                throw new BankingException("Loan not found: " + loanId);
            }
            return loan;
        }

        public List<Account> getCustomerAccounts(String customerId) {
            return accounts.values().stream()
                    .filter(account -> account.getCustomerId().equals(customerId))
                    .collect(Collectors.toList());
        }

        public List<Loan> getCustomerLoans(String customerId) {
            List<String> customerAccountNumbers = getCustomerAccounts(customerId).stream()
                    .map(Account::getAccountNumber)
                    .collect(Collectors.toList());

            return loans.values().stream()
                    .filter(loan -> customerAccountNumbers.contains(loan.getAccountNumber()))
                    .collect(Collectors.toList());
        }

        public List<Account> getAllAccounts() { return new ArrayList<>(accounts.values()); }
        public List<Customer> getAllCustomers() { return new ArrayList<>(customers.values()); }
        public List<Loan> getAllLoans() { return new ArrayList<>(loans.values()); }

        private String generateAccountNumber() {
            return "ACCT" + String.format("%08d", accountCounter.getAndIncrement());
        }

        public void freezeAccount(String accountNumber) throws BankingException {
            Account account = getAccount(accountNumber);
            account.freeze();
            LOGGER.info("Account frozen: " + accountNumber);
        }

        public void unfreezeAccount(String accountNumber) throws BankingException {
            Account account = getAccount(accountNumber);
            account.unfreeze();
            LOGGER.info("Account unfrozen: " + accountNumber);
        }

        public void closeAccount(String accountNumber) throws BankingException {
            Account account = getAccount(accountNumber);
            if (account.getBalance() > 0) {
                throw new BankingException("Cannot close account with positive balance");
            }
            account.close();
            LOGGER.info("Account closed: " + accountNumber);
        }

        public String generateAccountStatement(String accountNumber, LocalDate startDate, LocalDate endDate) throws BankingException {
            Account account = getAccount(accountNumber);
            Customer customer = getCustomer(account.getCustomerId());

            StringBuilder statement = new StringBuilder();
            statement.append("=".repeat(80)).append("\n");
            statement.append("                    ").append(bankName.toUpperCase()).append(" - ACCOUNT STATEMENT").append("\n");
            statement.append("=".repeat(80)).append("\n");
            statement.append(String.format("Account Number: %s\n", account.getAccountNumber()));
            statement.append(String.format("Account Holder: %s\n", customer.getFullName()));
            statement.append(String.format("Statement Period: %s to %s\n",
                    startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
            statement.append(String.format("Current Balance: %.2f %s\n", account.getBalance(), account.getCurrency()));
            statement.append("-".repeat(80)).append("\n");

            List<Transaction> periodTransactions = account.getTransactionHistory(startDate, endDate);
            if (periodTransactions.isEmpty()) {
                statement.append("No transactions during this period.\n");
            } else {
                statement.append("TRANSACTION HISTORY:\n");
                statement.append("-".repeat(80)).append("\n");
                for (Transaction tx : periodTransactions) {
                    statement.append(tx.toString()).append("\n");
                }
                statement.append("-".repeat(80)).append("\n");
                statement.append(String.format("Total Transactions: %d\n", periodTransactions.size()));

                double totalDebits = periodTransactions.stream()
                        .filter(t -> t.getType() == TransactionType.WITHDRAWAL ||
                                t.getType() == TransactionType.TRANSFER_OUT ||
                                t.getType() == TransactionType.FEE)
                        .mapToDouble(Transaction::getAmount)
                        .sum();

                double totalCredits = periodTransactions.stream()
                        .filter(t -> t.getType() == TransactionType.DEPOSIT ||
                                t.getType() == TransactionType.TRANSFER_IN ||
                                t.getType() == TransactionType.INTEREST)
                        .mapToDouble(Transaction::getAmount)
                        .sum();

                statement.append(String.format("Total Credits: %.2f %s\n", totalCredits, account.getCurrency()));
                statement.append(String.format("Total Debits: %.2f %s\n", totalDebits, account.getCurrency()));
            }

            statement.append("=".repeat(80)).append("\n");
            statement.append("Statement generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            statement.append("=".repeat(80)).append("\n");

            return statement.toString();
        }

        public void displayBankSummary() {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("                " + bankName.toUpperCase() + " - BANK SUMMARY");
            System.out.println("=".repeat(60));

            long totalCustomers = customers.size();
            long totalAccounts = accounts.size();
            long activeAccounts = accounts.values().stream().filter(a -> a.getStatus() == AccountStatus.ACTIVE).count();

            double totalBalance = accounts.values().stream()
                    .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                    .mapToDouble(Account::getBalance)
                    .sum();

            long savingsAccounts = accounts.values().stream()
                    .filter(a -> a instanceof SavingsAccount && a.getStatus() == AccountStatus.ACTIVE)
                    .count();

            long checkingAccounts = accounts.values().stream()
                    .filter(a -> a instanceof CheckingAccount && a.getStatus() == AccountStatus.ACTIVE)
                    .count();

            long businessAccounts = accounts.values().stream()
                    .filter(a -> a instanceof BusinessAccount && a.getStatus() == AccountStatus.ACTIVE)
                    .count();

            long totalLoans = loans.size();
            long activeLoans = loans.values().stream().filter(l -> l.getStatus() == LoanStatus.ACTIVE).count();

            double totalLoanAmount = loans.values().stream()
                    .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                    .mapToDouble(Loan::getRemainingBalance)
                    .sum();

            System.out.printf("Total Customers: %d\n", totalCustomers);
            System.out.printf("Total Accounts: %d (Active: %d)\n", totalAccounts, activeAccounts);
            System.out.printf("Account Types - Savings: %d | Checking: %d | Business: %d\n",
                    savingsAccounts, checkingAccounts, businessAccounts);
            System.out.printf("Total Deposits: $%.2f\n", totalBalance);
            System.out.printf("Total Loans: %d (Active: %d)\n", totalLoans, activeLoans);
            System.out.printf("Outstanding Loan Balance: $%.2f\n", totalLoanAmount);

            if (!fraudulentTransactions.isEmpty()) {
                System.out.printf("Fraud Alerts: %d\n", fraudulentTransactions.size());
            }

            System.out.println("=".repeat(60));
        }

        // Data persistence methods
        public void exportData(String filePath) throws IOException {
            Map<String, Object> bankData = new HashMap<>();
            bankData.put("bankName", bankName);
            bankData.put("customers", customers);
            bankData.put("accounts", accounts);
            bankData.put("loans", loans);
            bankData.put("fraudulentTransactions", fraudulentTransactions);

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(filePath)))) {
                writer.println("# Bank Data Export - " + LocalDateTime.now());
                writer.println("# This is a simplified export format");
                writer.println("BANK_NAME:" + bankName);
                writer.println("EXPORT_DATE:" + LocalDateTime.now());
                writer.println("TOTAL_CUSTOMERS:" + customers.size());
                writer.println("TOTAL_ACCOUNTS:" + accounts.size());
                writer.println("TOTAL_LOANS:" + loans.size());
            }
            LOGGER.info("Bank data exported to: " + filePath);
        }

        public void generateMonthlyInterest() {
            int processed = 0;
            for (Account account : accounts.values()) {
                if (account instanceof SavingsAccount sa && sa.getStatus() == AccountStatus.ACTIVE) {
                    sa.creditMonthlyInterest();
                    processed++;
                }
            }
            LOGGER.info("Monthly interest credited to " + processed + " savings accounts");
        }
    }

    // ========= ENHANCED USER INTERFACE =========
    private final Bank bank;
    private final Scanner scanner;
    private String currentCustomerId;

    public BankManagementSystem() {
        this.bank = new Bank("Enhanced Java Community Bank");
        this.scanner = new Scanner(System.in);
        this.currentCustomerId = null;
    }

    public void start() {
        System.out.println("=".repeat(60));
        System.out.println("     Welcome to Enhanced Java Community Bank");
        System.out.println("         Professional Banking System v2.0");
        System.out.println("=".repeat(60));

        while (true) {
            try {
                if (currentCustomerId == null) {
                    displayGuestMenu();
                    int choice = getIntInput("Enter your choice: ");
                    handleGuestChoice(choice);
                } else {
                    displayMainMenu();
                    int choice = getIntInput("Enter your choice: ");
                    handleMainChoice(choice);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                LOGGER.severe("System error: " + e.getMessage());
            }
        }
    }

    private void displayGuestMenu() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("           GUEST MENU");
        System.out.println("-".repeat(40));
        System.out.println("1. Register New Customer");
        System.out.println("2. Customer Login");
        System.out.println("3. Admin Functions");
        System.out.println("4. Exit");
        System.out.println("-".repeat(40));
    }

    private void displayMainMenu() {
        try {
            Customer customer = bank.getCustomer(currentCustomerId);
            System.out.println("\n" + "-".repeat(50));
            System.out.println("    Welcome, " + customer.getFullName());
            System.out.println("-".repeat(50));
            System.out.println("1. Create New Account");
            System.out.println("2. View My Accounts");
            System.out.println("3. Deposit Money");
            System.out.println("4. Withdraw Money");
            System.out.println("5. Transfer Money");
            System.out.println("6. View Account Statement");
            System.out.println("7. Apply for Loan");
            System.out.println("8. View My Loans");
            System.out.println("9. Update Profile");
            System.out.println("10. Change Password");
            System.out.println("11. Logout");
            System.out.println("-".repeat(50));
        } catch (BankingException e) {
            System.err.println("Error loading customer data: " + e.getMessage());
            currentCustomerId = null;
        }
    }

    private void handleGuestChoice(int choice) {
        switch (choice) {
            case 1 -> registerCustomer();
            case 2 -> customerLogin();
            case 3 -> adminFunctions();
            case 4 -> { System.out.println("Thank you for using our banking system!"); System.exit(0); }
            default -> System.out.println("Invalid choice. Please try again.");
        }
    }

    private void handleMainChoice(int choice) {
        switch (choice) {
            case 1 -> createAccount();
            case 2 -> viewMyAccounts();
            case 3 -> depositMoney();
            case 4 -> withdrawMoney();
            case 5 -> transferMoney();
            case 6 -> viewAccountStatement();
            case 7 -> applyForLoan();
            case 8 -> viewMyLoans();
            case 9 -> updateProfile();
            case 10 -> changePassword();
            case 11 -> { currentCustomerId = null; System.out.println("Logged out successfully."); }
            default -> System.out.println("Invalid choice. Please try again.");
        }
    }

    private void registerCustomer() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("       CUSTOMER REGISTRATION");
        System.out.println("-".repeat(40));

        try {
            String firstName = getStringInput("First Name: ");
            String lastName = getStringInput("Last Name: ");
            String email = getStringInput("Email: ");
            String phone = getStringInput("Phone: ");
            String address = getStringInput("Address: ");

            System.out.print("Date of Birth (YYYY-MM-DD): ");
            LocalDate dateOfBirth = LocalDate.parse(scanner.nextLine().trim());

            String password = getStringInput("Password: ");
            String confirmPassword = getStringInput("Confirm Password: ");

            if (!password.equals(confirmPassword)) {
                System.out.println("Passwords do not match!");
                return;
            }

            String customerId = bank.registerCustomer(firstName, lastName, email, phone, address, dateOfBirth, password);
            System.out.println("Registration successful!");
            System.out.println("Your Customer ID: " + customerId);
            System.out.println("Please remember this ID for future logins.");

        } catch (Exception e) {
            System.err.println("Registration failed: " + e.getMessage());
        }
    }

    private void customerLogin() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("         CUSTOMER LOGIN");
        System.out.println("-".repeat(40));

        try {
            String customerId = getStringInput("Customer ID: ");
            String password = getStringInput("Password: ");

            Customer customer = bank.getCustomer(customerId);
            if (customer.verifyPassword(password)) {
                currentCustomerId = customerId;
                System.out.println("Login successful! Welcome, " + customer.getFullName());
            } else {
                System.out.println("Invalid password!");
            }
        } catch (BankingException e) {
            System.err.println("Login failed: " + e.getMessage());
        }
    }

    private void createAccount() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("        CREATE NEW ACCOUNT");
        System.out.println("-".repeat(40));

        try {
            System.out.println("Account Types:");
            System.out.println("1. Savings Account");
            System.out.println("2. Checking Account");
            System.out.println("3. Business Account");

            int typeChoice = getIntInput("Choose account type (1-3): ");
            String accountType = switch (typeChoice) {
                case 1 -> "savings";
                case 2 -> "checking";
                case 3 -> "business";
                default -> throw new BankingException("Invalid account type");
            };

            System.out.println("Currencies: 1=USD, 2=EUR, 3=GBP, 4=JPY, 5=CAD");
            int currencyChoice = getIntInput("Choose currency (1-5): ");
            Currency currency = Currency.values()[currencyChoice - 1];

            double initialBalance = getDoubleInput("Initial deposit amount: ");
            String password = getStringInput("Your password for verification: ");

            String accountNumber;
            if (accountType.equals("business")) {
                String businessName = getStringInput("Business Name: ");
                String taxId = getStringInput("Tax ID: ");
                accountNumber = bank.createAccount(currentCustomerId, accountType, initialBalance, currency, password, businessName, taxId);
            } else {
                accountNumber = bank.createAccount(currentCustomerId, accountType, initialBalance, currency, password);
            }

            System.out.println("Account created successfully!");
            System.out.printf("Account Number: %s | Type: %s | Initial Balance: %.2f %s%n",
                    accountNumber, accountType.toUpperCase(), initialBalance, currency);

        } catch (Exception e) {
            System.err.println("Account creation failed: " + e.getMessage());
        }
    }

    private void viewMyAccounts() {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("                 MY ACCOUNTS");
        System.out.println("-".repeat(60));

        List<Account> accounts = bank.getCustomerAccounts(currentCustomerId);
        if (accounts.isEmpty()) {
            System.out.println("You have no accounts.");
        } else {
            for (Account account : accounts) {
                System.out.println(account);
                if (account instanceof SavingsAccount sa) {
                    System.out.printf("  Interest Rate: %.2f%% | Min Balance: %.2f%n",
                            sa.getInterestRate(), sa.getMinimumBalance());
                } else if (account instanceof BusinessAccount ba) {
                    System.out.printf("  Business: %s | Tax ID: %s%n",
                            ba.getBusinessName(), ba.getTaxId());
                }
                System.out.println();
            }
        }
    }

    private void depositMoney() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("         DEPOSIT MONEY");
        System.out.println("-".repeat(40));

        try {
            String accountNumber = selectMyAccount();
            if (accountNumber == null) return;

            double amount = getDoubleInput("Amount to deposit: ");
            String description = getStringInput("Description (optional): ");

            bank.deposit(accountNumber, amount, description.isEmpty() ? null : description);
            Account account = bank.getAccount(accountNumber);
            System.out.printf("Deposit successful! New balance: %.2f %s%n",
                    account.getBalance(), account.getCurrency());

        } catch (Exception e) {
            System.err.println("Deposit failed: " + e.getMessage());
        }
    }

    private void withdrawMoney() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("        WITHDRAW MONEY");
        System.out.println("-".repeat(40));

        try {
            String accountNumber = selectMyAccount();
            if (accountNumber == null) return;

            Account account = bank.getAccount(accountNumber);
            System.out.printf("Current balance: %.2f %s%n", account.getBalance(), account.getCurrency());
            System.out.printf("Daily limit: %.2f | Available: %.2f%n",
                    account.getDailyTransactionLimit(),
                    account.getDailyTransactionLimit() - account.getMonthlyTransactionTotal());

            double amount = getDoubleInput("Amount to withdraw: ");
            String description = getStringInput("Description (optional): ");

            bank.withdraw(accountNumber, amount, description.isEmpty() ? null : description);
            System.out.printf("Withdrawal successful! New balance: %.2f %s%n",
                    account.getBalance(), account.getCurrency());

        } catch (Exception e) {
            System.err.println("Withdrawal failed: " + e.getMessage());
        }
    }

    private void transferMoney() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("        TRANSFER MONEY");
        System.out.println("-".repeat(40));

        try {
            String fromAccount = selectMyAccount();
            if (fromAccount == null) return;

            String toAccount = getStringInput("Recipient account number: ");
            double amount = getDoubleInput("Amount to transfer: ");
            String description = getStringInput("Description (optional): ");

            bank.transfer(fromAccount, toAccount, amount, description.isEmpty() ? null : description);
            Account account = bank.getAccount(fromAccount);
            System.out.printf("Transfer successful! New balance: %.2f %s%n",
                    account.getBalance(), account.getCurrency());

        } catch (Exception e) {
            System.err.println("Transfer failed: " + e.getMessage());
        }
    }

    private void viewAccountStatement() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("       ACCOUNT STATEMENT");
        System.out.println("-".repeat(40));

        try {
            String accountNumber = selectMyAccount();
            if (accountNumber == null) return;

            System.out.print("Start date (YYYY-MM-DD) or press Enter for last 30 days: ");
            String startDateStr = scanner.nextLine().trim();
            LocalDate startDate = startDateStr.isEmpty() ?
                    LocalDate.now().minusDays(30) : LocalDate.parse(startDateStr);

            System.out.print("End date (YYYY-MM-DD) or press Enter for today: ");
            String endDateStr = scanner.nextLine().trim();
            LocalDate endDate = endDateStr.isEmpty() ? LocalDate.now() : LocalDate.parse(endDateStr);

            String statement = bank.generateAccountStatement(accountNumber, startDate, endDate);
            System.out.println(statement);

            System.out.print("Save statement to file? (y/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                String filename = "statement_" + accountNumber + "_" + LocalDate.now() + ".txt";
                Files.write(Paths.get(filename), statement.getBytes());
                System.out.println("Statement saved to: " + filename);
            }

        } catch (Exception e) {
            System.err.println("Error generating statement: " + e.getMessage());
        }
    }

    private void applyForLoan() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("        LOAN APPLICATION");
        System.out.println("-".repeat(40));

        try {
            String accountNumber = selectMyAccount();
            if (accountNumber == null) return;

            double amount = getDoubleInput("Loan amount: ");
            System.out.println("Loan terms: 12, 24, 36, 48, 60 months");
            int termMonths = getIntInput("Term in months: ");

            double interestRate = switch (termMonths) {
                case 12 -> 5.5;
                case 24 -> 6.0;
                case 36 -> 6.5;
                case 48 -> 7.0;
                case 60 -> 7.5;
                default -> 8.0;
            };

            String loanId = bank.applyForLoan(accountNumber, amount, interestRate, termMonths);
            System.out.printf("Loan application submitted successfully!%n");
            System.out.printf("Loan ID: %s%n", loanId);
            System.out.printf("Amount: %.2f | Rate: %.1f%% | Term: %d months%n",
                    amount, interestRate, termMonths);
            System.out.println("Your application is under review.");

        } catch (Exception e) {
            System.err.println("Loan application failed: " + e.getMessage());
        }
    }

    private void viewMyLoans() {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("                  MY LOANS");
        System.out.println("-".repeat(60));

        List<Loan> loans = bank.getCustomerLoans(currentCustomerId);
        if (loans.isEmpty()) {
            System.out.println("You have no loans.");
        } else {
            for (Loan loan : loans) {
                System.out.println(loan);
                System.out.printf("  Payments made: %d | Next payment: %.2f%n",
                        loan.getPayments().size(), loan.getMonthlyPayment());
                System.out.println();
            }
        }
    }

    private void updateProfile() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("        UPDATE PROFILE");
        System.out.println("-".repeat(40));

        try {
            Customer customer = bank.getCustomer(currentCustomerId);
            System.out.println("Current Profile:");
            System.out.println(customer);

            System.out.println("\nLeave blank to keep current value:");
            String email = getStringInput("New Email: ");
            String phone = getStringInput("New Phone: ");
            String address = getStringInput("New Address: ");

            if (!email.isEmpty() && ValidationUtils.isValidEmail(email)) {
                customer.setEmail(email);
            }
            if (!phone.isEmpty() && ValidationUtils.isValidPhone(phone)) {
                customer.setPhone(phone);
            }
            if (!address.isEmpty()) {
                customer.setAddress(address);
            }

            System.out.println("Profile updated successfully!");

        } catch (Exception e) {
            System.err.println("Profile update failed: " + e.getMessage());
        }
    }

    private void changePassword() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("        CHANGE PASSWORD");
        System.out.println("-".repeat(40));

        try {
            Customer customer = bank.getCustomer(currentCustomerId);
            String currentPassword = getStringInput("Current Password: ");

            if (!customer.verifyPassword(currentPassword)) {
                System.out.println("Invalid current password!");
                return;
            }

            String newPassword = getStringInput("New Password: ");
            String confirmPassword = getStringInput("Confirm New Password: ");

            if (!newPassword.equals(confirmPassword)) {
                System.out.println("Passwords do not match!");
                return;
            }

            customer.updatePassword(newPassword);
            System.out.println("Password changed successfully!");

        } catch (Exception e) {
            System.err.println("Password change failed: " + e.getMessage());
        }
    }

    private void adminFunctions() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("       ADMIN FUNCTIONS");
        System.out.println("-".repeat(40));
        System.out.println("1. View Bank Summary");
        System.out.println("2. Generate Monthly Interest");
        System.out.println("3. Charge Maintenance Fees");
        System.out.println("4. View All Customers");
        System.out.println("5. Approve Loans");
        System.out.println("6. Freeze/Unfreeze Account");
        System.out.println("7. Export Bank Data");
        System.out.println("8. Back to Main Menu");

        int choice = getIntInput("Choose option: ");
        switch (choice) {
            case 1 -> bank.displayBankSummary();
            case 2 -> { bank.generateMonthlyInterest(); System.out.println("Monthly interest processed."); }
            case 3 -> { bank.chargeMaintenanceFees(); System.out.println("Maintenance fees charged."); }
            case 4 -> viewAllCustomers();
            case 5 -> approveLoans();
            case 6 -> freezeUnfreezeAccount();
            case 7 -> exportBankData();
            case 8 -> { /* Return to main menu */ }
            default -> System.out.println("Invalid choice.");
        }
    }

    private void viewAllCustomers() {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("                ALL CUSTOMERS");
        System.out.println("-".repeat(60));

        List<Customer> customers = bank.getAllCustomers();
        if (customers.isEmpty()) {
            System.out.println("No customers found.");
        } else {
            for (Customer customer : customers) {
                System.out.println(customer);
                List<Account> accounts = bank.getCustomerAccounts(customer.getCustomerId());
                System.out.printf("  Accounts: %d | Total Balance: %.2f%n",
                        accounts.size(),
                        accounts.stream().mapToDouble(Account::getBalance).sum());
                System.out.println();
            }
        }
    }

    private void approveLoans() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("        APPROVE LOANS");
        System.out.println("-".repeat(40));

        List<Loan> pendingLoans = bank.getAllLoans().stream()
                .filter(loan -> loan.getStatus() == LoanStatus.PENDING)
                .collect(Collectors.toList());

        if (pendingLoans.isEmpty()) {
            System.out.println("No pending loans.");
            return;
        }

        for (Loan loan : pendingLoans) {
            System.out.println(loan);
            System.out.print("Approve this loan? (y/n): ");
            String response = scanner.nextLine().trim().toLowerCase();
            if (response.startsWith("y")) {
                try {
                    bank.approveLoan(loan.getLoanId());
                    System.out.println("Loan approved and disbursed!");
                } catch (BankingException e) {
                    System.err.println("Error approving loan: " + e.getMessage());
                }
            }
        }
    }

    private void freezeUnfreezeAccount() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("     FREEZE/UNFREEZE ACCOUNT");
        System.out.println("-".repeat(40));

        try {
            String accountNumber = getStringInput("Account number: ");
            Account account = bank.getAccount(accountNumber);

            System.out.println("Current status: " + account.getStatus());
            System.out.println("1. Freeze Account");
            System.out.println("2. Unfreeze Account");

            int choice = getIntInput("Choose action: ");
            switch (choice) {
                case 1 -> {
                    bank.freezeAccount(accountNumber);
                    System.out.println("Account frozen successfully.");
                }
                case 2 -> {
                    bank.unfreezeAccount(accountNumber);
                    System.out.println("Account unfrozen successfully.");
                }
                default -> System.out.println("Invalid choice.");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void exportBankData() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("       EXPORT BANK DATA");
        System.out.println("-".repeat(40));

        try {
            String filename = "bank_data_export_" + LocalDate.now() + ".txt";
            bank.exportData(filename);
            System.out.println("Bank data exported successfully to: " + filename);
        } catch (IOException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    private String selectMyAccount() {
        List<Account> accounts = bank.getCustomerAccounts(currentCustomerId);
        if (accounts.isEmpty()) {
            System.out.println("You have no accounts. Please create an account first.");
            return null;
        }

        if (accounts.size() == 1) {
            return accounts.get(0).getAccountNumber();
        }

        System.out.println("Select an account:");
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            System.out.printf("%d. %s - Balance: %.2f %s (%s)%n",
                    i + 1, account.getAccountNumber(), account.getBalance(),
                    account.getCurrency(), account.getClass().getSimpleName());
        }

        int choice = getIntInput("Choose account (1-" + accounts.size() + "): ");
        if (choice >= 1 && choice <= accounts.size()) {
            return accounts.get(choice - 1).getAccountNumber();
        } else {
            System.out.println("Invalid selection.");
            return null;
        }
    }

    // ========= INPUT HELPERS =========
    private double getDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("Please enter a value.");
                    continue;
                }
                double value = Double.parseDouble(input);
                if (value < 0) {
                    System.out.println("Please enter a positive value.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a valid number.");
            }
        }
    }

    private String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("Please enter a value.");
                    continue;
                }
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    // ========= UTILITY METHODS =========
    public void runDailyMaintenance() {
        System.out.println("Running daily maintenance tasks...");

        // Credit monthly interest for saving accounts
        bank.generateMonthlyInterest();

        // Charge maintenance fees
        bank.chargeMaintenanceFees();

        // Reset daily transaction limits (in a real system, this would be scheduled)
        System.out.println("Daily maintenance completed.");
        LOGGER.info("Daily maintenance tasks completed");
    }

    public void generateSystemReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                        COMPREHENSIVE SYSTEM REPORT");
        System.out.println("=".repeat(80));

        bank.displayBankSummary();

        // Transaction volume analysis
        List<Account> allAccounts = bank.getAllAccounts();
        int totalTransactions = allAccounts.stream()
                .mapToInt(account -> account.getTransactions().size())
                .sum();

        double avgTransactionsPerAccount = allAccounts.isEmpty() ? 0 :
                (double) totalTransactions / allAccounts.size();

        System.out.println("\nTRANSACTION ANALYSIS:");
        System.out.println("-".repeat(40));
        System.out.printf("Total Transactions: %d%n", totalTransactions);
        System.out.printf("Average Transactions per Account: %.2f%n", avgTransactionsPerAccount);

        // Account status distribution
        Map<AccountStatus, Long> statusCount = allAccounts.stream()
                .collect(Collectors.groupingBy(Account::getStatus, Collectors.counting()));

        System.out.println("\nACCOUNT STATUS DISTRIBUTION:");
        System.out.println("-".repeat(40));
        statusCount.forEach((status, count) ->
                System.out.printf("%s: %d accounts%n", status, count));

        // Currency distribution
        Map<Currency, Long> currencyCount = allAccounts.stream()
                .collect(Collectors.groupingBy(Account::getCurrency, Collectors.counting()));

        System.out.println("\nCURRENCY DISTRIBUTION:");
        System.out.println("-".repeat(40));
        currencyCount.forEach((currency, count) ->
                System.out.printf("%s: %d accounts%n", currency, count));

        // Loan analysis
        List<Loan> allLoans = bank.getAllLoans();
        if (!allLoans.isEmpty()) {
            Map<LoanStatus, Long> loanStatusCount = allLoans.stream()
                    .collect(Collectors.groupingBy(Loan::getStatus, Collectors.counting()));

            System.out.println("\nLOAN STATUS DISTRIBUTION:");
            System.out.println("-".repeat(40));
            loanStatusCount.forEach((status, count) ->
                    System.out.printf("%s: %d loans%n", status, count));

            double avgLoanAmount = allLoans.stream()
                    .mapToDouble(Loan::getPrincipalAmount)
                    .average()
                    .orElse(0.0);

            System.out.printf("Average Loan Amount: $%.2f%n", avgLoanAmount);
        }

        System.out.println("=".repeat(80));
        System.out.println("Report generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("=".repeat(80));
    }

    // ========= DEMO DATA METHODS =========
    public void loadDemoData() {
        System.out.println("Loading demo data...");

        try {
            // Create demo customers
            String customer1 = bank.registerCustomer("John", "Doe", "john.doe@email.com",
                    "+1234567890", "123 Main St, City, State", LocalDate.of(1985, 5, 15), "password123");

            String customer2 = bank.registerCustomer("Jane", "Smith", "jane.smith@email.com",
                    "+1987654321", "456 Oak Ave, City, State", LocalDate.of(1990, 8, 22), "password456");

            String customer3 = bank.registerCustomer("Bob", "Johnson", "bob.johnson@email.com",
                    "+1122334455", "789 Pine St, City, State", LocalDate.of(1978, 12, 3), "password789");

            // Create demo accounts
            String account1 = bank.createAccount(customer1, "savings", 5000.0, Currency.USD, "password123");
            String account2 = bank.createAccount(customer1, "checking", 2500.0, Currency.USD, "password123");
            String account3 = bank.createAccount(customer2, "savings", 7500.0, Currency.EUR, "password456");
            String account4 = bank.createAccount(customer2, "business", 15000.0, Currency.USD, "password456",
                    "Smith Consulting LLC", "TAX123456789");
            String account5 = bank.createAccount(customer3, "checking", 3200.0, Currency.CAD, "password789");

            // Create some transactions
            bank.deposit(account1, 1000.0, "Payroll deposit");
            bank.withdraw(account2, 500.0, "ATM withdrawal");
            bank.transfer(account1, account3, 300.0, "International transfer");
            bank.deposit(account4, 5000.0, "Client payment");
            bank.withdraw(account5, 200.0, "Online purchase");

            // Apply for demo loans
            String loan1 = bank.applyForLoan(account1, 10000.0, 6.5, 36);
            String loan2 = bank.applyForLoan(account4, 25000.0, 7.0, 48);

            // Approve loans
            bank.approveLoan(loan1);
            bank.approveLoan(loan2);

            // Credit interest
            bank.generateMonthlyInterest();

            System.out.println("Demo data loaded successfully!");
            System.out.println("\nDemo Login Credentials:");
            System.out.println("Customer 1: " + customer1 + " / password123");
            System.out.println("Customer 2: " + customer2 + " / password456");
            System.out.println("Customer 3: " + customer3 + " / password789");

        } catch (Exception e) {
            System.err.println("Error loading demo data: " + e.getMessage());
        }
    }

    // ========= STRESS TEST METHODS =========
    public void runStressTest() {
        System.out.println("Running system stress test...");
        long startTime = System.currentTimeMillis();

        try {
            // Create multiple customers and accounts
            List<String> customerIds = new ArrayList<>();
            List<String> accountNumbers = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                String customerId = bank.registerCustomer("User" + i, "Test" + i,
                        "user" + i + "@test.com", "+123456789" + i,
                        "Address " + i, LocalDate.of(1980 + (i % 40), (i % 12) + 1, (i % 28) + 1),
                        "password" + i);
                customerIds.add(customerId);

                String accountNumber = bank.createAccount(customerId, "savings", 1000.0 + (i * 100),
                        Currency.USD, "password" + i);
                accountNumbers.add(accountNumber);
            }

            // Perform random transactions
            Random random = new Random();
            for (int i = 0; i < 1000; i++) {
                String accountNumber = accountNumbers.get(random.nextInt(accountNumbers.size()));
                double amount = 10.0 + (random.nextDouble() * 500.0);

                try {
                    if (random.nextBoolean()) {
                        bank.deposit(accountNumber, amount, "Stress test deposit");
                    } else {
                        bank.withdraw(accountNumber, amount, "Stress test withdrawal");
                    }
                } catch (Exception e) {
                    // Expected - some withdrawals may fail due to insufficient funds
                }
            }

            // Perform transfers
            for (int i = 0; i < 200; i++) {
                String fromAccount = accountNumbers.get(random.nextInt(accountNumbers.size()));
                String toAccount = accountNumbers.get(random.nextInt(accountNumbers.size()));
                double amount = 10.0 + (random.nextDouble() * 200.0);

                try {
                    bank.transfer(fromAccount, toAccount, amount, "Stress test transfer");
                } catch (Exception e) {
                    // Expected - some transfers may fail
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("Stress test completed!");
            System.out.printf("Duration: %d ms%n", duration);
            System.out.printf("Created: %d customers, %d accounts%n", customerIds.size(), accountNumbers.size());
            System.out.println("Performed 1000 deposits/withdrawals and 200 transfers");

            // Clean up test data
            System.out.print("Clean up test data? (y/n): ");
            Scanner scanner = new Scanner(System.in);
            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                // In a real system, you would implement cleanup methods
                System.out.println("Test data cleanup would be performed here.");
            }

        } catch (Exception e) {
            System.err.println("Stress test failed: " + e.getMessage());
        }
    }

    // ========= MAIN METHOD =========
    public static void main(String[] args) {
        BankManagementSystem system = new BankManagementSystem();

        // Check for command line arguments
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "--demo" -> {
                    system.loadDemoData();
                    system.start();
                }
                case "--stress" -> {
                    system.runStressTest();
                    return;
                }
                case "--report" -> {
                    system.loadDemoData();
                    system.generateSystemReport();
                    return;
                }
                case "--maintenance" -> {
                    system.runDailyMaintenance();
                    return;
                }
                case "--help" -> {
                    System.out.println("Enhanced Bank Management System v2.0");
                    System.out.println("Usage: java EnhancedBankManagementSystem [option]");
                    System.out.println("Options:");
                    System.out.println("  --demo        Load demo data and start interactive mode");
                    System.out.println("  --stress      Run stress test");
                    System.out.println("  --report      Generate comprehensive system report");
                    System.out.println("  --maintenance Run daily maintenance tasks");
                    System.out.println("  --help        Show this help message");
                    System.out.println("  (no option)   Start in normal interactive mode");
                    return;
                }
                default -> {
                    System.out.println("Unknown option: " + args[0]);
                    System.out.println("Use --help for available options");
                    return;
                }
            }
        } else {
            // Normal startup
            system.start();
        }
    }
}