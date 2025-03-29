package edu.up.isgc.vgc.tools;

import edu.up.isgc.vgc.tools.Pipeline;
import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import javax.imageio.ImageIO;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class OpenAI {
    private static final String API_KEY = "API KEY HERE";
    public static final String OPENAI_URL = "https://api.openai.com/v1";
    private static final String IMG_GEN_URL = OPENAI_URL + "/images/generations";
    private static final String IMG_TXT_URL = OPENAI_URL + "/chat/completions";
    private static final String TXT_AUDIO_URL = OPENAI_URL + "/audio/speech";
    private static final String GEN_IMG_DIR = "Outputs/genimages";

    public static String[] generateImage(String prompt, String name, int width, int height) {
        try {
            String size = width + "x" + height;

            // Request base64 response format
            JSONObject payload = new JSONObject()
                    .put("model", "dall-e-2")
                    .put("prompt", "Generate a postcard image of: " + prompt)
                    .put("n", 2)
                    .put("size", size)
                    .put("response_format", "b64_json");  // Changed from "url"

            List<Function<String[], String[]>> pipeline = List.of(
                    input -> new String[]{"curl", IMG_GEN_URL},
                    input -> new String[]{"-X", "POST"},
                    input -> new String[]{"-H", "Content-Type: application/json"},
                    input -> new String[]{"-H", "Authorization: Bearer " + API_KEY},
                    input -> new String[]{"-d", payload.toString()}
            );

            String[] command = Pipeline.biLambda(pipeline, CMD::concat);
            String response = CMD.expect(command);
            return handleImageResponse(response, name);
        } catch (Exception e) {
            System.err.println("Image generation failed: " + e.getMessage());
            return null;
        }
    }

    private static String[] handleImageResponse(String response, String name) {
        try {
            JSONObject json = new JSONObject(response);
            String base64Image01 = json.getJSONArray("data")
                    .getJSONObject(0)
                    .getString("b64_json");  // Changed from "url"
            String base64Image02 = json.getJSONArray("data")
                    .getJSONObject(1)
                    .getString("b64_json");  // Changed from "url"

            String outputPath01 = GEN_IMG_DIR + name + "01.png";
            String outputPath02 = GEN_IMG_DIR + name + "02.png";
            saveBase64Image(base64Image01, outputPath01);
            saveBase64Image(base64Image02, outputPath02);
            return new String[] {outputPath01, outputPath02};
        } catch (Exception e) {
            System.err.println("Failed to process image response: " + e.getMessage());
            return null;
        }
    }

    private static void saveBase64Image(String base64Image, String outputPath) {
        try {
            // Decode Base64 string
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // Create output directory if needed
            Path outputDir = Paths.get(GEN_IMG_DIR);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Write bytes to file
            try (OutputStream stream = new FileOutputStream(outputPath)) {
                stream.write(imageBytes);
            }

            // Verify image integrity
            BufferedImage image = ImageIO.read(new File(outputPath));
            if (image == null) {
                throw new IOException("Invalid image data received");
            }
        } catch (IOException e) {
            System.err.println("Image save failed: " + e.getMessage());
            // Clean up corrupted file
            new File(outputPath).delete();
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid Base64 data: " + e.getMessage());
        }
    }

    private static void downloadImage(String imageUrl, String outputPath) {
        try {
            // Create HTTP client with modern settings
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS) // Handle redirects
                    .connectTimeout(Duration.ofSeconds(10)) // Timeout settings
                    .build();

            // Create request with proper headers
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .header("User-Agent", "Mozilla/5.0") // Some servers require this
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            // Send request and handle response
            HttpResponse<InputStream> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            // Check HTTP status
            if (response.statusCode() != 200) {
                throw new IOException("HTTP error: " + response.statusCode());
            }

            // Create output directory if needed
            Path outputDir = Paths.get(GEN_IMG_DIR);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Read and save image
            try (InputStream inputStream = response.body()) {
                BufferedImage image = ImageIO.read(inputStream);
                if (image == null) {
                    throw new IOException("Failed to decode image from stream");
                }

                Path outputPathObj = Paths.get(outputPath);
                if (!ImageIO.write(image, "png", outputPathObj.toFile())) {
                    throw new IOException("No PNG writer available");
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Image download failed: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // Preserve interrupt status
            }
        }
    }
}