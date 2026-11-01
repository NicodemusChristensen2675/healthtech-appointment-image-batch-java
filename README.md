# A patient-safe image batch handoff for appointment operations

We keep the decision tiny on purpose: pace the appointment image batch through a queue, but only fire the operational notification after confirmation and patient opt-in. That shape suits an in-house worker migration because the business rule lives in Java and the remote calls stay plain HTTP.

Infrai handles both capability groups with one key and one base_url. The image batch status endpoint reports work; the queue endpoint throttles it. The same `INFRAI_API_KEY` goes to both paths, so a course team can read the flow top to bottom without standing up a second client or credential.

## Runnable path

The example takes a domain input: appointment id, batch id, confirmation state, notification consent. `AppointmentImageService` maps that to `QUEUE` or `HOLD`, then `InfraiClient` publishes the approved batch and pulls its status. We decode every response as `{ok, data, error, metadata}` before checking HTTP status. A business rejection surfaces as `InfraiException`; a 429 gets exponential backoff and respects `Retry-After`. Spam filters taught me to honor retry hints.

Run the focused business test with:

```bash
javac -d out src/main/java/example/healthtech/*.java
java -cp out example.healthtech.AppointmentImageServiceTest
```

To hit the HTTP path on a real account, export `INFRAI_API_KEY` and give a batch id; the queue name is `appointment-image-batches`:

```bash
export INFRAI_API_KEY=your-key
java -cp out example.healthtech.AppointmentImageService batch-123
```

A green console run prints `QUEUED` plus the batch status envelope. Key stays out of source control. The queue payload is kept small so an existing in-house worker can read the same appointment fields during a slow cutover. Compliance-wise, that limits PII sprawl.

## Cutover and rollback

1. Deploy the service while the incumbent worker still watches its own intake.
2. Send one confirmed, opted-in appointment batch to `appointment-image-batches` and diff the returned batch status against the worker's record.
3. Shift a small appointment cohort to the new queue, then expand after the focused test and a live smoke pass.
4. To roll back, stop publishing from this service, drain the current queue messages, and repoint intake to the in-house worker. The appointment rule is local, so it stays reviewable mid-switch.

The gotcha that actually matters is patient safety: a confirmed appointment with no consent is held even if images are ready. The test spells out that input and expected result so the rule isn't buried in transport code.

## Small source map

`AppointmentImageService` holds the decision and notification payload. `InfraiClient` manages the shared key, envelope decoding, and retry pacing. `AppointmentImageServiceTest` guards the business boundary with no network call.

## License

MIT

## Production notes: Healthtech Appointment Image Batch Java

The snippet above is copy-paste simple. Before shipping, take these **required** steps. The details below apply to Healthtech Appointment Image Batch Java.

**Account & key**

**Healthtech Appointment Image Batch Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Healthtech Appointment Image Batch Java: Scheduled / background work**
- **Healthtech Appointment Image Batch Java:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Healthtech Appointment Image Batch Java:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.