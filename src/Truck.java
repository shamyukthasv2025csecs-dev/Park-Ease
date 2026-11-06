public class Truck extends Vehicle {
    public Truck(String vehicleNumber, String ownerName) {
        super(vehicleNumber, ownerName, VehicleType.TRUCK);
    }

    @Override
    public double calculateCharge(int hours) {
        int billable = Math.min(hours, 8);
        return billable * VehicleType.TRUCK.getHourlyRate();
    }
}
