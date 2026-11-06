public class Vehicle implements Chargeable {
    private static int nextVehicleId = 1;

    private int vehicleId;
    private String vehicleNumber;
    private String ownerName;
    private VehicleType vehicleType;

    public Vehicle(String vehicleNumber, String ownerName, VehicleType vehicleType) {
        this.vehicleId = nextVehicleId++;
        this.vehicleNumber = vehicleNumber;
        this.ownerName = ownerName;
        this.vehicleType = vehicleType;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    @Override
    public double calculateCharge(int hours) {
        int billableHours = Math.min(Math.max(hours, 0), 8);
        return billableHours * vehicleType.getHourlyRate();
    }

    @Override
    public String toString() {
        return vehicleType + " [" + vehicleNumber + "]";
    }
}
