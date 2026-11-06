import java.util.Arrays;

public class ParkingLotAllocationTest {
    public static void main(String[] args) {
        boolean[] available = new boolean[ParkingLot.CAPACITY];
        Arrays.fill(available, true);

        assertSlots(new int[] {1}, ParkingLot.findAvailableSlot(available, VehicleType.BIKE), "bike starts at slot 1");
        assertSlots(new int[] {1}, ParkingLot.findAvailableSlot(available, VehicleType.CAR), "car starts at slot 1");
        assertSlots(new int[] {1, 2}, ParkingLot.findAvailableSlot(available, VehicleType.TRUCK), "truck uses slots 1 and 2");

        available[0] = false;
        assertSlots(new int[] {2}, ParkingLot.findAvailableSlot(available, VehicleType.CAR), "car uses the next free slot");
        assertSlots(new int[] {2, 3}, ParkingLot.findAvailableSlot(available, VehicleType.TRUCK), "truck uses the first free consecutive pair");

        available[1] = false;
        assertSlots(new int[] {3, 4}, ParkingLot.findAvailableSlot(available, VehicleType.TRUCK), "truck skips a blocked pair");

        Arrays.fill(available, false);
        if (ParkingLot.findAvailableSlot(available, VehicleType.BIKE) != null) {
            throw new AssertionError("A vehicle must not be allocated when all slots are occupied.");
        }

        System.out.println("Parking slot allocation tests passed.");
    }

    private static void assertSlots(int[] expected, int[] actual, String description) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(description + ": expected " + Arrays.toString(expected)
                    + " but got " + Arrays.toString(actual));
        }
    }
}
