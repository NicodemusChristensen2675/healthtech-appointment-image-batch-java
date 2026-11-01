package example.healthtech;

public final class AppointmentImageService {
    public enum Decision { QUEUE, HOLD }

    public Decision decide(AppointmentImage appointment) {
        return appointment.confirmed() && appointment.consented() ? Decision.QUEUE : Decision.HOLD;
    }

    public String notificationPayload(AppointmentImage appointment) {
        if (decide(appointment) != Decision.QUEUE) {
            throw new IllegalStateException("appointment is not ready for a patient-safe notification");
        }
        return "{\"appointment_id\":\"" + clean(appointment.appointmentId())
            + "\",\"batch_id\":\"" + clean(appointment.batchId())
            + "\",\"event\":\"image_batch_ready\"}";
    }

    private String clean(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static void main(String[] args) throws Exception {
        String batchId = args.length == 0 ? "batch-demo" : args[0];
        AppointmentImage appointment = new AppointmentImage("appointment-demo", batchId, true, true);
        AppointmentImageService service = new AppointmentImageService();
        if (service.decide(appointment) != Decision.QUEUE) {
            System.out.println("HOLD");
            return;
        }
        InfraiClient client = new InfraiClient(System.getenv("INFRAI_API_KEY"));
        String published = client.publish("appointment-image-batches", service.notificationPayload(appointment));
        String status = client.batchStatus(batchId);
        System.out.println("QUEUED " + published);
        System.out.println(status);
    }
}
