import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {
    public void insertPayment(int ticketId, double amount, LocalDateTime paymentTime) throws SQLException {
        String sql = "INSERT INTO payments (ticket_id, amount, payment_time) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, ticketId);
            statement.setDouble(2, amount);
            statement.setTimestamp(3, Timestamp.valueOf(paymentTime));
            statement.executeUpdate();
        }
    }

    public double getTotalRevenue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getDouble(1);
            }
        }
        return 0;
    }

    public List<String> getPaymentHistory() throws SQLException {
        List<String> payments = new ArrayList<>();
        String sql = "SELECT p.payment_id, t.ticket_number, p.amount, p.payment_time " +
                "FROM payments p JOIN parking_tickets t ON p.ticket_id = t.ticket_id ORDER BY p.payment_id DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                payments.add("Payment ID: " + resultSet.getInt("payment_id") +
                        " | Ticket: " + resultSet.getString("ticket_number") +
                        " | Amount: Rs. " + resultSet.getDouble("amount") +
                        " | Time: " + resultSet.getTimestamp("payment_time"));
            }
        }
        return payments;
    }
}
