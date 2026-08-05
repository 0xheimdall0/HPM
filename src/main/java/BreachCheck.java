import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class BreachCheck {
    // Check how many times a password appears in the HIBP database
    static int timesPwned(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) sb.append(String.format("%02X", b));
        String hash = sb.toString();

        String prefix = hash.substring(0, 5);
        String suffix = hash.substring(5);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.pwnedpasswords.com/range/" + prefix))
                .timeout(java.time.Duration.ofSeconds(10))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        for (String line : response.body().split("\n")) {
            String[] parts = line.trim().split(":");
            if (parts[0].equalsIgnoreCase(suffix)) {
                return Integer.parseInt(parts[1]);
            }
        }
        return 0;
    }
}
