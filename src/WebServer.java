import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebServer {
    private static final int PORT = 8080;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final ParkingLot parkingLot;

    private WebServer(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    public static void main(String[] args) throws IOException, SQLException {
        try (Connection ignored = DatabaseConnection.getConnection()) {
            // Confirm the database connection before opening the website.
        }

        WebServer application = new WebServer(new ParkingLot());
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/", application::handleRequest);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("ParkEase is running at http://localhost:" + PORT);
        System.out.println("Keep this terminal open while using the website. Press Ctrl+C to stop.");
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if ("/styles.css".equals(path) && "GET".equals(exchange.getRequestMethod())) {
                serveStylesheet(exchange);
            } else if ("/".equals(path) && "GET".equals(exchange.getRequestMethod())) {
                sendHtml(exchange, renderPage(""));
            } else if (("/park".equals(path) || "/exit".equals(path))
                    && "POST".equals(exchange.getRequestMethod())) {
                handleForm(exchange, path);
            } else {
                sendHtml(exchange, renderErrorPage("Page not found."), 404);
            }
        } catch (SQLException e) {
            sendHtml(exchange, renderErrorPage("Database error: " + e.getMessage()), 500);
        } catch (IllegalArgumentException e) {
            sendHtml(exchange, renderErrorPage(e.getMessage()), 400);
        } catch (ParkingFullException e) {
            sendHtml(exchange, renderErrorPage(e.getMessage()), 409);
        } finally {
            exchange.close();
        }
    }

    private void handleForm(HttpExchange exchange, String path) throws IOException, SQLException, ParkingFullException {
        Map<String, String> form = parseForm(exchange);
        if ("/park".equals(path)) {
            String typeValue = form.getOrDefault("vehicleType", "").trim();
            VehicleType type;
            try {
                type = VehicleType.valueOf(typeValue);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Select a valid vehicle type.");
            }

            String vehicleNumber = form.getOrDefault("vehicleNumber", "").trim().toUpperCase();
            if (!isValidVehicleNumber(vehicleNumber)) {
                throw new IllegalArgumentException("Enter a valid registration number, for example TN37AB1234.");
            }
            String ownerName = form.getOrDefault("ownerName", "").trim();
            if (ownerName.isEmpty() || ownerName.length() > 100) {
                throw new IllegalArgumentException("Enter an owner name between 1 and 100 characters.");
            }

            Vehicle vehicle = createVehicle(type, vehicleNumber, ownerName);
            Ticket ticket = parkingLot.parkVehicle(vehicle);
            String slots = ticket.getSlotNumbers().toString();
            sendHtml(exchange, renderPage("Ticket " + ticket.getTicketNumber() + " created. Allocated slot(s): " + slots));
        } else {
            String ticketNumber = form.getOrDefault("ticketNumber", "").trim().toUpperCase();
            if (!ticketNumber.matches("T-[0-9]+")) {
                throw new IllegalArgumentException("Enter a ticket number such as T-1001.");
            }
            Ticket ticket = parkingLot.exitVehicle(ticketNumber);
            sendHtml(exchange, renderPage("Ticket " + ticket.getTicketNumber() + " completed. Charge: Rs. "
                    + String.format("%.2f", ticket.getParkingFee()) + " for "
                    + ticket.getBillableHours() + " billable hour(s)."));
        }
    }

    private Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        int contentLength = 0;
        try {
            contentLength = Integer.parseInt(exchange.getRequestHeaders().getFirst("Content-Length"));
        } catch (NumberFormatException | NullPointerException ignored) {
        }
        if (contentLength > 8192) {
            throw new IllegalArgumentException("Form submission is too large.");
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> fields = new HashMap<>();
        for (String pair : body.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            fields.put(
                    URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return fields;
    }

    private String renderPage(String message) throws SQLException, IOException {
        String template = Files.readString(Path.of("web", "index.html"), StandardCharsets.UTF_8);
        StringBuilder dashboard = new StringBuilder();
        appendSummary(dashboard);
        appendSlotMap(dashboard);
        appendParkingHistory(dashboard);
        appendPayments(dashboard);

        String messageHtml = message == null || message.isBlank()
                ? ""
                : "<div class=\"notice\" role=\"status\">" + escapeHtml(message) + "</div>";

        return template.replace("{{MESSAGE}}", messageHtml)
                .replace("{{DASHBOARD}}", dashboard.toString());
    }

    private void appendSummary(StringBuilder html) throws SQLException {
        String sql = "SELECT " +
                "COALESCE(SUM(is_occupied), 0) AS occupied_slots, " +
                "COUNT(DISTINCT CASE WHEN is_occupied = 1 THEN vehicle_id END) AS active_vehicles " +
                "FROM parking_slots";
        int occupied;
        int activeVehicles;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            occupied = resultSet.getInt("occupied_slots");
            activeVehicles = resultSet.getInt("active_vehicles");
        }

        int completed;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM parking_tickets WHERE ticket_status = 'COMPLETED'");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            completed = resultSet.getInt(1);
        }

        double revenue;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM payments");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            revenue = resultSet.getDouble(1);
        }

        html.append("<section class=\"stats-grid\">");
        appendStat(html, "Occupied slots", occupied + " / 20", "Currently in use", "blue");
        appendStat(html, "Available slots", String.valueOf(20 - occupied), "Ready for vehicles", "green");
        appendStat(html, "Vehicles parked", String.valueOf(activeVehicles), "Active tickets", "purple");
        appendStat(html, "Total revenue", "Rs. " + String.format("%.2f", revenue), completed + " completed sessions", "orange");
        html.append("</section>");
    }

    private void appendStat(StringBuilder html, String label, String value, String detail, String color) {
        html.append("<article class=\"stat-card ").append(color).append("\"><span>")
                .append(escapeHtml(label)).append("</span><strong>").append(escapeHtml(value))
                .append("</strong><small>").append(escapeHtml(detail)).append("</small></article>");
    }

    private void appendSlotMap(StringBuilder html) throws SQLException {
        html.append("<section class=\"card data-card\" id=\"slots\"><div class=\"section-title\"><div><p class=\"eyebrow\">LIVE OVERVIEW</p>")
                .append("<h2>Parking slots</h2><p>Truck allocations occupy two adjacent spaces.</p></div>")
                .append("<div class=\"legend\"><span><i class=\"available-dot\"></i>Available</span><span><i class=\"occupied-dot\"></i>Occupied</span></div></div>")
                .append("<div class=\"slot-grid\">");
        String sql = "SELECT p.slot_id, p.is_occupied, v.vehicle_number, v.vehicle_type " +
                "FROM parking_slots p LEFT JOIN vehicles v ON p.vehicle_id = v.vehicle_id ORDER BY p.slot_id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int slot = resultSet.getInt("slot_id");
                boolean occupied = resultSet.getBoolean("is_occupied");
                String vehicle = resultSet.getString("vehicle_number");
                String type = resultSet.getString("vehicle_type");
                html.append("<div class=\"slot ").append(occupied ? "occupied" : "available").append("\"><span class=\"slot-number\">")
                        .append(String.format("%02d", slot)).append("</span><strong>")
                        .append(occupied ? escapeHtml(vehicle == null ? "Occupied" : vehicle) : "Available")
                        .append("</strong><small>").append(occupied ? escapeHtml(type == null ? "Vehicle" : type) : "Open space")
                        .append("</small></div>");
            }
        }
        html.append("</div></section>");
    }

    private void appendParkingHistory(StringBuilder html) throws SQLException {
        html.append("<section class=\"card data-card\" id=\"history\"><div class=\"section-title\"><div><p class=\"eyebrow\">ACTIVITY</p><h2>Parking history</h2>")
                .append("<p>Recent and active parking tickets.</p></div></div><div class=\"table-wrap\"><table><thead><tr>")
                .append("<th>Ticket</th><th>Vehicle</th><th>Type</th><th>Entry</th><th>Exit</th><th>Fee</th><th>Status</th>")
                .append("</tr></thead><tbody>");
        String sql = "SELECT t.ticket_number, v.vehicle_number, v.vehicle_type, t.entry_time, t.exit_time, " +
                "t.parking_fee, t.ticket_status FROM parking_tickets t JOIN vehicles v " +
                "ON t.vehicle_id = v.vehicle_id ORDER BY t.ticket_id DESC LIMIT 50";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            boolean hasRows = false;
            while (resultSet.next()) {
                hasRows = true;
                html.append("<tr><td class=\"ticket-code\">").append(escapeHtml(resultSet.getString("ticket_number")))
                        .append("</td><td>").append(escapeHtml(resultSet.getString("vehicle_number")))
                        .append("</td><td>").append(escapeHtml(resultSet.getString("vehicle_type")))
                        .append("</td><td>").append(formatTimestamp(resultSet.getTimestamp("entry_time")))
                        .append("</td><td>").append(formatTimestamp(resultSet.getTimestamp("exit_time")))
                        .append("</td><td>Rs. ").append(String.format("%.2f", resultSet.getDouble("parking_fee")))
                        .append("</td><td><span class=\"status ").append("COMPLETED".equals(resultSet.getString("ticket_status")) ? "done" : "active")
                        .append("\">").append(escapeHtml(resultSet.getString("ticket_status"))).append("</span></td></tr>");
            }
            if (!hasRows) {
                html.append("<tr><td colspan=\"7\" class=\"empty\">No parking history yet.</td></tr>");
            }
        }
        html.append("</tbody></table></div></section>");
    }

    private void appendPayments(StringBuilder html) throws SQLException {
        html.append("<section class=\"card data-card\" id=\"payments\"><div class=\"section-title\"><div><p class=\"eyebrow\">COLLECTIONS</p><h2>Payment history</h2>")
                .append("<p>Payments are saved when a vehicle exits.</p></div></div><div class=\"table-wrap\"><table><thead><tr>")
                .append("<th>Payment ID</th><th>Ticket</th><th>Amount</th><th>Payment time</th></tr></thead><tbody>");
        String sql = "SELECT p.payment_id, t.ticket_number, p.amount, p.payment_time FROM payments p " +
                "JOIN parking_tickets t ON p.ticket_id = t.ticket_id ORDER BY p.payment_id DESC LIMIT 50";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            boolean hasRows = false;
            while (resultSet.next()) {
                hasRows = true;
                html.append("<tr><td>").append(resultSet.getInt("payment_id"))
                        .append("</td><td class=\"ticket-code\">").append(escapeHtml(resultSet.getString("ticket_number")))
                        .append("</td><td>Rs. ").append(String.format("%.2f", resultSet.getDouble("amount")))
                        .append("</td><td>").append(formatTimestamp(resultSet.getTimestamp("payment_time"))).append("</td></tr>");
            }
            if (!hasRows) {
                html.append("<tr><td colspan=\"4\" class=\"empty\">No payments recorded yet.</td></tr>");
            }
        }
        html.append("</tbody></table></div></section>");
    }

    private String formatTimestamp(java.sql.Timestamp timestamp) {
        return timestamp == null ? "—" : timestamp.toLocalDateTime().format(DATE_FORMAT);
    }

    private String renderErrorPage(String message) {
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>ParkEase error</title>"
                + "<link rel=\"stylesheet\" href=\"/styles.css\"></head><body><main class=\"page\">"
                + "<div class=\"notice error\">" + escapeHtml(message)
                + "</div><a class=\"button button-primary\" href=\"/\">Return to dashboard</a></main></body></html>";
    }

    private void serveStylesheet(HttpExchange exchange) throws IOException {
        Path stylesheet = Path.of("web", "styles.css");
        byte[] content = Files.readAllBytes(stylesheet);
        exchange.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(200, content.length);
        exchange.getResponseBody().write(content);
    }

    private void sendHtml(HttpExchange exchange, String html) throws IOException {
        sendHtml(exchange, html, 200);
    }

    private void sendHtml(HttpExchange exchange, String html, int statusCode) throws IOException {
        byte[] content = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, content.length);
        exchange.getResponseBody().write(content);
    }

    private boolean isValidVehicleNumber(String vehicleNumber) {
        if (vehicleNumber.length() < 8 || vehicleNumber.length() > 12) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < vehicleNumber.length(); i++) {
            char character = vehicleNumber.charAt(i);
            if (Character.isLetter(character)) {
                hasLetter = true;
            } else if (Character.isDigit(character)) {
                hasDigit = true;
            } else {
                return false;
            }
        }
        return hasLetter && hasDigit;
    }

    private Vehicle createVehicle(VehicleType type, String vehicleNumber, String ownerName) {
        return switch (type) {
            case BIKE -> new Bike(vehicleNumber, ownerName);
            case CAR -> new Car(vehicleNumber, ownerName);
            case TRUCK -> new Truck(vehicleNumber, ownerName);
        };
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
