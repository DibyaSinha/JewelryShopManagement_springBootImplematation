package org.example;

import org.example.exception.AuthenticationException;
import org.example.model.*;
import org.example.repository.impl.DailyRateRepositoryImpl;
import org.example.repository.interfaces.DailyRateRepository;
import org.example.service.AuthService;
import org.example.service.BillHistoryService;
import org.example.service.BillingService;
import org.example.service.JewelryService;
import org.example.service.CustomerService;
import org.example.util.InvoiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    static Scanner sc = new Scanner(System.in);
    static User currentUser;

    static AuthService authService = new AuthService();
    static JewelryService jewelryService = new JewelryService();
    static BillingService billingService = new BillingService();
    static BillHistoryService billHistoryService = new BillHistoryService();
    static DailyRateRepository rateRepo = new DailyRateRepositoryImpl();
    static CustomerService customerService = new CustomerService();

    public static void main(String[] args) {
        System.out.println("===== JEWELRY SHOP MANAGEMENT SYSTEM =====");
        System.out.println("         ===== CHANDI FASHION =====");

        while (true) {
            System.out.println("\n1. Login");
            System.out.println("2. Exit");

            try {
                int choice = Integer.parseInt(sc.nextLine());
                if (choice == 1) {
                    login();
                } else {
                    System.out.println("Exiting system. Goodbye!");
                    System.exit(0);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    static void login() {
        try {
            System.out.print("Username: ");
            String username = sc.nextLine();

            System.out.print("Password: ");
            String password = sc.nextLine();

            currentUser = authService.login(username, password);
            System.out.println("✔ Login successful");

            if (currentUser.getRole().equalsIgnoreCase("ADMIN")) {
                adminMenu();
            } else {
                staffMenu();
            }

        } catch (AuthenticationException e) {
            System.out.println("❌ " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during login", e);
            System.out.println("❌ An error occurred: " + e.getMessage());
        }
    }

    static void adminMenu() {
        while (true) {
            displayTodayRates();
            System.out.println("\n===== ADMIN PANEL =====");
            System.out.println("1. Add Staff");
            System.out.println("2. Add Jewelry Design");
            System.out.println("3. View Jewelry");
            System.out.println("4. Add Stock");
            System.out.println("5. Update Today's Metal Rate");
            System.out.println("6. View Sales");
            System.out.println("7. View Bills");
            System.out.println("8. Manage Customers");
            System.out.println("9. Logout");

            try {
                int ch = Integer.parseInt(sc.nextLine());
                switch (ch) {
                    case 1 -> addStaff();
                    case 2 -> addJewelry();
                    case 3 -> viewJewelry();
                    case 4 -> addStock();
                    case 5 -> updateRate();
                    case 6 -> viewSales();
                    case 7 -> viewBills();
                    case 8 -> manageCustomers();
                    case 9 -> { currentUser = null; return; }
                    default -> System.out.println("Invalid option");
                }
            } catch (Exception e) {
                System.out.println("❌ " + e.getMessage());
            }
        }
    }

    static void manageCustomers() {
        System.out.println("\n===== MANAGE CUSTOMERS =====");
        System.out.print("Enter Mobile Number: ");
        String mobile = sc.nextLine();
        System.out.print("Enter Name: ");
        String name = sc.nextLine();
        System.out.print("Enter Fixed Discount Percentage (e.g. 10.5): ");
        double discount = Double.parseDouble(sc.nextLine());

        customerService.addOrUpdateCustomer(mobile, name, discount);
        System.out.println("✔ Customer saved successfully.");
    }

    static void viewSales() {
        System.out.println("\n===== SALES OPTIONS =====");
        System.out.println("1. View Today's Sale");
        System.out.println("2. View Monthly Sale");
        System.out.println("3. View Total Sale Till Now");
        System.out.print("Select an option: ");

        try {
            int subCh = Integer.parseInt(sc.nextLine());
            switch (subCh) {
                case 1 -> {
                    double todayTotal = billingService.getTodayTotalSale();
                    java.util.Map<String, Double> breakdown = billingService.getTodaySalesBySeller();
                    System.out.println("\n===== TODAY'S SALES REPORT =====");
                    System.out.println("Date: " + java.time.LocalDate.now());
                    System.out.println("Overall Total: ₹" + String.format("%.2f", todayTotal));
                    System.out.println("\n--- Sales by Seller ---");
                    if (breakdown.isEmpty()) {
                        System.out.println("No sales recorded today.");
                    } else {
                        breakdown.forEach((seller, total) ->
                            System.out.println(String.format("%-15s : ₹%.2f", seller, total))
                        );
                    }
                    System.out.println("=================================");
                }
                case 2 -> {
                    double monthlyTotal = billingService.getMonthlyTotalSale();
                    System.out.println("\n===== MONTHLY SALES REPORT =====");
                    System.out.println("Month: " + java.time.LocalDate.now().getMonth() + " " + java.time.LocalDate.now().getYear());
                    System.out.println("Total Monthly Revenue: ₹" + String.format("%.2f", monthlyTotal));
                    System.out.println("================================");
                }
                case 3 -> {
                    double grandTotal = billingService.getGrandTotalSale();
                    System.out.println("\n===== ALL-TIME SALES REPORT =====");
                    System.out.println("Total Sale till now: ₹" + String.format("%.2f", grandTotal));
                    System.out.println("=================================");
                }
                default -> System.out.println("Invalid option");
            }
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    static void viewBills() {
        System.out.println("\n===== BILL MANAGEMENT =====");
        System.out.println("1. View All Bills");
        System.out.println("2. Search Bill by ID");
        System.out.print("Select an option: ");

        try {
            int subCh = Integer.parseInt(sc.nextLine());
            if (subCh == 1) {
                viewAllBills();
            } else if (subCh == 2) {
                searchBillById();
            } else {
                System.out.println("Invalid option");
            }
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    static void viewAllBills() {
        List<Bill> bills = billHistoryService.getAllBills();
        if (bills.isEmpty()) {
            System.out.println("No bills found in history.");
            return;
        }

        System.out.println("\n===== BILL HISTORY =====");
        System.out.println(String.format("%-8s | %-16s | %-10s", "Bill ID", "Date", "Total"));
        System.out.println("----------------------------------------");
        for (Bill b : bills) {
            System.out.println(String.format("%-8d | %-16s | ₹%.2f",
                b.getBillId(),
                b.getBillDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yy HH:mm")),
                b.getGrandTotal()));
        }
    }

    static void searchBillById() {
        System.out.print("Enter Bill ID: ");
        try {
            long id = Long.parseLong(sc.nextLine());
            Bill bill = billHistoryService.getBillById(id);
            if (bill == null) {
                System.out.println("❌ Bill not found with ID: " + id);
                return;
            }

            System.out.println("\n===== BILL DETAILS (ID: " + bill.getBillId() + ") =====");
            System.out.println("Date   : " + bill.getBillDate());
            System.out.println("Seller : User ID " + bill.getSellerId());
            if (bill.getCustomerMobile() != null && !bill.getCustomerMobile().isEmpty()) {
                System.out.println("Customer Mobile: " + bill.getCustomerMobile());
            }
            System.out.println("\nItems:");
            System.out.println(String.format("%-15s | %-5s | %-10s", "Item", "Qty", "Amount"));
            for (BillItem item : bill.getItems()) {
                System.out.println(String.format("%-15s | %-5d | ₹%.2f",
                    item.getJewelry().getName(), item.getQuantity(), item.totalBeforeGST()));
            }
            System.out.println("----------------------------------------");
            System.out.println("Subtotal   : ₹" + String.format("%.2f", bill.getTotalAmount()));
            if (bill.getDiscountAmount() > 0) {
                System.out.println("Discount   :-₹" + String.format("%.2f", bill.getDiscountAmount()));
            }
            System.out.println("Grand Total: ₹" + String.format("%.2f", bill.getGrandTotal()));
            System.out.println("=========================================");

        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid ID format.");
        }
    }

    static void staffMenu() {
        while (true) {
            displayTodayRates();
            System.out.println("\n===== STAFF PANEL =====");
            System.out.println("1. Create Bill");
            System.out.println("2. Logout");

            try {
                int ch = Integer.parseInt(sc.nextLine());
                switch (ch) {
                    case 1 -> createBill();
                    case 2 -> { currentUser = null; return; }
                    default -> System.out.println("Invalid option");
                }
            } catch (Exception e) {
                System.out.println("❌ " + e.getMessage());
            }
        }
    }

    static void displayTodayRates() {
        java.util.Map<String, String> rates = billingService.getTodayRates();
        System.out.println("\n[ TODAY'S METAL RATES ]");
        System.out.println("GOLD  : " + rates.get("GOLD"));
        System.out.println("SILVER: " + rates.get("SILVER"));
    }

    static void addStaff() {
        System.out.print("Staff username: ");
        String username = sc.nextLine();
        System.out.print("Staff password: ");
        String password = sc.nextLine();
        authService.register(username, password, "STAFF");
        System.out.println("✔ Staff account created");
    }

    static void addJewelry() {
        System.out.print("Name: ");
        String name = sc.nextLine();
        System.out.print("Company Name: ");
        String company = sc.nextLine();
        System.out.print("Type (GOLD/SILVER/DIAMOND): ");
        String type = sc.nextLine().toUpperCase();
        System.out.print("Weight (grams): ");
        double weight = Double.parseDouble(sc.nextLine());
        System.out.print("Stock quantity: ");
        int stock = Integer.parseInt(sc.nextLine());
        System.out.print("Making charge %: ");
        double making = Double.parseDouble(sc.nextLine());

        jewelryService.addJewelry(name, company, type, weight, stock, making);
        System.out.println("✔ Jewelry design added");
    }

    static void viewJewelry() {
        List<Jewelry> list = jewelryService.getAll();
        System.out.println("\nID | NAME | COMPANY | TYPE | WEIGHT | STOCK");
        for (Jewelry j : list) {
            System.out.println(j.getId() + " | " + j.getName() + " | " + j.getCompanyName() + " | " + j.getType() + " | " + j.getWeight() + "g | " + j.getStock());
        }
    }

    static void addStock() {
        viewJewelry();
        System.out.print("Jewelry ID: ");
        long id = Long.parseLong(sc.nextLine());
        System.out.print("Quantity to add: ");
        int qty = Integer.parseInt(sc.nextLine());
        jewelryService.addStock(id, qty);
        System.out.println("✔ Stock updated");
    }

    static void updateRate() {
        System.out.print("Metal type (GOLD/SILVER): ");
        String type = sc.nextLine().toUpperCase();
        System.out.print("Today's price per gram: ");
        double rate = Double.parseDouble(sc.nextLine());
        rateRepo.save(new DailyRate(type, rate));
        System.out.println("✔ Today's rate updated");
    }

    static void createBill() {
        System.out.print("\nEnter Customer Mobile (Press Enter to skip): ");
        String mobile = sc.nextLine();
        
        if (!mobile.trim().isEmpty()) {
            Customer c = customerService.getCustomerByMobile(mobile);
            if (c != null) {
                System.out.println("Found Customer: " + c.getName() + " | Discount: " + c.getDiscountPercent() + "%");
            } else {
                System.out.println("Customer not found. No discount will be applied.");
            }
        }

        List<BillItem> items = new ArrayList<>();
        while (true) {
            viewJewelry();
            System.out.print("\nJewelry ID: ");
            long id = Long.parseLong(sc.nextLine());
            Jewelry j = jewelryService.getById(id);

            System.out.print("Quantity: ");
            int qty = Integer.parseInt(sc.nextLine());

            double todayRate = billingService.getTodayRate(j.getType());
            items.add(new BillItem(j, qty, todayRate));

            System.out.print("Add more items? (y/n): ");
            String more = sc.nextLine();
            if (!more.equalsIgnoreCase("y")) break;
        }

        Bill bill = billingService.generateBill(items, currentUser.getId(), mobile.trim());
        System.out.println("\n===== BILL GENERATED =====");
        System.out.println("Bill ID       : " + bill.getBillId());
        System.out.println("Total Amount  : ₹" + String.format("%.2f", bill.getGrandTotal()));

        System.out.println("✔ Invoice generated and saved to database.");
    }
}
