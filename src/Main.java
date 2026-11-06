import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ParkingLot parkingLot = new ParkingLot();

        while (true) {
            System.out.println();
            System.out.println("========== PARKEASE ==========");
            System.out.println("1. Park Vehicle");
            System.out.println("2. Exit Vehicle");
            System.out.println("3. View Parking Slot Map");
            System.out.println("4. View Summary");
            System.out.println("5. View Parking History");
            System.out.println("6. View Payment History");
            System.out.println("7. Quit");
            System.out.print("Enter your choice: ");

            String choiceInput = scanner.nextLine();
            int choice;

            try {
                choice = Integer.parseInt(choiceInput.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid menu choice. Please enter a number from 1 to 7.");
                continue;
            }

            try {
                switch (choice) {
                    case 1 -> parkVehicleFlow(scanner, parkingLot);
                    case 2 -> exitVehicleFlow(scanner, parkingLot);
                    case 3 -> System.out.println(parkingLot.getSlotMap());
                    case 4 -> System.out.println(parkingLot.getSummary());
                    case 5 -> displayHistory(parkingLot.getParkingHistory());
                    case 6 -> displayHistory(parkingLot.getPaymentHistory());
                    case 7 -> {
                        System.out.println("Thank you for using ParkEase. Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid menu choice. Please choose between 1 and 7.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (ParkingFullException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private static void parkVehicleFlow(Scanner scanner, ParkingLot parkingLot) throws ParkingFullException, SQLException {
        System.out.print("Vehicle type (1-Bike, 2-Car, 3-Truck): ");
        String typeInput = scanner.nextLine().trim();

        VehicleType vehicleType;
        try {
            int typeChoice = Integer.parseInt(typeInput);
            switch (typeChoice) {
                case 1 -> vehicleType = VehicleType.BIKE;
                case 2 -> vehicleType = VehicleType.CAR;
                case 3 -> vehicleType = VehicleType.TRUCK;
                default -> throw new IllegalArgumentException("Invalid vehicle type selected.");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Vehicle type must be a number.");
        }

        System.out.print("Vehicle registration number: ");
        String vehicleNumber = scanner.nextLine().trim();
        if (!isValidVehicleNumber(vehicleNumber)) {
            throw new IllegalArgumentException("Invalid registration number. Example: TN37AB1234");
        }

        System.out.print("Owner name: ");
        String ownerName = scanner.nextLine().trim();
        if (ownerName.isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be empty.");
        }

        Vehicle vehicle = createVehicle(vehicleType, vehicleNumber, ownerName);
        Ticket ticket = parkingLot.parkVehicle(vehicle);
        System.out.println("Ticket generated successfully.");
        System.out.println("Ticket Number: " + ticket.getTicketNumber());
        System.out.println("Allocated Slot(s): " + ticket.getSlotNumbers());
    }

    private static void exitVehicleFlow(Scanner scanner, ParkingLot parkingLot) throws SQLException {
        System.out.print("Ticket number: ");
        String ticketNumber = scanner.nextLine().trim();
        Ticket ticket = parkingLot.exitVehicle(ticketNumber);
        System.out.println("Parking completed successfully.");
        System.out.println("Ticket Number: " + ticket.getTicketNumber());
        System.out.println("Parking Fee: Rs. " + ticket.getParkingFee());
        System.out.println("Billable Hours: " + ticket.getBillableHours());
    }

    private static void displayHistory(List<String> records) {
        if (records == null || records.isEmpty()) {
            System.out.println("No records found.");
            return;
        }

        for (String record : records) {
            System.out.println(record);
        }
    }

    private static boolean isValidVehicleNumber(String vehicleNumber) {
        if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
            return false;
        }

        String value = vehicleNumber.trim();
        if (value.length() < 8 || value.length() > 12) {
            return false;
        }

        int letterCount = 0;
        int digitCount = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isLetter(ch)) {
                letterCount++;
            } else if (Character.isDigit(ch)) {
                digitCount++;
            } else {
                return false;
            }
        }

        return letterCount > 0 && digitCount > 0;
    }

    private static Vehicle createVehicle(VehicleType vehicleType, String vehicleNumber, String ownerName) {
        switch (vehicleType) {
            case BIKE:
                return new Bike(vehicleNumber, ownerName);
            case CAR:
                return new Car(vehicleNumber, ownerName);
            case TRUCK:
                return new Truck(vehicleNumber, ownerName);
            default:
                throw new IllegalArgumentException("Unsupported vehicle type.");
        }
    }
}




/*
cd C:\ParkEase\Park-Ease
javac -cp "lib\mysql-connector-j-8.4.0.jar" -d out src\*.java
java -cp "out;lib\mysql-connector-j-8.4.0.jar" WebServer
 */