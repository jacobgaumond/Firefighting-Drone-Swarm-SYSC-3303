import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Objects;

public class DroneGUI extends JFrame {
    private GridPanel grid;
    private JTextArea logArea;

    private int activeFires;
    private JLabel activeFireLabel;

    // GUI Constructor
    public DroneGUI() {
        setTitle("Group 2, Drone GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // calculate dynamic grid dimensions using zones
        int maxX = 0, maxY = 0;
        for (ZoneMap.Zone zone : ZoneMap.getAllZones().values()) {
            if (zone != null) {
                maxX = Math.max(maxX, zone.endX / 100);
                maxY = Math.max(maxY, zone.endY / 100);
            }
        }
        grid = new GridPanel(maxX, maxY);

        // create sidebar
        JPanel sideBar = createRightSideContainer();

        // add to UI
        add(grid, BorderLayout.CENTER); 
        add(sideBar, BorderLayout.EAST);

        // startListening();
    }

    // === UI COMPONENTS ===
    // Grid Manager (Zone, Drone, and Fire creation/handling)
    class GridPanel extends JPanel {
        private final int cols;
        private final int rows;

        public Map<Integer, JLabel> fireLabels = new HashMap<>();

        // Grid Constructor
        public GridPanel(int cols, int rows) {
            this.cols = cols;
            this.rows = rows;

            this.setLayout(null);
            this.setBackground(Color.WHITE);
            
            initializeZones(); // create zones

            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    repositionComponents(); // handle resizing
                }
            });
        }

        // Create Zones
        public void initializeZones() {
            for (Entry<Integer, ZoneMap.Zone> entry : ZoneMap.getAllZones().entrySet()) {
                int zoneId = entry.getKey();
                ZoneMap.Zone zone = entry.getValue();
                
                int gridStartX = zone.startX / 100;
                int gridStartY = zone.startY / 100;
                
                // Add zone label at grid's first cell
                this.addZoneLabel(gridStartX, gridStartY, "Z(" + zoneId + ")", new Color(158, 194, 211), Color.BLACK);
                
                // Add fire label at center of zone
                this.createFireLabel(zoneId);
            }
        }

        // Zone Label at top left
        public void addZoneLabel(int gridX, int gridY, String text, Color bg, Color fg) {
            JLabel label = new JLabel(text);
            
            label.setOpaque(true);
            label.setBackground(bg);
            label.setForeground(fg);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            label.putClientProperty("gridX", gridX);
            label.putClientProperty("gridY", gridY);

            this.add(label);
            repositionComponents();
        }

        // Fire Label at center
        public void createFireLabel(int zoneId) {
            JLabel label = new JLabel();

            label.setOpaque(true);
            label.setBackground(new Color(130, 255, 95));
            label.setForeground(Color.BLACK);
            label.setHorizontalAlignment(SwingConstants.CENTER);

            // place Label in center cell (scale down by 100)
            label.putClientProperty("gridX", ZoneMap.getX(zoneId) / 100);
            label.putClientProperty("gridY", ZoneMap.getY(zoneId) / 100);

            // Keep track for updating
            fireLabels.put(zoneId, label);

            this.add(label);
            repositionComponents();
        }

        // Update fire status
        private void fireStatusChange(int zone, String fireLevel) {
            JLabel fireLabel = fireLabels.get(zone);
            if (fireLabel == null) return;
            
            if (Objects.equals(fireLevel, "")) { 
                // Extinguished Fires
                fireLabel.setBackground(new Color(130, 255, 95));
                fireLabel.setText(fireLevel);
                if (activeFires > 0) activeFires--;
            } else {
                // Active Fires
                fireLabel.setBackground(new Color(255, 103, 95));
                fireLabel.setText(fireLevel);
                activeFires++;
            }
            activeFireLabel.setText("Active Fires: " + activeFires);
        }

        // TODO
        private void createDroneLabel() { }
        private void trackDeployedDrone() { }

        // dynamic components resizing
        private void repositionComponents() {
            double unitW = (double) getWidth() / cols;
            double unitH = (double) getHeight() / rows;

            for (Component c : getComponents()) {
                if (c instanceof JLabel) {
                    int gx = (int) ((Integer) ((JLabel) c).getClientProperty("gridX"));
                    int gy = (int) ((Integer) ((JLabel) c).getClientProperty("gridY"));
                    c.setBounds((int) (gx * unitW), (int) (gy * unitH), (int) unitW, (int) unitH);
                }
            }
        }

        @Override
        // Draw gridlines and zone boundaries
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            Color lightGrid = new Color(83, 83, 83, 50);
            Color darkGrid = new Color(30, 30, 30);

            double unitW = (double) getWidth() / cols;
            double unitH = (double) getHeight() / rows;

            // Whole Grid
            g2.setStroke(new BasicStroke(1));
            g2.setColor(lightGrid);
            for (int i = 0; i <= cols; i++) {
                int x = (int) (i * unitW);
                g2.drawLine(x, 0, x, getHeight());
            }
            for (int i = 0; i <= rows; i++) {
                int y = (int) (i * unitH);
                g2.drawLine(0, y, getWidth(), y);
            }
            g2.setStroke(new BasicStroke(3));
            g2.setColor(darkGrid);

            // Zone boundaries
            for (ZoneMap.Zone zone : ZoneMap.getAllZones().values()) {
                int x1 = (int) ((zone.startX / 100) * unitW);
                int y1 = (int) ((zone.startY / 100) * unitH);
                int x2 = (int) ((zone.endX / 100) * unitW);
                int y2 = (int) ((zone.endY / 100) * unitH);
                
                g2.drawRect(x1, y1, x2 - x1, y2 - y1);
            }

            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

    // Right-side sidebar
    private JPanel createRightSideContainer() {
        // Side bar
        JPanel sideWrapper = new JPanel(new GridBagLayout());
        
        sideWrapper.setPreferredSize(new Dimension(400, 0)); // auto vertical
        sideWrapper.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, Color.LIGHT_GRAY));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        // Legend
        JPanel legendBar = createLegendPanel();
        gbc.gridy = 0;
        gbc.weighty = 0;
        sideWrapper.add(legendBar, gbc);

        // Fire Counter
        gbc.gridy = 1;
        gbc.weighty = 0.0;
        sideWrapper.add(createFireCounter(), gbc);

        // Logs
        JPanel bottomHalf = createLogsPanel();
        gbc.gridy = 2;
        gbc.weighty = 1.0; // fill vertically
        sideWrapper.add(bottomHalf, gbc);

        return sideWrapper;
    }

    // Legend
    private JPanel createLegendPanel() {
        JPanel p = new JPanel();

        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Legend"));
        p.setBackground(new Color(245, 245, 245));

        p.add(createLegendItem("Zone Label", new Color(158, 194, 211), "Z(n)"));
        p.add(createLegendItem("Active Fire", new Color(255, 103, 95), ""));
        p.add(createLegendItem("Extinguished Fire", new Color(130, 255, 95), ""));
        p.add(createLegendItem("Drone Outbound", new Color(255, 180, 95), "D(n)"));
        p.add(createLegendItem("Drone Extinguishing fire", new Color(106, 131, 95), "D(n)"));
        p.add(createLegendItem("Drone returning", new Color(201, 95, 255), "D(n)"));

        return p;
    }
    private JPanel createLegendItem(String text, Color color, String boxText) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setOpaque(false);

        JLabel box = new JLabel(boxText);
        box.setOpaque(true);
        box.setPreferredSize(new Dimension(30, 30));
        box.setBackground(color);
        box.setForeground(Color.BLACK); // Text
        box.setHorizontalAlignment(SwingConstants.CENTER); // Center text horizontally
        box.setVerticalAlignment(SwingConstants.CENTER); // Center text vertically
        box.setFont(new Font("SansSerif", Font.BOLD, 10));
        box.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        p.add(box);
        p.add(new JLabel(text)); // The descriptive text next to box

        return p;
    }

    // Fire Counter
    private JPanel createFireCounter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

        p.setBorder(BorderFactory.createTitledBorder("Fires"));
        p.setBackground(new Color(245, 245, 245));

        activeFireLabel = new JLabel("Active Fires: 0");
        activeFireLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        p.add(activeFireLabel);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); // prevent resizing

        return p;
    }

    // Logs
    private JPanel createLogsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Logs"));

        logArea = new JTextArea();
        logArea.setEditable(false); // Recommended for logs
        logArea.setLineWrap(true);

        JScrollPane scroll = new JScrollPane(logArea);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    // === MAIN ===
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // For testing, create a dummy zone map
            ZoneMap.addZone(1, new ZoneMap.Zone(0, 0, 700, 700));
            ZoneMap.addZone(2, new ZoneMap.Zone(700, 0, 1200, 700));
            ZoneMap.addZone(3, new ZoneMap.Zone(0, 700, 500, 1400));
            ZoneMap.addZone(4, new ZoneMap.Zone(500, 700, 1200, 1400));
            ZoneMap.addZone(5, new ZoneMap.Zone(1200, 0, 2100, 1400));
            
            DroneGUI gui = new DroneGUI();
            gui.setVisible(true);

            //quick test for fireStatusChange and logging messag
            Timer timer1 = new Timer(1000, e -> {
                gui.logMessage("FIRE_DETECTED_3_H");
                gui.grid.fireStatusChange(3, "H");
            });

            Timer timer2 = new Timer(3000, e -> {
                gui.logMessage("FIRE_EXTINGUISHED_3");
                gui.grid.fireStatusChange(3, "");
            });
            timer1.setRepeats(false);
            timer1.start();
            timer2.setRepeats(false);
            timer2.start();
        });
    }

    // === HELPERS ===
    // update logs
    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            // Remove trailing X's and whitespace, then append
            String cleanMsg = message.replaceAll("X+$", "").trim();
            logArea.append(cleanMsg + "\n");

            // Auto-scroll to the bottom
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}

// FUTURE ITERATIONS:
            
    // public static final int DRONE_GUI_PORT = 9503;

    /**Listerner for later implentation
    private void startListening() {
        Thread listenerThread = new Thread(() -> {
        // This is your fireIncident-style Receive Socket
        try (DatagramSocket guiSocket = new DatagramSocket(DRONE_GUI_PORT)) {
        byte[] buffer = new byte[100]; // Matching the project's 100-byte buffer

        while (true) {
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

        // Block until a packet arrives (Just like FireIncidentSubsystem)
        guiSocket.receive(receivePacket);

        // Convert and Clean padding
        String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
        String cleanMessage = received.trim();

        // Display in GUI
        logMessage("RECV: " + cleanMessage);


        }
        } catch (Exception e) {
        logMessage("GUI Socket Error: " + e.getMessage());
        }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    **/