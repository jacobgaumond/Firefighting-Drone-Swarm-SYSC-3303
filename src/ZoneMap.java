import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ZoneMap {

    // mapping zoneId to Zone object
    private static Map<Integer, Zone> zones = new HashMap<>();

    public static class Zone {

        public int startX, startY, endX, endY;

        public Zone(int startX, int startY, int endX, int endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
    }

    // load zones
    public static void loadZones(String fileName) {
        try (Scanner scanner = new Scanner(new File(fileName))) {
            if (scanner.hasNextLine()) {
                scanner.nextLine(); // skip header
            }
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");

                int zoneId = Integer.parseInt(parts[0]);

                String[] start = parts[1].replace("(", "").replace(")", "").split(";");
                int startX = Integer.parseInt(start[0]);
                int startY = Integer.parseInt(start[1]);

                String[] end = parts[2].replace("(", "").replace(")", "").split(";");
                int endX = Integer.parseInt(end[0]);
                int endY = Integer.parseInt(end[1]);

                // add zone to HashMap
                addZone(zoneId, new Zone(startX, startY, endX, endY));
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error loading zone file: " + fileName + " - " + e.getMessage());
            System.exit(1);
        }
    }

    //adding a zone
    public static void addZone(int id, Zone zone) {
        zones.put(id, zone);
    }

    public static Zone getZone(int zoneId) {
        Zone zone = zones.get(zoneId);
        if (zone == null) {
            System.out.println("Zone with id " + zoneId + " not found");
        }
        return zone;
    }

    // get all zones
    public static Map<Integer, Zone> getAllZones() {
        return zones;
    }

    //get the center of the zone X and for Y
    public static int getX(int zoneId) {
        Zone zone = getZone(zoneId);
        return (zone.startX + zone.endX) / 2;
    }

    public static int getY(int zoneId) {
        Zone zone = getZone(zoneId);
        return (zone.startY + zone.endY) / 2;
    }

    // print all zones
    public static void printZones() {
        if (zones.isEmpty()) {
            System.out.println("No zones loaded.");
            return;
        }

        System.out.println("-------------Zones Loaded--------------");
        for (Map.Entry<Integer, Zone> entry : zones.entrySet()) {
            int id = entry.getKey();
            Zone z = entry.getValue();
            System.out.println("Zone ID: " + id
                    + ", Start: (" + z.startX + ";" + z.startY + ")"
                    + ", End: (" + z.endX + ";" + z.endY + ")"
                    + ", Center: (" + getX(id) + ";" + getY(id) + ")");
        }
        System.out.println("=========================");
    }
}
