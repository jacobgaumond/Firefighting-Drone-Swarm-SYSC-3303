import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.swing.*;

public class DroneGUI extends JFrame {
    private JPanel gridPanel;
    private JPanel droneOverlay; 
    private JTextArea logArea;

    private int activeFires;
    private JLabel activeFireLabel;
    
    private int cols;
    private int rows;
    private int cellWidth;
    private int cellHeight;
    
    private Map<Integer, JLabel> fireLabels = new HashMap<>();
    private Map<Integer, JLabel> droneLabels = new HashMap<>();
    private Map<Integer, Thread> droneAnimationThreads = new HashMap<>();
    private final static int animationDelay = 20;
    private final static int buffer = 120; 
    
    private final static Color activeFireColor = new Color(255, 0, 0);
    private final static Color extinguishedFireColor = new Color(77, 167, 46);
        
    private final static Color droneOutboundColor = new Color(255, 191, 0);
    private final static Color droneExtinguishingColor = new Color(77, 167, 46);
    private final static Color droneReturningColor = new Color(216, 109, 205);

    public static void main(String[] args) {
        String zoneFileName = "src/data/Sample_zone_file.csv"; // TODO: Fix ZoneMap
        ZoneMap.loadZones(zoneFileName);
        ZoneMap.printZones();

        DroneGUI gui = new DroneGUI();
        gui.setVisible(true);
    }

    // GUI Constructor
    public DroneGUI() {
        setTitle("Group 2, Drone GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Calculate grid dimensions using zones maxes
        int maxX = 0, maxY = 0;
        for (ZoneMap.Zone zone : ZoneMap.getAllZones().values()) {
            if (zone != null) {
                maxX = Math.max(maxX, zone.endX / 100);
                maxY = Math.max(maxY, zone.endY / 100);
            }
        }
        this.cols = maxX;
        this.rows = maxY;
        
        // Pane to hold both grid and drone overlay
        JLayeredPane layeredPane = new JLayeredPane();
        
        // Create grid
        this.gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                DroneGUI.this.paintGrid(g);
            }
        };
        gridPanel.setLayout(null);
        gridPanel.setBackground(Color.WHITE);
        layeredPane.add(gridPanel, Integer.valueOf(0));   
        
        // Create drone overlay
        this.droneOverlay = new JPanel();
        droneOverlay.setLayout(null);
        droneOverlay.setOpaque(false);
        layeredPane.add(droneOverlay, Integer.valueOf(1));
        
        // Add resize listener to layered pane
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Resize both panels to match layered pane
                gridPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                droneOverlay.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                
                // Update cell dimensions
                cellWidth = gridPanel.getWidth() / cols;
                cellHeight = gridPanel.getHeight() / rows;
                
