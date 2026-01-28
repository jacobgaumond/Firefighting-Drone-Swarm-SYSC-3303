import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.*;
import static java.lang.Thread.sleep;

public class DroneGUI extends JFrame {
    private JTextArea logArea;
    private GridPanel mainGrid;
    public static final int DRONEGUI_PORT = 5002;
    //This is the mainframe in charge of organizing the values
    public DroneGUI() {
        setTitle("Group 2, Drone GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        //helper for static grid pieces
        GridPanel mainGrid = new GridPanel(21, 14);
        GridHelper.initializeZones(mainGrid);

        // Create the Right Side container (we'll define this below)
        JPanel rightSide = createRightSideContainer();

        add(mainGrid, BorderLayout.CENTER); //fills remaining space when growing/shrinking
        add(rightSide, BorderLayout.EAST);//stays to the rightside
        startListening();
    }

    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            // This is your fireIncident-style Receive Socket
            try (DatagramSocket guiSocket = new DatagramSocket(5002)) {
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

                /* If the GUI ever needs to "Reply" (like FireIncident confirms receipt),
                   you would add your send logic here using receivePacket.getAddress()
                */
                }
            } catch (Exception e) {
                logMessage("GUI Socket Error: " + e.getMessage());
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    /**
     * Organizes the right-side sidebar into the Legend of squares and the statistics
     */
    private JPanel createRightSideContainer() {
        JPanel sideWrapper = new JPanel(new GridBagLayout());
        sideWrapper.setPreferredSize(new Dimension(400, 0)); //stretch vertically
        sideWrapper.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, Color.LIGHT_GRAY));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; // stretches panel

        // The legend
        JPanel legendBar = createLegendPanel();
        gbc.gridy = 0;
        gbc.weighty = 0;
        sideWrapper.add(legendBar, gbc);

        // The logs
        JPanel bottomHalf = createLogsPanel();
        gbc.gridy = 1;
        gbc.weighty = 1.0; //this will stretch down
        sideWrapper.add(bottomHalf, gbc);

        return sideWrapper;
    }

    private JPanel createLogsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Logs"));

        // Initialize the class field here instead of a local variable
        logArea = new JTextArea();
        logArea.setEditable(false); // Recommended for logs
        logArea.setLineWrap(true);

        JScrollPane scroll = new JScrollPane(logArea);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            // Remove trailing X's and whitespace, then append
            String cleanMsg = message.replaceAll("X+$", "").trim();
            logArea.append(cleanMsg + "\n");

            // Auto-scroll to the bottom
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    /** The legend that was shown in the assignment*/
    private JPanel createLegendPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Legend"));
        p.setBackground(new Color(245, 245, 245));

        p.add(createLegendItem("Zone Label",new Color(158, 194, 211),"Z(n)"));
        p.add(createLegendItem("Active Fire", new Color(255,103, 95),""));
        p.add(createLegendItem("Extinguished Fire",new Color(130, 255, 95),""));
        p.add(createLegendItem("Drone Outbound", new Color(255,180, 95),"D(n)"));
        p.add(createLegendItem("Drone Extinguishing fire", new Color(106, 131, 95),"D(n)"));
        p.add(createLegendItem("Drone returning", new Color(201,95,255 ),"D(n)"));
        return p;
    }



    private JPanel createLegendItem(String text, Color color, String boxText) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setOpaque(false);

        // Boxes are labels to hold text
        JLabel box = new JLabel(boxText);
        box.setOpaque(true);
        box.setPreferredSize(new Dimension(30, 30)); // Size of the square
        box.setBackground(color);
        box.setForeground(Color.BLACK); // Make text readable against dark colors
        box.setHorizontalAlignment(SwingConstants.CENTER); // Center text horizontally
        box.setVerticalAlignment(SwingConstants.CENTER);   // Center text vertically
        box.setFont(new Font("SansSerif", Font.BOLD, 10));
        box.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        p.add(box);
        p.add(new JLabel(text)); // The descriptive text next to the box
        return p;
    }


/** This is the grid
 *  All moving drone components happen on the grid, grid is able to be resize
 *
 * **/

    public static class GridHelper {

        //creates all the zone labels in the corner of the zones
        public static void initializeZones(GridPanel panel) {
            Color zoneColor = new Color(158, 194, 211);
            panel.addSquareLabel(0, 0, "Z(1)", zoneColor);
            panel.addSquareLabel(7, 0, "Z(2)", zoneColor);
            panel.addSquareLabel(12, 0, "Z(5)", zoneColor);
            panel.addSquareLabel(0, 7, "Z(3)", zoneColor);
            panel.addSquareLabel(5, 7, "Z(4)", zoneColor);
        }
    }
    // Grid Panel creations
    class GridPanel extends JPanel {
        private final int cols;
        private final int rows;

        public GridPanel(int cols, int rows) {
            this.cols = cols;
            this.rows = rows;
            this.setLayout(null);
            this.setBackground(Color.WHITE);

            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    repositionComponents();
                }
            });
        }

        public void addSquareLabel(int gridX, int gridY, String text, Color bg) {
            addSquareLabel(gridX, gridY, text, bg, Color.BLACK);
        }


        /** This is used for the squares on the grid, they will represent zone numbers, fires and drones*/
        public void addSquareLabel(int gridX, int gridY, String text, Color bg, Color fg) {
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


        /** This is for the resize of all components on the Grid**/
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

        @Override/** This creaates the gridlines first making gridlines for the whole zone
         *and then making darker gridlines to represent each zon
         * */
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            Color lightGrid = new Color(83, 83, 83, 50);
            Color darkGrid = new Color(30, 30, 30);

            double unitW = (double) getWidth() / cols;
            double unitH = (double) getHeight() / rows;

            // Draw the total light grid
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

            /** Draw all the darker segments */

            int x12 = (int) (12 * unitW);
            int x7 = (int) (7 * unitW);
            int yHalf = (int) (7 * unitH);
            int x5 = (int) (5 * unitW);
            g2.setStroke(new BasicStroke(3));
            g2.setColor(darkGrid);

            g2.drawLine(x7, 0, x7, yHalf); //Z1 || Z2

            g2.drawLine(x5, yHalf, x5, getHeight());//Z3 || Z4

            g2.drawLine(x12, 0, x12, getHeight()); // Z2,Z4 || Z5

            g2.drawLine(0, yHalf, x12, yHalf);// (Z1, Z2) -- (Z3, Z4)

            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);//border
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DroneGUI gui = new DroneGUI();
            gui.setVisible(true); // Double-ensure it is visible
        });
    }
}