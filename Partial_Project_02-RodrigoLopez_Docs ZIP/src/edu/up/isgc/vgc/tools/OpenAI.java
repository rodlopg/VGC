package edu.up.isgc.vgc.tools;

import edu.up.isgc.vgc.tools.Pipeline;
import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.imageio.ImageIO;

public class OpenAI {
    private static final String API_KEY = "API KEY HERE";
    private static final String IMG_GEN_URL = "https://api.openai.com/v1/images/generations";
    private static final String IMG_TXT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String TXT_AUDIO_URL = "https://api.openai.com/v1/audio/speech";
    private static final String GEN_IMG_DIR = "src/up/edu/isgc/genimages/";

    public static String generateImage(String prompt, String name, int width, int height) {
        try {
            String size = getValidSize(width, height);
            if (size == null) return null;

            JSONObject payload = new JSONObject()
                    .put("model", "dall-e-2")
                    .put("prompt", "Generate a postcard image of: " + prompt)
                    .put("n", 1)
                    .put("size", size)
                    .put("response_format", "url");

            List<Function<String[], String[]>> pipeline = new ArrayList<>();
            pipeline.add(input -> new String[]{"-X", "POST"});
            pipeline.add(input -> new String[]{"-H", "Content-Type: application/json"});
            pipeline.add(input -> new String[]{"-H", "Authorization: Bearer " + API_KEY});
            pipeline.add(input -> new String[]{"-d", payload.toString()});

            String[] command = Pipeline.biLambda(pipeline, CMD::concat);
            command = CMD.concat(new String[]{"curl", IMG_GEN_URL}, command);

            String response = CMD.expect(command);
            return handleImageResponse(response, name);
        } catch (Exception e) {
            System.err.println("Image generation failed: " + e.getMessage());
            return null;
        }
    }

    public static String describeImage(String imageUrl) {
        try {
            URL url = new URL(IMG_TXT_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);

            JSONObject message = new JSONObject()
                    .put("role", "user")
                    .put("content", new JSONArray()
                            .put(new JSONObject().put("type", "text").put("text", "Describe this image in detail"))
                            .put(new JSONObject().put("type", "image_url")
                                    .put("image_url", new JSONObject().put("url", imageUrl)))
                    );

            JSONObject payload = new JSONObject()
                    .put("model", "gpt-4o")
                    .put("messages", new JSONArray().put(message))
                    .put("max_tokens", 300);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() != 200) {
                System.err.println("API Error: " + conn.getResponseMessage());
                return null;
            }

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

    public static String generateAudio(String text, String outputName) {
        try {
            JSONObject payload = new JSONObject()
                    .put("model", "tts-1")
                    .put("input", text)
                    .put("voice", "alloy")
                    .put("response_format", "mp3");

            List<Function<String[], String[]>> pipeline = new ArrayList<>();
            pipeline.add(input -> new String[]{"-X", "POST"});
            pipeline.add(input -> new String[]{"-H", "Content-Type: application/json"});
            pipeline.add(input -> new String[]{"-H", "Authorization: Bearer " + API_KEY});
            pipeline.add(input -> new String[]{"-d", payload.toString()});
            pipeline.add(input -> new String[]{"-o", GEN_IMG_DIR + outputName + ".mp3"});

            String[] command = Pipeline.biLambda(pipeline, CMD::concat);
            command = CMD.concat(new String[]{"curl", TXT_AUDIO_URL}, command);

            CMD.expect(command);
            return GEN_IMG_DIR + outputName + ".mp3";
        } catch (Exception e) {
            System.err.println("Audio generation failed: " + e.getMessage());
            return null;
        }
    }

    private static String getValidSize(int width, int height) {
        if (width >= 1024 && height >= 1024) return "1024x1024";
        if (width > height) return "1792x1024";
        return "1024x1792";
    }

    private static String handleImageResponse(String response, String name) {
        try {
            JSONObject json = new JSONObject(response);
            String imageUrl = json.getJSONArray("data")
                    .getJSONObject(0)
                    .getString("url");

            String outputPath = GEN_IMG_DIR + name + ".png";
            downloadImage(imageUrl, outputPath);
            return outputPath;
        } catch (Exception e) {
            System.err.println("Failed to process image response: " + e.getMessage());
            return null;
        }
    }

    private static void downloadImage(String imageUrl, String outputPath) {
        try {
            BufferedImage image = ImageIO.read(new URL(imageUrl));
            File outputFile = new File(outputPath);
            ImageIO.write(image, "png", outputFile);
        } catch (IOException e) {
            System.err.println("Image download failed: " + e.getMessage());
        }
    }
}