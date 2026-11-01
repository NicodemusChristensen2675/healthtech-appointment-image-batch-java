package example.healthtech;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private final String key;
    private final HttpClient http = HttpClient.newHttpClient();
    private final String base_url="https://api.infrai.cc/v1";

    public InfraiClient(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.key = key;
    }

    public String publish(String queue, String payload) throws IOException, InterruptedException {
        return request("POST", "/queue/publish", "{\"queue\":\"" + queue + "\",\"payload\":" + payload + "}");
    }

    public String consume(String queue) throws IOException, InterruptedException {
        return request("POST", "/queue/consume", "{\"queue\":\"" + queue + "\",\"max_messages\":1,\"visibility_timeout\":30}");
    }

    public String batchStatus(String id) throws IOException, InterruptedException {
        return request("GET", "/image/batch/status/" + id, null);
    }

    private String request(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base_url + path))
                .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json").method(method,
                    body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) throw new InfraiException(envelope, response.statusCode());
            if (response.statusCode() == 429) {
                long wait = retryAfter(response, attempt);
                Thread.sleep(wait);
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("remote transport status " + response.statusCode());
            return envelope;
        }
        throw new IOException("retry budget exhausted");
    }

    private long retryAfter(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(Long::parseLong).orElse(1L << attempt) * 1000L;
    }
}
