public enum VehicleType {
    BIKE(10, 1),
    CAR(30, 1),
    TRUCK(60, 2);

    private final int hourlyRate;
    private final int slotsNeeded;

    VehicleType(int hourlyRate, int slotsNeeded) {
        this.hourlyRate = hourlyRate;
        this.slotsNeeded = slotsNeeded;
    }

    public int getHourlyRate() {
        return hourlyRate;
    }

    public int getSlotsNeeded() {
        return slotsNeeded;
    }
}
