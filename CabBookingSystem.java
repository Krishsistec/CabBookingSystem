import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;


public class CabBookingSystem {
    static boolean DEMO_MODE = true;  // Demo mode without database
    static Map<String, String> demoUsers = new HashMap<>();
    static java.util.List<String[]> demoBookings = new ArrayList<>();
    static {
        demoUsers.put("demo", "demo");
        demoUsers.put("john", "password123");
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    static class DBConnection {
        private static final String URL  = "jdbc:mysql://localhost:3306/cab_booking_db";
        private static final String USER = "root";
        private static final String PASS = "root"; 
        private static Connection conn;

        static Connection get() {
            try {
                if (conn == null || conn.isClosed()) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    conn = DriverManager.getConnection(URL, USER, PASS);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "DB Connection Failed!\n" + e.getMessage() +
                    "\n\nMake sure:\n1. MySQL is running\n2. You ran schema.sql\n3. Password is correct",
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
            return conn;
        }
    }

    static final Color BG_DARK      = new Color(13, 17, 30);
    static final Color BG_CARD      = new Color(22, 28, 48);
    static final Color ACCENT       = new Color(255, 180, 0);
    static final Color ACCENT2      = new Color(255, 120, 0);
    static final Color TEXT_WHITE   = new Color(100, 220, 255);
    static final Color TEXT_GRAY    = new Color(140, 150, 180);
    static final Color FIELD_BG     = new Color(30, 38, 60);
    static final Color BORDER_COL   = new Color(50, 65, 100);
    static final DecimalFormat DF   = new DecimalFormat("#,##0.00");

    static class LoginFrame extends JFrame {
        private JTextField usernameField;
        private JPasswordField passwordField;

        LoginFrame() {
            setTitle("CabSwift — Login");
            setSize(900, 580);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setResizable(false);
            buildUI();
        }

        private void buildUI() {
            JPanel root = new JPanel(new GridLayout(1, 2)) {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(BG_DARK); g.fillRect(0,0,getWidth(),getHeight());
                }
            };

            JPanel left = new JPanel(null) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    GradientPaint gp = new GradientPaint(0,0,new Color(255,140,0), getWidth(),getHeight(),new Color(160,50,0));
                    g2.setPaint(gp); g2.fillRect(0,0,getWidth(),getHeight());
                    g2.setColor(new Color(255,255,255,15));
                    for (int i=0;i<7;i++) { int r=50+i*55; g2.fillOval(getWidth()/2-r,getHeight()/2-r,r*2,r*2); }
                }
            };
            addTo(left, label("🚕",  "Segoe UI Emoji", Font.PLAIN, 68, Color.WHITE, 0, 110, 450, 90));
            addTo(left, label("CabSwift", "Georgia", Font.BOLD, 40, Color.WHITE, 0, 210, 450, 50));
            addTo(left, label("Fast. Safe. Reliable.", "Georgia", Font.ITALIC, 15, new Color(255,235,190), 0, 268, 450, 28));
            JLabel feat = new JLabel("<html><center>&#10003; Instant Booking<br>&#10003; Live Fare Preview<br>&#10003; 24/7 Support</center></html>", SwingConstants.CENTER);
            feat.setFont(new Font("Segoe UI", Font.PLAIN, 13)); feat.setForeground(new Color(255,230,170)); feat.setBounds(60, 315, 330, 75);
            left.add(feat);

            JPanel right = new JPanel(null) {
                @Override protected void paintComponent(Graphics g) { g.setColor(BG_DARK); g.fillRect(0,0,getWidth(),getHeight()); }
            };

            JPanel card = card(40, 50, 370, 440);

            addTo(card, label("Welcome Back", "Georgia", Font.BOLD, 26, TEXT_WHITE, 28, 25, 320, 36));
            addTo(card, label("Sign in to your account", "Segoe UI", Font.PLAIN, 13, TEXT_GRAY, 28, 64, 320, 20));

            JSeparator sep = new JSeparator(); sep.setForeground(BORDER_COL); sep.setBounds(28,92,314,2); card.add(sep);

            addTo(card, label("Username", "Segoe UI", Font.BOLD, 11, TEXT_GRAY, 28, 108, 300, 18));
            usernameField = styledField(28, 128, 314, false);
            card.add(usernameField);

            addTo(card, label("Password", "Segoe UI", Font.BOLD, 11, TEXT_GRAY, 28, 190, 300, 18));
            passwordField = (JPasswordField) styledField(28, 210, 314, true);
            card.add(passwordField);

            JButton loginBtn = fancyButton("SIGN IN", 28, 290, 314, 44, true);
            JButton regBtn   = fancyButton("New user? Register here", 28, 348, 314, 38, false);
            card.add(loginBtn); card.add(regBtn);

            loginBtn.addActionListener(e -> doLogin());
            regBtn.addActionListener(e -> { new RegisterFrame(this).setVisible(true); });
            passwordField.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) { if(e.getKeyCode()==KeyEvent.VK_ENTER) doLogin(); }
            });

            right.add(card);
            JLabel footer = label("© 2025 CabSwift  |  Raipur, Chhattisgarh", "Segoe UI", Font.PLAIN, 11, new Color(60,75,110), 0, 520, 450, 18);
            footer.setHorizontalAlignment(SwingConstants.CENTER);
            right.add(footer);

            root.add(left); root.add(right);
            setContentPane(root);
        }

        private void doLogin() {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();
            if (user.isEmpty() || pass.isEmpty()) { err("Enter username and password."); return; }
            
            if (DEMO_MODE) {
                if (demoUsers.containsKey(user) && demoUsers.get(user).equals(pass)) {
                    String name = user.equals("demo") ? "Demo User" : "John Doe";
                    dispose(); new BookingFrame(1, name).setVisible(true);
                } else { err("Invalid username or password!\n\nDemo credentials:\nUsername: demo\nPassword: demo"); }
            } else {
                try {
                    PreparedStatement ps = DBConnection.get().prepareStatement(
                        "SELECT id, full_name FROM users WHERE username=? AND password=?");
                    ps.setString(1, user); ps.setString(2, pass);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        int uid = rs.getInt("id"); String name = rs.getString("full_name");
                        dispose(); new BookingFrame(uid, name).setVisible(true);
                    } else { err("Invalid username or password!"); }
                    rs.close(); ps.close();
                } catch (SQLException ex) { err("DB Error: " + ex.getMessage()); }
            }
        }

        private void err(String msg) { JOptionPane.showMessageDialog(this, msg, "Login Error", JOptionPane.ERROR_MESSAGE); }
    }

    static class RegisterFrame extends JDialog {
        private JTextField nameF, emailF, phoneF, userF;
        private JPasswordField passF, confF;

        RegisterFrame(JFrame parent) {
            super(parent, "Register — CabSwift", true);
            setSize(460, 560);
            setLocationRelativeTo(parent);
            setResizable(false);
            buildUI();
        }

        private void buildUI() {
            JPanel root = new JPanel(null) {
                @Override protected void paintComponent(Graphics g) { g.setColor(BG_DARK); g.fillRect(0,0,getWidth(),getHeight()); }
            };

            addTo(root, label("Create Account", "Georgia", Font.BOLD, 24, TEXT_WHITE, 28, 18, 400, 34));
            addTo(root, label("Join CabSwift and start booking rides", "Segoe UI", Font.PLAIN, 13, TEXT_GRAY, 28, 55, 400, 20));

            nameF  = regField(root, "Full Name",         28, 92);
            emailF = regField(root, "Email Address",     28, 152);
            phoneF = regField(root, "Phone Number",      28, 212);
            userF  = regField(root, "Username",          28, 272);
            passF  = (JPasswordField) regField(root, "Password", 28, 332, true);
            confF  = (JPasswordField) regField(root, "Confirm Password", 28, 392, true);

            JButton create = fancyButton("CREATE ACCOUNT", 28, 456, 400, 40, true);
            JButton cancel = fancyButton("Cancel",         28, 504, 400, 32, false);
            root.add(create); root.add(cancel);

            create.addActionListener(e -> doRegister());
            cancel.addActionListener(e -> dispose());
            setContentPane(root);
        }

        private JTextField regField(JPanel p, String lbl, int x, int y) {
            return regField(p, lbl, x, y, false);
        }

        private JTextField regField(JPanel p, String lbl, int x, int y, boolean pwd) {
            addTo(p, label(lbl, "Segoe UI", Font.BOLD, 11, TEXT_GRAY, x, y, 400, 16));
            JTextField f = pwd ? new JPasswordField() : new JTextField();
            f.setBounds(x, y+18, 400, 36);
            f.setBackground(FIELD_BG); f.setForeground(TEXT_WHITE); f.setCaretColor(ACCENT);
            f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL,1), BorderFactory.createEmptyBorder(4,10,4,10)));
            f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            p.add(f); return f;
        }

        private void doRegister() {
            String name = nameF.getText().trim(), email = emailF.getText().trim();
            String phone = phoneF.getText().trim(), user = userF.getText().trim();
            String pass = new String(passF.getPassword()).trim();
            String conf = new String(confF.getPassword()).trim();

            if (name.isEmpty()||email.isEmpty()||phone.isEmpty()||user.isEmpty()||pass.isEmpty()) {
                JOptionPane.showMessageDialog(this,"All fields are required!","Error",JOptionPane.ERROR_MESSAGE); return;
            }
            if (!pass.equals(conf)) {
                JOptionPane.showMessageDialog(this,"Passwords do not match!","Error",JOptionPane.ERROR_MESSAGE); return;
            }
            if (pass.length()<6) {
                JOptionPane.showMessageDialog(this,"Password must be at least 6 characters!","Error",JOptionPane.ERROR_MESSAGE); return;
            }
            
            if (DEMO_MODE) {
                if (demoUsers.containsKey(user)) {
                    JOptionPane.showMessageDialog(this,"Username already exists!","Error",JOptionPane.ERROR_MESSAGE);
                } else {
                    demoUsers.put(user, pass);
                    JOptionPane.showMessageDialog(this,"Account created! Please login.","Success",JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                }
            } else {
                try {
                    PreparedStatement ps = DBConnection.get().prepareStatement(
                        "INSERT INTO users (full_name,email,phone,username,password) VALUES (?,?,?,?,?)");
                    ps.setString(1,name); ps.setString(2,email); ps.setString(3,phone);
                    ps.setString(4,user); ps.setString(5,pass);
                    ps.executeUpdate(); ps.close();
                    JOptionPane.showMessageDialog(this,"Account created! Please login.","Success",JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (SQLIntegrityConstraintViolationException ex) {
                    JOptionPane.showMessageDialog(this,"Username or Email already exists!","Error",JOptionPane.ERROR_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    static class BookingFrame extends JFrame {
        private final int userId;
        private final String userName;

        // Booking-tab fields
        private JComboBox<String> cabCombo;
        private JTextField pickupF, dropF, distF, tdateF;
        private JLabel baseLbl, taxLbl, totalLbl, driverLbl, cabNoLbl;

        private JTable table;
        private DefaultTableModel model;

        private static final double[] RATES = {10.0, 14.0, 18.0, 16.0, 7.0};
        private static final String[][] DRIVER_INFO = {
            {"Ramesh Kumar",  "MP07-AB-1234"},
            {"Suresh Singh",  "MP07-CD-5678"},
            {"Mahesh Verma",  "MP07-EF-9012"},
            {"Dinesh Sharma", "MP07-GH-3456"},
            {"Rakesh Patel",  "MP07-IJ-7890"}
        };

        BookingFrame(int userId, String userName) {
            this.userId = userId; this.userName = userName;
            setTitle("CabSwift — Dashboard");
            setSize(1150, 720);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            buildUI();
            loadBookings();
        }

        private void buildUI() {
            JPanel root = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) { g.setColor(BG_DARK); g.fillRect(0,0,getWidth(),getHeight()); }
            };
            root.add(buildTopBar(), BorderLayout.NORTH);
            root.add(buildTabs(),   BorderLayout.CENTER);
            setContentPane(root);
        }
        // ── Top bar ──────────────────────────────────────────
        private JPanel buildTopBar() {
            JPanel bar = new JPanel(null) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g;
                    g2.setPaint(new GradientPaint(0,0,new Color(255,140,0),getWidth(),0,new Color(170,55,0)));
                    g2.fillRect(0,0,getWidth(),getHeight());
                }
            };
            bar.setPreferredSize(new Dimension(1150, 60));

            addTo(bar, label("🚕  CabSwift", "Georgia", Font.BOLD, 22, Color.WHITE, 18, 12, 230, 36));
            addTo(bar, label("Welcome, " + userName, "Segoe UI", Font.PLAIN, 14, new Color(255,235,190), 270, 18, 500, 24));

            JButton logout = fancyButton("Logout", 1020, 14, 100, 32, false);
            logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
            bar.add(logout);
            return bar;
        }
        private JTabbedPane buildTabs() {
            JTabbedPane tabs = new JTabbedPane();
            tabs.setBackground(BG_DARK);
            tabs.setForeground(TEXT_WHITE);
            tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
            tabs.addTab("📋   Book a Ride",  buildBookingTab());
            tabs.addTab("🗒   My Bookings",  buildBookingsTab());
            return tabs;
        }
        private JPanel buildBookingTab() {
            JPanel outer = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) { g.setColor(BG_DARK); g.fillRect(0,0,getWidth(),getHeight()); }
            };

            JPanel card = card(0, 0, 980, 560); // positioned by GridBagLayout
            card.setPreferredSize(new Dimension(980, 560));

            // ── Header ───────────────────────────────────────
            addTo(card, label("Book Your Ride", "Georgia", Font.BOLD, 22, ACCENT,  28, 20, 500, 30));
            addTo(card, label("Fill in the details below for an instant cab booking", "Segoe UI", Font.PLAIN, 13, TEXT_GRAY, 28, 54, 600, 20));

            // ── Row 1: Cab Type | Travel Date ────────────────
            addTo(card, label("Cab Type", "Segoe UI", Font.BOLD, 11, TEXT_GRAY, 28, 90, 400, 18));
            String[] types = {"Mini (Hatchback)  —  Rs.10/km",
                              "Sedan             —  Rs.14/km",
                              "SUV               —  Rs.18/km",
                              "Prime Sedan       —  Rs.16/km",
                              "Auto Rickshaw     —  Rs.7/km"};
            cabCombo = new JComboBox<>(types);
            cabCombo.setBounds(28, 110, 420, 38);
            cabCombo.setBackground(FIELD_BG); cabCombo.setForeground(TEXT_WHITE);
            cabCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cabCombo.addActionListener(e -> { calcFare(); refreshDriver(); });
            card.add(cabCombo);

            addTo(card, label("Travel Date  (YYYY-MM-DD)", "Segoe UI", Font.BOLD, 11, TEXT_GRAY, 480, 90, 280, 18));
            tdateF = inputField(card, LocalDate.now().toString(), 480, 110, 240);

            // ── Row 2: Pickup | Drop ─────────────────────────
            addTo(card, label("Pickup Location", "Segoe UI", Font.BOLD, 11, TEXT_GRAY, 28, 168, 400, 18));
            pickupF = inputField(card, "", 28, 188, 420);

            addTo(card, label("Drop Location", "Segoe UI", Font.BOLD, 11, TEXT_GRAY, 480, 168, 400, 18));
            dropF   = inputField(card, "", 480, 188, 420);

            // ── Row 3: Distance | Fare card ──────────────────
            addTo(card, label("Distance  (km)", "Segoe UI", Font.BOLD, 11, TEXT_GRAY, 28, 246, 200, 18));
            distF = inputField(card, "", 28, 266, 200);
            distF.addKeyListener(new KeyAdapter() {
                @Override public void keyReleased(KeyEvent e) { calcFare(); }
            });

            // Fare breakdown panel
            JPanel fareBox = new JPanel(null) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(28,38,62)); g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                    g2.setColor(BORDER_COL); g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                }
            };
            fareBox.setOpaque(false); fareBox.setBounds(480, 246, 420, 150);

            addTo(fareBox, label("Fare Breakdown", "Segoe UI", Font.BOLD, 13, ACCENT, 15, 12, 250, 20));

            addTo(fareBox, label("Base Fare :", "Segoe UI", Font.PLAIN, 12, TEXT_GRAY,   15, 42, 180, 20));
            baseLbl = label("Rs.0.00", "Segoe UI", Font.BOLD, 12, TEXT_WHITE, 230, 42, 160, 20); fareBox.add(baseLbl);

            addTo(fareBox, label("Tax (12%) :", "Segoe UI", Font.PLAIN, 12, TEXT_GRAY,   15, 68, 180, 20));
            taxLbl  = label("Rs.0.00", "Segoe UI", Font.BOLD, 12, TEXT_WHITE, 230, 68, 160, 20); fareBox.add(taxLbl);
            JSeparator fs = new JSeparator(); fs.setForeground(BORDER_COL); fs.setBounds(15,96,390,2); fareBox.add(fs);

            addTo(fareBox, label("Total :", "Segoe UI", Font.BOLD, 14, TEXT_WHITE, 15, 106, 180, 24));
            totalLbl = label("Rs.0.00", "Segoe UI", Font.BOLD, 16, ACCENT, 230, 104, 170, 28); fareBox.add(totalLbl);
            card.add(fareBox);

            // Driver info panel
            JPanel drvBox = new JPanel(null) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255,140,0,22)); g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                    g2.setColor(new Color(255,140,0,70)); g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                }
            };
            drvBox.setOpaque(false); drvBox.setBounds(28, 330, 380, 72);
            addTo(drvBox, label("Assigned Driver", "Segoe UI", Font.BOLD, 11, ACCENT,      12,  8, 300, 18));
            driverLbl = label("Driver : Ramesh Kumar",  "Segoe UI", Font.PLAIN, 12, TEXT_WHITE, 12, 28, 340, 18);
            cabNoLbl  = label("Cab No : MP07-AB-1234",  "Segoe UI", Font.PLAIN, 12, TEXT_WHITE, 12, 48, 340, 18);
            drvBox.add(driverLbl); drvBox.add(cabNoLbl);
            card.add(drvBox);

            // Confirm button
            JButton confirm = fancyButton("CONFIRM BOOKING", 28, 430, 924, 52, true);
            confirm.addActionListener(e -> doBooking());
            card.add(confirm);

            outer.add(card);
            return outer;
        }

        // fare calculation
        private void calcFare() {
            try {
                double dist = Double.parseDouble(distF.getText().trim());
                int    idx  = cabCombo.getSelectedIndex();
                double rate = (idx>=0&&idx<RATES.length) ? RATES[idx] : 10.0;
                double base = dist * rate;
                double tax  = base * 0.12;
                baseLbl.setText("Rs." + DF.format(base));
                taxLbl.setText("Rs." + DF.format(tax));
                totalLbl.setText("Rs." + DF.format(base+tax));
            } catch (NumberFormatException e) {
                baseLbl.setText("Rs.0.00"); taxLbl.setText("Rs.0.00"); totalLbl.setText("Rs.0.00");
            }
        }

        private void refreshDriver() {
            int i = cabCombo.getSelectedIndex();
            if (i>=0&&i<DRIVER_INFO.length) {
                driverLbl.setText("Driver : " + DRIVER_INFO[i][0]);
                cabNoLbl.setText("Cab No : "  + DRIVER_INFO[i][1]);
            }
        }

        private void doBooking() {
            String pickup = pickupF.getText().trim();
            String drop   = dropF.getText().trim();
            String distS  = distF.getText().trim();
            String tdate  = tdateF.getText().trim();

            if (pickup.isEmpty()||drop.isEmpty()||distS.isEmpty()||tdate.isEmpty()) {
                JOptionPane.showMessageDialog(this,"All fields are required!","Validation",JOptionPane.WARNING_MESSAGE); return;
            }
            double dist;
            try { dist = Double.parseDouble(distS); }
            catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,"Distance must be a valid number!","Validation",JOptionPane.WARNING_MESSAGE); return;
            }
            if (dist <= 0) {
                JOptionPane.showMessageDialog(this,"Distance must be greater than 0!","Validation",JOptionPane.WARNING_MESSAGE); return;
            }

            int    idx   = cabCombo.getSelectedIndex();
            double rate  = RATES[idx];
            double base  = dist * rate;
            double tax   = base * 0.12;
            double total = base + tax;
            int    cabId = idx + 1;
            String bid   = "CBS" + (System.currentTimeMillis() % 100000);

            if (DEMO_MODE) {
                String[] cabTypes = {"Mini (Hatchback)","Sedan","SUV","Prime Sedan","Auto Rickshaw"};
                demoBookings.add(new String[]{bid, cabTypes[idx], pickup, drop, String.valueOf(dist), 
                    String.valueOf(base), String.valueOf(tax), String.valueOf(total),
                    LocalDate.now().toString(), tdate, "CONFIRMED"});
                    
                JOptionPane.showMessageDialog(this,
                    "Booking Confirmed!\n\n" +
                    "Booking ID  : " + bid + "\n" +
                    "Pickup      : " + pickup + "\n" +
                    "Drop        : " + drop + "\n" +
                    "Distance    : " + dist + " km\n" +
                    "Total Fare  : Rs." + DF.format(total) + "\n\n" +
                    "Driver : " + DRIVER_INFO[idx][0] + "  |  " + DRIVER_INFO[idx][1],
                    "Booking Confirmed!", JOptionPane.INFORMATION_MESSAGE);

                pickupF.setText(""); dropF.setText(""); distF.setText("");
                tdateF.setText(LocalDate.now().toString());
                baseLbl.setText("Rs.0.00"); taxLbl.setText("Rs.0.00"); totalLbl.setText("Rs.0.00");
                loadBookings();
            } else {
                try {
                    PreparedStatement ps = DBConnection.get().prepareStatement(
                        "INSERT INTO bookings (booking_id,user_id,cab_id,pickup_location,drop_location," +
                        "distance_km,base_fare,tax_amount,total_fare,travel_date) VALUES (?,?,?,?,?,?,?,?,?,?)");
                    ps.setString(1,bid);  ps.setInt(2,userId);  ps.setInt(3,cabId);
                    ps.setString(4,pickup); ps.setString(5,drop);
                    ps.setDouble(6,dist); ps.setDouble(7,base); ps.setDouble(8,tax);
                    ps.setDouble(9,total); ps.setString(10,tdate);
                    ps.executeUpdate(); ps.close();

                    JOptionPane.showMessageDialog(this,
                        "Booking Confirmed!\n\n" +
                        "Booking ID  : " + bid + "\n" +
                        "Pickup      : " + pickup + "\n" +
                        "Drop        : " + drop + "\n" +
                        "Distance    : " + dist + " km\n" +
                        "Total Fare  : Rs." + DF.format(total) + "\n\n" +
                        "Driver : " + DRIVER_INFO[idx][0] + "  |  " + DRIVER_INFO[idx][1],
                        "Booking Confirmed!", JOptionPane.INFORMATION_MESSAGE);

                    pickupF.setText(""); dropF.setText(""); distF.setText("");
                    tdateF.setText(LocalDate.now().toString());
                    baseLbl.setText("Rs.0.00"); taxLbl.setText("Rs.0.00"); totalLbl.setText("Rs.0.00");
                    loadBookings();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this,"Booking failed: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private JPanel buildBookingsTab() {
            JPanel panel = new JPanel(new BorderLayout(0,10)) {
                @Override protected void paintComponent(Graphics g) { g.setColor(BG_DARK); g.fillRect(0,0,getWidth(),getHeight()); }
            };
            panel.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));

            JPanel topBar = new JPanel(new BorderLayout());
            topBar.setOpaque(false);
            addTo(topBar, label("My Bookings", "Georgia", Font.BOLD, 22, ACCENT, 0, 0, 300, 32));

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btns.setOpaque(false);

            JButton refresh = tableBtn("Refresh",          false);
            JButton cancel  = tableBtn("Cancel Selected",  true);
            JButton delete  = tableBtn("Delete Selected",  true);
            btns.add(refresh); btns.add(cancel); btns.add(delete);

            refresh.addActionListener(e -> loadBookings());
            cancel.addActionListener(e  -> doCancelBooking());
            delete.addActionListener(e  -> doDeleteBooking());

            topBar.add(btns, BorderLayout.EAST);
            panel.add(topBar, BorderLayout.NORTH);

            String[] cols = {"Booking ID","Cab Type","Pickup","Drop",
                             "Dist (km)","Base Fare","Tax","Total","Booked On","Travel Date","Status"};
            model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            table = new JTable(model);
            table.setBackground(BG_CARD);
            table.setForeground(TEXT_WHITE);
            table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            table.setRowHeight(30);
            table.setGridColor(BORDER_COL);
            table.setSelectionBackground(new Color(255,150,0,55));
            table.setSelectionForeground(TEXT_WHITE);
            table.setShowGrid(true);
            table.setIntercellSpacing(new Dimension(1,1));
            table.getTableHeader().setBackground(new Color(255,140,0));
            table.getTableHeader().setForeground(Color.BLACK);
            table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            table.getTableHeader().setReorderingAllowed(false);

            table.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object v,
                        boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setFont(new Font("Segoe UI", Font.BOLD, 11));
                    String s = String.valueOf(v);
                    setForeground("CONFIRMED".equals(s) ? new Color(80,220,100) :
                                  "CANCELLED".equals(s) ? new Color(255,80,80)  : TEXT_GRAY);
                    setBackground(sel ? new Color(255,150,0,55) : BG_CARD);
                    return this;
                }
            });

            int[] w = {110,140,150,150,80,90,80,100,140,100,90};
            for (int i=0;i<w.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

            JScrollPane scroll = new JScrollPane(table);
            scroll.getViewport().setBackground(BG_CARD);
            scroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
            panel.add(scroll, BorderLayout.CENTER);

            JPanel hint = new JPanel(new FlowLayout(FlowLayout.LEFT,12,6)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2=(Graphics2D)g; g2.setColor(BG_CARD); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                }
            };
            hint.setOpaque(false);
            JLabel hintLbl = new JLabel("Tip:  Select a row then click  'Cancel Selected'  to cancel  |  'Delete Selected'  to permanently remove.");
            hintLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12)); hintLbl.setForeground(TEXT_GRAY);
            hint.add(hintLbl);
            panel.add(hint, BorderLayout.SOUTH);

            return panel;
        }

        private void loadBookings() {
            model.setRowCount(0);
            if (DEMO_MODE) {
                for (String[] booking : demoBookings) {
                    model.addRow(new Object[]{
                        booking[0],
                        booking[1],
                        booking[2],
                        booking[3],
                        String.format("%.1f", Double.parseDouble(booking[4])),
                        "Rs." + DF.format(Double.parseDouble(booking[5])),
                        "Rs." + DF.format(Double.parseDouble(booking[6])),
                        "Rs." + DF.format(Double.parseDouble(booking[7])),
                        booking[8],
                        booking[9],
                        booking[10]
                    });
                }
            } else {
                try {
                    PreparedStatement ps = DBConnection.get().prepareStatement(
                        "SELECT b.booking_id,c.cab_type,b.pickup_location,b.drop_location," +
                        "b.distance_km,b.base_fare,b.tax_amount,b.total_fare," +
                        "b.booking_date,b.travel_date,b.status " +
                        "FROM bookings b JOIN cabs c ON b.cab_id=c.id " +
                        "WHERE b.user_id=? ORDER BY b.booking_date DESC");
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getString("booking_id"),
                            rs.getString("cab_type"),
                            rs.getString("pickup_location"),
                            rs.getString("drop_location"),
                            String.format("%.1f", rs.getDouble("distance_km")),
                            "Rs." + DF.format(rs.getDouble("base_fare")),
                            "Rs." + DF.format(rs.getDouble("tax_amount")),
                            "Rs." + DF.format(rs.getDouble("total_fare")),
                            rs.getString("booking_date"),
                            rs.getString("travel_date"),
                            rs.getString("status")
                        });
                    }
                    rs.close(); ps.close();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this,"Error loading: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void doCancelBooking() {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this,"Select a booking first.","No Selection",JOptionPane.WARNING_MESSAGE); return; }
            String bid = (String) model.getValueAt(row,0);
            String status = (String) model.getValueAt(row,10);
            if ("CANCELLED".equals(status)) { JOptionPane.showMessageDialog(this,"Already cancelled.","Info",JOptionPane.INFORMATION_MESSAGE); return; }
            if (JOptionPane.showConfirmDialog(this,"Cancel booking  " + bid + "?","Confirm",JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            
            if (DEMO_MODE) {
                for (String[] b : demoBookings) {
                    if (b[0].equals(bid)) { b[10] = "CANCELLED"; break; }
                }
                JOptionPane.showMessageDialog(this,"Booking cancelled.","Done",JOptionPane.INFORMATION_MESSAGE);
                loadBookings();
            } else {
                try {
                    PreparedStatement ps = DBConnection.get().prepareStatement(
                        "UPDATE bookings SET status='CANCELLED' WHERE booking_id=? AND user_id=?");
                    ps.setString(1,bid); ps.setInt(2,userId);
                    ps.executeUpdate(); ps.close();
                    JOptionPane.showMessageDialog(this,"Booking cancelled.","Done",JOptionPane.INFORMATION_MESSAGE);
                    loadBookings();
                } catch (SQLException ex) { JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
            }
        }

        private void doDeleteBooking() {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this,"Select a booking first.","No Selection",JOptionPane.WARNING_MESSAGE); return; }
            String bid = (String) model.getValueAt(row,0);
            if (JOptionPane.showConfirmDialog(this,
                    "Permanently DELETE booking  " + bid + "?\nThis cannot be undone.",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
            
            if (DEMO_MODE) {
                demoBookings.removeIf(b -> b[0].equals(bid));
                JOptionPane.showMessageDialog(this,"Booking deleted.","Done",JOptionPane.INFORMATION_MESSAGE);
                loadBookings();
            } else {
                try {
                    PreparedStatement ps = DBConnection.get().prepareStatement(
                        "DELETE FROM bookings WHERE booking_id=? AND user_id=?");
                    ps.setString(1,bid); ps.setInt(2,userId);
                    ps.executeUpdate(); ps.close();
                    JOptionPane.showMessageDialog(this,"Booking deleted.","Done",JOptionPane.INFORMATION_MESSAGE);
                    loadBookings();
                } catch (SQLException ex) { JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
            }
        }
    }
    static JTextField inputField(JPanel p, String text, int x, int y, int w) {
        JTextField f = new JTextField(text);
        f.setBounds(x,y,w,38);
        f.setBackground(FIELD_BG); f.setForeground(TEXT_WHITE); f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL,1), BorderFactory.createEmptyBorder(4,10,4,10)));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(f); return f;
    }

    static JLabel label(String text, String font, int style, int size, Color color, int x, int y, int w, int h) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(font, style, size));
        l.setForeground(color);
        l.setBounds(x,y,w,h);
        return l;
    }

    static void addTo(JPanel p, Component c) { p.add(c); }

    /** Rounded dark card panel */
    static JPanel card(int x, int y, int w, int h) {
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD); g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
            }
        };
        card.setOpaque(false); card.setBounds(x,y,w,h);
        return card;
    }

    static JButton fancyButton(String text, int x, int y, int w, int h, boolean primary) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base;
                if (primary) {
                    base = getModel().isPressed() ? ACCENT2.darker() : getModel().isRollover() ? ACCENT2 : ACCENT;
                    g2.setColor(base); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                    g2.setColor(BG_DARK);
                } else {
                    base = getModel().isRollover() ? BORDER_COL : FIELD_BG;
                    g2.setColor(base); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                    g2.setColor(getModel().isRollover() ? ACCENT : TEXT_GRAY);
                }
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,
                        (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setBounds(x,y,w,h);
        btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    static JButton tableBtn(String text, boolean danger) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = danger
                    ? (getModel().isRollover() ? new Color(190,40,40) : new Color(140,28,28))
                    : (getModel().isRollover() ? BORDER_COL : FIELD_BG);
                g2.setColor(c); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(TEXT_WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,
                        (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setPreferredSize(new Dimension(160,34));
        btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Styled field (text or password) — used in login */
    static JTextField styledField(int x, int y, int w, boolean isPass) {
        JTextField f = isPass ? new JPasswordField() : new JTextField();
        f.setBounds(x,y,w,42);
        f.setBackground(FIELD_BG); f.setForeground(TEXT_WHITE); f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL,1), BorderFactory.createEmptyBorder(5,12,5,12)));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return f;
    }
}