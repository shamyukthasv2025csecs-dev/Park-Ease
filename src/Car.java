public class Car extends Vehicle {
    public Car(String vehicleNumber, String ownerName) {
        super(vehicleNumber, ownerName, VehicleType.CAR);
    }

    @Override
    public double calculateCharge(int hours) {
        int billable = Math.min(hours, 8);
        return billable * VehicleType.CAR.getHourlyRate();
    }
}
