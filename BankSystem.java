// Java CLI Bank Management System (Improved with Encapsulation, Exception Handling, Go Back options)

import java.io.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

abstract class Account implements Serializable {
    private String name, username, password, accountType;
    private int accountId;
    private double balance;
    protected LocalDate lastInterestDate;
    private List<String> transactionHistory;

    public Account(String name, String username, String password, int id, String type, double balance) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.accountId = id;
        this.accountType = type;
        this.balance = balance;
        this.lastInterestDate = LocalDate.now();
        this.transactionHistory = new ArrayList<>();
        this.transactionHistory.add("Account opened with balance: Rs." + balance);
    }

    abstract void calculateInterest();

    public void deposit(double amt) {
        if (amt > 0) {
            balance += amt;
            transactionHistory.add("Deposited Rs." + amt);
        }
    }

    public boolean withdraw(double amt) {
        if (amt > 0 && balance >= amt) {
            balance -= amt;
            transactionHistory.add("Withdrawn Rs." + amt);
            return true;
        }
        return false;
    }

    public boolean checkPassword(String input) {
        return password.equals(input);
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public double getBalance() {
        return balance;
    }

    public void saveTransaction(String message) {
        transactionHistory.add(message);
    }

    public String getDetails() {
        return "Name: " + name + ", Username: " + username + ", ID: " + accountId + ", Type: " + accountType +", Balance: Rs." + String.format("%.2f", balance);
    }

    public void printTransactions() {
        System.out.println("--- Transaction History ---");
        for (String t : transactionHistory) {
            System.out.println(t);
        }
    }
}

class SavingsAccount extends Account {
    static final double INTEREST_RATE = 9.5;

    public SavingsAccount(String name, String username, String password, int id, double balance) {
        super(name, username, password, id, "Savings", balance);
    }

    @Override
    void calculateInterest() {
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(lastInterestDate, today);
        if (days <= 0) return;
        double interest = (getBalance() * INTEREST_RATE * days) / (100 * 365);
        deposit(interest);
        saveTransaction("Interest of Rs." + String.format("%.2f", interest) + " added on " + today);
        System.out.printf("Interest of Rs.%.2f added.\n", interest);
        lastInterestDate = today;
    }
}

class CurrentAccount extends Account {
    public CurrentAccount(String name, String username, String password, int id, double balance) {
        super(name, username, password, id, "Current", balance);
    }

    @Override
    void calculateInterest() {}
}

class AccountFactory {
    public static Account createAccount(String name, String username, String password, int id, int type, double balance) {
        return (type == 1)
                ? new SavingsAccount(name, username, password, id, balance)
                : new CurrentAccount(name, username, password, id, balance);
    }
}

class FileManager {
    private static final String DB_PATH = "bank_users";
    private static FileManager instance;

    private FileManager() {
        File dir = new File(DB_PATH);
        if (!dir.exists()) dir.mkdir();
    }

    public static FileManager getInstance() {
        if (instance == null) {
            instance = new FileManager();
        }
        return instance;
    }

    public void save(Account acc) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DB_PATH + "/" + acc.getUsername() + ".dat"))) {
            out.writeObject(acc);
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    public Account load(String username) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DB_PATH + "/" + username + ".dat"))) {
            return (Account) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public void delete(String username) {
        File file = new File(DB_PATH + "/" + username + ".dat");
        if (file.exists()) file.delete();
    }

    public List<Account> loadAll() {
        List<Account> accounts = new ArrayList<>();
        File folder = new File(DB_PATH);
        if (!folder.exists()) folder.mkdir();
        for (File file : folder.listFiles()) {
            Account acc = load(file.getName().replace(".dat", ""));
            if (acc != null) accounts.add(acc);
        }
        return accounts;
    }
}

public class BankSystem {
    static Scanner sc = new Scanner(System.in);
    static int nextId = loadNextId();
    static FileManager fileManager = FileManager.getInstance();

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n==============================\nWelcome to Bank CLI System\n==============================");
            System.out.println("1. Login as User\n2. Login as Admin\n3. Exit");
            System.out.print("Enter your choice (1/2/3): ");
            String input = sc.nextLine();

            switch (input) {
                case "1" -> userLogin();
                case "2" -> adminLogin();
                case "3" -> System.exit(0);
                default -> System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
    }

