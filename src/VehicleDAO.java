import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class VehicleDAO {
    public int insertVehicle(Vehicle vehicle) throws SQLException {
        String sql = "INSERT INTO vehicles (vehicle_number, owner_name, vehicle_type) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, vehicle.getVehicleNumber());
            statement.setString(2, vehicle.getOwnerName());
            statement.setString(3, vehicle.getVehicleType().name());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Vehicle insert did not return a generated key.");
        }
    }

    public Vehicle findByVehicleNumber(String vehicleNumber) throws SQLException {
        String sql = "SELECT vehicle_id, vehicle_number, owner_name, vehicle_type FROM vehicles WHERE vehicle_number = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, vehicleNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Vehicle vehicle = new Vehicle(
                            resultSet.getString("vehicle_number"),
                            resultSet.getString("owner_name"),
                            VehicleType.valueOf(resultSet.getString("vehicle_type"))
                    );
                    vehicle.setVehicleId(resultSet.getInt("vehicle_id"));
                    return vehicle;
                }
            }
        }
        return null;
    }

    public boolean existsActiveVehicle(String vehicleNumber) throws SQLException {
        String sql = "SELECT COUNT(*) FROM parking_slots ps " +
                "JOIN vehicles v ON ps.vehicle_id = v.vehicle_id " +
                "WHERE v.vehicle_number = ? AND ps.is_occupied = 1";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, vehicleNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public Vehicle findById(int vehicleId) throws SQLException {
        String sql = "SELECT vehicle_id, vehicle_number, owner_name, vehicle_type FROM vehicles WHERE vehicle_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, vehicleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Vehicle vehicle = new Vehicle(
                            resultSet.getString("vehicle_number"),
                            resultSet.getString("owner_name"),
                            VehicleType.valueOf(resultSet.getString("vehicle_type"))
                    );
                    vehicle.setVehicleId(resultSet.getInt("vehicle_id"));
                    return vehicle;
                }
            }
        }
        return null;
    }
}
