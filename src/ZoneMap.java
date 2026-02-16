import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ZoneMap {
    public static class Zone {
        public int startX, startY, endX, endY;

        public Zone(int startX, int startY, int endX, int endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
    }

    //mapping zoneId to Zone object
    private static Map<Integer, Zone> zones = new HashMap<>();


    //load zones
    public static void loadZones(String fileName) {
        try (Scanner scanner = new Scanner(new File(fileName))) {
            if (scanner.hasNextLine()) scanner.nextLine(); //skip header
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

                zones.put(zoneId, new Zone(startX, startY, endX, endY));
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

    //get the center of the zone X and for Y
    public static int getX(int zoneId) {
        Zone zone = zones.get(zoneId);
        return (zone.startX + zone.endX) / 2;
    }
    public static int getY(int zoneId) {
        Zone zone = zones.get(zoneId);
        return (zone.startY + zone.endY) / 2;
    }

}