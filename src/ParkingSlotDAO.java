import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ParkingSlotDAO {
    public void initializeSlots() throws SQLException {
        String selectSql = "SELECT COUNT(*) FROM parking_slots";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement(selectSql);
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next() && resultSet.getInt(1) == 20) {
                return;
            }
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String deleteSql = "DELETE FROM parking_slots";
                try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                    deleteStatement.executeUpdate();
                }

                String insertSql = "INSERT INTO parking_slots (slot_id, is_occupied, vehicle_id) VALUES (?, 0, NULL)";
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                    for (int slotId = 1; slotId <= 20; slotId++) {
                        insertStatement.setInt(1, slotId);
                        insertStatement.addBatch();
                    }
                    insertStatement.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void reserveSlot(int slotId, int vehicleId) throws SQLException {
        String sql = "UPDATE parking_slots SET is_occupied = 1, vehicle_id = ? WHERE slot_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, vehicleId);
            statement.setInt(2, slotId);
            statement.executeUpdate();
        }
    }

    public void releaseSlot(int slotId) throws SQLException {
        String sql = "UPDATE parking_slots SET is_occupied = 0, vehicle_id = NULL WHERE slot_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, slotId);
            statement.executeUpdate();
        }
    }

    public List<Integer> getOccupiedSlotsForVehicle(int vehicleId) throws SQLException {
        List<Integer> slots = new ArrayList<>();
        String sql = "SELECT slot_id FROM parking_slots WHERE vehicle_id = ? AND is_occupied = 1 ORDER BY slot_id";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, vehicleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    slots.add(resultSet.getInt("slot_id"));
                }
            }
        }
        return slots;
    }

    public int getOccupiedCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM parking_slots WHERE is_occupied = 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    public int getAvailableCount() throws SQLException {
        return 20 - getOccupiedCount();
    }
}
