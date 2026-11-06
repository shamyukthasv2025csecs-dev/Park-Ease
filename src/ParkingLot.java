import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ParkingLot {
    public static final int CAPACITY = 20;

    private final Vehicle[] slots = new Vehicle[CAPACITY];
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ParkingSlotDAO parkingSlotDAO = new ParkingSlotDAO();
    private final TicketDAO ticketDAO = new TicketDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    public ParkingLot() {
        try {
            parkingSlotDAO.initializeSlots();
            loadOccupiedSlotsFromDatabase();
        } catch (SQLException e) {
            System.out.println("Database warning: slot initialization skipped. " + e.getMessage());
        }
    }

    private void loadOccupiedSlotsFromDatabase() throws SQLException {
        String sql = "SELECT slot_id, vehicle_id FROM parking_slots WHERE is_occupied = 1";
        try (java.sql.Connection connection = DatabaseConnection.getConnection();
             java.sql.PreparedStatement statement = connection.prepareStatement(sql);
             java.sql.ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int slotId = resultSet.getInt("slot_id");
                int vehicleId = resultSet.getInt("vehicle_id");
                Vehicle vehicle = vehicleDAO.findById(vehicleId);
                if (vehicle != null) {
                    slots[slotId - 1] = vehicle;
                }
            }
        }
    }

    public Ticket parkVehicle(Vehicle vehicle) throws ParkingFullException, SQLException {
        if (vehicleDAO.existsActiveVehicle(vehicle.getVehicleNumber())) {
            throw new IllegalArgumentException("This vehicle is already parked and active.");
        }

        int[] slotIndexes = findAvailableSlot(vehicle.getVehicleType());
        if (slotIndexes == null) {
            throw new ParkingFullException("Parking is full. No suitable slot is available.");
        }

        int vehicleId = vehicleDAO.insertVehicle(vehicle);
        vehicle.setVehicleId(vehicleId);

        Ticket ticket = new Ticket();
        ticket.setVehicleId(vehicleId);
        ticket.setTicketNumber(ticketDAO.generateUniqueTicketNumber());
        ticket.setEntryTime(LocalDateTime.now());
        ticket.setTicketStatus("ACTIVE");
        ticket = ticketDAO.insertTicket(ticket);

        for (int slotIndex : slotIndexes) {
            slots[slotIndex] = vehicle;
            parkingSlotDAO.reserveSlot(slotIndex + 1, vehicleId);
            ticket.addSlotNumber(slotIndex + 1);
            ticketDAO.insertTicketSlot(ticket.getTicketId(), slotIndex + 1);
        }

        return ticket;
    }

    public Ticket exitVehicle(String ticketNumber) throws SQLException {
        Ticket ticket = ticketDAO.findTicketByNumber(ticketNumber);
        if (ticket == null) {
            throw new IllegalArgumentException("Invalid ticket number.");
        }
        if (!"ACTIVE".equalsIgnoreCase(ticket.getTicketStatus())) {
            throw new IllegalArgumentException("This ticket is already completed.");
        }

        Vehicle vehicle = vehicleDAO.findById(ticket.getVehicleId());
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle information could not be found.");
        }

        LocalDateTime exitTime = LocalDateTime.now();
        long minutes = Duration.between(ticket.getEntryTime(), exitTime).toMinutes();
        int actualHours = (int) Math.ceil(minutes / 60.0);
        int billableHours = Math.min(actualHours, 8);
        double parkingFee = vehicle.calculateCharge(billableHours);

        ticketDAO.updateTicketOnExit(ticket.getTicketId(), billableHours, parkingFee, exitTime);
        paymentDAO.insertPayment(ticket.getTicketId(), parkingFee, exitTime);

        for (int slotId : parkingSlotDAO.getOccupiedSlotsForVehicle(ticket.getVehicleId())) {
            parkingSlotDAO.releaseSlot(slotId);
            slots[slotId - 1] = null;
        }

        ticket.setExitTime(exitTime);
        ticket.setBillableHours(billableHours);
        ticket.setParkingFee(parkingFee);
        ticket.setTicketStatus("COMPLETED");
        return ticket;
    }

    private int[] findAvailableSlot(VehicleType vehicleType) {
        int slotsNeeded = vehicleType.getSlotsNeeded();
        for (int i = 0; i <= CAPACITY - slotsNeeded; i++) {
            boolean available = true;
            for (int j = 0; j < slotsNeeded; j++) {
                if (slots[i + j] != null) {
                    available = false;
                    break;
                }
            }
            if (available) {
                if (vehicleType == VehicleType.TRUCK) {
                    return new int[] { i, i + 1 };
                }
                return new int[] { i };
            }
        }
        return null;
    }

    public String getSlotMap() {
        StringBuilder builder = new StringBuilder();
        builder.append("Slot Map\n");
        builder.append("----------------------------------------\n");
        for (int i = 0; i < CAPACITY; i++) {
            Vehicle vehicle = slots[i];
            if (vehicle == null) {
                builder.append(String.format("%02d: Available\n", (i + 1)));
            } else {
                builder.append(String.format("%02d: %s (%s)\n",
                        (i + 1), vehicle.getVehicleNumber(), vehicle.getVehicleType()));
            }
        }
        return builder.toString();
    }

    public String getSummary() {
        int occupied = 0;
        for (Vehicle slot : slots) {
            if (slot != null) {
                occupied++;
            }
        }
        int available = CAPACITY - occupied;
        int completedSessions = 0;

        try {
            for (Ticket ticket : ticketDAO.getParkingHistory()) {
                if ("COMPLETED".equalsIgnoreCase(ticket.getTicketStatus())) {
                    completedSessions++;
                }
            }
        } catch (SQLException ignored) {
        }

        double revenue;
        try {
            revenue = paymentDAO.getTotalRevenue();
        } catch (SQLException e) {
            revenue = 0;
        }

        return "Total Capacity: " + CAPACITY + "\n" +
                "Occupied Slots: " + occupied + "\n" +
                "Available Slots: " + available + "\n" +
                "Vehicles Currently Parked: " + occupied + "\n" +
                "Completed Parking Sessions: " + completedSessions + "\n" +
                "Revenue Collected: Rs. " + revenue;
    }

    public List<String> getParkingHistory() {
        List<String> history = new ArrayList<>();
        try {
            for (Ticket ticket : ticketDAO.getParkingHistory()) {
                history.add("Ticket: " + ticket.getTicketNumber() +
                        " | Vehicle: " + vehicleDAO.findById(ticket.getVehicleId()).getVehicleNumber() +
                        " | Type: " + vehicleDAO.findById(ticket.getVehicleId()).getVehicleType() +
                        " | Entry: " + ticket.getEntryTime() +
                        " | Exit: " + (ticket.getExitTime() == null ? "-" : ticket.getExitTime()) +
                        " | Fee: Rs. " + ticket.getParkingFee() +
                        " | Status: " + ticket.getTicketStatus());
            }
        } catch (SQLException e) {
            history.add("Database error while loading parking history: " + e.getMessage());
        }
        return history;
    }

    public List<String> getPaymentHistory() {
        List<String> history = new ArrayList<>();
        try {
            history.addAll(paymentDAO.getPaymentHistory());
        } catch (SQLException e) {
            history.add("Database error while loading payment history: " + e.getMessage());
        }
        return history;
    }
}
