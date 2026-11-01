package example.healthtech;

public final class AppointmentImageServiceTest {
    public static void main(String[] args) {
        AppointmentImageService service = new AppointmentImageService();
        check(service.decide(new AppointmentImage("a-1", "b-1", true, true)) == AppointmentImageService.Decision.QUEUE,
            "confirmed and consented input queues the batch");
        check(service.decide(new AppointmentImage("a-2", "b-2", true, false)) == AppointmentImageService.Decision.HOLD,
            "confirmed input without consent is held");
        System.out.println("patient-safe decision test passed");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
