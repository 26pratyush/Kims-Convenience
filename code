import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;

public class SupermarketBillingSystemGUI extends JFrame {
    private Connection conn;
    private JTextArea outputTextArea;

    public SupermarketBillingSystemGUI() {
        super("Supermarket Billing System");
        initialize();
        connectToDatabase();
    }

    private void initialize() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());

        outputTextArea = new JTextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setFont(new Font("Arial", Font.PLAIN, 16)); // Set font size
        outputTextArea.setAlignmentX(Component.CENTER_ALIGNMENT); // Align text to center
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        add(panel);

        showStartWindow();
    }

    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/Supermarket",
                    "root", "percy560077");
        } catch (Exception e) {
            outputTextArea.append("Error connecting to database: " + e.getMessage() + "\n");
        }
    }

    private void showStartWindow() {
        outputTextArea.setText("\t\tWelcome Cashier\n");
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

        int passcodeInt = Integer.parseInt(passcode);
        if (passcodeInt == 1234) {
            startBilling();
        } else {
            JOptionPane.showMessageDialog(this, "Wrong passcode entered. Please try again.");
            showPasscodeDialog();
        }
    }

    private void startBilling() {
        try {
            PreparedStatement pstmt = conn.prepareStatement("select * from Items where id =?");
            PreparedStatement pstmt2 = conn.prepareStatement("update Items set stock=? where id=?");

            ArrayList<String> receipt = new ArrayList<>();
            double gross = billing(pstmt, pstmt2, receipt);
            displayReceipt(receipt);
            displayTotal(gross);

            conn.close();
        } catch (Exception e) {
            outputTextArea.append("Caught Exception: " + e.getMessage() + "\n");
        }
    }

    private double billing(PreparedStatement pstmt, PreparedStatement pstmt2, ArrayList<String> receipt)
            throws SQLException {
        double grossamt = 0.0;

        String name = "", id = "";
        int stock, quantity, ret = 0, slno = 1;
        double rate;

        outputTextArea.append("\n\t\tEnter your purchases (enter 'done' to finish):\n");

        while (true) {
            id = JOptionPane.showInputDialog("Enter item ID:");
            if (id == null || id.equalsIgnoreCase("done")) {
                break;
            }

            pstmt.setString(1, id);
            ResultSet rset = pstmt.executeQuery();
            if (!rset.next()) {
                outputTextArea.append("\t\tItem not found. Please enter a valid item.\n");
                continue;
            } else {
                name = rset.getString("name");
                rate = rset.getDouble("rate");
                stock = rset.getInt("stock");

                outputTextArea.append(
                        "Item Name : " + name + "\t Rate/kg : " + rate + "rs\t Stock available : " + stock + "kgs\n");

                while (true) {
                    String quantityStr = JOptionPane.showInputDialog("Enter quantity of items needed:");
                    if (quantityStr == null) {
                        break;
                    }
                    try {
                        quantity = Integer.parseInt(quantityStr);
                    } catch (NumberFormatException e) {
                        outputTextArea.append("Invalid quantity. Please enter a number.\n");
                        continue;
                    }

                    if (quantity < stock) {
                        double indigrossamt = quantity * rate;
                        grossamt += indigrossamt;
                        stock -= quantity;

                        pstmt2.setInt(1, stock);
                        pstmt2.setString(2, id);
                        ret = pstmt2.executeUpdate();

                        outputTextArea.append("Gross Amount : " + indigrossamt + "rs\t");
                        outputTextArea.append("Stock remaining : " + stock + "kgs\n");

                        receipt.add(String.valueOf(slno));
                        receipt.add(name);
                        receipt.add(String.valueOf(rate));
                        receipt.add(String.valueOf(quantity));
                        receipt.add(String.valueOf(indigrossamt));
                        break;
                    } else {
                        outputTextArea.append("Exceeded available stock. Enter less than " + stock + "\n");
                    }
                }
                slno++;
            }
        }

        return grossamt;
    }

    private void displayReceipt(ArrayList<String> receipt) {
        outputTextArea.append("\nBILL:\nNo.\tItem Name \t Rate/Kg(Rs) \t Quantity(kg) \t Gross Amount(Rs)\n");
        for (int i = 0; i < receipt.size(); i += 5) {
            outputTextArea.append(String.format("%s\t%-21s%7s%10s%27s\n", receipt.get(i), receipt.get(i + 1),
                    receipt.get(i + 2), receipt.get(i + 3), receipt.get(i + 4)));
        }
    }

    private void displayTotal(double gross) {
        double cgst = 0.025;
        double sgst = 0.025;
        double net = gross + (gross * cgst) + (gross * sgst);

        outputTextArea.append("============================================\n");
        outputTextArea.append("Gross Amount: Rs." + gross + "\n");
        outputTextArea.append("CGST (2.5 % applied): Rs." + (gross * cgst) + "\n");
        outputTextArea.append("SGST (2.5 % applied): Rs." + (gross * sgst) + "\n");
        outputTextArea.append("Net Amount: Rs." + net + "\n");
        outputTextArea.append("============================================\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SupermarketBillingSystemGUI().setVisible(true);
            }
        });
    }
}
