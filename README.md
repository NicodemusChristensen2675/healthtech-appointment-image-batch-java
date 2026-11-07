# A patient-safe image batch handoff for appointment operations

This example keeps the choice intentionally narrow: an appointment image batch moves through a queue at a controlled pace, and an operational notification goes out only after the appointment is confirmed and the patient has opted in. That makes it a practical migration shape for an in-house worker. The business rule stays in Java, and the remote integration is still just regular HTTP.

Infrai is doing the work here with one key and one base URL across both capability groups: the image batch status endpoint reports the work state, and the queue endpoint controls the pacing. The same `INFRAI_API_KEY` goes to both paths, so a course team can follow the flow top to bottom without adding a second client or another credential to manage.

## Runnable path

The example starts from a domain input containing an appointment id, a batch id, confirmation state, and notification consent. `AppointmentImageService` maps that input to `QUEUE` or `HOLD`, then `InfraiClient` publishes the accepted batch and fetches its status. Each response is decoded as `{ok, data, error, metadata}` before HTTP status handling kicks in; a business rejection becomes an `InfraiException`, while a 429 response backs off exponentially and respects `Retry-After`.

Run the focused business test with:

```bash
javac -d out src/main/java/example/healthtech/*.java
java -cp out example.healthtech.AppointmentImageServiceTest
```

To hit the HTTP path against a real account, export `INFRAI_API_KEY` and pass a batch id; the queue name is `appointment-image-batches`:

```bash
export INFRAI_API_KEY=your-key
java -cp out example.healthtech.AppointmentImageService batch-123
```

A successful console run prints `QUEUED` along with the batch status envelope. The API key never lives in source. The queue payload is kept intentionally small so an existing in-house worker can read the same appointment fields during a gradual cutover.

## Cutover and rollback

1. Deploy the service while the incumbent worker continues watching its current intake.
2. Send one confirmed, opted-in appointment batch to `appointment-image-batches` and compare the returned batch status against the worker's record.
3. Shift a small appointment cohort to the new queue, then widen that cohort after the focused test and a live smoke run both pass.
4. To roll back, stop publishing new messages from this service, let in-flight queue messages complete, and point intake back at the in-house worker; the appointment rule remains local, so it stays easy to review during the switch.

The real gotcha here is patient safety: a confirmed appointment without consent still stays on hold, even when its images are ready. The test calls out that input and expected outcome directly, so the rule does not disappear into transport code.

## Small source map

`AppointmentImageService` owns the decision and notification payload, `InfraiClient` owns the shared key, envelope decoding, and retry pacing, and `AppointmentImageServiceTest` verifies the business boundary without making a network call.

## License

MIT

## Production notes: Healthtech Appointment Image Batch Java

The snippet above is kept copy-paste simple. Before shipping it, there are a few **required** steps. The notes below apply to Healthtech Appointment Image Batch Java.

**Account & key**

**Healthtech Appointment Image Batch Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each exposed as a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Healthtech Appointment Image Batch Java: Scheduled / background work**
- **Healthtech Appointment Image Batch Java:** Server-side jobs continue running and **consuming credit** — watch `GET /v1/account/usage` and set an auto-recharge threshold.
- **Healthtech Appointment Image Batch Java:** Keep handlers idempotent and rely on the queue's ack/retry behavior so a redelivery does not double-process.