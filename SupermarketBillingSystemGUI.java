package dbms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;

public class dbmsproject extends JFrame {
    private Connection conn;
    private JTextArea outputTextArea;
    private JTextArea billTextArea;

    public dbmsproject() {
        super("Supermarket Billing System");
        initialize();
        connectToDatabase();
    }

    private void initialize() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Gradient background for the main panel
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setPaint(new GradientPaint(0, 0, Color.decode("#6A82FB"), 0, getHeight(), Color.decode("#FC5C7D")));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BorderLayout());
        add(mainPanel);

        // Header
        JLabel headerLabel = new JLabel("Welcome to Kim's Convenience", JLabel.CENTER);
        headerLabel.setFont(new Font("Verdana", Font.BOLD, 24));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Output Text Area (Modernized)
        outputTextArea = new JTextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setFont(new Font("Arial", Font.PLAIN, 16));
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        outputTextArea.setForeground(Color.BLACK);
        outputTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane outputScrollPane = new JScrollPane(outputTextArea);
        outputScrollPane.setPreferredSize(new Dimension(900, 300));
        outputScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.WHITE, 2),
            "Billing Details",
            0,
            0,
            new Font("Arial", Font.BOLD, 16),
            Color.WHITE
        ));
        mainPanel.add(outputScrollPane, BorderLayout.CENTER);

        // Bill Text Area (Modernized)
        billTextArea = new JTextArea();
        billTextArea.setEditable(false);
        billTextArea.setFont(new Font("Courier New", Font.PLAIN, 14));
        billTextArea.setForeground(Color.BLACK);
        billTextArea.setBackground(Color.decode("#F5F5F5"));
        billTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane billScrollPane = new JScrollPane(billTextArea);
        billScrollPane.setPreferredSize(new Dimension(900, 250));
        billScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.WHITE, 2),
            "Final Bill",
            0,
            0,
            new Font("Arial", Font.BOLD, 16),
            Color.WHITE
        ));
        mainPanel.add(billScrollPane, BorderLayout.SOUTH);
        
        showStartWindow();
    }


    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/dbms",
                    "root", ""); //add your password 
        } catch (Exception e) {
            outputTextArea.append("Error connecting to database: " + e.getMessage() + "\n");
        }
    }

    private void showStartWindow() {
        outputTextArea.setText("\t\n");
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

                // Display the current item details
                outputTextArea.append(String.format("Item: %s | Price: %.2f | Stock: %d\n", itemName, price, stock));

                String quantityStr = JOptionPane.showInputDialog("Enter quantity:");
                if (quantityStr == null || !quantityStr.matches("\\d+"))
                    continue;
                int quantity = Integer.parseInt(quantityStr);

                // Assertion to handle insufficient stock
                if (quantity > stock) {
                    JOptionPane.showMessageDialog(this,
                            String.format("Insufficient stock for %s.\nAvailable: %d\nRequested: %d", itemName, stock, quantity),
                            "Stock Error",
                            JOptionPane.ERROR_MESSAGE);
                    outputTextArea.append("Insufficient stock for " + itemName + ". Available: " + stock + "\n");
                    continue;
                }

                double itemTotal = quantity * price * (1 - discount / 100);
                gross += itemTotal;
                stock -= quantity;

                updateInventoryStmt.setInt(1, stock);
                updateInventoryStmt.setString(2, itemId);
                updateInventoryStmt.executeUpdate();

                // Update receipt and display scanned item details
                receipt.add(String.format("%s\t%s\t%.2f\t%d\t%.2f", itemId, itemName, price, quantity, itemTotal));
                outputTextArea.append(String.format("Scanned: %s | Quantity: %d | Remaining Stock: %d | Price: %.2f\n",
                        itemName, quantity, stock, itemTotal));
            }

            gross = applyRewardPoints(gross, rewardPoints);

         // Calculate new points earned and update database
         int newPointsEarned = (int) (gross / 100);
         outputTextArea.append("New Reward Points Earned: " + newPointsEarned + "\n");

         if (customerNumber != null && !customerNumber.isEmpty()) {
             int updatedRewardPoints = rewardPoints - (int) gross + newPointsEarned; // Calculate updated reward points
             updateCustomerStmt.setInt(1, updatedRewardPoints);
             updateCustomerStmt.setString(2, customerNumber);
             updateCustomerStmt.executeUpdate();
         }


      // After calculating and finalizing gross
         displayReceiptPopup(receipt, customerName, customerNumber, gross);


        } catch (SQLException e) {
            outputTextArea.append("Error during billing: " + e.getMessage() + "\n");
        }
    }

    private double applyRewardPoints(double gross, int rewardPoints) {
        outputTextArea.append(String.format("Gross Amount Before Rewards: %.2f\n", gross)); // Show gross before rewards

        int pointsToRedeem = Math.min(rewardPoints, (int) gross);
        gross -= pointsToRedeem;

        outputTextArea.append("Reward Points Redeemed: " + pointsToRedeem + "\n");

        return gross;
    }


    private void displayReceiptPopup(ArrayList<String> receipt, String customerName, String customerNumber, double gross) {
        // Create a new dialog for the receipt
        JDialog receiptDialog = new JDialog(this, "Bill Receipt", true);
        receiptDialog.setSize(600, 800);
        receiptDialog.setLocationRelativeTo(this);

        // Create a text area to display the bill
        JTextArea billTextArea = new JTextArea();
        billTextArea.setEditable(false);
        billTextArea.setFont(new Font("Courier New", Font.PLAIN, 14));

        // Add receipt content
        billTextArea.setText("----- Kim's Convenience -----\n");
        billTextArea.append("Customer: " + customerName + " | Customer No: " + customerNumber + "\n");
        billTextArea.append("------------------------------------------\n");
        billTextArea.append("ItemID  ItemName  Price  Quantity  Total\n");
        billTextArea.append("------------------------------------------\n");
        for (String line : receipt) {
            billTextArea.append(line + "\n");
        }

        // Add totals and thank you message
        double cgst = gross * 0.025;
        double sgst = gross * 0.025;
        double net = gross + cgst + sgst;
        billTextArea.append("------------------------------------------\n");
        billTextArea.append(String.format("Gross Amount: %.2f\nCGST: %.2f\nSGST: %.2f\nNet Amount: %.2f\n", gross, cgst, sgst, net));
        billTextArea.append("\nThank you for shopping at Kim's Convenience!\n");

        // Add the text area to a scroll pane
        JScrollPane scrollPane = new JScrollPane(billTextArea);

        // Add the scroll pane to the dialog
        receiptDialog.add(scrollPane, BorderLayout.CENTER);

        // Add a close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> receiptDialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        receiptDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Make the dialog visible
        receiptDialog.setVisible(true);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new dbmsproject().setVisible(true));
    }
}
