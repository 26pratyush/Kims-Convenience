package dbms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;

public class SupermarketBillingSystem extends JFrame {
    private Connection conn;
    private JTextArea outputTextArea;
    private JTextArea billTextArea;

    public SupermarketBillingSystem() {
        super("Supermarket Billing System");
        initialize();
        connectToDatabase();
    }

    private void initialize() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.decode("#f0f0f0"));

        outputTextArea = new JTextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setFont(new Font("Arial", Font.PLAIN, 16));
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        outputTextArea.setForeground(Color.BLACK);

        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        scrollPane.setPreferredSize(new Dimension(800, 400));

        billTextArea = new JTextArea();
        billTextArea.setEditable(false);
        billTextArea.setFont(new Font("Courier New", Font.PLAIN, 14));
        billTextArea.setForeground(Color.BLACK);
        billTextArea.setBackground(Color.decode("#f7f7f7"));
        JScrollPane billScroll = new JScrollPane(billTextArea);
        billScroll.setPreferredSize(new Dimension(800, 250));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(billScroll, BorderLayout.SOUTH);

        add(panel);

        showStartWindow();
    }

    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/dbms",
                    "root", "percy560077"); 
        } catch (Exception e) {
            outputTextArea.append("Error connecting to database: " + e.getMessage() + "\n");
        }
    }

    private void showStartWindow() {
        outputTextArea.setText("\t\tWelcome to Kim's Convenience\n");
        Timer timer = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPasscodeDialog();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void showPasscodeDialog() {
        String passcode = JOptionPane.showInputDialog(this, "Enter your 4-digit passcode:");
        if (passcode == null || passcode.length() != 4 || !passcode.matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "Invalid passcode. Please enter a 4-digit number.");
            showPasscodeDialog();
            return;
        }

        try (PreparedStatement stmt = conn
                .prepareStatement("SELECT CashierName FROM CashierInfo WHERE CashierID = ?")) {
            stmt.setInt(1, Integer.parseInt(passcode));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                outputTextArea.append("Welcome, " + rs.getString("CashierName") + "!\n");
                startBilling();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid passcode. Please try again.");
                showPasscodeDialog();
            }
        } catch (SQLException e) {
            outputTextArea.append("Error validating passcode: " + e.getMessage() + "\n");
        }
    }

    private void startBilling() {
        ArrayList<String> receipt = new ArrayList<>();
        double gross = 0.0;

        try {
            PreparedStatement inventoryStmt = conn.prepareStatement("SELECT * FROM Inventory WHERE ItemID = ?");
            PreparedStatement discountStmt = conn.prepareStatement("SELECT * FROM Discount WHERE ItemID = ?");
            PreparedStatement updateInventoryStmt = conn
                    .prepareStatement("UPDATE Inventory SET Stock = ? WHERE ItemID = ?");
            PreparedStatement customerStmt = conn
                    .prepareStatement("SELECT * FROM CustomerInfo WHERE CustomerNumber = ?");
            PreparedStatement updateCustomerStmt = conn
                    .prepareStatement("UPDATE CustomerInfo SET RewardPoints = ? WHERE CustomerNumber = ?");

            String customerNumber = JOptionPane.showInputDialog("Enter Customer Number:");
            int rewardPoints = 0;
            String customerName = "";
            if (customerNumber != null && !customerNumber.isEmpty()) {
                customerStmt.setString(1, customerNumber);
                ResultSet rs = customerStmt.executeQuery();
                if (rs.next()) {
                    rewardPoints = rs.getInt("RewardPoints");
                    customerName = rs.getString("CustomerName");
                    outputTextArea.append("Customer: " + customerName + " | Reward Points: " + rewardPoints + "\n");
                } else {
                    JOptionPane.showMessageDialog(this, "Customer not found. Starting billing without reward points.");
                }
            }

            while (true) {
                String itemId = JOptionPane.showInputDialog("Enter Item ID (or 'done' to finish):");
                if (itemId == null || itemId.equalsIgnoreCase("done"))
                    break;

                inventoryStmt.setString(1, itemId);
                ResultSet inventoryRs = inventoryStmt.executeQuery();
                if (!inventoryRs.next()) {
                    outputTextArea.append("Item not found. Please try again.\n");
                    continue;
                }

                String itemName = inventoryRs.getString("ItemName");
                int stock = inventoryRs.getInt("Stock");

                discountStmt.setString(1, itemId);
                ResultSet discountRs = discountStmt.executeQuery();
                double price = 0.0;
                double discount = 0.0;
                if (discountRs.next()) {
                    price = discountRs.getDouble("Price");
                    discount = discountRs.getDouble("Discount");
                }

                outputTextArea.append(String.format("Item: %s | Price: %.2f | Stock: %d\n", itemName, price, stock));

                String quantityStr = JOptionPane.showInputDialog("Enter quantity:");
                if (quantityStr == null || !quantityStr.matches("\\d+"))
                    continue;
                int quantity = Integer.parseInt(quantityStr);

                if (quantity > stock) {
                    outputTextArea.append("Insufficient stock for " + itemName + ". Available: " + stock + "\n");
                    continue;
                }

                double itemTotal = quantity * price * (1 - discount / 100);
                gross += itemTotal;
                stock -= quantity;

                updateInventoryStmt.setInt(1, stock);
                updateInventoryStmt.setString(2, itemId);
                updateInventoryStmt.executeUpdate();

                receipt.add(String.format("%s\t%s\t%.2f\t%d\t%.2f", itemId, itemName, price, quantity, itemTotal));
                outputTextArea.append(String.format("Scanned: %s | Quantity: %d | Remaining Stock: %d | Price: %.2f\n",
                        itemName, quantity, stock, itemTotal));
            }

            gross = applyRewardPoints(gross, rewardPoints);
            if (customerNumber != null && !customerNumber.isEmpty()) {
                updateCustomerStmt.setInt(1, (int) (gross / 100)); // E.g., 1 point for every Rs. 100 spent
                updateCustomerStmt.setString(2, customerNumber);
                updateCustomerStmt.executeUpdate();
            }

            displayReceipt(receipt, customerName, customerNumber);
            displayTotal(gross);

        } catch (SQLException e) {
            outputTextArea.append("Error during billing: " + e.getMessage() + "\n");
        }
    }

    private double applyRewardPoints(double gross, int rewardPoints) {
        int pointsToRedeem = Math.min(rewardPoints, (int) gross);
        gross -= pointsToRedeem;
        outputTextArea.append("Reward Points Redeemed: " + pointsToRedeem + "\n");
        return gross;
    }

    private void displayReceipt(ArrayList<String> receipt, String customerName, String customerNumber) {
        billTextArea.setText("----- Kim's Convenience -----\n");
        billTextArea.append("Customer: " + customerName + " | Customer No: " + customerNumber + "\n");
        billTextArea.append("------------------------------------------\n");
        billTextArea.append("ItemID  ItemName  Price  Quantity  Total\n");
        billTextArea.append("------------------------------------------\n");
        for (String line : receipt) {
            billTextArea.append(line + "\n");
        }
    }

    private void displayTotal(double gross) {
        double cgst = gross * 0.025;
        double sgst = gross * 0.025;
        double net = gross + cgst + sgst;
        billTextArea.append("------------------------------------------\n");
        billTextArea.append(String.format("Gross Amount: %.2f\nCGST: %.2f\nSGST: %.2f\nNet Amount: %.2f\n", gross,
                cgst, sgst, net));
        billTextArea.append("\nThank you for shopping at Kim's Convenience!\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SupermarketBillingSystem().setVisible(true));
    }
}
