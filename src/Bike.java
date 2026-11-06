public class Bike extends Vehicle {
    public Bike(String vehicleNumber, String ownerName) {
        super(vehicleNumber, ownerName, VehicleType.BIKE);
    }

    @Override
    public double calculateCharge(int hours) {
        int billable = Math.min(hours, 8);
        return billable * VehicleType.BIKE.getHourlyRate();
    }
}
