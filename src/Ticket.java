import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Ticket {
    private int ticketId;
    private int vehicleId;
    private String ticketNumber;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private int billableHours;
    private double parkingFee;
    private String ticketStatus;
    private List<Integer> slotNumbers = new ArrayList<>();

    public Ticket() {
    }

    public Ticket(int ticketId, int vehicleId, String ticketNumber, LocalDateTime entryTime,
                  LocalDateTime exitTime, int billableHours, double parkingFee, String ticketStatus) {
        this.ticketId = ticketId;
        this.vehicleId = vehicleId;
        this.ticketNumber = ticketNumber;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.billableHours = billableHours;
        this.parkingFee = parkingFee;
        this.ticketStatus = ticketStatus;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public int getBillableHours() {
        return billableHours;
    }

    public void setBillableHours(int billableHours) {
        this.billableHours = billableHours;
    }

    public double getParkingFee() {
        return parkingFee;
    }

    public void setParkingFee(double parkingFee) {
        this.parkingFee = parkingFee;
    }

    public String getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(String ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public List<Integer> getSlotNumbers() {
        return slotNumbers;
    }

    public void setSlotNumbers(List<Integer> slotNumbers) {
        this.slotNumbers = slotNumbers;
    }

    public void addSlotNumber(int slotNumber) {
        this.slotNumbers.add(slotNumber);
    }
}
