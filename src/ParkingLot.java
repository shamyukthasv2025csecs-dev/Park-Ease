import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
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

    public synchronized Ticket parkVehicle(Vehicle vehicle) throws ParkingFullException, SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean[] available = new boolean[CAPACITY];
                String slotsSql = "SELECT slot_id, is_occupied FROM parking_slots ORDER BY slot_id FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(slotsSql);
                     ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        available[resultSet.getInt("slot_id") - 1] = !resultSet.getBoolean("is_occupied");
                    }
                }

                String activeSql = "SELECT COUNT(*) FROM parking_slots ps " +
                        "JOIN vehicles v ON ps.vehicle_id = v.vehicle_id " +
                        "WHERE v.vehicle_number = ? AND ps.is_occupied = 1";
                try (PreparedStatement statement = connection.prepareStatement(activeSql)) {
                    statement.setString(1, vehicle.getVehicleNumber());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next() && resultSet.getInt(1) > 0) {
                            throw new IllegalArgumentException("This vehicle is already parked and active.");
                        }
                    }
                }

                int[] slotIds = findAvailableSlot(available, vehicle.getVehicleType());
                if (slotIds == null) {
                    throw new ParkingFullException("Parking is full. No suitable slot is available.");
                }

                int vehicleId = findOrCreateVehicle(connection, vehicle);
                String ticketNumber = generateTicketNumber(connection);
                LocalDateTime entryTime = LocalDateTime.now();
                String ticketSql = "INSERT INTO parking_tickets " +
                        "(vehicle_id, ticket_number, entry_time, ticket_status) VALUES (?, ?, ?, 'ACTIVE')";
                int ticketId;
                try (PreparedStatement statement = connection.prepareStatement(ticketSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, vehicleId);
                    statement.setString(2, ticketNumber);
                    statement.setTimestamp(3, Timestamp.valueOf(entryTime));
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Ticket creation did not return an ID.");
                        }
                        ticketId = keys.getInt(1);
                    }
                }

                String reserveSql = "UPDATE parking_slots SET is_occupied = 1, vehicle_id = ? " +
                        "WHERE slot_id = ? AND is_occupied = 0";
                String ticketSlotSql = "INSERT INTO ticket_slots (ticket_id, slot_id) VALUES (?, ?)";
                try (PreparedStatement reserve = connection.prepareStatement(reserveSql);
                     PreparedStatement ticketSlot = connection.prepareStatement(ticketSlotSql)) {
                    for (int slotId : slotIds) {
                        reserve.setInt(1, vehicleId);
                        reserve.setInt(2, slotId);
                        if (reserve.executeUpdate() != 1) {
                            throw new SQLException("Parking slot " + slotId + " is no longer available.");
                        }
                        ticketSlot.setInt(1, ticketId);
                        ticketSlot.setInt(2, slotId);
                        ticketSlot.executeUpdate();
                    }
                }

                connection.commit();
                vehicle.setVehicleId(vehicleId);
                Ticket ticket = new Ticket();
                ticket.setTicketId(ticketId);
                ticket.setVehicleId(vehicleId);
                ticket.setTicketNumber(ticketNumber);
                ticket.setEntryTime(entryTime);
                ticket.setTicketStatus("ACTIVE");
                for (int slotId : slotIds) {
                    slots[slotId - 1] = vehicle;
                    ticket.addSlotNumber(slotId);
                }
                return ticket;
            } catch (SQLException | RuntimeException | ParkingFullException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public synchronized Ticket exitVehicle(String ticketNumber) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Ticket ticket;
                Vehicle vehicle;
                String ticketSql = "SELECT t.ticket_id, t.vehicle_id, t.ticket_number, t.entry_time, " +
                        "t.ticket_status, v.vehicle_number, v.owner_name, v.vehicle_type " +
                        "FROM parking_tickets t JOIN vehicles v ON t.vehicle_id = v.vehicle_id " +
                        "WHERE t.ticket_number = ? FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(ticketSql)) {
                    statement.setString(1, ticketNumber);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new IllegalArgumentException("Invalid ticket number.");
                        }
                        if (!"ACTIVE".equalsIgnoreCase(resultSet.getString("ticket_status"))) {
                            throw new IllegalArgumentException("This ticket is already completed.");
                        }
                        ticket = new Ticket();
                        ticket.setTicketId(resultSet.getInt("ticket_id"));
                        ticket.setVehicleId(resultSet.getInt("vehicle_id"));
                        ticket.setTicketNumber(resultSet.getString("ticket_number"));
                        ticket.setEntryTime(resultSet.getTimestamp("entry_time").toLocalDateTime());
                        vehicle = createVehicle(
                                resultSet.getString("vehicle_number"),
                                resultSet.getString("owner_name"),
                                VehicleType.valueOf(resultSet.getString("vehicle_type")));
                        vehicle.setVehicleId(ticket.getVehicleId());
                    }
                }

                LocalDateTime exitTime = LocalDateTime.now();
                long minutes = Duration.between(ticket.getEntryTime(), exitTime).toMinutes();
                int billableHours = Math.min((int) Math.ceil(minutes / 60.0), 8);
                double parkingFee = vehicle.calculateCharge(billableHours);

                String updateTicketSql = "UPDATE parking_tickets SET exit_time = ?, billable_hours = ?, " +
                        "parking_fee = ?, ticket_status = 'COMPLETED' WHERE ticket_id = ? AND ticket_status = 'ACTIVE'";
                try (PreparedStatement statement = connection.prepareStatement(updateTicketSql)) {
                    statement.setTimestamp(1, Timestamp.valueOf(exitTime));
                    statement.setInt(2, billableHours);
                    statement.setDouble(3, parkingFee);
                    statement.setInt(4, ticket.getTicketId());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Ticket status changed before exit could be completed.");
                    }
                }

                String paymentSql = "INSERT INTO payments (ticket_id, amount, payment_time) VALUES (?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(paymentSql)) {
                    statement.setInt(1, ticket.getTicketId());
                    statement.setDouble(2, parkingFee);
                    statement.setTimestamp(3, Timestamp.valueOf(exitTime));
                    statement.executeUpdate();
                }

                List<Integer> releasedSlots = new ArrayList<>();
                String findSlotsSql = "SELECT slot_id FROM parking_slots WHERE vehicle_id = ? AND is_occupied = 1 FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(findSlotsSql)) {
                    statement.setInt(1, ticket.getVehicleId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            releasedSlots.add(resultSet.getInt("slot_id"));
                        }
                    }
                }
                if (releasedSlots.isEmpty()) {
                    throw new SQLException("No occupied parking slots were found for this ticket.");
                }

                String releaseSql = "UPDATE parking_slots SET is_occupied = 0, vehicle_id = NULL " +
                        "WHERE vehicle_id = ? AND is_occupied = 1";
                try (PreparedStatement statement = connection.prepareStatement(releaseSql)) {
                    statement.setInt(1, ticket.getVehicleId());
                    statement.executeUpdate();
                }

                connection.commit();
                for (int slotId : releasedSlots) {
                    slots[slotId - 1] = null;
                }
                ticket.setExitTime(exitTime);
                ticket.setBillableHours(billableHours);
                ticket.setParkingFee(parkingFee);
                ticket.setTicketStatus("COMPLETED");
                return ticket;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    static int[] findAvailableSlot(boolean[] available, VehicleType vehicleType) {
        int slotsNeeded = vehicleType.getSlotsNeeded();
        for (int i = 0; i <= CAPACITY - slotsNeeded; i++) {
            boolean candidateAvailable = true;
            for (int j = 0; j < slotsNeeded; j++) {
                if (!available[i + j]) {
                    candidateAvailable = false;
                    break;
                }
            }
            if (candidateAvailable) {
                if (vehicleType == VehicleType.TRUCK) {
                    return new int[] { i + 1, i + 2 };
                }
                return new int[] { i + 1 };
            }
        }
        return null;
    }

    private int findOrCreateVehicle(Connection connection, Vehicle vehicle) throws SQLException {
        String findSql = "SELECT vehicle_id FROM vehicles WHERE vehicle_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(findSql)) {
            statement.setString(1, vehicle.getVehicleNumber());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int vehicleId = resultSet.getInt("vehicle_id");
                    String updateSql = "UPDATE vehicles SET owner_name = ?, vehicle_type = ? WHERE vehicle_id = ?";
                    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                        update.setString(1, vehicle.getOwnerName());
                        update.setString(2, vehicle.getVehicleType().name());
                        update.setInt(3, vehicleId);
                        update.executeUpdate();
                    }
                    return vehicleId;
                }
            }
        }

        String insertSql = "INSERT INTO vehicles (vehicle_number, owner_name, vehicle_type) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, vehicle.getVehicleNumber());
            statement.setString(2, vehicle.getOwnerName());
            statement.setString(3, vehicle.getVehicleType().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Vehicle insert did not return an ID.");
    }

    private String generateTicketNumber(Connection connection) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTR(ticket_number, 3) AS UNSIGNED)), 1000) + 1 " +
                "FROM parking_tickets";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return "T-" + resultSet.getInt(1);
            }
        }
        throw new SQLException("Ticket number could not be generated.");
    }

    private Vehicle createVehicle(String vehicleNumber, String ownerName, VehicleType vehicleType) {
        return switch (vehicleType) {
            case BIKE -> new Bike(vehicleNumber, ownerName);
            case CAR -> new Car(vehicleNumber, ownerName);
            case TRUCK -> new Truck(vehicleNumber, ownerName);
        };
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