    static void userLogin() {
        System.out.print("Enter Username: ");
        String uname = sc.nextLine();
        System.out.print("Enter Password: ");
        String pass = sc.nextLine();
        Account user = fileManager.load(uname);
        if (user != null && user.checkPassword(pass)) {
            System.out.println("Login successful! Welcome, " + user.getName());
            userDashboard(user);
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    static void userDashboard(Account user) {
        while (true) {
            System.out.println("\n========= User Dashboard =========");
            System.out.println("1. Deposit\n2. Withdraw\n3. Check Balance\n4. View Interest\n5. View Transaction History\n6. Exit");
            System.out.print("Enter your choice (1-6): ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> {
                    System.out.print("Amount to deposit: ");
                    try {
                        double amt = Double.parseDouble(sc.nextLine());
                        user.deposit(amt);
                        fileManager.save(user);
                        System.out.printf("Deposited. New balance: Rs.%.2f\n", user.getBalance());
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a numeric value.");
                    }
                }
                case "2" -> {
                    try {
                        System.out.print("Amount to withdraw: ");
                        double amt = Double.parseDouble(sc.nextLine());
                        if (amt <= 0) throw new IllegalArgumentException("Withdrawal must be > 0");
                        if (!user.withdraw(amt)) throw new IllegalArgumentException("Insufficient funds.");
                        fileManager.save(user);
                        System.out.printf("Withdrawn. New balance: Rs.%.2f\n", user.getBalance());
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "3" -> System.out.println(user.getDetails());
                case "4" -> {
                    user.calculateInterest();
                    fileManager.save(user);
                }
                case "5" -> user.printTransactions();
                case "6" -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid option. Please choose 1-6.");
            }
        }
    }

    static void adminLogin() {
        System.out.print("Enter Admin Username: ");
        String uname = sc.nextLine();
        System.out.print("Enter Password: ");
        String pass = sc.nextLine();
        if (uname.equals("admin") && pass.equals("admin123")) {
            adminDashboard();
        } else {
            System.out.println("Invalid admin credentials.");
        }
    }

    static void adminDashboard() {
        while (true) {
            System.out.println("\n========= Admin Dashboard =========");
            System.out.println("1. Create Account\n2. View All Accounts\n3. Delete Account\n4. Exit");
            System.out.print("Enter your choice (1-4): ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> createAccount();
                case "2" -> fileManager.loadAll().forEach(acc -> System.out.println(acc.getDetails()));
                case "3" -> {
                    System.out.print("Enter username to delete (or type 'back' to cancel): ");
                    String uname = sc.nextLine();
                    if (uname.equalsIgnoreCase("back")) {
                        System.out.println("Cancelled deletion.");
                        continue;
                    }
                    Account acc = fileManager.load(uname);
                    if (acc != null) {
                        fileManager.delete(uname);
                        System.out.println("User deleted.");
                    } else {
                        System.out.println("User not found.");
                    }
                }
                case "4" -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid option. Please choose 1-4.");
            }
        }
    }

    static void createAccount() {
        try {
            System.out.print("Full Name: ");
            String name = sc.nextLine();
            System.out.print("Username: ");
            String uname = sc.nextLine();
            System.out.print("Password: ");
            String pass = sc.nextLine();
            System.out.print("Account Type (1. Savings / 2. Current): ");
            int type = Integer.parseInt(sc.nextLine());
            System.out.print("Initial Deposit: ");
            double amt = Double.parseDouble(sc.nextLine());

            Account newAcc = AccountFactory.createAccount(name, uname, pass, nextId, type, amt);
            fileManager.save(newAcc);
            nextId++;
            saveNextId(nextId);
            System.out.println("Account created successfully!");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    private static int loadNextId() {
        try (BufferedReader br = new BufferedReader(new FileReader("nextId.txt"))) {
            return Integer.parseInt(br.readLine());
        } catch (Exception e) {
            return 1001; // default starting ID
        }
    }

    private static void saveNextId(int id) {
        try (PrintWriter out = new PrintWriter("nextId.txt")) {
            out.println(id);
        } catch (IOException e) {
            System.out.println("Error saving next ID: " + e.getMessage());
        }
    }
}
