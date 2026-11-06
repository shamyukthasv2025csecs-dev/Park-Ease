import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {
    public Ticket insertTicket(Ticket ticket) throws SQLException {
        String sql = "INSERT INTO parking_tickets (vehicle_id, ticket_number, entry_time, ticket_status) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, ticket.getVehicleId());
            statement.setString(2, ticket.getTicketNumber());
            statement.setTimestamp(3, Timestamp.valueOf(ticket.getEntryTime()));
            statement.setString(4, ticket.getTicketStatus());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    ticket.setTicketId(generatedKeys.getInt(1));
                    return ticket;
                }
            }
        }
        throw new SQLException("Ticket insertion failed.");
    }

    public void insertTicketSlot(int ticketId, int slotId) throws SQLException {
        String sql = "INSERT INTO ticket_slots (ticket_id, slot_id) VALUES (?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            statement.setInt(2, slotId);
            statement.executeUpdate();
        }
    }

    public Ticket findActiveTicketByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT ticket_id, vehicle_id, ticket_number, entry_time, exit_time, billable_hours, parking_fee, ticket_status " +
                "FROM parking_tickets WHERE vehicle_id = ? AND ticket_status = 'ACTIVE' ORDER BY ticket_id DESC LIMIT 1";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, vehicleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTicket(resultSet);
                }
            }
        }
        return null;
    }

    public Ticket findTicketByNumber(String ticketNumber) throws SQLException {
        String sql = "SELECT ticket_id, vehicle_id, ticket_number, entry_time, exit_time, billable_hours, parking_fee, ticket_status " +
                "FROM parking_tickets WHERE ticket_number = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ticketNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTicket(resultSet);
                }
            }
        }
        return null;
    }

    public void updateTicketOnExit(int ticketId, int billableHours, double parkingFee, LocalDateTime exitTime) throws SQLException {
        String sql = "UPDATE parking_tickets SET exit_time = ?, billable_hours = ?, parking_fee = ?, ticket_status = 'COMPLETED' WHERE ticket_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(exitTime));
            statement.setInt(2, billableHours);
            statement.setDouble(3, parkingFee);
            statement.setInt(4, ticketId);
            statement.executeUpdate();
        }
    }

    public List<Ticket> getParkingHistory() throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.ticket_id, t.vehicle_id, t.ticket_number, t.entry_time, t.exit_time, t.billable_hours, t.parking_fee, t.ticket_status, " +
                "v.vehicle_number, v.vehicle_type FROM parking_tickets t JOIN vehicles v ON t.vehicle_id = v.vehicle_id ORDER BY t.ticket_id DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Ticket ticket = mapTicket(resultSet);
                tickets.add(ticket);
            }
        }
        return tickets;
    }

    public String generateUniqueTicketNumber() throws SQLException {
        String prefix = "T-";
        int last = 1000;

        String sql = "SELECT MAX(CAST(SUBSTR(ticket_number, 3) AS UNSIGNED)) FROM parking_tickets";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next() && resultSet.getObject(1) != null) {
                last = resultSet.getInt(1);
            }
        }
        return prefix + (last + 1);
    }

    private Ticket mapTicket(ResultSet resultSet) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setTicketId(resultSet.getInt("ticket_id"));
        ticket.setVehicleId(resultSet.getInt("vehicle_id"));
        ticket.setTicketNumber(resultSet.getString("ticket_number"));
        Timestamp entry = resultSet.getTimestamp("entry_time");
        if (entry != null) {
            ticket.setEntryTime(entry.toLocalDateTime());
        }
        Timestamp exit = resultSet.getTimestamp("exit_time");
        if (exit != null) {
            ticket.setExitTime(exit.toLocalDateTime());
        }
        ticket.setBillableHours(resultSet.getInt("billable_hours"));
        ticket.setParkingFee(resultSet.getDouble("parking_fee"));
        ticket.setTicketStatus(resultSet.getString("ticket_status"));
        return ticket;
    }
}
