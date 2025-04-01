package edu.up.isgc.vgc.tools;

import edu.up.isgc.vgc.tools.Pipeline;
import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static env.Environment.API_KEY_HERE;

public class OpenAI {
    // API key loaded from environment
    private static final String API_KEY = API_KEY_HERE();

    // OpenAI API base URL and endpoints for image generation, text-to-speech, and more
    public static final String OPENAI_URL = "https://api.openai.com/v1";
    private static final String IMG_GEN_URL = OPENAI_URL + "/images/generations";
    private static final String IMG_TXT_URL = OPENAI_URL + "/chat/completions";
    private static final String TXT_AUDIO_URL = OPENAI_URL + "/audio/speech";
    private static final String GEN_IMG_DIR = "Outputs/genimages/";

    // Method to generate postcards using OpenAI's DALL-E 2 model
    public static String[] generatePostcards(String prompt, int count) {
        try {
            // Construct the JSON payload for the API request
            JSONObject payload = new JSONObject()
                    .put("model", "dall-e-2")
                    .put("prompt", "Generate a postcard image of: " + prompt)
                    .put("n", count)
                    .put("size", "1024x1024")
                    .put("response_format", "b64_json");

            // Command to call OpenAI's API using curl (Windows safe)
            String[] command = {
                    "cmd.exe", "/c", // Start a new shell
                    "curl",
                    "-X", "POST", // HTTP POST request
                    IMG_GEN_URL,
                    "-H", "\"Content-Type: application/json\"",
                    "-H", "\"Authorization: Bearer " + API_KEY + "\"",
                    "-d", "\"" + payload.toString().replace("\"", "\\\"") + "\"",
                    "--ssl-no-revoke", // Bypass Windows SSL certificate check
                    "--fail-with-body" // Include error body in case of failure
            };

            // Debug: Print the exact curl command being executed
            System.out.println("Executing: " + String.join(" ", command));

            // Execute the curl command and read the response
            Process process = Runtime.getRuntime().exec(command);
            String response = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Wait for the process to finish and check for errors
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IOException("cURL failed (" + exitCode + "): " + error);
            }

            // Handle multiple generated images and return their paths
            return handleMultipleImages(response, "postcard");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return new String[0];
        }
    }

    // Method to process and save multiple images from the API response
    private static String[] handleMultipleImages(String response, String baseName) {
        try {
            // Trim and validate response
            response = response.trim();
            if (response.isEmpty()) {
                throw new IOException("Empty response data");
            }

            // Parse the JSON response from OpenAI
            JSONObject json = new JSONObject(response);

            // Check for errors in the API response
            if (json.has("error")) {
                throw new IOException("API Error: " + json.getJSONObject("error").getString("message"));
            }

            // Extract image data and save images to files
            JSONArray data = json.getJSONArray("data");
            String[] imagePaths = new String[data.length()];

            Path outputDir = Paths.get(GEN_IMG_DIR);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Save each image and store the file paths
            for (int i = 0; i < data.length(); i++) {
                String base64Image = data.getJSONObject(i).getString("b64_json");
                String fileName = baseName + "_" + System.currentTimeMillis() + "_" + (i+1) + ".png";
                String fullPath = GEN_IMG_DIR + fileName;

                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                try (OutputStream stream = new FileOutputStream(fullPath)) {
                    stream.write(imageBytes);
                }
                imagePaths[i] = fullPath;
            }
            return imagePaths;

        } catch (Exception e) {
            System.err.println("Image processing failed: " + e.getMessage());
            return new String[0];
        }
    }

    // Method to describe an image using OpenAI's GPT model
    public static String describeImage(String imageInput) {
        try {
            // Set up HTTP connection to the OpenAI API for image description
            URL url = new URL(IMG_TXT_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);

            // Prepare the image input as either URL or Base64 data
            JSONObject imageContent = new JSONObject();
            if (imageInput.startsWith("http://") || imageInput.startsWith("https://")) {
                imageContent.put("type", "image_url")
                        .put("image_url", new JSONObject().put("url", imageInput));
            } else {
                String base64Data = imageInput.replaceFirst("^data:image/\\w+;base64,", "");
                imageContent.put("type", "image_url")
                        .put("image_url", new JSONObject()
                                .put("url", "data:image/png;base64," + base64Data));
            }

            // Set up the user message to request a description from GPT-4o
            JSONObject message = new JSONObject()
                    .put("role", "user")
                    .put("content", new JSONArray()
                            .put(new JSONObject()
                                    .put("type", "text")
                                    .put("text", "Describe this image in detail"))
                            .put(imageContent)
                    );

            // Construct the final payload for the API request
            JSONObject payload = new JSONObject()
                    .put("model", "gpt-4o")
                    .put("messages", new JSONArray().put(message))
                    .put("max_tokens", 300);

            // Send the API request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Check the response status
            if (conn.getResponseCode() != 200) {
                System.err.println("API Error: " + conn.getResponseMessage());
                return null;
            }

            // Read and parse the response to extract the description
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return new JSONObject(response.toString())
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }
        } catch (Exception e) {
            System.err.println("Image description failed: " + e.getMessage());
            return null;
        }
    }

    // Method to generate audio from text using OpenAI's text-to-speech model
    public static String generateAudio(String text, String outputName) {
        try {
            // Prepare the payload for text-to-speech API request
            JSONObject payload = new JSONObject()
                    .put("model", "tts-1")
                    .put("input", text)
                    .put("voice", "alloy")
                    .put("response_format", "mp3");

            // Use Java HttpClient to send the request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TXT_AUDIO_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            // Send the request and receive the response
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // If successful, save the audio file
            if (response.statusCode() == 200) {
                String outputPath = GEN_IMG_DIR + outputName + ".mp3";
                try (OutputStream out = new FileOutputStream(outputPath)) {
                    response.body().transferTo(out);
                }
                return outputPath;
            }
            return null;
        } catch (Exception e) {
            System.err.println("Audio generation failed: " + e.getMessage());
            return null;
        }
    }

    // Helper method to determine the valid image size based on dimensions
    private static String getValidSize(int width, int height) {
        if (width >= 1024 && height >= 1024) return "1024x1024";
        if (width > height) return "1792x1024";
        return "1024x1792";
    }

    // Helper method for handling multiple images based on URLs
    public static String[] processImages(List<String> imageUrls) {
        return imageUrls.stream().map(imageUrl -> {
            try {
                return generatePostcards(imageUrl, 3)[0]; // Take the first generated postcard image
            } catch (Exception e) {
                return "error";
            }
        }).toArray(String[]::new);
    }

    // Helper method to fetch specific image from its URL and validate output file
    public static String fetchImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            BufferedImage img = ImageIO.read(url);
            if (img == null) {
                return "Error downloading image";
            }
            return "Download successful!";
        } catch (IOException e) {
            return "Error downloading image: " + e.getMessage();
        }
    }
}