                repositionComponents();
            }
        });
        
        // create zones
        initializeZones(); 

        // create sidebar
        JPanel sideBar = createSidebar();

        // add to UI
        add(layeredPane, BorderLayout.CENTER); 
        add(sideBar, BorderLayout.EAST);
    }

    // ========== UI COMPONENTS ==========
    // Zones
    public void initializeZones() {
        // For each Zone in ZoneMap
        for (Entry<Integer, ZoneMap.Zone> entry : ZoneMap.getAllZones().entrySet()) {
            int zoneId = entry.getKey();
            ZoneMap.Zone zone = entry.getValue();
            
            int gridStartX = zone.startX / 100;
            int gridStartY = zone.startY / 100;
            
            // Add zone label at grid's first cell
            this.createZoneLabel(gridStartX, gridStartY, "Z(" + zoneId + ")");
            
            // Add fire label at center of zone
            this.createFireLabel(zoneId);
        }
    }

    // Zone Label
    public void createZoneLabel(int gridX, int gridY, String text) {
        JLabel label = new JLabel(text);
        
        label.setOpaque(true);
        label.setBackground(new Color(158, 194, 211));
        label.setForeground(Color.BLACK);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        label.putClientProperty("gridX", gridX);
        label.putClientProperty("gridY", gridY);

        this.gridPanel.add(label);
    }

    // Fire Label
    public void createFireLabel(int zoneId) {
        JLabel label = new JLabel();

        label.setOpaque(true);
        label.setBackground(extinguishedFireColor);
        label.setForeground(Color.BLACK);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setText("");

        // Center cell
        label.putClientProperty("gridX", ZoneMap.getX(zoneId) / 100);
        label.putClientProperty("gridY", ZoneMap.getY(zoneId) / 100);

        // Hashmap for future referencing 
        fireLabels.put(zoneId, label);

        this.gridPanel.add(label);
    }

    // Drone Label
    public void createDroneLabel(int droneId) {
        JLabel label = new JLabel("D(" + droneId + ")");
        
        label.setOpaque(true);
        label.setBackground(droneOutboundColor);
        label.setForeground(Color.BLACK);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        // Start invisible at (0,0)
        label.setVisible(false);
        label.setBounds(0, 0, cellWidth, cellHeight);
        
        // Hashmap for future referencing 
        droneLabels.put(droneId, label);
        
        this.droneOverlay.add(label);
        this.logMessage("Drone " + droneId + " created at (0,0)");
    }

    // Dynamic components resizing
    private void repositionComponents() {
        if (this.gridPanel == null) return;
        
        // Update cell dimensions
        cellWidth = this.gridPanel.getWidth() / cols;
        cellHeight = this.gridPanel.getHeight() / rows;

        // Fix grid
        for (Component c : this.gridPanel.getComponents()) {
            if (c instanceof JLabel) {
                int gx = (int) ((Integer) ((JLabel) c).getClientProperty("gridX"));
                int gy = (int) ((Integer) ((JLabel) c).getClientProperty("gridY"));
                c.setBounds(gx * cellWidth, gy * cellHeight, cellWidth, cellHeight);
            }
        }
        
        // fix drone labels
        for (Component c : this.droneOverlay.getComponents()) {
            if (c instanceof JLabel) {
                c.setSize(cellWidth, cellHeight);
            }
        }
    }

    // Draw gridlines and zone boundaries
    private void paintGrid(Graphics g) {
        if (this.gridPanel == null) return;
        
        Graphics2D g2 = (Graphics2D) g;

        Color lightGrid = new Color(83, 83, 83, 50);
        Color darkGrid = new Color(30, 30, 30);

        // Whole Grid
        g2.setStroke(new BasicStroke(1));
        g2.setColor(lightGrid);
        for (int i = 0; i <= cols; i++) {
            int x = (i * cellWidth);
            g2.drawLine(x, 0, x, this.gridPanel.getHeight());
        }
        for (int i = 0; i <= rows; i++) {
            int y = (i * cellHeight);
            g2.drawLine(0, y, this.gridPanel.getWidth(), y);
        }
        g2.setStroke(new BasicStroke(3));
        g2.setColor(darkGrid);

        // Zone boundaries
        for (ZoneMap.Zone zone : ZoneMap.getAllZones().values()) {
            int x1 = ((zone.startX / 100) * cellWidth);
            int y1 = ((zone.startY / 100) * cellHeight);
            int x2 = ((zone.endX / 100) * cellWidth);
            int y2 = ((zone.endY / 100) * cellHeight);
            
            g2.drawRect(x1, y1, x2 - x1, y2 - y1);
        }

        g2.drawRect(0, 0, this.gridPanel.getWidth() - 1, this.gridPanel.getHeight() - 1);
    }

    // Right-side sidebar
    private JPanel createSidebar() {
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
        p.add(createLegendItem("Active Fire", activeFireColor, ""));
        p.add(createLegendItem("Extinguished Fire", extinguishedFireColor, ""));
        p.add(createLegendItem("Drone Outbound", droneOutboundColor, "D(n)"));
        p.add(createLegendItem("Drone Extinguishing fire", droneExtinguishingColor, "D(n)"));
        p.add(createLegendItem("Drone returning", droneReturningColor, "D(n)"));
        return p;
    }
    private JPanel createLegendItem(String text, Color color, String boxText) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setOpaque(false);

        JLabel box = new JLabel(boxText);
        box.setOpaque(true);
        box.setPreferredSize(new Dimension(30, 30));
        box.setBackground(color);
        box.setForeground(Color.BLACK);
        box.setHorizontalAlignment(SwingConstants.CENTER); 
        box.setVerticalAlignment(SwingConstants.CENTER); 
        box.setFont(new Font("SansSerif", Font.BOLD, 10));
        box.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        p.add(box);
        p.add(new JLabel(text)); // descriptive text next to box

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

        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);
        logArea.setLineWrap(true);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    // ========== METHODS ==========
    // Log a message
    public void logMessage(String message) {
        // Remove trailing X's and whitespace, then append
        String cleanMsg = message.replaceAll("X+$", "").trim();
        logArea.append(cleanMsg + "\n");

        // Auto-scroll to the bottom
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // Update fire status
    public void fireStatusChange(int zoneId, String fireLevel) {
        JLabel fireLabel = fireLabels.get(zoneId);
        if (fireLabel == null) return;
        
        if (Objects.equals(fireLevel, "")) { 
            // Extinguished
            fireLabel.setBackground(extinguishedFireColor);
            fireLabel.setText(fireLevel);

            if (activeFires > 0) activeFires--;
            logMessage("Fire in Zone " + zoneId + " extinguished");
        } else {
            // Active
            fireLabel.setBackground(activeFireColor);

            if(fireLabel.getText().equals("")){
                activeFires++; // new fire, not update
            }

            fireLabel.setText(fireLevel);

            logMessage("Fire detected in Zone " + zoneId + " (Level: " + fireLevel + ")");
        }
        activeFireLabel.setText("Active Fires: " + activeFires);
    }
    
    // Update drone position (animate movement)
    public void updateDronePosition(int droneId, int pixelX, int pixelY) {
        JLabel droneLabel = droneLabels.get(droneId);
        if (droneLabel == null) return;
        
        droneLabel.setLocation(pixelX, pixelY);
    }
    
    // Send drone to zone
    public void moveDroneToZone(int droneId, int zoneId, long travelTime, int fluidRemaining) {
        JLabel droneLabel = droneLabels.get(droneId);
        if (droneLabel == null) return;
        
        // Cancel current animation
        Thread existingThread = droneAnimationThreads.get(droneId);
        if (existingThread != null && existingThread.isAlive()) {
            existingThread.interrupt();
        }
        
        // Set to outbound color
        droneLabel.setVisible(true);
        droneLabel.setBackground(droneOutboundColor);
        
        logMessage("Drone " + droneId + " outbound to Zone " + zoneId + "| fluid remaining: " + fluidRemaining);
        
        // Current position (important later when interrupted on return)
        int startX = droneLabel.getX();
        int startY = droneLabel.getY();
        
        // Calculate target zone center
        int targetX = (ZoneMap.getX(zoneId) / 100) * cellWidth; // centering
        int targetY = (ZoneMap.getY(zoneId) / 100) * cellHeight;
        
        // Animation based on travel time
        final int steps = (int) Math.max(1, (travelTime-buffer) / animationDelay);  // frames needed to match travel time
        
        // Thread for asynchronous animation
        Thread animationThread = new Thread(() -> {
            for (int step = 0; step <= steps; step++) {
                // animation completion %
                double progress = (double) step / steps;
                
                // step
                int newX = (int) (startX + (targetX - startX) * progress);
                int newY = (int) (startY + (targetY - startY) * progress);
                
                updateDronePosition(droneId, newX, newY);
                
                try {
                    Thread.sleep(animationDelay); // delay between animation steps
                } catch (InterruptedException e) {
                    break; // stop animation if interrupted
                }
            }
            droneAnimationThreads.remove(droneId);
        });
        // reference in case of needed interruption
        droneAnimationThreads.put(droneId, animationThread);
        animationThread.start();
    }
    
    // Extinguish fire
    public void extinguishFire(int droneId, int zoneId, long dropTime) {
        JLabel droneLabel = droneLabels.get(droneId);
        if (droneLabel == null) return;
        
        // Cancel current animation
        Thread existingThread = droneAnimationThreads.get(droneId);
        if (existingThread != null && existingThread.isAlive()) {
            existingThread.interrupt();
        }
        
        // Change color to extinguishing
        droneLabel.setBackground(droneExtinguishingColor);
        logMessage("Drone " + droneId + " extinguishing fire");
        
        // Wait for drop time
        Thread animationThread = new Thread(() -> {
            try {
                Thread.sleep(dropTime);
            } catch (InterruptedException e) {}
            droneAnimationThreads.remove(droneId);
        });
        // reference in case of needed interruption
        droneAnimationThreads.put(droneId, animationThread);
        animationThread.start();
    }
    
    // Return drone to origin
    public void returnDrone(int droneId, long travelTime) {
        JLabel droneLabel = droneLabels.get(droneId);
        if (droneLabel == null) return;
        
        // Cancel current animation
        Thread existingThread = droneAnimationThreads.get(droneId);
        if (existingThread != null && existingThread.isAlive()) {
            existingThread.interrupt();
        }
        
        // Set returning color
        droneLabel.setVisible(true);
        droneLabel.setBackground(droneReturningColor);
        
        logMessage("Drone " + droneId + " returning to origin");
        
        // Current position
        int startX = droneLabel.getX();
        int startY = droneLabel.getY();
        
        // Target is origin
        int targetX = 0;
        int targetY = 0;
        
        // Animation based on travel time
        final int steps = (int) Math.max(1, (travelTime-buffer) / animationDelay); 
        
        // Thread for asynchronous animation
        Thread animationThread = new Thread(() -> {
            for (int step = 0; step <= steps; step++) {
                // animation completion %
                double progress = (double) step / steps;
                
                // step
                int newX = (int) (startX + (targetX - startX) * progress);
                int newY = (int) (startY + (targetY - startY) * progress);
                
                updateDronePosition(droneId, newX, newY);
                
                // onComplete
                if (step >= steps) {
                    droneLabel.setVisible(false);
                }
                
                try {
                    Thread.sleep(animationDelay); // delay between animation steps
                } catch (InterruptedException e) {
                    break; // Stop animation if interrupted
                }
            }
            droneAnimationThreads.remove(droneId);
        });
        // reference in case of needed interruption
        droneAnimationThreads.put(droneId, animationThread);
        animationThread.start();
    }

    // ========== GETTERS ==========
    // Get the current position of a drone (potentially in transit)
    public int[] getCurrentDronePosition(int droneId) {
        JLabel droneLabel = droneLabels.get(droneId);
        
        int x = (int) ((droneLabel.getX() / cellWidth) * 100);
        int y = (int) ((droneLabel.getY() / cellHeight) * 100);
        
        return new int[]{x, y};
    }
}