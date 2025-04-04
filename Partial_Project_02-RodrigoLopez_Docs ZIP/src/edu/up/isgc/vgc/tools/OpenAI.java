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

/**
 * Utility class for interacting with OpenAI's API services including:
 * - Image generation (DALL-E)
 * - Image description (GPT-4 Vision)
 * - Text-to-speech (TTS)
 */
public class OpenAI {
    // API configuration constants
    private static final String API_KEY = API_KEY_HERE(); //ADD YOUR API KEY HERE
    public static final String OPENAI_URL = "https://api.openai.com/v1";
    private static final String IMG_GEN_URL = OPENAI_URL + "/images/generations";
    private static final String IMG_TXT_URL = OPENAI_URL + "/chat/completions";
    private static final String TXT_AUDIO_URL = OPENAI_URL + "/audio/speech";
    private static final String GEN_IMG_DIR = "Outputs/genimages/";

    /**
     * Generates postcard images using DALL-E API
     * @param prompt The text prompt for image generation
     * @param count Number of images to generate
     * @return Array of file paths to generated images
     */
    public static String[] generatePostcards(String prompt, int count) {
        try {
            // Prepare the JSON payload for DALL-E API
            JSONObject payload = new JSONObject()
                    .put("model", "dall-e-2")
                    .put("prompt", "Generate a postcard image of: " + prompt)
                    .put("n", count)
                    .put("size", "1024x1024")
                    .put("response_format", "b64_json");

            // Windows-safe curl command construction
            String[] command = {
                    "cmd.exe", "/c", // Start new shell
                    "curl",
                    "-X", "POST",
                    IMG_GEN_URL,
                    "-H", "\"Content-Type: application/json\"",
                    "-H", "\"Authorization: Bearer " + API_KEY + "\"",
                    "-d", "\"" + payload.toString().replace("\"", "\\\"") + "\"",
                    "--ssl-no-revoke", // Bypass Windows certificate check
                    "--fail-with-body" // Better error handling
            };

            // Debug: Print the exact command
            System.out.println("Executing: " + String.join(" ", command));

            // Execute the curl command
            Process process = Runtime.getRuntime().exec(command);
            String response = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Check for errors
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IOException("cURL failed (" + exitCode + "): " + error);
            }

            // Process the API response and save images
            return handleMultipleImages(response, "postcard");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return new String[0];
        }
    }

    /**
     * Processes API response containing multiple images and saves them to disk
     * @param response JSON response from OpenAI API
     * @param baseName Base name for saved image files
     * @return Array of paths to saved image files
     */
    private static String[] handleMultipleImages(String response, String baseName) {
        try {
            // Trim and validate response
            response = response.trim();
            if (response.isEmpty()) {
                throw new IOException("Empty response data");
            }

            // Parse JSON response
            JSONObject json = new JSONObject(response);

            // Check for API errors
            if (json.has("error")) {
                throw new IOException("API Error: " + json.getJSONObject("error").getString("message"));
            }

            // Process each image in the response
            JSONArray data = json.getJSONArray("data");
            String[] imagePaths = new String[data.length()];

            // Ensure output directory exists
            Path outputDir = Paths.get(GEN_IMG_DIR);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Save each image to disk
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

    /**
     * Generates a text description of an image using GPT-4 Vision
     * @param imageInput Either a URL or base64-encoded image data
     * @return Text description of the image
     */
    public static String describeImage(String imageInput) {
        try {
            // Set up HTTP connection to OpenAI API
            URL url = new URL(IMG_TXT_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);

            // Prepare image content based on input type (URL or base64)
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

            // Construct the message payload
            JSONObject message = new JSONObject()
                    .put("role", "user")
                    .put("content", new JSONArray()
                            .put(new JSONObject()
                                    .put("type", "text")
                                    .put("text", "Describe this image in detail"))
                            .put(imageContent)
                    );

            // Complete API request payload
            JSONObject payload = new JSONObject()
                    .put("model", "gpt-4o")
                    .put("messages", new JSONArray().put(message))
                    .put("max_tokens", 300);

            // Send the request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Check for errors
            if (conn.getResponseCode() != 200) {
                System.err.println("API Error: " + conn.getResponseMessage());
                return null;
            }

            // Read and parse the response
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

    /**
     * Converts text to speech using OpenAI's TTS API
     * @param text The text to convert to speech
     * @param outputName Base name for the output audio file
     * @return Path to the generated MP3 file
     */
    public static String generateAudio(String text, String outputName) {
        try {
            // Prepare the TTS API request payload
            JSONObject payload = new JSONObject()
                    .put("model", "tts-1")
                    .put("input", text)
                    .put("voice", "alloy")
                    .put("response_format", "mp3");

            // Use Java's HttpClient for the API request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TXT_AUDIO_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            // Send request and handle response
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

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

    /**
     * Determines the appropriate image size for DALL-E generation
     * @param width Desired image width
     * @param height Desired image height
     * @return String representing the closest supported size
     */
    private static String getValidSize(int width, int height) {
        if (width >= 1024 && height >= 1024) return "1024x1024";
        if (width > height) return "1792x1024";
        return "1024x1792";
    }

    /**
     * Processes a single image response from the API
     * @param response JSON response from OpenAI
     * @param name Base name for the output file
     * @return Path to the saved image file
     */
    private static String handleImageResponse(String response, String name) {
        try {
            // Parse the JSON response
            JSONObject json = new JSONObject(response);
            String base64Image = json.getJSONArray("data")
                    .getJSONObject(0)
                    .getString("b64_json");

            // Save the image and return its path
            String outputPath = GEN_IMG_DIR + name + ".png";
            saveBase64Image(base64Image, outputPath);
            return outputPath;
        } catch (Exception e) {
            System.err.println("Failed to process image response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves a base64-encoded image to disk
     * @param base64Image The image data in base64 format
     * @param outputPath Path where to save the image
     */
    private static void saveBase64Image(String base64Image, String outputPath) {
        try {
            // Decode and save the image
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            Path outputDir = Paths.get(GEN_IMG_DIR);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            try (OutputStream stream = new FileOutputStream(outputPath)) {
                stream.write(imageBytes);
            }

            // Verify the image was saved correctly
            BufferedImage image = ImageIO.read(new File(outputPath));
            if (image == null) {
                throw new IOException("Invalid image data received");
            }
        } catch (IOException e) {
            System.err.println("Image save failed: " + e.getMessage());
            new File(outputPath).delete();
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid Base64 data: " + e.getMessage());
        }
    }
}