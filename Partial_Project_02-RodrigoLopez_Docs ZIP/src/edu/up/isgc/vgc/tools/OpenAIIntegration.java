package edu.up.isgc.vgc.tools;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.tools.CMD;
import org.json.JSONObject;

/**
 * Handles integration with the OpenAI API for generating postcards and text-to-speech narration.
 */
public class OpenAIIntegration extends Component {
    private static final String API_KEY = "your_openai_api_key";
    private static final String IMAGE_GENERATION_URL = "https://api.openai.com/v1/images/generations";
    private static final String TEXT_TO_SPEECH_URL = "https://api.openai.com/v1/audio/speech";

    public OpenAIIntegration(int width, int height, String date, Double duration, String type, String path) {
        super(width, height, date, duration, type, path);
    }

    @Override
    public void printAttributes() {
        System.out.println("|Path: " + getPath());
        System.out.println("||Resolution: " + getWidth() + ":" + getHeight());
        System.out.println("|||Date: " + getDate());
        System.out.println("||||Duration: " + getDuration());
        System.out.println("|||||Type: " + getType());
    }

    // Modified method to handle JSON response and return image URL
    public String generatePostcard(String mood) {
        String prompt = createImagePrompt(mood);
        String jsonPayload = String.format("{\"prompt\": \"%s\", \"n\": 1, \"size\": \"1024x1024\", \"response_format\": \"url\"}",
                prompt.replace("\"", "\\\""));

        String[] command = {
                "curl", "-X", "POST", IMAGE_GENERATION_URL,
                "-H", "Authorization: Bearer " + API_KEY,
                "-H", "Content-Type: application/json",
                "-d", jsonPayload
        };

        String response = CMD.expect(command);
        JSONObject jsonResponse = new JSONObject(response);
        return jsonResponse.getJSONArray("data").getJSONObject(0).getString("url");
    }

    // New method to generate narration and save audio file
    public String generateNarration(String imageDescription) {
        String jsonPayload = String.format("{\"model\": \"tts-1\", \"input\": \"%s\", \"voice\": \"alloy\", \"response_format\": \"mp3\"}",
                imageDescription.replace("\"", "\\\""));

        String outputPath = "narration_" + System.currentTimeMillis() + ".mp3";
        String[] command = {
                "curl", "-X", "POST", TEXT_TO_SPEECH_URL,
                "-H", "Authorization: Bearer " + API_KEY,
                "-H", "Content-Type: application/json",
                "-d", jsonPayload,
                "-o", outputPath
        };

        CMD.expect(command);
        return outputPath;
    }

    private String createImagePrompt(String mood) {
        return "A beautiful travel postcard showing a destination that evokes " + mood +
                " mood. Include landmarks, natural scenery, and cultural elements. " +
                "Use vibrant colors and professional photography style.";
    }

    @Override
    public String returnIFormat() { return "AIImage"; }


}